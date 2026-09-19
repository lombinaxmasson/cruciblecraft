package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.FuelGeneratorBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.energy.converter.BurningBoxWorldEffects;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorEnergy;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorIdentityPolicy;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.material.ChemicalFluidRegistrationGate;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.steam.ExactFluidTransfer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * Signed-eut fuel consumer. Recipe power is unit-buffered independently from
 * the configured output packet, and every exhaust/energy mutation is gated by
 * a simulation-first check.
 */
public final class FuelGeneratorBlockEntity extends MachineCoverHostBlockEntity
        implements IEnergyHandler {
    private static final String SCHEMA_KEY =
            "fuel_generator_schema_version";
    private static final String IDENTITY_KEY = "fuel_generator_id";
    private static final String ENERGY_IDENTITY_KEY = "energy_identity";
    private static final String QUARANTINE_KEY =
            "fuel_generator_quarantine";
    /** GT6 {@code MultiTileEntityGeneratorLiquid} {@code mCooldown = 100}. */
    private static final int HEAT_EMIT_COOLDOWN_TICKS = 100;
    /**
     * GT6 {@code TE_Behavior_Active_Trinary.mData == 0}: leftover known fuel
     * is voided after 64 inactive ticks so a different fuel can be filled.
     */
    private static final int LEFTOVER_FUEL_SWAP_TICKS = 64;
    private final FuelGeneratorSpec spec;
    private final FuelGeneratorEnergy energy;
    private final FluidTank input;
    private final List<FluidTank> outputs;
    private final IFluidHandler inputView = new InputHandler();
    private final List<IFluidHandler> outputViews;
    private final PerTickEnergyBudget outputBudget =
            new PerTickEnergyBudget();
    private ResourceLocation activeRecipe;
    private int progress;
    private int duration;
    private String status = "idle";
    private boolean clientSyncPending;
    private long lastClientSyncGameTime = Long.MIN_VALUE;
    private FuelGeneratorIdentityPolicy.Identity persistedIdentity;
    private String identityQuarantine = "";
    private boolean quarantineWarningLogged;
    private int heatEmitCooldown;
    private boolean burning;
    private boolean stopped;
    private int leftoverIdleTicks;

    public FuelGeneratorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUEL_GENERATOR.get(), pos, state);
        if (!(state.getBlock() instanceof FuelGeneratorBlock block)) {
            throw new IllegalArgumentException(
                    "Fuel generator block entity requires a configured block");
        }
        spec = block.spec();
        energy = new FuelGeneratorEnergy(
                spec.outputPacketSize(), spec.energyCapacity());
        persistedIdentity = FuelGeneratorIdentityPolicy.current(spec);
        input = new FluidTank(
                spec.inputCapacityMb(), spec::acceptsInput) {
            @Override
            protected void onContentsChanged() {
                markPersistentMutation();
            }
        };
        List<FluidTank> created = new ArrayList<>();
        for (int index = 0; index < spec.outputTanks(); index++) {
            created.add(new FluidTank(spec.outputCapacityMb()) {
                @Override
                protected void onContentsChanged() {
                    markPersistentMutation();
                }
            });
        }
        outputs = List.copyOf(created);
        List<IFluidHandler> views = new ArrayList<>();
        for (int index = 0; index < outputs.size(); index++) {
            views.add(new OutputHandler(index));
        }
        outputViews = List.copyOf(views);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!quarantineWarningLogged
                && !identityQuarantine.isBlank()
                && level != null
                && !level.isClientSide) {
            quarantineWarningLogged = true;
            CrucibleCraft.LOGGER.warn(
                    "Quarantined fuel generator {} at {} {}: {}",
                    spec.id(),
                    level.dimension().location(),
                    worldPosition,
                    identityQuarantine);
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FuelGeneratorBlockEntity generator) {
        generator.tickCovers();
        generator.tickGeneration();
        generator.pushExhaust();
        emitOutput(level, pos, generator);
        generator.updateLitState();
        generator.flushClientSync(level.getGameTime());
    }

    private static void emitOutput(
            Level level, BlockPos pos, FuelGeneratorBlockEntity generator) {
        EnergyType type = generator.spec.outputEnergyType();
        Direction side = generator.energyOutputSide();
        if (!generator.identityQuarantine.isBlank()) {
            return;
        }
        if (type.sizeIrrelevant()) {
            boolean live = generator.burning || generator.heatEmitCooldown > 0;
            if (generator.heatEmitCooldown > 0) {
                generator.heatEmitCooldown--;
            }
            long rate = generator.spec.maximumOutputPacketsPerTick();
            if (live && generator.energy.stored() >= rate) {
                BurningBoxWorldEffects.trySpreadFlame(
                        level, pos, generator.spec.efficiencyBps());
            }
            if ((generator.burning || generator.heatEmitCooldown > 0)
                    && generator.energy.stored() < rate * 2L) {
                Direction facing = generator.front();
                if (facing != null) {
                    BurningBoxWorldEffects.burnFront(
                            level, pos.relative(facing));
                }
            }
            if (!live || generator.energy.stored() < rate) {
                return;
            }
            long offered = Math.min(rate, generator.energy.stored());
            EnergyEmitter.pushToSide(
                    level,
                    pos,
                    type,
                    type.emitPacketSize(generator.spec.outputPacketSize()),
                    offered,
                    side);
            generator.energy.discardUnits(rate);
            generator.markPersistentMutation();
            return;
        }
        long packet = generator.spec.outputPacketSize();
        if (generator.energy.stored() < packet) {
            return;
        }
        EnergyEmitter.pushToSide(
                level,
                pos,
                type,
                packet,
                1L,
                side);
        generator.energy.discardUnits(packet);
        generator.markPersistentMutation();
    }

    private void tickGeneration() {
        if (level == null || level.isClientSide) {
            return;
        }
        if (!identityQuarantine.isBlank()) {
            setStatus("identity_quarantined");
            return;
        }
        if (!spec.requiresIgnition() && stopped) {
            setStatus("stopped");
            return;
        }
        if (spec.requiresIgnition()) {
            if (!burning && heatEmitCooldown <= 0) {
                Direction facing = front();
                if (facing != null
                        && BurningBoxWorldEffects.tryAutoIgnite(
                                level, worldPosition.relative(facing))) {
                    burning = true;
                    markPersistentMutation();
                }
            }
            if (!burning && heatEmitCooldown <= 0) {
                if (input.isEmpty() && activeRecipe == null) {
                    reset("idle");
                }
                return;
            }
            Direction facing = front();
            if (facing == null
                    || !BurningBoxWorldEffects.hasFrontAir(
                            level, worldPosition.relative(facing))) {
                if (facing != null) {
                    BurningBoxWorldEffects.burnFront(
                            level, worldPosition.relative(facing));
                }
                burning = false;
                heatEmitCooldown = 0;
                setStatus("no_air");
                markPersistentMutation();
                return;
            }
        }
        GTRecipe recipe;
        if (activeRecipe == null) {
            if (input.isEmpty()) {
                leftoverIdleTicks = 0;
                if (spec.requiresIgnition()) {
                    burning = false;
                }
                reset("idle");
                return;
            }
            RecipeMap.Match match = findFuel(false);
            if (match == null) {
                if (spec.requiresIgnition()) {
                    burning = false;
                }
                voidUnusableFuel();
                return;
            }
            leftoverIdleTicks = 0;
            recipe = match.recipe();
            if (spec.validate(recipe).isPresent()) {
                reset("invalid_recipe");
                return;
            }
            if (!hasOutputRoom(recipe)) {
                setStatus("exhaust_blocked");
                return;
            }
            long firstTickEnergy =
                    spec.generatedEnergyAtTick(recipe, 0);
            if (firstTickEnergy > 0L
                    && !energy.canGenerate(firstTickEnergy)) {
                setStatus("energy_output_blocked");
                return;
            }
            consumeFuel(recipe);
            activeRecipe = match.id();
            progress = 0;
            duration = recipe.duration();
            markPersistentMutation();
        } else {
            recipe = spec.requireRecipeMap().entry(activeRecipe)
                    .map(RecipeMap.Entry::recipe)
                    .orElse(null);
            if (recipe == null || spec.validate(recipe).isPresent()) {
                reset("invalid_recipe");
                return;
            }
        }
        if (!hasOutputRoom(recipe)) {
            setStatus("exhaust_blocked");
            return;
        }
        long generatedThisTick =
                spec.generatedEnergyAtTick(recipe, progress);
        if (generatedThisTick > 0L
                && !energy.canGenerate(generatedThisTick)) {
            setStatus("energy_output_blocked");
            return;
        }
        boolean completes = progress + 1 >= duration;
        if (completes) {
            commitOutputs(recipe);
        }
        if (generatedThisTick > 0L) {
            energy.generate(generatedThisTick);
        }
        progress++;
        setStatus("running");
        if (completes) {
            progress = 0;
            activeRecipe = null;
            duration = 0;
        }
        if (spec.requiresIgnition()
                && energy.stored() < spec.maximumOutputPacketsPerTick()) {
            burning = false;
        }
        markTickMutation();
    }

    /**
     * GT6 {@code MultiTileEntityMotorLiquid}: {@code FL.move} exhaust to
     * {@code OPOS[mFacing]}, then vent leftover gas when that cell has no
     * collision.
     */
    private void pushExhaust() {
        if (level == null || level.isClientSide || !spec.pushesExhaust()) {
            return;
        }
        Direction front = front();
        if (front == null) {
            return;
        }
        List<Direction> sides = spec.resolvedExhaustSides(front);
        boolean changed = false;
        for (int index = 0; index < outputs.size() && index < sides.size(); index++) {
            FluidTank tank = outputs.get(index);
            if (tank.isEmpty()) {
                continue;
            }
            Direction side = sides.get(index);
            BlockPos target = worldPosition.relative(side);
            if (!level.hasChunkAt(target)) {
                continue;
            }
            IFluidHandler neighbor = level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    target,
                    side.getOpposite());
            if (neighbor != null
                    && ExactFluidTransfer.move(
                            tank, neighbor, tank.getFluidAmount()) > 0) {
                changed = true;
            }
            if (!tank.isEmpty()
                    && gaseous(tank.getFluid())
                    && !hasCollision(level, target)) {
                tank.setFluid(FluidStack.EMPTY);
                changed = true;
            }
        }
        if (changed) {
            markPersistentMutation();
        }
    }

    private static boolean gaseous(FluidStack stack) {
        return !stack.isEmpty()
                && ModFluids.chemicalState(stack.getFluid())
                        .orElse(ChemicalFluidRegistrationGate.State.LIQUID)
                        == ChemicalFluidRegistrationGate.State.GAS;
    }

    private static boolean hasCollision(Level level, BlockPos pos) {
        return !level.getBlockState(pos)
                .getCollisionShape(level, pos)
                .isEmpty();
    }

    private boolean hasOutputRoom(GTRecipe recipe) {
        List<FluidStack> hosted = spec.hostedFluidOutputs(recipe);
        for (int index = 0; index < hosted.size(); index++) {
            FluidStack output = hosted.get(index);
            if (outputs.get(index).fill(
                            output, IFluidHandler.FluidAction.SIMULATE)
                    != output.getAmount()) {
                return false;
            }
        }
        return true;
    }

    private RecipeMap.Match findFuel(boolean ignoreAmount) {
        FluidStack offered = input.getFluid();
        if (offered.isEmpty()) {
            return null;
        }
        FluidStack query = ignoreAmount
                ? offered.copyWithAmount(Integer.MAX_VALUE)
                : offered;
        return spec.requireRecipeMap().findMatch(
                        new GTRecipeQuery(List.of(), List.of(query)))
                .orElse(null);
    }

    /**
     * GT6 MotorLiquid/HotFluid: unknown fluid is voided immediately; leftover
     * of a known fuel waits 64 inactive ticks. Burning boxes void both at
     * once while they are trying to burn.
     */
    private void voidUnusableFuel() {
        boolean knownType = findFuel(true) != null;
        boolean delaySwap = knownType && !spec.requiresIgnition();
        if (delaySwap) {
            leftoverIdleTicks++;
            if (leftoverIdleTicks < LEFTOVER_FUEL_SWAP_TICKS) {
                reset("invalid_fuel");
                return;
            }
        }
        leftoverIdleTicks = 0;
        input.setFluid(FluidStack.EMPTY);
        reset("idle");
    }

    /**
     * GT6 MotorLiquid: trash the exhaust tank if it holds anything, otherwise
     * the whole fuel tank. GeneratorLiquid / gas boxes only have the fuel tank.
     */
    public boolean trashWithPlunger() {
        if (spec.pushesExhaust()) {
            for (FluidTank tank : outputs) {
                if (tank.isEmpty()) {
                    continue;
                }
                tank.setFluid(FluidStack.EMPTY);
                markPersistentMutation();
                return true;
            }
        }
        if (input.isEmpty()) {
            return false;
        }
        input.setFluid(FluidStack.EMPTY);
        leftoverIdleTicks = 0;
        markPersistentMutation();
        return true;
    }

    private void consumeFuel(GTRecipe recipe) {
        FluidStack required = recipe.fluidInputs().getFirst();
        FluidStack drained = input.drain(
                required, IFluidHandler.FluidAction.EXECUTE);
        if (drained.getAmount() != required.getAmount()
                || !FluidStack.isSameFluidSameComponents(
                        drained, required)) {
            throw new IllegalStateException(
                    "Fuel changed after generator simulation");
        }
        if (spec.outputEnergyType().sizeIrrelevant()) {
            heatEmitCooldown = HEAT_EMIT_COOLDOWN_TICKS;
            if (spec.requiresIgnition()) {
                burning = true;
            }
        }
    }

    private void commitOutputs(GTRecipe recipe) {
        List<FluidStack> hosted = spec.hostedFluidOutputs(recipe);
        for (int index = 0; index < hosted.size(); index++) {
            FluidStack output = hosted.get(index);
            if (outputs.get(index).fill(
                            output, IFluidHandler.FluidAction.EXECUTE)
                    != output.getAmount()) {
                throw new IllegalStateException(
                        "Generator exhaust changed after simulation");
            }
        }
    }

    private void reset(String nextStatus) {
        boolean changed = activeRecipe != null
                || progress != 0
                || duration != 0
                || !status.equals(nextStatus);
        activeRecipe = null;
        progress = 0;
        duration = 0;
        status = nextStatus;
        if (changed) {
            markPersistentMutation();
        }
    }

    public IFluidHandler fluids(Direction side) {
        Direction front = front();
        if (side == null || front == null) {
            return null;
        }
        int outputIndex = spec.resolvedExhaustSides(front).indexOf(side);
        if (outputIndex >= 0) {
            return outputViews.get(outputIndex);
        }
        return side == energyOutputSide() ? null : inputView;
    }

    public String status() {
        return status;
    }

    public int progress() {
        return progress;
    }

    @Override
    public int duration() {
        return duration;
    }

    public boolean stopped() {
        return stopped;
    }

    @Override
    public boolean switchableOnOff() {
        return !spec.requiresIgnition()
                || spec.inputPhase() == FuelGeneratorSpec.InputPhase.GAS;
    }

    @Override
    public boolean getStateOnOff() {
        if (spec.inputPhase() == FuelGeneratorSpec.InputPhase.GAS) {
            return burning;
        }
        return !stopped;
    }

    @Override
    public boolean setStateOnOff(boolean on) {
        if (spec.inputPhase() == FuelGeneratorSpec.InputPhase.GAS) {
            if (burning && !on) {
                burning = false;
                heatEmitCooldown = 0;
                markPersistentMutation();
            }
            return burning;
        }
        if (!spec.requiresIgnition()) {
            boolean nextStopped = !on;
            if (stopped != nextStopped) {
                stopped = nextStopped;
                markPersistentMutation();
            }
            return !stopped;
        }
        return true;
    }

    @Override
    public boolean runningActively() {
        return burning || activeRecipe != null || progress > 0;
    }

    @Override
    public boolean hasFluidTanks() {
        return true;
    }

    @Override
    protected void onHostChanged() {
        markPersistentMutation();
    }

    public int inputAmount() {
        return input.getFluidAmount();
    }

    public int outputAmount(int index) {
        return outputs.get(index).getFluidAmount();
    }

    public long energyStored() {
        return energy.stored();
    }

    public long energyCapacity() {
        return energy.capacity();
    }

    public boolean tryIgnite(
            Player player,
            InteractionHand hand,
            Direction hitFace,
            ItemStack stack) {
        if (!spec.requiresIgnition()
                || hitFace == null
                || hitFace != front()
                || !stack.is(Items.FLINT_AND_STEEL)) {
            return false;
        }
        ignite();
        stack.hurtAndBreak(
                1,
                player,
                net.minecraft.world.entity.LivingEntity.getSlotForHand(hand));
        if (level != null) {
            level.playSound(
                    null,
                    worldPosition,
                    SoundEvents.FLINTANDSTEEL_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
        }
        return true;
    }

    /** GameTest / flint: GT6 igniter sets burning and cooldown 100. */
    public void ignite() {
        if (!spec.requiresIgnition()) {
            return;
        }
        burning = true;
        heatEmitCooldown = HEAT_EMIT_COOLDOWN_TICKS;
        markPersistentMutation();
    }

    public boolean burning() {
        return burning;
    }

    /** GameTest helper: fill the remaining HU/RU buffer without a recipe. */
    public boolean seedStoredEnergy(long units) {
        long room = energy.capacity() - energy.stored();
        if (units <= 0L || room <= 0L) {
            return energy.stored() > 0L;
        }
        energy.generate(Math.min(units, room));
        setStatus("running");
        markPersistentMutation();
        return true;
    }

    public void drainStoredEnergy() {
        energy.restore(new FuelGeneratorEnergy.State(
                0L, energy.generated(), energy.extracted()));
        setStatus("idle");
        markPersistentMutation();
    }

    public long energyGenerated() {
        return energy.generated();
    }

    public long energyExtracted() {
        return energy.extracted();
    }

    public EnergyType outputEnergyType() {
        return spec.outputEnergyType();
    }

    public boolean identityQuarantined() {
        return !identityQuarantine.isBlank();
    }

    public String identityQuarantine() {
        return identityQuarantine;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return identityQuarantine.isBlank()
                && type == spec.outputEnergyType()
                && side != null
                && side == energyOutputSide();
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && energy.stored() >= spec.outputPacketSize()
                        && outputBudget.claim(
                                        budgetGameTime(),
                                        1L,
                                        spec.maximumOutputPacketsPerTick(),
                                        true)
                                > 0L
                ? spec.outputPacketSize()
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || size != spec.outputPacketSize()
                || maximum <= 0L
                || energy.stored() < spec.outputPacketSize()
                || outputBudget.claim(
                                budgetGameTime(),
                                maximum,
                                spec.maximumOutputPacketsPerTick(),
                                true)
                        <= 0L) {
            return 0L;
        }
        long available = energy.extract(
                size,
                outputBudget.claim(
                        budgetGameTime(),
                        maximum,
                        spec.maximumOutputPacketsPerTick(),
                        true),
                true);
        if (!simulate && level != null && !level.isClientSide) {
            long claimed = outputBudget.claim(
                    budgetGameTime(),
                    available,
                    spec.maximumOutputPacketsPerTick(),
                    false);
            if (claimed != available
                    || energy.extract(size, available, false)
                            != available) {
                throw new IllegalStateException(
                        "Generator output changed after simulation");
            }
            markPersistentMutation();
        }
        return available;
    }

    @Override
    public long stored(EnergyType type) {
        return type == spec.outputEnergyType()
                ? energy.stored()
                : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == spec.outputEnergyType()
                ? spec.energyCapacity()
                : 0L;
    }

    private long budgetGameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(FuelGeneratorBlock.FACING)
                ? state.getValue(FuelGeneratorBlock.FACING)
                : null;
    }

    private Direction energyOutputSide() {
        return spec.energyOutputSide(front());
    }

    private void setStatus(String nextStatus) {
        if (!status.equals(nextStatus)) {
            status = nextStatus;
            markTickMutation();
        }
    }

    private void updateLitState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean lit = "running".equals(status)
                || (spec.requiresIgnition() && burning);
        if (state.hasProperty(FuelGeneratorBlock.LIT)
                && state.getValue(FuelGeneratorBlock.LIT) != lit) {
            level.setBlock(
                    worldPosition,
                    state.setValue(FuelGeneratorBlock.LIT, lit),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void markPersistentMutation() {
        setChanged();
        markTickMutation();
    }


    private void markTickMutation() {
        clientSyncPending = true;
    }

    private void flushClientSync(long gameTime) {
        if (!clientSyncPending
                || lastClientSyncGameTime != Long.MIN_VALUE
                        && gameTime - lastClientSyncGameTime < 20L) {
            return;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
        clientSyncPending = false;
        lastClientSyncGameTime = gameTime;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("input", input.writeToNBT(
                registries, new CompoundTag()));
        for (int index = 0; index < outputs.size(); index++) {
            tag.put("output_" + index, outputs.get(index).writeToNBT(
                    registries, new CompoundTag()));
        }
        FuelGeneratorEnergy.State energyState = energy.snapshot();
        tag.putLong("energy", energyState.stored());
        tag.putLong("energy_generated", energyState.generated());
        tag.putLong("energy_extracted", energyState.extracted());
        tag.putLong("packet_size", spec.outputPacketSize());
        if (activeRecipe != null) {
            tag.putString("active_recipe", activeRecipe.toString());
        }
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        tag.putString("status", status);
        tag.putInt(
                SCHEMA_KEY, persistedIdentity.schemaVersion());
        tag.putString(
                IDENTITY_KEY, persistedIdentity.generatorId());
        tag.putString(
                ENERGY_IDENTITY_KEY,
                persistedIdentity.energyIdentity());
        if (!identityQuarantine.isBlank()) {
            tag.putString(QUARANTINE_KEY, identityQuarantine);
        }
        tag.putInt("heat_emit_cooldown", heatEmitCooldown);
        tag.putBoolean("burning", burning);
        tag.putBoolean("stopped", stopped);
        tag.putInt("leftover_idle_ticks", leftoverIdleTicks);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        FuelGeneratorIdentityPolicy.Identity savedIdentity =
                readIdentity(tag);
        FuelGeneratorIdentityPolicy.Decision identity =
                FuelGeneratorIdentityPolicy.resolve(
                        spec, savedIdentity);
        persistedIdentity = identity.persistedIdentity();
        identityQuarantine =
                identity.quarantineReason().orElse("");
        if (tag.contains("input")) {
            input.readFromNBT(registries, tag.getCompound("input"));
        }
        for (int index = 0; index < outputs.size(); index++) {
            if (tag.contains("output_" + index)) {
                outputs.get(index).readFromNBT(
                        registries,
                        tag.getCompound("output_" + index));
            }
        }
        long stored = Math.max(
                0L,
                Math.min(
                        spec.energyCapacity(),
                        tag.getLong("energy")));
        long generated = tag.contains(
                        "energy_generated", Tag.TAG_ANY_NUMERIC)
                ? Math.max(stored, tag.getLong("energy_generated"))
                : stored;
        long extracted = Math.max(
                0L, tag.getLong("energy_extracted"));
        energy.restore(new FuelGeneratorEnergy.State(
                stored, generated, extracted));
        activeRecipe = ResourceLocation.tryParse(
                tag.getString("active_recipe"));
        progress = Math.max(0, tag.getInt("progress"));
        duration = Math.max(0, tag.getInt("duration"));
        status = tag.getString("status");
        if (status.isBlank()) {
            status = "idle";
        }
        if (!identityQuarantine.isBlank()) {
            status = "identity_quarantined";
        }
        heatEmitCooldown = Math.max(0, tag.getInt("heat_emit_cooldown"));
        burning = tag.getBoolean("burning");
        stopped = tag.getBoolean("stopped");
        leftoverIdleTicks = Math.max(0, tag.getInt("leftover_idle_ticks"));
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("energy", energy.stored());
        tag.putString("status", status);
        tag.putInt("progress", progress);
        tag.putInt("duration", duration);
        if (!identityQuarantine.isBlank()) {
            tag.putString(QUARANTINE_KEY, identityQuarantine);
        }
        saveCoverNbt(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        energy.restore(new FuelGeneratorEnergy.State(
                Math.max(
                        0L,
                        Math.min(
                                spec.energyCapacity(),
                                tag.getLong("energy"))),
                energy.generated(),
                energy.extracted()));
        status = tag.getString("status");
        progress = Math.max(0, tag.getInt("progress"));
        duration = Math.max(0, tag.getInt("duration"));
        identityQuarantine = tag.getString(QUARANTINE_KEY);
        loadCoverNbt(tag, registries);
    }

    private FuelGeneratorIdentityPolicy.Identity readIdentity(
            CompoundTag tag) {
        boolean anyIdentity = tag.contains(SCHEMA_KEY)
                || tag.contains(IDENTITY_KEY)
                || tag.contains(ENERGY_IDENTITY_KEY);
        if (!anyIdentity) {
            return null;
        }
        return new FuelGeneratorIdentityPolicy.Identity(
                tag.contains(SCHEMA_KEY, Tag.TAG_ANY_NUMERIC)
                        ? tag.getInt(SCHEMA_KEY)
                        : FuelGeneratorIdentityPolicy
                                .MISSING_SCHEMA_VERSION,
                tag.contains(IDENTITY_KEY, Tag.TAG_STRING)
                        ? tag.getString(IDENTITY_KEY)
                        : "",
                tag.contains(ENERGY_IDENTITY_KEY, Tag.TAG_STRING)
                        ? tag.getString(ENERGY_IDENTITY_KEY)
                        : "");
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag(), registries);
        }
    }

    private final class InputHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0 ? input.getFluid() : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? input.getCapacity() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0 && input.isFluidValid(stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return input.fill(resource, action);
        }

        @Override
        public FluidStack drain(
                FluidStack resource, FluidAction action) {
            return FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            return FluidStack.EMPTY;
        }
    }

    private final class OutputHandler implements IFluidHandler {
        private final int outputIndex;

        private OutputHandler(int outputIndex) {
            this.outputIndex = outputIndex;
        }

        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            return tank == 0
                    ? outputs.get(outputIndex).getFluid()
                    : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0
                    ? outputs.get(outputIndex).getCapacity()
                    : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return false;
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return 0;
        }

        @Override
        public FluidStack drain(
                FluidStack resource, FluidAction action) {
            return outputs.get(outputIndex).drain(resource, action);
        }

        @Override
        public FluidStack drain(int maximum, FluidAction action) {
            return outputs.get(outputIndex).drain(maximum, action);
        }
    }
}
