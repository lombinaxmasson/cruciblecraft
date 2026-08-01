package com.masson.cruciblecraft.content.blockentity;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.menu.CokeOvenMenu;
import com.masson.cruciblecraft.content.multiblock.CokeOvenStructure;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.machine.component.RecipeProcessor;
import com.masson.cruciblecraft.machine.processing.AdjacentEnergyConsumer;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeCache;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class CokeOvenBlockEntity extends BlockEntity implements MenuProvider {
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int TANK_CAPACITY = 32_000;
    /** The coke oven consumes a minimal steady 1 HU for each processing tick. */
    public static final long PROCESS_HEAT_PER_TICK = 1L;
    private static final int IGNITION_WINDOW_TICKS = 40;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markCapabilityMutation();
        }
    };
    private final FluidTank tank = new FluidTank(
            TANK_CAPACITY,
            stack -> stack.is(ModFluids.CREOSOTE_SOURCE.get())) {
        @Override
        protected void onContentsChanged() {
            markCapabilityMutation();
        }
    };
    private final IItemHandler externalItems = new ExternalItemHandler();
    private final IFluidHandler externalFluids = new ExtractOnlyFluidHandler();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> processor.progress();
                case 1 -> processor.duration();
                case 2 -> tank.getFluidAmount();
                case 3 -> tank.getCapacity();
                case 4 -> structureValid ? 1 : 0;
                case 5 -> heated ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> processor.setProgress(value);
                case 1 -> processor.setDuration(value);
                case 4 -> structureValid = value != 0;
                default -> {
                }
            }
        }

        @Override
        public int getCount() {
            return 6;
        }
    };

    private final RecipeProcessor processor = new RecipeProcessor();
    private final GTRecipeCache recipeCache = new GTRecipeCache(ModRecipeMaps.COKE_OVEN);
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private int ignitionTicks;
    private boolean structureValid;
    private boolean heated;
    private MachineTransaction pendingTransaction;

    public CokeOvenBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COKE_OVEN.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CokeOvenBlockEntity cokeOven) {
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (CheckpointDecisions.onPositionPhase(level.getGameTime(), phaseKey, 20)) {
            boolean previousStructure = cokeOven.structureValid;
            cokeOven.structureValid = CokeOvenStructure.isValid(
                    level,
                    pos,
                    state.getValue(CokeOvenBlock.FACING));
            if (previousStructure != cokeOven.structureValid) {
                cokeOven.setChanged();
                cokeOven.syncToClient();
                cokeOven.checkpoint.checkpointed();
                cokeOven.checkpoint.synced();
            }
        }

        cokeOven.heated = cokeOven.queryHeated();
        if (!cokeOven.structureValid || !cokeOven.heated) {
            cokeOven.ignitionTicks = 0;
            cokeOven.setLit(false);
            return;
        }
        if (cokeOven.ignitionTicks > 0) {
            cokeOven.ignitionTicks--;
        }

        Optional<RecipeMap.Match> match = cokeOven.findRecipe();
        if (match.isEmpty()) {
            cokeOven.resetProgress();
            cokeOven.setLit(false);
            return;
        }
        GTRecipe recipe = match.get().recipe();
        if (!cokeOven.selectRecipe(match.get())) {
            cokeOven.setLit(false);
            return;
        }
        if (cokeOven.ignitionTicks <= 0) {
            cokeOven.setLit(false);
            return;
        }
        cokeOven.ignitionTicks = IGNITION_WINDOW_TICKS;
        if (!cokeOven.canStoreOutputs(recipe)) {
            cokeOven.setLit(false);
            return;
        }
        if (!cokeOven.consumeProcessingHeat()) {
            cokeOven.setLit(false);
            return;
        }

        cokeOven.processor.advance();
        cokeOven.checkpoint.markDirty();
        cokeOven.setLit(true);
        if (cokeOven.processor.complete()) {
            cokeOven.complete(recipe);
        }
        if (cokeOven.checkpoint.shouldCheckpoint(level.getGameTime(), phaseKey, 20)) {
            cokeOven.setChanged();
            cokeOven.syncToClient();
            cokeOven.checkpoint.checkpointed();
            cokeOven.checkpoint.synced();
        }
    }

    public boolean ignite() {
        if (level == null || level.isClientSide || !structureValid) {
            return false;
        }
        if (!heated) {
            heated = queryHeated();
        }
        if (!heated) {
            return false;
        }
        ignitionTicks = IGNITION_WINDOW_TICKS;
        setChanged();
        syncToClient();
        checkpoint.checkpointed();
        checkpoint.synced();
        return true;
    }

    private Optional<RecipeMap.Match> findRecipe() {
        if (level == null || inventory.getStackInSlot(INPUT_SLOT).isEmpty()) {
            return Optional.empty();
        }
        return recipeCache.findItems(inventory.getStackInSlot(INPUT_SLOT));
    }

    private boolean selectRecipe(
            RecipeMap.Match match) {
        GTRecipe recipe = match.recipe();
        ItemStack output = recipe.itemOutputs().stream().findFirst().orElse(ItemStack.EMPTY);
        if (output.isEmpty()) {
            return false;
        }
        String identity = match.id().toString();
        if (processor.select(identity, recipe.duration())) {
            setChanged();
        }
        return true;
    }

    private boolean canStoreOutputs(GTRecipe recipe) {
        if (recipe.itemOutputs().size() != 1
                || recipe.fluidOutputs().size() != 1
                || recipe.outputChances().getFirst() != GTRecipe.GUARANTEED_CHANCE) {
            return false;
        }
        pendingTransaction = MachineTransaction.prepare(
                recipe,
                List.of(
                        inventory.getStackInSlot(INPUT_SLOT),
                        inventory.getStackInSlot(OUTPUT_SLOT)),
                List.of(INPUT_SLOT),
                List.of(OUTPUT_SLOT),
                List.of(tank.getFluid()),
                List.of(),
                List.of(new ProcessingMachineSpec.TankSpec(0, tank.getCapacity())),
                recipe.itemOutputs()).orElse(null);
        return pendingTransaction != null;
    }

    private void complete(GTRecipe recipe) {
        if (pendingTransaction == null || !pendingTransaction.commit(transactionResources())) {
            return;
        }
        pendingTransaction = null;
        Optional<RecipeMap.Match> next = findRecipe();
        processor.clearActive(next.map(match -> match.recipe().duration()).orElse(0));
        setChanged();
        syncToClient();
        checkpoint.checkpointed();
        checkpoint.synced();
    }

    private void resetProgress() {
        if (processor.reset()) {
            setChanged();
        }
    }

    private boolean queryHeated() {
        var source = heatSource();
        return transferProcessingHeat(source, true);
    }

    private boolean consumeProcessingHeat() {
        var source = heatSource();
        return transferProcessingHeat(source, false);
    }

    private boolean transferProcessingHeat(IEnergyHandler source, boolean simulate) {
        return AdjacentEnergyConsumer.consume(
                source,
                EnergyType.HEAT,
                Direction.UP,
                PROCESS_HEAT_PER_TICK,
                simulate);
    }

    private MachineTransaction.ResourceAccess transactionResources() {
        return new MachineTransaction.ResourceAccess() {
            @Override public int itemCount() { return inventory.getSlots(); }
            @Override public ItemStack item(int slot) {
                return inventory.getStackInSlot(slot).copy();
            }
            @Override public void setItem(int slot, ItemStack stack) {
                inventory.setStackInSlot(slot, stack);
            }
            @Override public int fluidCount() { return 1; }
            @Override public FluidStack fluid(int index) {
                return index == 0 ? tank.getFluid().copy() : FluidStack.EMPTY;
            }
            @Override public void setFluid(int index, FluidStack stack) {
                if (index != 0) {
                    throw new IndexOutOfBoundsException(index);
                }
                tank.setFluid(stack);
            }
        };
    }

    private IEnergyHandler heatSource() {
        if (level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(CokeOvenBlock.FACING);
        BlockPos heatSource = CokeOvenStructure.heatSource(worldPosition, facing);
        if (!level.hasChunkAt(heatSource)) {
            return null;
        }
        return level.getCapability(
                ModCapabilities.ENERGY,
                heatSource,
                Direction.UP);
    }

    private void setLit(boolean lit) {
        if (level == null || getBlockState().getValue(CokeOvenBlock.LIT) == lit) {
            return;
        }
        level.setBlock(
                worldPosition,
                getBlockState().setValue(CokeOvenBlock.LIT, lit),
                Block.UPDATE_CLIENTS);
    }

    public boolean structureValid() {
        return structureValid;
    }

    public int progress() {
        return processor.progress();
    }

    public int recipeDuration() {
        return processor.duration();
    }

    public int creosoteAmount() {
        return tank.getFluidAmount();
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public IItemHandler externalItems() {
        return externalItems;
    }

    public IFluidHandler externalFluids() {
        return externalFluids;
    }

    public ContainerData data() {
        return data;
    }

    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }

    public void dropContents() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        stack);
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.cruciblecraft.coke_oven");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId,
            Inventory playerInventory,
            Player player) {
        return new CokeOvenMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        tank.readFromNBT(registries, tag.getCompound("tank"));
        ResourceLocation savedRecipe = tag.contains("active_recipe")
                ? ResourceLocation.tryParse(tag.getString("active_recipe"))
                : null;
        processor.restore(
                savedRecipe == null ? "" : savedRecipe.toString(),
                tag.getInt("progress"),
                tag.getInt("recipe_duration"));
        ignitionTicks = Math.max(0, tag.getInt("ignition_ticks"));
        structureValid = tag.getBoolean("structure_valid");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", processor.progress());
        tag.putInt("recipe_duration", processor.duration());
        tag.putInt("ignition_ticks", ignitionTicks);
        tag.putBoolean("structure_valid", structureValid);
        if (!processor.activeId().isEmpty()) {
            tag.putString("active_recipe", processor.activeId());
        }
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void markCapabilityMutation() {
        setChanged();
        checkpoint.markDirty();
    }

    private final class ExternalItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            return 2;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return slot == INPUT_SLOT
                    ? inventory.insertItem(INPUT_SLOT, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return slot == OUTPUT_SLOT
                    ? inventory.extractItem(OUTPUT_SLOT, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT && inventory.isItemValid(INPUT_SLOT, stack);
        }
    }

    private final class ExtractOnlyFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return tank.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tankIndex) {
            return tank.getFluidInTank(tankIndex);
        }

        @Override
        public int getTankCapacity(int tankIndex) {
            return tank.getTankCapacity(tankIndex);
        }

        @Override
        public boolean isFluidValid(int tankIndex, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return tank.drain(resource, action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return tank.drain(maxDrain, action);
        }
    }
}
