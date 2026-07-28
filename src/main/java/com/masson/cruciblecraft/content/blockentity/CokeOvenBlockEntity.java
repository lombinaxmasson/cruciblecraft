package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

import com.masson.cruciblecraft.api.heat.IHeatSource;
import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.menu.CokeOvenMenu;
import com.masson.cruciblecraft.content.multiblock.CokeOvenStructure;
import com.masson.cruciblecraft.recipe.CokeOvenRecipe;
import com.masson.cruciblecraft.recipe.CokeOvenRecipeInput;
import com.masson.cruciblecraft.recipe.CokeOvenProcess;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModRecipes;

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
import net.minecraft.world.item.crafting.RecipeHolder;
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
    public static final double PROCESS_HEAT_PER_TICK = 1.0;
    private static final int IGNITION_WINDOW_TICKS = 40;

    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT;
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private final FluidTank tank = new FluidTank(
            TANK_CAPACITY,
            stack -> stack.is(ModFluids.CREOSOTE_SOURCE.get())) {
        @Override
        protected void onContentsChanged() {
            setChanged();
        }
    };
    private final IItemHandler externalItems = new ExternalItemHandler();
    private final IFluidHandler externalFluids = new ExtractOnlyFluidHandler();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> progress;
                case 1 -> recipeDuration;
                case 2 -> tank.getFluidAmount();
                case 3 -> tank.getCapacity();
                case 4 -> structureValid ? 1 : 0;
                case 5 -> isHeated() ? 1 : 0;
                default -> 0;
            };
        }

        @Override
        public void set(int index, int value) {
            switch (index) {
                case 0 -> progress = value;
                case 1 -> recipeDuration = value;
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

    private int progress;
    private int recipeDuration;
    private int ignitionTicks;
    private boolean structureValid;
    private ResourceLocation activeRecipeId;

    public CokeOvenBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COKE_OVEN.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CokeOvenBlockEntity cokeOven) {
        if (level.getGameTime() % 20L == 0L) {
            boolean previousStructure = cokeOven.structureValid;
            cokeOven.structureValid = CokeOvenStructure.isValid(
                    level,
                    pos,
                    state.getValue(CokeOvenBlock.FACING));
            if (previousStructure != cokeOven.structureValid) {
                cokeOven.setChanged();
                cokeOven.syncToClient();
            }
        }

        boolean heated = cokeOven.isHeated();
        if (!cokeOven.structureValid || !heated) {
            cokeOven.ignitionTicks = 0;
            cokeOven.setLit(false);
            return;
        }
        if (cokeOven.ignitionTicks > 0) {
            cokeOven.ignitionTicks--;
        }

        Optional<RecipeHolder<CokeOvenRecipe>> recipe = cokeOven.findRecipe();
        if (recipe.isEmpty()) {
            cokeOven.resetProgress();
            cokeOven.setLit(false);
            return;
        }
        if (!cokeOven.selectRecipe(recipe.get())) {
            cokeOven.setLit(false);
            return;
        }
        if (cokeOven.ignitionTicks <= 0) {
            cokeOven.setLit(false);
            return;
        }
        cokeOven.ignitionTicks = IGNITION_WINDOW_TICKS;
        if (!cokeOven.canStoreOutputs(recipe.get().value())) {
            cokeOven.setLit(false);
            return;
        }
        if (!cokeOven.consumeProcessingHeat()) {
            cokeOven.setLit(false);
            return;
        }

        cokeOven.progress = CokeOvenProcess.advance(
                cokeOven.progress,
                cokeOven.recipeDuration);
        cokeOven.setLit(true);
        if (cokeOven.progress >= cokeOven.recipeDuration) {
            cokeOven.complete(recipe.get().value());
        }
        if (cokeOven.progress % 20 == 0) {
            cokeOven.setChanged();
            cokeOven.syncToClient();
        }
    }

    public boolean ignite() {
        if (level == null || level.isClientSide || !structureValid || !isHeated()) {
            return false;
        }
        ignitionTicks = IGNITION_WINDOW_TICKS;
        setChanged();
        syncToClient();
        return true;
    }

    private Optional<RecipeHolder<CokeOvenRecipe>> findRecipe() {
        if (level == null || inventory.getStackInSlot(INPUT_SLOT).isEmpty()) {
            return Optional.empty();
        }
        return level.getRecipeManager().getRecipeFor(
                ModRecipes.COKE_OVEN_TYPE.get(),
                new CokeOvenRecipeInput(inventory.getStackInSlot(INPUT_SLOT)),
                level);
    }

    private boolean selectRecipe(RecipeHolder<CokeOvenRecipe> holder) {
        if (holder.id().equals(activeRecipeId)) {
            return true;
        }
        activeRecipeId = holder.id();
        progress = 0;
        recipeDuration = holder.value().duration();
        setChanged();
        return true;
    }

    private boolean canStoreOutputs(CokeOvenRecipe recipe) {
        ItemStack currentOutput = inventory.getStackInSlot(OUTPUT_SLOT);
        ItemStack recipeOutput = recipe.output();
        boolean compatible = currentOutput.isEmpty()
                || ItemStack.isSameItemSameComponents(currentOutput, recipeOutput);
        if (!CokeOvenProcess.hasItemCapacity(
                currentOutput.getCount(),
                currentOutput.isEmpty()
                        ? recipeOutput.getMaxStackSize()
                        : currentOutput.getMaxStackSize(),
                recipeOutput.getCount(),
                compatible)) {
            return false;
        }
        return CokeOvenProcess.hasFluidCapacity(
                tank.getFluidAmount(),
                tank.getCapacity(),
                recipe.fluidOutput().getAmount())
                && tank.fill(recipe.fluidOutput(), IFluidHandler.FluidAction.SIMULATE)
                == recipe.fluidOutput().getAmount();
    }

    private void complete(CokeOvenRecipe recipe) {
        inventory.extractItem(INPUT_SLOT, 1, false);
        ItemStack currentOutput = inventory.getStackInSlot(OUTPUT_SLOT);
        if (currentOutput.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, recipe.output().copy());
        } else {
            currentOutput.grow(recipe.output().getCount());
            inventory.setStackInSlot(OUTPUT_SLOT, currentOutput);
        }
        tank.fill(recipe.fluidOutput(), IFluidHandler.FluidAction.EXECUTE);
        progress = 0;
        activeRecipeId = null;
        Optional<RecipeHolder<CokeOvenRecipe>> next = findRecipe();
        recipeDuration = next.map(holder -> holder.value().duration()).orElse(0);
        setChanged();
        syncToClient();
    }

    private void resetProgress() {
        if (progress != 0 || recipeDuration != 0 || activeRecipeId != null) {
            progress = 0;
            recipeDuration = 0;
            activeRecipeId = null;
            setChanged();
        }
    }

    private boolean isHeated() {
        var source = heatSource();
        return source != null
                && source.extractHeat(PROCESS_HEAT_PER_TICK, true) >= PROCESS_HEAT_PER_TICK;
    }

    private boolean consumeProcessingHeat() {
        var source = heatSource();
        return source != null
                && source.extractHeat(PROCESS_HEAT_PER_TICK, false) >= PROCESS_HEAT_PER_TICK;
    }

    private IHeatSource heatSource() {
        if (level == null) {
            return null;
        }
        Direction facing = getBlockState().getValue(CokeOvenBlock.FACING);
        BlockPos heatSource = CokeOvenStructure.heatSource(worldPosition, facing);
        return level.getCapability(
                ModCapabilities.HEAT_SOURCE,
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
        return progress;
    }

    public int recipeDuration() {
        return recipeDuration;
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
        progress = Math.max(0, tag.getInt("progress"));
        recipeDuration = Math.max(0, tag.getInt("recipe_duration"));
        ignitionTicks = Math.max(0, tag.getInt("ignition_ticks"));
        structureValid = tag.getBoolean("structure_valid");
        activeRecipeId = tag.contains("active_recipe")
                ? ResourceLocation.tryParse(tag.getString("active_recipe"))
                : null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
        tag.put("tank", tank.writeToNBT(registries, new CompoundTag()));
        tag.putInt("progress", progress);
        tag.putInt("recipe_duration", recipeDuration);
        tag.putInt("ignition_ticks", ignitionTicks);
        tag.putBoolean("structure_valid", structureValid);
        if (activeRecipeId != null) {
            tag.putString("active_recipe", activeRecipeId.toString());
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
