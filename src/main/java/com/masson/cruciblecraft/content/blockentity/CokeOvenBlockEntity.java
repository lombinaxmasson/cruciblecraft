package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.IntStream;

import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockPortAggregator;
import com.masson.cruciblecraft.content.multiblock.MultiblockBuilderRecheckable;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureValidator;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.tileentity.ProgressHost;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.machine.component.RecipeProcessor;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ParallelRecipeOperations;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineAutoIo;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeCache;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
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
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GT6 {@code MultiTileEntityCokeOven}: 3×3×3 firebrick shell, ambient TU,
 * igniter start, bounded TU input, consume-at-start, parallel 16, fluid
 * auto-out under the oven.
 */
public final class CokeOvenBlockEntity extends BlockEntity
        implements MenuProvider, ProgressHost, MultiblockBuilderRecheckable,
        IEnergyHandler {
    public static final ResourceLocation STRUCTURE_ID =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "coke_oven");
    public static final int INPUT_SLOT = 0;
    public static final int OUTPUT_SLOT = 1;
    public static final int OUTPUT_SLOT_COUNT = 9;
    public static final int SLOT_COUNT = 1 + OUTPUT_SLOT_COUNT;
    public static final List<Integer> OUTPUT_SLOTS =
            IntStream.range(OUTPUT_SLOT, SLOT_COUNT).boxed().toList();
    public static final int TANK_CAPACITY =
            CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY;
    public static final int PARALLEL = 16;
    public static final int IGNITION_WINDOW_TICKS = 40;
    /** GT6 {@code NBT_INPUT}; one ambient TU is added each formed tick. */
    public static final long PROCESS_TU_PER_TICK = 1L;
    public static final long TU_CAPACITY = 16L;
    public static final long TU_PACKET_MAX = 16L;

    private final ItemStackHandler inventory = new ItemStackHandler(SLOT_COUNT) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT;
        }

        @Override
        protected void onContentsChanged(int slot) {
            markCapabilityMutation();
        }

        @Override
        public void deserializeNBT(HolderLookup.Provider provider, CompoundTag nbt) {
            CompoundTag sized = nbt.copy();
            sized.putInt("Size", SLOT_COUNT);
            super.deserializeNBT(provider, sized);
        }
    };
    private final FluidTank tank = new FluidTank(TANK_CAPACITY) {
        @Override
        protected void onContentsChanged() {
            markCapabilityMutation();
        }
    };
    private final IItemHandler insertItems = new AutomationItemHandler(true, false);
    private final IItemHandler extractItems = new AutomationItemHandler(false, true);
    private final IItemHandler combinedItems = new AutomationItemHandler(true, true);
    private final IFluidHandler extractFluids = new ExtractOnlyFluidHandler();
    private final ContainerData data = new ContainerData() {
        @Override
        public int get(int index) {
            return switch (index) {
                case 0 -> processor.progress();
                case 1 -> processor.duration();
                case 2 -> tank.getFluidAmount();
                case 3 -> tank.getCapacity();
                case 4 -> structureValid ? 1 : 0;
                case 5 -> ignitionTicks > 0 ? 1 : 0;
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
    private final MachineEnergyBuffer energy =
            new MachineEnergyBuffer(TU_CAPACITY, TU_PACKET_MAX);
    private final GTRecipeCache recipeCache = new GTRecipeCache(ModRecipeMaps.COKE_OVEN);
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private int ignitionTicks;
    private boolean structureValid;
    private boolean structureChecked;
    private int operations;
    private final List<ItemStack> pendingOutputs = new ArrayList<>();
    private FluidStack pendingFluid = FluidStack.EMPTY;
    private Set<BlockPos> boundPorts = Set.of();

    public CokeOvenBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.COKE_OVEN.get(), pos, blockState);
    }

    public static BlockPos structureCenter(BlockPos controller, Direction facing) {
        return controller.relative(facing.getOpposite());
    }

    /**
     * GT6 {@code isInsideStructure}: the 3×3×3 AABB around the hollow center.
     */
    public static boolean isInsideStructure(
            BlockPos controller,
            Direction facing,
            BlockPos pos) {
        BlockPos center = structureCenter(controller, facing);
        return pos.getX() >= center.getX() - 1
                && pos.getX() <= center.getX() + 1
                && pos.getY() >= center.getY() - 1
                && pos.getY() <= center.getY() + 1
                && pos.getZ() >= center.getZ() - 1
                && pos.getZ() <= center.getZ() + 1;
    }

    /**
     * GT6 {@code getFluidOutputTarget}: 3×3 tanks directly under the bottom
     * firebrick layer, i.e. two blocks below the hollow center.
     */
    public static List<BlockPos> fluidDrainPositions(
            BlockPos controller,
            Direction facing) {
        BlockPos origin = structureCenter(controller, facing).below(2);
        List<BlockPos> positions = new ArrayList<>(9);
        for (int x = -1; x <= 1; x++) {
            for (int z = -1; z <= 1; z++) {
                positions.add(origin.offset(x, 0, z));
            }
        }
        return positions;
    }

    public static int parallelOperations(int available, int perOperation) {
        if (available <= 0 || perOperation <= 0) {
            return 0;
        }
        return Math.min(PARALLEL, available / perOperation);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CokeOvenBlockEntity cokeOven) {
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (!cokeOven.structureChecked
                || CheckpointDecisions.onPositionPhase(level.getGameTime(), phaseKey, 20)) {
            cokeOven.refreshStructure(state);
        }

        if (cokeOven.ignitionTicks > 0) {
            cokeOven.ignitionTicks--;
        }
        cokeOven.autoOutputFluids();
        if (!cokeOven.structureValid) {
            cokeOven.setLit(false);
            return;
        }

        cokeOven.energy.insert(PROCESS_TU_PER_TICK, 1L, false);
        if (cokeOven.hasActiveProcess()) {
            cokeOven.advanceWithAvailableTu();
            cokeOven.checkpoint.markDirty();
            cokeOven.setLit(true);
            if (cokeOven.processor.complete() && cokeOven.emitPending()) {
                cokeOven.finishCycle();
            }
        } else if (!cokeOven.startRecipe()) {
            cokeOven.setLit(false);
        } else {
            cokeOven.advanceWithAvailableTu();
            cokeOven.checkpoint.markDirty();
            cokeOven.setLit(true);
        }
        if (cokeOven.checkpoint.shouldCheckpoint(level.getGameTime(), phaseKey, 20)) {
            cokeOven.setChanged();
            cokeOven.syncToClient();
            cokeOven.checkpoint.checkpointed();
            cokeOven.checkpoint.synced();
        }
    }

    public ResourceLocation structureId() {
        return STRUCTURE_ID;
    }

    @Override
    public void requestBuilderRecheck() {
        if (level != null && !level.isClientSide) {
            refreshStructure(getBlockState());
        }
    }

    public boolean isInsideStructure(BlockPos pos) {
        return isInsideStructure(
                worldPosition,
                getBlockState().getValue(CokeOvenBlock.FACING),
                pos);
    }

    public boolean ignite() {
        if (level == null || level.isClientSide) {
            return false;
        }
        ignitionTicks = IGNITION_WINDOW_TICKS;
        setChanged();
        syncToClient();
        checkpoint.checkpointed();
        checkpoint.synced();
        return true;
    }

    public void resetBySoftHammer() {
        processor.reset();
        operations = 0;
        pendingOutputs.clear();
        pendingFluid = FluidStack.EMPTY;
        setLit(false);
        setChanged();
        syncToClient();
    }

    public boolean trashWithPlunger() {
        if (tank.getFluidAmount() <= 0) {
            return false;
        }
        tank.drain(1_000, IFluidHandler.FluidAction.EXECUTE);
        return true;
    }

    public void clearBindings() {
        if (level != null && !level.isClientSide) {
            MultiblockPortAggregator.unbindLoaded(
                    level, worldPosition, boundPorts);
        }
        boundPorts = Set.of();
    }

    private void refreshStructure(BlockState state) {
        structureChecked = true;
        boolean previous = structureValid;
        var definition = MultiblockStructureCatalog.find(STRUCTURE_ID);
        if (definition.isEmpty() || level == null) {
            clearBindings();
            structureValid = false;
        } else {
            Direction facing = state.getValue(CokeOvenBlock.FACING);
            MultiblockStructureValidator.ValidationResult validation =
                    MultiblockStructureValidator.validate(
                            definition.get(),
                            level,
                            worldPosition,
                            facing);
            if (validation.status()
                    == MultiblockStructureValidator.Status.UNLOADED) {
                return;
            }
            boundPorts = claimFirebricks(validation, boundPorts);
            structureValid = validation.valid() && bricksBoundToUs(validation);
        }
        if (previous != structureValid) {
            setChanged();
            syncToClient();
            checkpoint.checkpointed();
            checkpoint.synced();
        }
    }

    /**
     * GT6 {@code checkAndSetTarget} still claims every present firebrick even
     * when the hollow check fails, so a broken oven keeps its wall.
     */
    private Set<BlockPos> claimFirebricks(
            MultiblockStructureValidator.ValidationResult validation,
            Set<BlockPos> previous) {
        LinkedHashSet<BlockPos> desired = new LinkedHashSet<>();
        validation.ports().forEach(
                matched -> desired.add(matched.position().immutable()));
        MultiblockPortAggregator.unbindLoaded(
                level,
                worldPosition,
                previous.stream()
                        .filter(position -> !desired.contains(position))
                        .collect(java.util.stream.Collectors.toSet()));
        LinkedHashSet<BlockPos> bound = new LinkedHashSet<>();
        for (var matched : validation.ports()) {
            BlockPos position = matched.position();
            if (!level.hasChunkAt(position)) {
                continue;
            }
            if (level.getBlockEntity(position)
                            instanceof FirebrickBlockEntity brick
                    && brick.canBind(worldPosition)) {
                brick.bind(worldPosition, STRUCTURE_ID);
                if (brick.controllerPosition()
                        .filter(worldPosition::equals)
                        .isPresent()) {
                    bound.add(position.immutable());
                }
            }
        }
        return Set.copyOf(bound);
    }

    private boolean bricksBoundToUs(
            MultiblockStructureValidator.ValidationResult validation) {
        if (level == null || validation.ports().isEmpty()) {
            return false;
        }
        for (var port : validation.ports()) {
            if (!(level.getBlockEntity(port.position())
                            instanceof FirebrickBlockEntity brick)
                    || brick.controllerPosition()
                            .filter(worldPosition::equals)
                            .isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasActiveProcess() {
        return !processor.activeId().isEmpty()
                && (processor.progress() > 0
                        || !pendingOutputs.isEmpty()
                        || !pendingFluid.isEmpty());
    }

    private Optional<RecipeMap.Match> findRecipe() {
        if (level == null || inventory.getStackInSlot(INPUT_SLOT).isEmpty()) {
            return Optional.empty();
        }
        return recipeCache.findItems(inventory.getStackInSlot(INPUT_SLOT));
    }

    private boolean startRecipe() {
        Optional<RecipeMap.Match> match = findRecipe();
        if (match.isEmpty()) {
            resetProgress();
            return false;
        }
        if (energy.stored() < PROCESS_TU_PER_TICK) {
            return false;
        }
        if (ignitionTicks <= 0 && processor.progress() <= 0) {
            return false;
        }
        GTRecipe recipe = match.get().recipe();
        if (recipe.itemOutputs().isEmpty()
                || recipe.itemInputCounts().isEmpty()
                || recipe.fluidOutputs().size() > 1) {
            return false;
        }
        int nextOperations = parallelOperations(
                inventory.getStackInSlot(INPUT_SLOT).getCount(),
                recipe.itemInputCounts().getFirst());
        if (nextOperations < 1) {
            return false;
        }
        GTRecipe scaled = ParallelRecipeOperations.scale(recipe, nextOperations);
        List<ItemStack> rolled = ParallelRecipeOperations.maximumItemOutputs(
                recipe, nextOperations);
        if (!outputsFit(scaled, rolled)) {
            return false;
        }
        int consume = scaled.itemInputCounts().getFirst();
        ItemStack taken = inventory.extractItem(INPUT_SLOT, consume, false);
        if (taken.getCount() != consume) {
            if (!taken.isEmpty()) {
                inventory.insertItem(INPUT_SLOT, taken, false);
            }
            return false;
        }
        processor.select(match.get().id().toString(), recipe.duration());
        operations = nextOperations;
        pendingOutputs.clear();
        for (ItemStack stack : rolled) {
            pendingOutputs.add(stack.copy());
        }
        pendingFluid = scaled.fluidOutputs().isEmpty()
                ? FluidStack.EMPTY
                : scaled.fluidOutputs().getFirst().copy();
        setChanged();
        return true;
    }

    private void advanceWithAvailableTu() {
        int work = (int) Math.min(TU_PACKET_MAX, energy.stored());
        if (work <= 0) {
            return;
        }
        energy.consume(work);
        processor.advance(work);
    }

    private boolean outputsFit(GTRecipe scaled, List<ItemStack> rolled) {
        return MachineTransaction.prepare(
                scaled,
                inventorySnapshot(),
                List.of(INPUT_SLOT),
                OUTPUT_SLOTS,
                List.of(tank.getFluid()),
                List.of(),
                List.of(new ProcessingMachineSpec.TankSpec(0, tank.getCapacity())),
                rolled).isPresent();
    }

    private List<ItemStack> inventorySnapshot() {
        List<ItemStack> items = new ArrayList<>(SLOT_COUNT);
        for (int slot = 0; slot < SLOT_COUNT; slot++) {
            items.add(inventory.getStackInSlot(slot));
        }
        return items;
    }

    boolean emitPending() {
        boolean remaining = false;
        for (int index = 0; index < pendingOutputs.size(); index++) {
            ItemStack stack = pendingOutputs.get(index);
            if (stack.isEmpty()) {
                continue;
            }
            ItemStack leftover = insertOutput(stack);
            pendingOutputs.set(index, leftover);
            if (!leftover.isEmpty()) {
                remaining = true;
            }
        }
        pendingOutputs.removeIf(ItemStack::isEmpty);
        if (!pendingFluid.isEmpty()) {
            int filled = tank.fill(pendingFluid, IFluidHandler.FluidAction.EXECUTE);
            pendingFluid.shrink(filled);
            if (!pendingFluid.isEmpty()) {
                remaining = true;
            }
        }
        return !remaining;
    }

    private ItemStack insertOutput(ItemStack offered) {
        ItemStack remaining = offered.copy();
        for (int slot : OUTPUT_SLOTS) {
            if (remaining.isEmpty()) {
                break;
            }
            ItemStack current = inventory.getStackInSlot(slot);
            if (current.isEmpty()) {
                int moved = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                inventory.setStackInSlot(slot, remaining.copyWithCount(moved));
                remaining.shrink(moved);
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(current, remaining)) {
                continue;
            }
            int moved = Math.min(
                    remaining.getCount(),
                    current.getMaxStackSize() - current.getCount());
            if (moved > 0) {
                current.grow(moved);
                remaining.shrink(moved);
                inventory.setStackInSlot(slot, current);
            }
        }
        return remaining;
    }

    private void finishCycle() {
        ignitionTicks = IGNITION_WINDOW_TICKS;
        operations = 0;
        pendingOutputs.clear();
        pendingFluid = FluidStack.EMPTY;
        Optional<RecipeMap.Match> next = findRecipe();
        processor.clearActive(next.map(match -> match.recipe().duration()).orElse(0));
        setChanged();
        syncToClient();
        checkpoint.checkpointed();
        checkpoint.synced();
    }

    private void resetProgress() {
        if (processor.reset()
                || !pendingOutputs.isEmpty()
                || !pendingFluid.isEmpty()
                || operations != 0) {
            pendingOutputs.clear();
            pendingFluid = FluidStack.EMPTY;
            operations = 0;
            setChanged();
        }
    }

    private void autoOutputFluids() {
        if (level == null || tank.getFluidAmount() <= 0) {
            return;
        }
        Direction facing = getBlockState().getValue(CokeOvenBlock.FACING);
        for (BlockPos drain : fluidDrainPositions(worldPosition, facing)) {
            if (!level.hasChunkAt(drain)) {
                continue;
            }
            IFluidHandler target = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    drain,
                    Direction.UP);
            if (target == null) {
                continue;
            }
            ProcessingMachineAutoIo.moveFluids(tank, target);
            if (tank.getFluidAmount() <= 0) {
                return;
            }
        }
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

    @Override
    public long progressValue(Direction side) {
        return Math.max(0L, progress());
    }

    @Override
    public long progressMax(Direction side) {
        return Math.max(0L, recipeDuration());
    }

    public int creosoteAmount() {
        return tank.getFluidAmount();
    }

    public FluidStack tankFluid() {
        return tank.getFluid();
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public IItemHandler externalItems() {
        return combinedItems;
    }

    public IItemHandler automationItems(Direction side) {
        if (side == null) {
            return combinedItems;
        }
        if (side == Direction.UP) {
            return insertItems;
        }
        if (side == Direction.DOWN) {
            return extractItems;
        }
        return null;
    }

    public IFluidHandler externalFluids() {
        return extractFluids;
    }

    public IFluidHandler automationFluids(Direction side) {
        if (side == Direction.UP) {
            return null;
        }
        return extractFluids;
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
        for (ItemStack stack : pendingOutputs) {
            if (!stack.isEmpty()) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        stack);
            }
        }
        pendingOutputs.clear();
        pendingFluid = FluidStack.EMPTY;
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
        return new com.masson.cruciblecraft.content.menu.CokeOvenMenu(
                containerId, playerInventory, this);
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
        operations = Math.max(0, tag.getInt("operations"));
        energy.restore(tag.getLong("tu_energy"));
        pendingOutputs.clear();
        if (tag.contains("pending_outputs", Tag.TAG_LIST)) {
            ListTag list = tag.getList("pending_outputs", Tag.TAG_COMPOUND);
            for (int index = 0; index < list.size(); index++) {
                ItemStack stack = ItemStack.parseOptional(
                        registries, list.getCompound(index));
                if (!stack.isEmpty()) {
                    pendingOutputs.add(stack);
                }
            }
        }
        pendingFluid = tag.contains("pending_fluid")
                ? FluidStack.parseOptional(
                        registries, tag.getCompound("pending_fluid"))
                : FluidStack.EMPTY;
        boundPorts = Set.of();
        if (tag.contains("bound_ports")) {
            LinkedHashSet<BlockPos> loaded = new LinkedHashSet<>();
            for (long packed : tag.getLongArray("bound_ports")) {
                loaded.add(BlockPos.of(packed));
            }
            boundPorts = Set.copyOf(loaded);
        }
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
        tag.putInt("operations", operations);
        tag.putLong("tu_energy", energy.stored());
        if (!processor.activeId().isEmpty()) {
            tag.putString("active_recipe", processor.activeId());
        }
        ListTag outputs = new ListTag();
        for (ItemStack stack : pendingOutputs) {
            if (!stack.isEmpty()) {
                outputs.add(stack.save(registries));
            }
        }
        tag.put("pending_outputs", outputs);
        if (!pendingFluid.isEmpty()) {
            tag.put("pending_fluid", pendingFluid.save(registries));
        }
        tag.putLongArray(
                "bound_ports",
                boundPorts.stream().mapToLong(BlockPos::asLong).toArray());
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

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return structureValid
                && side != null
                && type == EnergyType.TIME;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)) {
            return 0L;
        }
        long accepted = energy.insert(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            markCapabilityMutation();
        }
        return accepted;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.TIME ? energy.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.TIME ? energy.capacity() : 0L;
    }

    private final class AutomationItemHandler implements IItemHandler {
        private final boolean insert;
        private final boolean extract;

        private AutomationItemHandler(boolean insert, boolean extract) {
            this.insert = insert;
            this.extract = extract;
        }

        @Override
        public int getSlots() {
            return inventory.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return inventory.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return insert && slot == INPUT_SLOT
                    ? inventory.insertItem(INPUT_SLOT, stack, simulate)
                    : stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            return extract && slot != INPUT_SLOT
                    ? inventory.extractItem(slot, amount, simulate)
                    : ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return inventory.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return insert
                    && slot == INPUT_SLOT
                    && inventory.isItemValid(INPUT_SLOT, stack);
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
