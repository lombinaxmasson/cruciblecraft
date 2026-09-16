package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.function.BooleanSupplier;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.heat.HotIngotProcessing;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.machine.processing.AdjacentEnergyConsumer;
import com.masson.cruciblecraft.machine.processing.ChanceOutputState;
import com.masson.cruciblecraft.machine.processing.GTRecipeFingerprint;
import com.masson.cruciblecraft.machine.processing.LayoutAwareItemStackHandler;
import com.masson.cruciblecraft.machine.processing.MachineExecutionPlan;
import com.masson.cruciblecraft.machine.processing.MachineIdentityPolicy;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.machine.processing.MachineTransaction;
import com.masson.cruciblecraft.machine.processing.ParallelRecipeOperations;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineDisplayData;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineEnergyPlacement;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineState;
import com.masson.cruciblecraft.machine.processing.ProcessingRuntime;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.machine.processing.IoChannel;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineAutoIo;
import com.masson.cruciblecraft.machine.processing.SidedFluidHandler;
import com.masson.cruciblecraft.machine.processing.SidedItemHandler;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverItems;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverSet;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeCache;
import com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.content.item.ProgrammedCircuitItem;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineIdentities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
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
        implements IEnergyHandler, MachineTransaction.ResourceAccess,
        com.masson.cruciblecraft.content.multiblock.MultiblockPortHost,
        MachineCoverHost {
    private static final int CHECKPOINT_INTERVAL = 20;

    private final MachineVariant variant;
    private final ProcessingMachineSpec spec;
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private final ProcessingRuntime runtime = new ProcessingRuntime();
    private final LayoutAwareItemStackHandler inventory;
    private final List<FluidTank> tanks;
    private final GTRecipeCache recipeCache;
    private final MachineEnergyBuffer energy;
    private final PipeCoverSet covers = new PipeCoverSet();
    private final Map<ProcessingMachineSpec.CapabilityAccess, IItemHandler> itemViews =
            new EnumMap<>(ProcessingMachineSpec.CapabilityAccess.class);
    private final Map<ProcessingMachineSpec.CapabilityAccess, IFluidHandler> fluidViews =
            new EnumMap<>(ProcessingMachineSpec.CapabilityAccess.class);
    private IFluidHandler maintenanceFluidView;

    private RecipeMap.Match selectedMatch;
    private MachineExecutionPlan selectedPlan;
    private Optional<String> selectedRecipeFingerprint = Optional.empty();
    private int selectedOperations = 1;
    private int restoredOperations = 1;
    private long workProgress;
    private long restoredWorkProgress;
    private long restoredWorkRequired;
    private List<ItemStack> rolledOutputs = List.of();
    private String restoredSelectedId = "";
    private boolean restoredRollValid;
    private String restoredRecipeFingerprint = "";
    private long selectedMapRevision = -1L;
    private long materialRevision = -1L;
    private long resourceRevision;
    private long powerDemand;
    private long preservedUnbufferedEnergy;
    private Optional<Integer> unsupportedProcessingVersion = Optional.empty();
    private Optional<Integer> unsupportedInventorySlots = Optional.empty();
    private Optional<String> materialQuarantine = Optional.empty();
    private boolean coverEnabled = true;
    private boolean coversStopped;
    private int selectorMode;
    private boolean disabledItemInput;
    private boolean disabledItemOutput;
    private boolean disabledFluidInput;
    private boolean disabledFluidOutput;
    private int screwdriverMode;
    private boolean inventoryChangedForAutoIo;
    private long lastSuccessfulGameTime = Long.MIN_VALUE;
    private long coverRecipeCacheTick = Long.MIN_VALUE;
    private Optional<RecipeMap.Match> coverRecipeCache;
    private MachineIdentityPolicy.Identity savedMachineIdentity =
            new MachineIdentityPolicy.Identity("", "", "", "");
    private Optional<String> persistedIdentityQuarantine = Optional.empty();
    private boolean quarantineWarningLogged;

    protected ProcessingMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            ProcessingMachineSpec spec) {
        this(type, pos, state, MachineVariant.legacy(spec));
    }

    protected ProcessingMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            MachineVariant variant) {
        super(type, pos, state);
        this.variant = variant;
        this.spec = variant.runtimeSpec();
        this.recipeCache = new GTRecipeCache(spec.requireRecipeMap());
        this.energy = energyBufferFor(spec);
        this.inventory = new LayoutAwareItemStackHandler(
                spec.items().slotCount(),
                (slot, stack) -> spec.items().accepts(slot, stack)
                        && automationAllowsInsert(slot),
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
                        || unsupportedInventorySlots.isPresent()
                        || materialQuarantine.isPresent())) {
            quarantineWarningLogged = true;
            if (unsupportedProcessingVersion.isPresent()) {
                CrucibleCraft.LOGGER.warn(
                        "Quarantined processing machine {} at {} {}: unsupported version {}",
                        spec.id(),
                        level.dimension().location(),
                        worldPosition,
                        unsupportedProcessingVersion.orElseThrow());
            } else if (unsupportedInventorySlots.isPresent()) {
                CrucibleCraft.LOGGER.warn(
                        "Quarantined processing machine {} at {} {}: saved inventory has "
                                + "{} slots but the live spec has {}",
                        spec.id(),
                        level.dimension().location(),
                        worldPosition,
                        unsupportedInventorySlots.orElseThrow(),
                        spec.items().slotCount());
            } else {
                CrucibleCraft.LOGGER.warn(
                        "Quarantined processing machine {} at {} {}: {}",
                        spec.id(),
                        level.dimension().location(),
                        worldPosition,
                        materialQuarantine.orElseThrow());
            }
        }
    }

    protected final void tickCoversServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        MachineCoverBehaviors.tickAll(this);
    }

    protected final void tickProcessingServer() {
        if (level == null || level.isClientSide) {
            return;
        }
        coverRecipeCacheTick = level.getGameTime();
        coverRecipeCache = null;
        tickCoversServer();
        if (unsupportedProcessingVersion.isPresent()
                || unsupportedInventorySlots.isPresent()
                || materialQuarantine.isPresent()) {
            return;
        }
        boolean inventoryPulse = inventoryChangedForAutoIo;
        inventoryChangedForAutoIo = false;
        if (!disabledFluidOutput) {
            autoOutputFluids();
        }
        if (!disabledItemInput) {
            autoInputItems();
        }
        if (!disabledFluidInput) {
            autoInputFluids();
        }
        if (!processingAllowed()) {
            autoOutputItems(inventoryPulse);
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
            autoOutputItems(inventoryPulse);
            return;
        }

        RecipeMap.Match match = found.get();
        GTRecipe recipe = match.recipe();
        Optional<String> invalid =
                GTRecipeMapLoader.isBathRemainderCompactRecipe(match.id())
                        ? Optional.empty()
                        : spec.validator().validate(recipe);
        beforeRuntimeSelect(match);
        Optional<MachineExecutionPlan> singlePlan =
                MachineExecutionPlan.create(
                        recipe,
                        variant.kind(),
                        variant.tierBand(),
                        1);
        if (invalid.isEmpty() && singlePlan.isEmpty()) {
            selectPowerExceeded(match);
            powerDemand = 0L;
            runtime.recipePowerExceeded(
                    match.id().toString(), recipe.duration());
            markMutation();
            checkpoint();
            autoOutputItems(inventoryPulse);
            return;
        }
        Optional<PlannedExecution> planned = invalid.isEmpty()
                ? planExecution(recipe)
                : Optional.empty();
        MachineExecutionPlan plan = planned
                .map(PlannedExecution::plan)
                .orElseGet(() -> singlePlan.orElseGet(
                        () -> new MachineExecutionPlan(
                                1L,
                                1L,
                                1L,
                                recipe.duration(),
                                recipe.duration(),
                                1,
                                0)));
        int operations = planned
                .map(PlannedExecution::operations)
                .orElse(1);
        select(match, operations, plan);
        adoptDisplayedProgress(plan);
        Optional<MachineTransaction> transaction = invalid.isEmpty()
                ? prepareTransaction(
                        ParallelRecipeOperations.scale(
                                recipe, operations))
                : Optional.empty();
        boolean capacity = transaction.isPresent();
        if ((screwdriverMode & 1) != 0
                && workProgress == 0L
                && !outputsCompletelyEmpty()) {
            capacity = false;
        }
        long remainingWork = Math.max(
                0L, plan.totalWork() - workProgress);
        Optional<PowerPlan> powerPlan =
                invalid.isEmpty() && capacity
                        ? planPower(plan, remainingWork)
                        : Optional.empty();
        powerDemand = powerPlan
                .map(PowerPlan::energyUnits)
                .orElse(Math.min(
                        remainingWork, plan.minimumPower()));
        boolean powered = powerPlan.isPresent();
        if (invalid.isEmpty() && capacity && powered) {
            executePowerForTransaction(
                    transaction.orElseThrow(),
                    this,
                    powerPlan.orElseThrow().executePower());
        }
        String previousStatus = runtime.status();
        String previousActive = runtime.processor().activeId();
        int previousProgress = runtime.processor().progress();
        int previousDuration = runtime.processor().duration();
        ProcessingRuntime.Result result;
        if (invalid.isEmpty() && capacity && powered) {
            workProgress = Math.min(
                    plan.totalWork(),
                    Math.addExact(
                            workProgress,
                            powerPlan.orElseThrow().workUnits()));
            result = runtime.tickWork(
                    match.id().toString(),
                    plan.effectiveDuration(),
                    true,
                    true,
                    workProgress,
                    plan.totalWork(),
                    spec.buffering());
        } else {
            result = runtime.tick(
                    match.id().toString(),
                    plan.effectiveDuration(),
                    invalid.isEmpty(),
                    capacity,
                    false,
                    spec.buffering());
        }

        if (result == ProcessingRuntime.Result.ADVANCED) {
            checkpoint.markDirty();
        } else if (result == ProcessingRuntime.Result.COMPLETE) {
            MachineTransaction completion = transaction.orElseThrow();
            if (!completion.commit(this)) {
                throw new IllegalStateException(
                        "Processing resources changed after committed energy extraction");
            }
            lastSuccessfulGameTime = level.getGameTime();
            onProcessingCompleted(match.id().toString());
            runtime.completed(plan.effectiveDuration());
            clearSelection();
            checkpoint.markDirty();
        } else if (!previousStatus.equals(runtime.status())
                || !previousActive.equals(runtime.processor().activeId())
                || previousProgress != runtime.processor().progress()
                || previousDuration != runtime.processor().duration()) {
            markMutation();
        }
        checkpoint();
        autoOutputItems(
                inventoryPulse
                        || result == ProcessingRuntime.Result.COMPLETE);
    }

    private Optional<RecipeMap.Match> findRecipe() {
        if (level != null
                && coverRecipeCacheTick == level.getGameTime()
                && coverRecipeCache != null) {
            return coverRecipeCache;
        }
        List<ItemStack> itemInputs = new ArrayList<>(
                spec.items().inputs().stream().map(inventory::getStackInSlot).toList());
        List<FluidStack> fluidInputs =
                spec.fluids().inputs().stream().map(
                        tank -> tanks.get(tank.index()).getFluid()).toList();
        if (itemInputs.stream().allMatch(ItemStack::isEmpty)
                && fluidInputs.stream().allMatch(FluidStack::isEmpty)) {
            coverRecipeCache = Optional.empty();
            return coverRecipeCache;
        }
        itemInputs.addAll(selectorCircuitOffered());
        coverRecipeCache = recipeCache.find(itemInputs, fluidInputs);
        return coverRecipeCache;
    }

    private List<ItemStack> selectorCircuitOffered() {
        if (!MachineCoverBehaviors.hasSelector(this)) {
            return List.of();
        }
        ItemStack circuit = new ItemStack(ModItems.PROGRAMMED_CIRCUIT.get());
        circuit.set(
                ModComponents.CIRCUIT_CONFIG.get(),
                ProgrammedCircuitItem.normalize(selectorMode + 1));
        return List.of(circuit);
    }

    void select(
            RecipeMap.Match match,
            int operations,
            MachineExecutionPlan plan) {
        select(
                match,
                operations,
                plan,
                () -> currentRecipeFingerprint(match.recipe()));
    }

    void select(
            RecipeMap.Match match,
            int operations,
            MachineExecutionPlan plan,
            Supplier<Optional<String>> fingerprintSupplier) {
        long mapRevision = spec.requireRecipeMap().revision();
        boolean structurallySame = selectedMatch != null
                && selectedMatch.id().equals(match.id())
                && selectedMapRevision == mapRevision
                && selectedOperations == operations
                && plan.equals(selectedPlan);
        if (structurallySame
                && selectedMatch.recipe() == match.recipe()) {
            return;
        }
        Optional<String> fingerprint = fingerprintSupplier.get();
        if (structurallySame
                && fingerprint.isPresent()
                && fingerprint.equals(selectedRecipeFingerprint)) {
            return;
        }
        if (selectedMatch != null && selectedMatch.id().equals(match.id())) {
            runtime.reset();
        }
        selectedMatch = match;
        selectedPlan = plan;
        selectedRecipeFingerprint = fingerprint;
        selectedOperations = operations;
        selectedMapRevision = mapRevision;
        ProcessingHostDecisions.SelectionPersistence persistence =
                ProcessingHostDecisions.selection(
                        restoredRollValid,
                        restoredSelectedId,
                        restoredRecipeFingerprint,
                        match.id().toString(),
                        fingerprint);
        if (persistence
                == ProcessingHostDecisions.SelectionPersistence.REUSE_RESTORED_ROLL) {
            if (restoredOperations != operations) {
                rolledOutputs = ParallelRecipeOperations.rollItemOutputs(
                        match.recipe(), operations, this::randomBelow);
                runtime.reset();
                workProgress = 0L;
            } else if (restoredWorkRequired == plan.totalWork()) {
                workProgress = Math.min(
                        restoredWorkProgress, plan.totalWork());
            } else {
                runtime.reset();
                workProgress = 0L;
            }
            restoredSelectedId = "";
            restoredRollValid = false;
            restoredRecipeFingerprint = "";
            restoredWorkProgress = 0L;
            restoredWorkRequired = 0L;
        } else {
            if (restoredSelectedId.equals(match.id().toString())) {
                runtime.reset();
            }
            workProgress = 0L;
            rolledOutputs = ParallelRecipeOperations.rollItemOutputs(
                    match.recipe(), operations, this::randomBelow);
            rolledOutputs = HotIngotProcessing.prepareOutputs(
                    rolledOutputs,
                    level == null ? 0L : level.getGameTime());
        }
        markMutation();
    }

    protected void beforeRuntimeSelect(RecipeMap.Match match) {}

    protected void onProcessingCompleted(String recipeId) {}

    private Optional<MachineTransaction> prepareTransaction(GTRecipe recipe) {
        return MachineTransaction.prepare(
                recipe,
                inventorySnapshot(),
                spec.items().inputs(),
                spec.items().outputs(),
                fluidSnapshot(),
                spec.fluids().inputs(),
                spec.fluids().outputs(),
                rolledOutputs,
                selectorCircuitOffered());
    }

    private Optional<PlannedExecution> planExecution(
            GTRecipe recipe) {
        for (int operations = variant.tierBand().parallelLimit();
                operations >= 1;
                operations--) {
            Optional<MachineExecutionPlan> plan =
                    MachineExecutionPlan.create(
                            recipe,
                            variant.kind(),
                            variant.tierBand(),
                            operations);
            if (plan.isEmpty()) {
                continue;
            }
            GTRecipe scaled = ParallelRecipeOperations.scale(
                    recipe, operations);
            Optional<MachineTransaction> capacity =
                    MachineTransaction.prepare(
                            scaled,
                            inventorySnapshot(),
                            spec.items().inputs(),
                            spec.items().outputs(),
                            fluidSnapshot(),
                            spec.fluids().inputs(),
                            spec.fluids().outputs(),
                            ParallelRecipeOperations.maximumItemOutputs(
                                    recipe, operations),
                            selectorCircuitOffered());
            if (capacity.isPresent()) {
                return Optional.of(new PlannedExecution(
                        operations, plan.orElseThrow()));
            }
        }
        return Optional.empty();
    }

    private void adoptDisplayedProgress(
            MachineExecutionPlan plan) {
        int displayed = runtime.processor().progress();
        int duration = runtime.processor().duration();
        if (displayed <= 0 || duration <= 0) {
            return;
        }
        long scaled = Math.multiplyExact(
                (long) displayed, plan.totalWork());
        long projected = scaled / duration
                + (scaled % duration == 0L ? 0L : 1L);
        workProgress = Math.max(
                workProgress,
                Math.min(projected, plan.totalWork()));
    }

    protected int randomBelow(int bound) {
        if (level == null || level.isClientSide) {
            throw new IllegalStateException("Chance outputs may only roll server-side");
        }
        return level.random.nextInt(bound);
    }

    private Optional<PowerPlan> planPower(
            MachineExecutionPlan plan, long remainingWork) {
        if (remainingWork <= 0L) {
            return Optional.empty();
        }
        if (plan.nominalPower() == 0L) {
            return Optional.of(new PowerPlan(
                    0L, 1L, () -> true));
        }
        if (spec.energy().type() == EnergyType.TIME) {
            long units = Math.max(1L, plan.nominalPower());
            return Optional.of(new PowerPlan(
                    units,
                    units,
                    () -> true));
        }
        boolean legacy = variant.kind().overclockPolicy()
                == com.masson.cruciblecraft.machine.processing
                        .MachineKindSpec.OverclockPolicy.LEGACY_TICKS;
        long minimum = legacy
                ? plan.minimumPower()
                : Math.max(
                        variant.tierBand().inputMinimum(),
                        plan.minimumPower());
        long maximum = legacy
                ? plan.nominalPower()
                : plan.maximumPower();
        if (spec.energy().mode() == ProcessingMachineSpec.EnergyMode.ADJACENT
                && spec.energy().type() == EnergyType.HEAT
                && energy != null
                && energy.stored() >= minimum) {
            long actual = Math.min(maximum, energy.stored());
            if (actual < minimum) {
                return Optional.empty();
            }
            long step = Math.min(actual, minimum);
            return Optional.of(new PowerPlan(
                    step,
                    step,
                    () -> energy.consume(step)));
        }
        if (spec.energy().mode() == ProcessingMachineSpec.EnergyMode.BUFFERED) {
            if (energy == null) {
                return Optional.empty();
            }
            long actual = Math.min(maximum, energy.stored());
            return actual >= minimum
                    ? Optional.of(new PowerPlan(
                            actual,
                            actual,
                            () -> energy.consume(actual)))
                    : Optional.empty();
        }
        return AdjacentEnergyConsumer.planWindow(
                adjacentEnergySource(),
                spec.energy().type(),
                adjacentEnergySourceSide(),
                minimum,
                maximum).map(window -> new PowerPlan(
                        window.units(),
                        window.units(),
                        window::execute));
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

    private void selectPowerExceeded(RecipeMap.Match match) {
        long mapRevision = spec.requireRecipeMap().revision();
        boolean structurallySame = selectedMatch != null
                && selectedMatch.id().equals(match.id())
                && selectedMapRevision == mapRevision
                && selectedPlan == null;
        if (structurallySame
                && selectedMatch.recipe() == match.recipe()) {
            return;
        }
        Optional<String> fingerprint =
                currentRecipeFingerprint(match.recipe());
        if (structurallySame
                && fingerprint.isPresent()
                && fingerprint.equals(selectedRecipeFingerprint)) {
            return;
        }
        clearSelection();
        selectedMatch = match;
        selectedRecipeFingerprint = fingerprint;
        selectedMapRevision = mapRevision;
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

    public final Direction facing() {
        return machineFront();
    }

    public boolean handleIoTool(ToolAction action, Direction side, Player player) {
        boolean server = level != null && !level.isClientSide;
        if (action == ToolAction.SCREWDRIVER) {
            if (server) {
                screwdriverMode = (screwdriverMode + 1) % 4;
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    (screwdriverMode & 1) != 0
                                            ? "message.cruciblecraft.machine.mode.output_empty"
                                            : "message.cruciblecraft.machine.mode.output_space"),
                            false);
                    player.displayClientMessage(
                            Component.translatable(
                                    (screwdriverMode & 2) != 0
                                            ? "message.cruciblecraft.machine.mode.input_empty"
                                            : "message.cruciblecraft.machine.mode.input_all"),
                            false);
                }
                markMutation();
            }
            return true;
        }
        if (action != ToolAction.MONKEY_WRENCH || side == null) {
            return false;
        }
        Direction front = machineFront();
        IoChannel items = spec.sidedIo().itemsChannel();
        IoChannel fluids = spec.sidedIo().fluidsChannel();
        boolean changed = false;
        if (items.autoInputWorld(front).orElse(null) == side) {
            if (server) {
                disabledItemInput = !disabledItemInput;
                tellAuto(player, "item_in", disabledItemInput);
            }
            changed = true;
        }
        if (items.autoOutputWorld(front).orElse(null) == side) {
            if (server) {
                disabledItemOutput = !disabledItemOutput;
                tellAuto(player, "item_out", disabledItemOutput);
            }
            changed = true;
        }
        if (fluids.autoInputWorld(front).orElse(null) == side) {
            if (server) {
                disabledFluidInput = !disabledFluidInput;
                tellAuto(player, "fluid_in", disabledFluidInput);
            }
            changed = true;
        }
        if (fluids.autoOutputWorld(front).orElse(null) == side) {
            if (server) {
                disabledFluidOutput = !disabledFluidOutput;
                tellAuto(player, "fluid_out", disabledFluidOutput);
            }
            changed = true;
        }
        if (changed && server) {
            markMutation();
        }
        return changed;
    }

    private static void tellAuto(Player player, String channel, boolean disabled) {
        if (player == null) {
            return;
        }
        player.displayClientMessage(
                Component.translatable(
                        "message.cruciblecraft.machine.auto."
                                + channel
                                +                 (disabled ? ".disabled" : ".enabled")),
                false);
    }

    private boolean automationAllowsInsert(int slot) {
        return (screwdriverMode & 2) == 0
                || inventory.getStackInSlot(slot).isEmpty();
    }

    private boolean outputsCompletelyEmpty() {
        for (int slot : spec.items().outputs()) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return false;
            }
        }
        for (ProcessingMachineSpec.TankSpec tank : spec.fluids().outputs()) {
            if (!tanks.get(tank.index()).getFluid().isEmpty()) {
                return false;
            }
        }
        return true;
    }

    private boolean hasOutputItems() {
        for (int slot : spec.items().outputs()) {
            if (!inventory.getStackInSlot(slot).isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private IItemHandler internalItemView(ProcessingMachineSpec.CapabilityAccess access) {
        return itemViews.computeIfAbsent(access, key -> new SidedItemHandler(
                inventory,
                key == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                        ? List.of() : spec.items().inputs(),
                key == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? List.of() : spec.items().outputs(),
                key,
                this::markMutation,
                this::automationAllowsInsert));
    }

    private IFluidHandler internalFluidView(ProcessingMachineSpec.CapabilityAccess access) {
        if (access == ProcessingMachineSpec.CapabilityAccess.INPUT
                && spec.fluids().inputs().isEmpty()) {
            return null;
        }
        if (access == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                && spec.fluids().outputs().isEmpty()) {
            return null;
        }
        return fluidViews.computeIfAbsent(access, key -> new SidedFluidHandler(
                tanks,
                key == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                        ? List.of()
                        : spec.fluids().inputs().stream()
                                .map(ProcessingMachineSpec.TankSpec::index)
                                .toList(),
                key == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? List.of()
                        : spec.fluids().outputs().stream()
                                .map(ProcessingMachineSpec.TankSpec::index)
                                .toList(),
                key,
                this::markMutation));
    }

    private void autoInputItems() {
        Direction side = spec.sidedIo().itemsChannel()
                .autoInputWorld(machineFront())
                .orElse(null);
        if (side == null) {
            return;
        }
        ProcessingMachineAutoIo.moveItems(
                ProcessingMachineAutoIo.neighborItems(level, worldPosition, side),
                internalItemView(ProcessingMachineSpec.CapabilityAccess.INPUT));
    }

    private void autoOutputItems(boolean pulse) {
        if (disabledItemOutput || !hasOutputItems()) {
            return;
        }
        Direction side = spec.sidedIo().itemsChannel()
                .autoOutputWorld(machineFront())
                .orElse(null);
        if (side == null) {
            return;
        }
        if (!pulse && level.getGameTime() % 200L != 5L) {
            return;
        }
        ProcessingMachineAutoIo.moveItems(
                internalItemView(ProcessingMachineSpec.CapabilityAccess.OUTPUT),
                ProcessingMachineAutoIo.neighborItems(level, worldPosition, side));
    }

    private void autoInputFluids() {
        Direction side = spec.sidedIo().fluidsChannel()
                .autoInputWorld(machineFront())
                .orElse(null);
        if (side == null) {
            return;
        }
        ProcessingMachineAutoIo.moveFluids(
                ProcessingMachineAutoIo.neighborFluids(level, worldPosition, side),
                internalFluidView(ProcessingMachineSpec.CapabilityAccess.INPUT));
    }

    private void autoOutputFluids() {
        Direction side = spec.sidedIo().fluidsChannel()
                .autoOutputWorld(machineFront())
                .orElse(null);
        if (side == null) {
            return;
        }
        ProcessingMachineAutoIo.moveFluids(
                internalFluidView(ProcessingMachineSpec.CapabilityAccess.OUTPUT),
                ProcessingMachineAutoIo.neighborFluids(level, worldPosition, side));
    }

    public final IItemHandler items(Direction side) {
        ProcessingMachineSpec.CapabilityAccess access =
                spec.sidedIo().items().resolve(machineFront(), side);
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            return null;
        }
        return itemViews.computeIfAbsent(access, key -> new SidedItemHandler(
                inventory,
                key == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                        ? List.of() : spec.items().inputs(),
                key == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? List.of() : spec.items().outputs(),
                key,
                this::markMutation,
                this::automationAllowsInsert));
    }

    public final IFluidHandler fluids(Direction side) {
        ProcessingMachineSpec.CapabilityAccess access =
                ProcessingMachineAutoIo.overlayFluidAccess(
                        spec.sidedIo().fluids().resolve(machineFront(), side),
                        side,
                        spec.sidedIo().fluidsChannel()
                                .autoOutputWorld(machineFront())
                                .orElse(null),
                        disabledFluidOutput);
        if (access == ProcessingMachineSpec.CapabilityAccess.NONE) {
            return null;
        }
        return fluidViews.computeIfAbsent(access, key -> new SidedFluidHandler(
                tanks,
                key == ProcessingMachineSpec.CapabilityAccess.OUTPUT
                        ? List.of()
                        : spec.fluids().inputs().stream()
                                .map(ProcessingMachineSpec.TankSpec::index)
                                .toList(),
                key == ProcessingMachineSpec.CapabilityAccess.INPUT
                        ? List.of()
                        : spec.fluids().outputs().stream()
                                .map(ProcessingMachineSpec.TankSpec::index)
                                .toList(),
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
            case OUTPUT, BOTH -> fluids(side);
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
        if (type != spec.energy().type() || side == null) {
            return false;
        }
        if (spec.energy().mode() == ProcessingMachineSpec.EnergyMode.BUFFERED) {
            return spec.sidedIo().energy().resolve(machineFront(), side)
                    == ProcessingMachineSpec.CapabilityAccess.INPUT;
        }
        return spec.energy().mode() == ProcessingMachineSpec.EnergyMode.ADJACENT
                && type == EnergyType.HEAT
                && energy != null
                && side == ProcessingMachineEnergyPlacement
                        .connection(spec, machineFront())
                        .providerOffset();
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
        if (variant.tierBand().overcharges(size)) {
            if (!simulate && amount > 0L) {
                runtime.overcharged();
                markMutation();
            }
            return Math.max(0L, amount);
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

    @Override
    public List<Integer> itemInputSlots() {
        return spec.items().inputs();
    }

    @Override
    public List<Integer> itemOutputSlots() {
        return spec.items().outputs();
    }

    @Override
    public List<Integer> fluidInputTanks() {
        return spec.fluids().inputs().stream()
                .map(ProcessingMachineSpec.TankSpec::index)
                .toList();
    }

    @Override
    public List<Integer> fluidOutputTanks() {
        return spec.fluids().outputs().stream()
                .map(ProcessingMachineSpec.TankSpec::index)
                .toList();
    }

    @Override
    public BlockState blockState() {
        return getBlockState();
    }

    public final ProcessingMachineSpec spec() { return spec; }
    public final MachineVariant variant() { return variant; }
    public final ItemStackHandler inventory() { return inventory; }
    public final List<FluidTank> tanks() { return tanks; }
    public final ProcessingRuntime runtime() { return runtime; }
    public final int progress() { return runtime.processor().progress(); }
    public final int duration() { return runtime.processor().duration(); }
    public final long powerDemandLong() { return powerDemand; }
    public final long workProgressLong() { return workProgress; }
    public final long workRequiredLong() {
        return selectedPlan == null
                ? restoredWorkRequired
                : selectedPlan.totalWork();
    }
    public final String pausedReason() {
        if (unsupportedProcessingVersion.isPresent()) {
            return ProcessingMachineDisplayData.UNSUPPORTED_VERSION;
        }
        if (materialQuarantine.isPresent()) {
            return "material_quarantined";
        }
        return unsupportedInventorySlots.isPresent()
                ? ProcessingMachineDisplayData.INVENTORY_LAYOUT_QUARANTINED
                : runtime.status();
    }
    public final int statusArgument() {
        return unsupportedProcessingVersion.or(() -> unsupportedInventorySlots).orElse(0);
    }
    public final long resourceRevision() { return resourceRevision; }

    @Override
    public final PipeCoverSet covers() {
        return covers;
    }

    @Override
    public final boolean setCover(Direction side, PipeCover cover) {
        if (side == null
                || cover == null
                || !MachineCoverBehaviors.canPlace(this, side, cover)) {
            return false;
        }
        boolean changed = covers.set(side, cover);
        if (changed) {
            markMutation();
            syncCoverUpdate(side);
        }
        return changed;
    }

    @Override
    public final void replaceCover(Direction side, PipeCover cover) {
        if (side == null) {
            return;
        }
        if (covers.set(side, cover)) {
            markMutation();
            syncCoverUpdate(side);
        }
    }

    @Override
    public final boolean configureCover(
            Direction side,
            CoverDefinition.ConfigField field,
            int value) {
        if (side == null || field == null) {
            return false;
        }
        if (!covers.configure(side, field, value)) {
            return false;
        }
        markMutation();
        syncCoverUpdate(side);
        return true;
    }

    @Override
    public final boolean removeCover(
            Direction side,
            net.minecraft.world.entity.player.Player player) {
        if (side == null) {
            return false;
        }
        Optional<PipeCover> taken = covers.take(side);
        if (taken.isEmpty()) {
            return false;
        }
        ItemStack stack = PipeCoverItems.stackFor(taken.orElseThrow());
        if (!stack.isEmpty()) {
            if (player == null || !player.addItem(stack)) {
                if (level != null && !level.isClientSide) {
                    Block.popResource(level, worldPosition, stack);
                }
            }
        }
        markMutation();
        syncCoverUpdate(side);
        return true;
    }

    public final void dropCovers() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (ItemStack stack : covers.removeAllAsItems()) {
            Block.popResource(level, worldPosition, stack);
        }
        markMutation();
        syncCoverUpdate(null);
    }

    @Override
    public final boolean coverEnabled() {
        return coverEnabled;
    }

    @Override
    public final void setCoverEnabled(boolean enabled) {
        if (coverEnabled != enabled) {
            coverEnabled = enabled;
            markMutation();
            notifyRedstone();
        }
    }

    @Override
    public final boolean coversStopped() {
        return coversStopped;
    }

    @Override
    public final void setCoversStopped(boolean stopped) {
        if (coversStopped != stopped) {
            coversStopped = stopped;
            markMutation();
        }
    }

    @Override
    public final boolean canTick() {
        return level != null && !level.isClientSide;
    }

    @Override
    public final boolean runningPossible() {
        return findRecipe().isPresent()
                || runningActively()
                || progress() > 0
                || workProgress > 0L
                || (runtime.active() && duration() > 0);
    }

    @Override
    public final boolean runningPassively() {
        String status = runtime.status();
        return runtime.active()
                && !"idle".equals(status)
                && !"underpowered".equals(status);
    }

    @Override
    public final boolean runningActively() {
        return runtime.active() && runtime.status().isEmpty();
    }

    @Override
    public final boolean runningSuccessfully() {
        return level != null
                && lastSuccessfulGameTime == level.getGameTime();
    }

    @Override
    public final int selectorMode() {
        return selectorMode;
    }

    @Override
    public final void setSelectorMode(int mode) {
        int bounded = Math.max(0, Math.min(15, mode));
        if (selectorMode != bounded) {
            selectorMode = bounded;
            recipeCache.invalidate();
            coverRecipeCache = null;
            coverRecipeCacheTick = Long.MIN_VALUE;
            markMutation();
        }
    }

    @Override
    public final boolean hasEnergyBuffer() {
        return energy != null;
    }

    @Override
    public final long energyStored() {
        return energy == null ? 0L : energy.stored();
    }

    @Override
    public final long energyCapacity() {
        return energy == null ? 0L : energy.capacity();
    }

    @Override
    public final boolean hasFluidTanks() {
        return !tanks.isEmpty();
    }

    @Override
    public final int fillAir(int amount) {
        if (amount <= 0 || tanks.isEmpty() || level == null) {
            return 0;
        }
        Optional<Fluid> air = MachineCoverBehaviors.ventAir(level, worldPosition);
        if (air.isEmpty()) {
            return 0;
        }
        List<FluidTank> targets = spec.fluids().inputs().isEmpty()
                ? tanks
                : spec.fluids().inputs().stream()
                        .map(tank -> tanks.get(tank.index()))
                        .toList();
        int remaining = amount;
        int filled = 0;
        Fluid fluid = air.orElseThrow();
        for (FluidTank tank : targets) {
            if (remaining <= 0) {
                break;
            }
            FluidStack current = tank.getFluid();
            if (!current.isEmpty() && !current.is(fluid)) {
                continue;
            }
            int space = tank.getCapacity() - current.getAmount();
            int accepted = Math.min(remaining, space);
            if (accepted <= 0) {
                continue;
            }
            tank.setFluid(new FluidStack(fluid, current.getAmount() + accepted));
            filled += accepted;
            remaining -= accepted;
        }
        return filled;
    }

    @Override
    public final int incomingRedstone(Direction side) {
        if (level == null || side == null) {
            return 0;
        }
        BlockPos neighbor = worldPosition.relative(side);
        return Math.max(
                level.getSignal(neighbor, side.getOpposite()),
                level.getDirectSignal(neighbor, side.getOpposite()));
    }

    @Override
    public final void notifyRedstone() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        for (Direction side : Direction.values()) {
            level.updateNeighborsAt(
                    worldPosition.relative(side),
                    getBlockState().getBlock());
        }
    }

    private void syncCoverUpdate(Direction side) {
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                Block.UPDATE_CLIENTS);
        notifyRedstone();
    }

    @Override
    public final long gameTime() {
        return level == null ? 0L : level.getGameTime();
    }

    @Override
    public final Level level() {
        return level;
    }

    @Override
    public final BlockPos hostPos() {
        return worldPosition;
    }

    final boolean processingAllowed() {
        return unsupportedProcessingVersion.isEmpty()
                && unsupportedInventorySlots.isEmpty()
                && materialQuarantine.isEmpty()
                && coverEnabled;
    }

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
        coverRecipeCache = null;
        coverRecipeCacheTick = Long.MIN_VALUE;
        inventoryChangedForAutoIo = true;
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
                restoredRollValid =
                        selectedRecipeFingerprint.isPresent();
                restoredRecipeFingerprint =
                        selectedRecipeFingerprint.orElse("");
                restoredOperations = selectedOperations;
                restoredWorkProgress = workProgress;
                restoredWorkRequired = selectedPlan == null
                        ? 0L
                        : selectedPlan.totalWork();
                markMutation();
            }
            selectedMatch = null;
            selectedPlan = null;
            selectedRecipeFingerprint = Optional.empty();
            selectedMapRevision = -1L;
        }
    }

    private boolean clearSelection() {
        boolean changed = selectedMatch != null
                || restoredRollValid
                || !restoredSelectedId.isEmpty()
                || workProgress != 0L
                || restoredWorkProgress != 0L
                || !rolledOutputs.isEmpty();
        selectedMatch = null;
        selectedPlan = null;
        selectedRecipeFingerprint = Optional.empty();
        selectedOperations = 1;
        workProgress = 0L;
        selectedMapRevision = -1L;
        restoredSelectedId = "";
        restoredRollValid = false;
        restoredRecipeFingerprint = "";
        restoredOperations = 1;
        restoredWorkProgress = 0L;
        restoredWorkRequired = 0L;
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
        boolean quarantinedIdentity = materialQuarantine.isPresent();
        MachineIdentityPolicy.Identity identity = quarantinedIdentity
                ? savedMachineIdentity
                : ModMachineIdentities.identityOf(variant);
        tag.merge(new ProcessingMachineState(
                runtime.processor().activeId(),
                progress(),
                duration(),
                runtime.status(),
                powerDemand,
                energy == null
                        ? preservedUnbufferedEnergy
                        : energy.stored(),
                resourceRevision,
                identity.machineKind(),
                identity.tierBand(),
                identity.materialId(),
                identity.energyIdentity(),
                selectedPlan == null
                        ? restoredOperations
                        : selectedOperations,
                selectedPlan == null
                        ? restoredWorkProgress
                        : workProgress,
                selectedPlan == null
                        ? restoredWorkRequired
                        : selectedPlan.totalWork(),
                persistedIdentityQuarantine,
                unsupportedProcessingVersion).write());
        tag.put("inventory", inventory.serializeNBT(registries));
        for (int tank = 0; tank < tanks.size(); tank++) {
            tag.put("tank_" + tank, tanks.get(tank).writeToNBT(registries, new CompoundTag()));
        }
        Optional<String> selectedFingerprint = selectedMatch == null
                ? Optional.empty()
                : selectedRecipeFingerprint;
        new ChanceOutputState(
                selectedMatch != null ? selectedFingerprint.isPresent() : restoredRollValid,
                selectedMatch != null ? selectedMatch.id().toString() : restoredSelectedId,
                selectedMatch != null
                        ? selectedFingerprint.orElse("")
                        : restoredRecipeFingerprint,
                rolledOutputs).write(tag, registries);
        tag.putBoolean("machine_cover_enabled", coverEnabled);
        tag.putBoolean("machine_covers_stopped", coversStopped);
        tag.putInt("machine_selector_mode", selectorMode);
        tag.putBoolean("gt6_disabled_item_in", disabledItemInput);
        tag.putBoolean("gt6_disabled_item_out", disabledItemOutput);
        tag.putBoolean("gt6_disabled_fluid_in", disabledFluidInput);
        tag.putBoolean("gt6_disabled_fluid_out", disabledFluidOutput);
        tag.putInt("gt6_screwdriver_mode", screwdriverMode);
        CompoundTag coverTag = new CompoundTag();
        covers.save(coverTag, registries);
        tag.put("machine_covers", coverTag);
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
        MachineIdentityPolicy.Decision identity =
                ModMachineIdentities.resolve(variant, state);
        savedMachineIdentity = identity.persistedIdentity();
        persistedIdentityQuarantine = state.identityQuarantine();
        materialQuarantine = identity.quarantineReason();
        restoredOperations = state.operations();
        restoredWorkProgress = state.workProgress();
        restoredWorkRequired = state.workRequired();
        ChanceOutputState chanceState = ChanceOutputState.read(tag, registries);
        restoredSelectedId = chanceState.recipeId().isEmpty()
                ? runtime.processor().activeId()
                : chanceState.recipeId();
        restoredRollValid = chanceState.valid();
        restoredRecipeFingerprint = chanceState.recipeFingerprint();
        powerDemand = state.powerDemand();
        if (energy != null) {
            energy.restore(state.energy());
            preservedUnbufferedEnergy = 0L;
        } else {
            preservedUnbufferedEnergy = state.energy();
        }
        resourceRevision = Math.max(resourceRevision, state.resourceRevision());
        rolledOutputs = chanceState.outputs();
        coverEnabled = !tag.contains("machine_cover_enabled")
                || tag.getBoolean("machine_cover_enabled");
        coversStopped = tag.getBoolean("machine_covers_stopped");
        selectorMode = Math.max(
                0,
                Math.min(15, tag.getInt("machine_selector_mode")));
        disabledItemInput = tag.getBoolean("gt6_disabled_item_in");
        disabledItemOutput = tag.getBoolean("gt6_disabled_item_out");
        disabledFluidInput = tag.getBoolean("gt6_disabled_fluid_in");
        disabledFluidOutput = tag.getBoolean("gt6_disabled_fluid_out");
        screwdriverMode = Math.max(0, Math.min(3, tag.getInt("gt6_screwdriver_mode")));
        if (tag.contains("machine_covers")) {
            covers.load(
                    tag.getCompound("machine_covers"),
                    registries);
        }
        selectedMatch = null;
        selectedPlan = null;
        selectedRecipeFingerprint = Optional.empty();
        selectedMapRevision = -1L;
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
        materialQuarantine.ifPresent(reason ->
                tag.putString("material_quarantine", reason));
        tag.putLong("power_demand", powerDemand);
        tag.putLong("energy", energy == null ? 0L : energy.stored());
        tag.putBoolean("machine_cover_enabled", coverEnabled);
        tag.putBoolean("machine_covers_stopped", coversStopped);
        tag.putInt("machine_selector_mode", selectorMode);
        CompoundTag coverTag = new CompoundTag();
        covers.save(coverTag, registries);
        tag.put("machine_covers", coverTag);
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
        materialQuarantine = tag.contains("material_quarantine")
                ? Optional.of(tag.getString("material_quarantine"))
                : Optional.empty();
        powerDemand = Math.max(0L, tag.getLong("power_demand"));
        if (energy != null) {
            energy.restore(tag.getLong("energy"));
        }
        coverEnabled = !tag.contains("machine_cover_enabled")
                || tag.getBoolean("machine_cover_enabled");
        coversStopped = tag.getBoolean("machine_covers_stopped");
        selectorMode = Math.max(
                0,
                Math.min(15, tag.getInt("machine_selector_mode")));
        if (tag.contains("machine_covers")) {
            covers.load(
                    tag.getCompound("machine_covers"),
                    registries);
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

    private static MachineEnergyBuffer energyBufferFor(ProcessingMachineSpec spec) {
        if (spec.energy().mode() == ProcessingMachineSpec.EnergyMode.BUFFERED) {
            return new MachineEnergyBuffer(
                    spec.energy().capacity(), spec.energy().maxPacket());
        }
        if (spec.energy().mode() == ProcessingMachineSpec.EnergyMode.ADJACENT
                && spec.energy().type() == EnergyType.HEAT) {
            long packet = Math.max(1L, spec.energy().maxPacket());
            long capacity = spec.energy().capacity() > 0L
                    ? Math.max(spec.energy().capacity(), packet)
                    : Math.multiplyExact(packet, 32L);
            return new MachineEnergyBuffer(capacity, packet);
        }
        return null;
    }

    private record PowerPlan(
            long energyUnits,
            long workUnits,
            BooleanSupplier executePower) {
        private PowerPlan {
            if (energyUnits < 0L
                    || workUnits <= 0L
                    || executePower == null) {
                throw new IllegalArgumentException(
                        "Invalid processing power plan");
            }
        }
    }

    private record PlannedExecution(
            int operations, MachineExecutionPlan plan) {}
}
