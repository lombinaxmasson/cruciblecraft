package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BooleanSupplier;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.heat.HotIngotProcessing;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.machine.processing.AdjacentEnergyConsumer;
import com.masson.cruciblecraft.machine.processing.ChanceOutputs;
import com.masson.cruciblecraft.machine.processing.ChanceOutputState;
import com.masson.cruciblecraft.machine.processing.GTRecipeFingerprint;
import com.masson.cruciblecraft.machine.processing.LayoutAwareItemStackHandler;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineDisplayData;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineState;
import com.masson.cruciblecraft.machine.processing.ProcessingRuntime;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;
import com.masson.cruciblecraft.machine.processing.SidedItemHandler;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeCache;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Shared server-authoritative processing host. Machine-specific classes supply
 * only a spec, facing, optional fluid filters, and adjacent energy source.
 */
public abstract class ProcessingMachineBlockEntity extends BlockEntity
        implements IEnergyHandler, MachineTransaction.ResourceAccess {
    private static final int CHECKPOINT_INTERVAL = 20;

    private final ProcessingMachineSpec spec;
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private final ProcessingRuntime runtime = new ProcessingRuntime();
    private final LayoutAwareItemStackHandler inventory;
    private final List<FluidTank> tanks;
    private final GTRecipeCache recipeCache;
    private final MachineEnergyBuffer energy;
    private final Map<ProcessingMachineSpec.CapabilityAccess, IItemHandler> itemViews =
            new EnumMap<>(ProcessingMachineSpec.CapabilityAccess.class);
    private final Map<ProcessingMachineSpec.CapabilityAccess, IFluidHandler> fluidViews =
            new EnumMap<>(ProcessingMachineSpec.CapabilityAccess.class);
    private IFluidHandler maintenanceFluidView;

    private RecipeMap.Match selectedMatch;
    private List<ItemStack> rolledOutputs = List.of();
    private String restoredSelectedId = "";
    private boolean restoredRollValid;
    private String restoredRecipeFingerprint = "";
    private long selectedMapRevision = -1L;
    private long materialRevision = -1L;
    private long resourceRevision;
    private long powerDemand;
    private Optional<Integer> unsupportedProcessingVersion = Optional.empty();
    private Optional<Integer> unsupportedInventorySlots = Optional.empty();
    private boolean quarantineWarningLogged;

    protected ProcessingMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            ProcessingMachineSpec spec) {
        super(type, pos, state);
        this.spec = spec;
        this.recipeCache = new GTRecipeCache(spec.requireRecipeMap());
        this.energy = spec.energy().mode() == ProcessingMachineSpec.EnergyMode.BUFFERED
                ? new MachineEnergyBuffer(spec.energy().capacity(), spec.energy().maxPacket())
                : null;
        this.inventory = new LayoutAwareItemStackHandler(
                spec.items().slotCount(),
                spec.items()::accepts,
                slot -> {
                    resourcesChanged();
                    onItemSlotChanged(slot);
                });
        List<FluidTank> created = new ArrayList<>();
        for (ProcessingMachineSpec.TankSpec tank : spec.fluids().all()) {
            int index = tank.index();
            created.add(new FluidTank(
                    tank.capacity(), stack -> isFluidInputValid(index, stack)) {
                @Override protected void onContentsChanged() {
                    resourcesChanged();
                }
            });
        }
        this.tanks = List.copyOf(created);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!quarantineWarningLogged
                && level != null
                && !level.isClientSide
                && (unsupportedProcessingVersion.isPresent()
                        || unsupportedInventorySlots.isPresent())) {
            quarantineWarningLogged = true;
            if (unsupportedProcessingVersion.isPresent()) {
                CrucibleCraft.LOGGER.warn(
                        "Quarantined processing machine {} at {} {}: unsupported version {}",
                        spec.id(),
                        level.dimension().location(),
                        worldPosition,
                        unsupportedProcessingVersion.orElseThrow());
            } else {
                CrucibleCraft.LOGGER.warn(
                        "Quarantined processing machine {} at {} {}: saved inventory has "
                                + "{} slots but the live spec has {}",
                        spec.id(),
                        level.dimension().location(),
                        worldPosition,
                        unsupportedInventorySlots.orElseThrow(),
                        spec.items().slotCount());
            }
        }
    }

    protected final void tickProcessingServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (unsupportedProcessingVersion.isPresent()
                || unsupportedInventorySlots.isPresent()) {
            return;
        }
        invalidateRevisions();
        Optional<RecipeMap.Match> found = findRecipe();
        if (found.isEmpty()) {
            ProcessingHostDecisions.IdleTransition transition =
                    ProcessingHostDecisions.idle(
                            clearSelection(), runtime.reset(), powerDemand);
            powerDemand = 0L;
            if (transition.persistentChange()) {
                markMutation();
            }
            checkpoint();
            return;
        }

        RecipeMap.Match match = found.get();
        GTRecipe recipe = match.recipe();
        Optional<String> invalid = spec.validator().validate(recipe);
        beforeRuntimeSelect(match);
        select(match);
        powerDemand = Math.max(0L, recipe.eut());
        Optional<MachineTransaction> transaction = invalid.isEmpty()
                ? prepareTransaction(recipe)
                : Optional.empty();
        boolean capacity = transaction.isPresent();
        Optional<PowerPlan> powerPlan = planPower(powerDemand);
        boolean powered = powerPlan.isPresent();
        if (invalid.isEmpty() && capacity && powered) {
            executePowerForTransaction(
                    transaction.orElseThrow(), this, powerPlan.orElseThrow()::execute);
        }
        String previousStatus = runtime.status();
        String previousActive = runtime.processor().activeId();
        int previousProgress = runtime.processor().progress();
        int previousDuration = runtime.processor().duration();
        ProcessingRuntime.Result result = runtime.tick(
                match.id().toString(),
                recipe.duration(),
                invalid.isEmpty(),
                capacity,
                powered,
                spec.buffering());

        if (result == ProcessingRuntime.Result.ADVANCED) {
            checkpoint.markDirty();
        } else if (result == ProcessingRuntime.Result.COMPLETE) {
            MachineTransaction completion = transaction.orElseThrow();
            if (!completion.commit(this)) {
                throw new IllegalStateException(
                        "Processing resources changed after committed energy extraction");
            }
            runtime.completed(recipe.duration());
            clearSelection();
            checkpoint.markDirty();
        } else if (!previousStatus.equals(runtime.status())
                || !previousActive.equals(runtime.processor().activeId())
                || previousProgress != runtime.processor().progress()
                || previousDuration != runtime.processor().duration()) {
            markMutation();
        }
        checkpoint();
    }

    private Optional<RecipeMap.Match> findRecipe() {
        List<ItemStack> itemInputs =
                spec.items().inputs().stream().map(inventory::getStackInSlot).toList();
        List<FluidStack> fluidInputs =
                spec.fluids().inputs().stream().map(
                        tank -> tanks.get(tank.index()).getFluid()).toList();
        if (itemInputs.stream().allMatch(ItemStack::isEmpty)
                && fluidInputs.stream().allMatch(FluidStack::isEmpty)) {
            return Optional.empty();
        }
        return recipeCache.find(itemInputs, fluidInputs);
    }

    private void select(RecipeMap.Match match) {
        long mapRevision = spec.requireRecipeMap().revision();
        boolean same = selectedMatch != null
                && selectedMatch.id().equals(match.id())
                && selectedMatch.recipe() == match.recipe()
                && selectedMapRevision == mapRevision;
        if (same) {
            return;
        }
        if (selectedMatch != null && selectedMatch.id().equals(match.id())) {
            runtime.reset();
        }
        selectedMatch = match;
        selectedMapRevision = mapRevision;
        Optional<String> fingerprint = currentRecipeFingerprint(match.recipe());
        ProcessingHostDecisions.SelectionPersistence persistence =
                ProcessingHostDecisions.selection(
                        restoredRollValid,
                        restoredSelectedId,
                        restoredRecipeFingerprint,
                        match.id().toString(),
                        fingerprint);
        if (persistence
                == ProcessingHostDecisions.SelectionPersistence.REUSE_RESTORED_ROLL) {
            restoredSelectedId = "";
            restoredRollValid = false;
            restoredRecipeFingerprint = "";
        } else {
            if (restoredSelectedId.equals(match.id().toString())) {
                runtime.reset();
            }
            rolledOutputs = ChanceOutputs.roll(
                    match.recipe().itemOutputs(),
                    match.recipe().outputChances(),
                    this::randomBelow);
            rolledOutputs = HotIngotProcessing.prepareOutputs(
                    rolledOutputs,
                    level == null ? 0L : level.getGameTime());
        }
        markMutation();
    }

    protected void beforeRuntimeSelect(RecipeMap.Match match) {}

    private Optional<MachineTransaction> prepareTransaction(GTRecipe recipe) {
        return MachineTransaction.prepare(
                recipe,
                inventorySnapshot(),
                spec.items().inputs(),
                spec.items().outputs(),
                fluidSnapshot(),
                spec.fluids().inputs(),
                spec.fluids().outputs(),
                rolledOutputs);
    }

    protected int randomBelow(int bound) {
        if (level == null || level.isClientSide) {
            throw new IllegalStateException("Chance outputs may only roll server-side");
        }
        return level.random.nextInt(bound);
    }

    private Optional<PowerPlan> planPower(long units) {
        if (units < 0L) {
            return Optional.empty();
        }
        if (units == 0L) {
            return Optional.of(() -> true);
        }
        if (spec.energy().mode() == ProcessingMachineSpec.EnergyMode.BUFFERED) {
            return energy.canConsume(units)
                    ? Optional.of(() -> energy.consume(units))
                    : Optional.empty();
        }
        return AdjacentEnergyConsumer.plan(
                adjacentEnergySource(),
                spec.energy().type(),
                adjacentEnergySourceSide(),
                units).map(plan -> plan::execute);
    }

    static void executePowerForTransaction(
            MachineTransaction transaction,
            MachineTransaction.ResourceAccess resources,
            BooleanSupplier executePower) {
        if (!transaction.stillValid(resources)) {
            throw new IllegalStateException(
                    "Processing resources changed during energy simulation");
        }
        if (!executePower.getAsBoolean()) {
            throw new IllegalStateException(
                    "Simulated processing energy disappeared before execution");
        }
        if (!transaction.stillValid(resources)) {
            throw new IllegalStateException(
                    "Processing resources changed during energy extraction");
        }
    }

    protected IEnergyHandler adjacentEnergySource() {
        return null;
    }

    protected Direction adjacentEnergySourceSide() {
        return Direction.UP;
    }

    protected boolean isFluidInputValid(int tank, FluidStack stack) {
        return spec.fluids().inputs().stream().anyMatch(value -> value.index() == tank);
    }

    protected void onItemSlotChanged(int slot) {}

    protected abstract Direction machineFront();

    public final IItemHandler items(Direction side) {
        ProcessingMachineSpec.CapabilityAccess access =
                spec.sidedIo().items().resolve(machineFront(), side);
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            return null;
        }
        return itemViews.computeIfAbsent(access, key -> new SidedItemHandler(
                inventory,
                key == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? spec.items().inputs() : spec.items().outputs(),
                key,
                this::markMutation));
    }

    public final IFluidHandler fluids(Direction side) {
        ProcessingMachineSpec.CapabilityAccess access =
                spec.sidedIo().fluids().resolve(machineFront(), side);
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            return null;
        }
        return fluidViews.computeIfAbsent(access, key -> new SidedFluidHandler(
                tanks,
                (key == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? spec.fluids().inputs() : spec.fluids().outputs())
                        .stream().map(ProcessingMachineSpec.TankSpec::index).toList(),
                key,
                this::markMutation));
    }

    /**
     * Player-only drain view selected from the clicked side. Input sides expose
     * the maintenance view; output sides reuse the normal drain-only view.
     */
    public final IFluidHandler playerDrainFluids(Direction side) {
        ProcessingMachineSpec.CapabilityAccess access =
                spec.sidedIo().fluids().resolve(machineFront(), side);
        return switch (access) {
            case INPUT -> maintenanceFluids();
            case OUTPUT -> fluids(side);
            case NONE -> null;
        };
    }

    /**
     * Player-only recovery view for input tanks. This is deliberately not
     * registered as an external capability, so automation remains input-only.
     */
    public final IFluidHandler maintenanceFluids() {
        if (spec.fluids().inputs().isEmpty()) {
            return null;
        }
        if (maintenanceFluidView == null) {
            maintenanceFluidView = new SidedFluidHandler(
                    tanks,
                    spec.fluids().inputs().stream()
                            .map(ProcessingMachineSpec.TankSpec::index).toList(),
                    ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                    this::markMutation);
        }
        return maintenanceFluidView;
    }

    @Override public boolean handles(EnergyType type, Direction side) {
        return spec.energy().mode() == ProcessingMachineSpec.EnergyMode.BUFFERED
                && type == spec.energy().type()
                && spec.sidedIo().energy().resolve(machineFront(), side)
                == ProcessingMachineSpec.CapabilityAccess.INPUT;
    }

    @Override public long insert(
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
            markMutation();
        }
        return accepted;
    }

    @Override public long stored(EnergyType type) {
        return energy != null && type == spec.energy().type() ? energy.stored() : 0L;
    }

    @Override public long capacity(EnergyType type) {
        return energy != null && type == spec.energy().type() ? energy.capacity() : 0L;
    }

    public final ProcessingMachineSpec spec() { return spec; }
    public final ItemStackHandler inventory() { return inventory; }
    public final List<FluidTank> tanks() { return tanks; }
    public final ProcessingRuntime runtime() { return runtime; }
    public final int progress() { return runtime.processor().progress(); }
    public final int duration() { return runtime.processor().duration(); }
    public final long powerDemandLong() { return powerDemand; }
    public final String pausedReason() {
        if (unsupportedProcessingVersion.isPresent()) {
            return ProcessingMachineDisplayData.UNSUPPORTED_VERSION;
        }
        return unsupportedInventorySlots.isPresent()
                ? ProcessingMachineDisplayData.INVENTORY_LAYOUT_QUARANTINED
                : runtime.status();
    }
    public final int statusArgument() {
        return unsupportedProcessingVersion.or(() -> unsupportedInventorySlots).orElse(0);
    }
    public final long resourceRevision() { return resourceRevision; }

    protected final void markMutation() {
        setChanged();
        checkpoint.markDirty();
    }

    protected final void restoreEnergy(long value) {
        if (energy != null) {
            energy.restore(value);
        }
    }

    private void resourcesChanged() {
        resourceRevision++;
        recipeCache.invalidate();
        markMutation();
    }

    private void invalidateRevisions() {
        long currentMaterialRevision =
                MaterialCatalog.isBootstrapped() ? MaterialCatalog.runtimeRevision() : -1L;
        if (currentMaterialRevision != materialRevision) {
            materialRevision = currentMaterialRevision;
            recipeCache.invalidate();
            if (selectedMatch != null) {
                restoredSelectedId = selectedMatch.id().toString();
                Optional<String> fingerprint =
                        currentRecipeFingerprint(selectedMatch.recipe());
                restoredRollValid = fingerprint.isPresent();
                restoredRecipeFingerprint = fingerprint.orElse("");
                markMutation();
            }
            selectedMatch = null;
            selectedMapRevision = -1L;
        }
    }

    private boolean clearSelection() {
        boolean changed = selectedMatch != null
                || restoredRollValid
                || !restoredSelectedId.isEmpty()
                || !rolledOutputs.isEmpty();
        selectedMatch = null;
        selectedMapRevision = -1L;
        restoredSelectedId = "";
        restoredRollValid = false;
        restoredRecipeFingerprint = "";
        rolledOutputs = List.of();
        return changed;
    }

    private void checkpoint() {
        if (level == null) {
            return;
        }
        long gameTime = level.getGameTime();
        long phase = CheckpointDecisions.phaseKey(
                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ());
        if (checkpoint.shouldCheckpoint(gameTime, phase, CHECKPOINT_INTERVAL)) {
            setChanged();
            checkpoint.checkpointed();
        }
        if (checkpoint.shouldSync(
                runtime.active(), gameTime, phase, CHECKPOINT_INTERVAL)) {
            level.sendBlockUpdated(
                    worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
            checkpoint.synced();
        }
    }

    @Override protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.merge(new ProcessingMachineState(
                runtime.processor().activeId(),
                progress(),
                duration(),
                runtime.status(),
                powerDemand,
                energy == null ? 0L : energy.stored(),
                resourceRevision,
                unsupportedProcessingVersion).write());
        tag.put("inventory", inventory.serializeNBT(registries));
        for (int tank = 0; tank < tanks.size(); tank++) {
            tag.put("tank_" + tank, tanks.get(tank).writeToNBT(registries, new CompoundTag()));
        }
        Optional<String> selectedFingerprint = selectedMatch == null
                ? Optional.empty()
                : GTRecipeFingerprint.recipe(
                        spec.recipeMapId(), selectedMatch.recipe(), registries);
        new ChanceOutputState(
                selectedMatch != null ? selectedFingerprint.isPresent() : restoredRollValid,
                selectedMatch != null ? selectedMatch.id().toString() : restoredSelectedId,
                selectedMatch != null
                        ? selectedFingerprint.orElse("")
                        : restoredRecipeFingerprint,
                rolledOutputs).write(tag, registries);
    }

    @Override protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeForLayout(registries, tag.getCompound("inventory"));
            unsupportedInventorySlots = inventory.layoutQuarantined()
                    ? Optional.of(inventory.loadedSlots())
                    : Optional.empty();
        }
        for (int tank = 0; tank < tanks.size(); tank++) {
            if (tag.contains("tank_" + tank)) {
                tanks.get(tank).readFromNBT(registries, tag.getCompound("tank_" + tank));
            }
        }
        ProcessingMachineState state = ProcessingMachineState.read(tag);
        runtime.restore(
                state.activeRecipe(), state.progress(), state.duration(), state.status());
        unsupportedProcessingVersion = state.unsupportedVersion();
        ChanceOutputState chanceState = ChanceOutputState.read(tag, registries);
        restoredSelectedId = chanceState.recipeId().isEmpty()
                ? runtime.processor().activeId()
                : chanceState.recipeId();
        restoredRollValid = chanceState.valid();
        restoredRecipeFingerprint = chanceState.recipeFingerprint();
        powerDemand = state.powerDemand();
        if (energy != null) {
            energy.restore(state.energy());
        }
        resourceRevision = Math.max(resourceRevision, state.resourceRevision());
        rolledOutputs = chanceState.outputs();
        recipeCache.invalidate();
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return writeClientTag(registries);
    }

    @Override public void handleUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        readClientTag(tag, registries);
    }

    @Override public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            readClientTag(packet.getTag(), registries);
        }
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    protected CompoundTag writeClientTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putString("active_recipe", runtime.processor().activeId());
        tag.putInt("progress", progress());
        tag.putInt("duration", duration());
        tag.putString("status", runtime.status());
        unsupportedProcessingVersion.ifPresent(version ->
                tag.putInt("unsupported_processing_version", version));
        unsupportedInventorySlots.ifPresent(slots ->
                tag.putInt("unsupported_inventory_slots", slots));
        tag.putLong("power_demand", powerDemand);
        tag.putLong("energy", energy == null ? 0L : energy.stored());
        tag.putInt("tank_count", tanks.size());
        for (int tank = 0; tank < tanks.size(); tank++) {
            tag.put("tank_" + tank, tanks.get(tank).writeToNBT(registries, new CompoundTag()));
        }
        return tag;
    }

    protected void readClientTag(CompoundTag tag, HolderLookup.Provider registries) {
        runtime.restore(
                tag.getString("active_recipe"),
                tag.getInt("progress"),
                tag.getInt("duration"),
                tag.getString("status"));
        int unsupportedVersion = tag.getInt("unsupported_processing_version");
        unsupportedProcessingVersion = unsupportedVersion > ProcessingMachineState.VERSION
                ? Optional.of(unsupportedVersion)
                : Optional.empty();
        int inventorySlots = tag.getInt("unsupported_inventory_slots");
        unsupportedInventorySlots = inventorySlots < 0
                || inventorySlots > spec.items().slotCount()
                ? Optional.of(inventorySlots)
                : Optional.empty();
        powerDemand = Math.max(0L, tag.getLong("power_demand"));
        if (energy != null) {
            energy.restore(tag.getLong("energy"));
        }
        for (int tank = 0; tank < Math.min(tanks.size(), tag.getInt("tank_count")); tank++) {
            tanks.get(tank).readFromNBT(registries, tag.getCompound("tank_" + tank));
        }
    }

    private List<ItemStack> inventorySnapshot() {
        List<ItemStack> result = new ArrayList<>(inventory.getSlots());
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            result.add(inventory.getStackInSlot(slot).copy());
        }
        return result;
    }

    private List<FluidStack> fluidSnapshot() {
        return tanks.stream().map(tank -> tank.getFluid().copy()).toList();
    }

    private Optional<String> currentRecipeFingerprint(GTRecipe recipe) {
        return level == null
                ? Optional.empty()
                : GTRecipeFingerprint.recipe(
                        spec.recipeMapId(), recipe, level.registryAccess());
    }

    @Override public int itemCount() { return inventory.getSlots(); }
    @Override public ItemStack item(int slot) { return inventory.getStackInSlot(slot).copy(); }
    @Override public void setItem(int slot, ItemStack stack) {
        inventory.setStackInSlot(slot, stack);
    }
    @Override public int fluidCount() { return tanks.size(); }
    @Override public FluidStack fluid(int tank) { return tanks.get(tank).getFluid().copy(); }
    @Override public void setFluid(int tank, FluidStack stack) {
        tanks.get(tank).setFluid(stack);
    }

    @FunctionalInterface
    private interface PowerPlan {
        boolean execute();
    }
}
