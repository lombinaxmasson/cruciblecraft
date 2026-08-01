package com.masson.cruciblecraft.content.blockentity;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.fluid.MoltenTransferMath;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.FillPlan;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.machine.component.CompositionTank;
import com.masson.cruciblecraft.machine.component.MachineCasing;
import com.masson.cruciblecraft.machine.component.SteelmakingController;
import com.masson.cruciblecraft.machine.component.ThermalComponent;
import com.masson.cruciblecraft.recipe.AlloyIndex.AlloyMatch;
import com.masson.cruciblecraft.recipe.SteelmakingProcess;
import com.masson.cruciblecraft.recipe.SteelmakingProcess.Batch;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

public class CrucibleBlockEntity extends BlockEntity implements IEnergyHandler {
    public static final int MAX_INGOTS = 8;
    public static final float AMBIENT_TEMPERATURE = 20.0f;
    public static final long HEAT_DISPLAY_CAPACITY = 64L;
    private static final double CASING_VOLUME_CM3 = 800.0;
    private static final double CM3_PER_UNIT = 0.15;
    private final CompositionTank contents = new CompositionTank();
    private final MachineCasing casing = new MachineCasing(Device.CRUCIBLE, CASING_VOLUME_CM3);
    private final ThermalComponent thermal = new ThermalComponent(AMBIENT_TEMPERATURE);
    private final SteelmakingController steelmaking = new SteelmakingController();
    private final IFluidHandler externalFluids = new CrucibleFluidHandler();
    private final CheckpointTracker checkpoint = new CheckpointTracker();

    public CrucibleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CrucibleBlockEntity crucible) {
        long incomingEnergy = crucible.thermal.takePendingHeat();

        float previousTemperature = crucible.thermal.authoritativeTemperature();
        long previousStoredEnergy = crucible.thermal.storedEnergy();
        int previousCooldown = crucible.thermal.cooldownTicks();
        long previousAir = crucible.steelmaking.storedAir();
        int previousReactionTicks = crucible.steelmaking.reactionTicks();
        boolean meltedDown = incomingEnergy == 0L && crucible.isThermallyQuiescent()
                ? false
                : crucible.advance(incomingEnergy, true);
        if (meltedDown) {
            level.destroyBlock(pos, false);
            return;
        }
        boolean processChanged = Float.compare(
                        previousTemperature,
                        crucible.thermal.authoritativeTemperature()) != 0
                || previousStoredEnergy != crucible.thermal.storedEnergy()
                || previousCooldown != crucible.thermal.cooldownTicks()
                || previousAir != crucible.steelmaking.storedAir()
                || previousReactionTicks != crucible.steelmaking.reactionTicks();
        if (processChanged) {
            crucible.checkpoint.markDirty();
        }
        long phaseKey = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (crucible.checkpoint.shouldCheckpoint(level.getGameTime(), phaseKey, 20)) {
            crucible.setChanged();
            crucible.checkpoint.checkpointed();
        }
        if (crucible.checkpoint.shouldSync(
                crucible.isActiveProcess(),
                level.getGameTime(),
                phaseKey,
                20)) {
            crucible.syncToClient();
            crucible.checkpoint.synced();
        }
    }

    public static void clientTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CrucibleBlockEntity crucible) {
        crucible.thermal.clientTick();
    }

    private boolean advance(long incomingEnergy, boolean authoritative) {
        boolean unknownMaterials = hasUnknownMaterials();
        if (authoritative && !unknownMaterials) {
            boolean boiled = contents.removeBoiling(thermal.authoritativeTemperature());
            if (boiled) {
                markMutation();
                emitBoilingEffects();
            }
            if (steelmaking.tick(contents, thermal.authoritativeTemperature()).immediateMutation()) {
                markMutation();
            }
        }

        thermal.advance(
                unknownMaterials ? 0L : incomingEnergy,
                totalWeightGrams());

        if (thermal.authoritativeTemperature() > casingMaxTemperature()) {
            if (authoritative) {
                contents.clear();
            }
            return true;
        }
        return false;
    }

    private boolean isThermallyQuiescent() {
        return thermal.isQuiescent()
                && !steelmaking.hasState();
    }

    private boolean isActiveProcess() {
        return !isThermallyQuiescent();
    }

    private void emitBoilingEffects() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }
        serverLevel.playSound(
                null,
                worldPosition,
                SoundEvents.FIRE_EXTINGUISH,
                SoundSource.BLOCKS,
                0.7F,
                1.3F);
        serverLevel.sendParticles(
                ParticleTypes.CLOUD,
                worldPosition.getX() + 0.5,
                worldPosition.getY() + 0.8,
                worldPosition.getZ() + 0.5,
                8,
                0.2,
                0.08,
                0.2,
                0.02);
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return switch (type) {
            case HEAT -> side == Direction.DOWN;
            case AIR -> side != null && side.getAxis().isHorizontal();
            default -> false;
        };
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || size == 0L || amount <= 0L) {
            return 0L;
        }
        if (type == EnergyType.HEAT) {
            if (hasUnknownMaterials()) {
                return 0L;
            }
            long accepted = thermal.queueHeat(size, amount, simulate);
            if (!simulate && accepted > 0L) {
                checkpoint.markDirty();
            }
            return accepted;
        }
        if (level == null || !level.getBlockState(worldPosition.above()).isAir()) {
            return 0L;
        }
        SteelmakingController.InjectionResult acceptance = steelmaking.previewInjection(
                contents.composition(),
                thermal.authoritativeTemperature());
        if (!acceptsAir(acceptance)) {
            return 0L;
        }
        long room = Math.max(
                0L,
                AirOutputModel.MAX_STORED_AIR - steelmaking.storedAir());
        long accepted = Math.min(amount, EnergyPackets.packetsForUnits(size, room));
        if (!simulate && accepted > 0L) {
            steelmaking.insertAir(
                    EnergyPackets.units(size, accepted),
                    contents.composition(),
                    thermal.authoritativeTemperature());
            checkpoint.markDirty();
        }
        return accepted;
    }

    @Override
    public long stored(EnergyType type) {
        return switch (type) {
            case HEAT -> thermal.totalStoredHeat();
            case AIR -> Math.max(0L, steelmaking.storedAir());
            default -> 0L;
        };
    }

    @Override
    public long capacity(EnergyType type) {
        return switch (type) {
            case HEAT -> HEAT_DISPLAY_CAPACITY;
            case AIR -> AirOutputModel.MAX_STORED_AIR;
            default -> 0L;
        };
    }

    public AirInjectionResult injectAir(long air) {
        SteelmakingController.InjectionResult result = steelmaking.insertAir(
                air,
                contents.composition(),
                thermal.authoritativeTemperature());
        if (acceptsAir(result)) {
            checkpoint.markDirty();
        }
        return mapInjectionResult(result);
    }

    private static boolean acceptsAir(SteelmakingController.InjectionResult result) {
        return result == SteelmakingController.InjectionResult.STARTED
                || result == SteelmakingController.InjectionResult.CONTINUED;
    }

    private static AirInjectionResult mapInjectionResult(
            SteelmakingController.InjectionResult result) {
        return switch (result) {
            case STARTED -> AirInjectionResult.STARTED;
            case CONTINUED -> AirInjectionResult.CONTINUED;
            case TOO_COLD -> AirInjectionResult.TOO_COLD;
            case INVALID_CHARGE -> AirInjectionResult.INVALID_CHARGE;
        };
    }

    public InsertResult insert(MaterialUnits.Entry entry, float inputTemperature) {
        var plan = CrucibleTransferCoordinator.planInsertion(
                entry,
                totalUnits(),
                maxUnits(),
                casing.materialId());
        if (plan.result() != InsertResult.SUCCESS) {
            return plan.result();
        }
        applyAdditions(plan.additions(), inputTemperature, true);
        return InsertResult.SUCCESS;
    }

    private void applyAdditions(
            Map<String, Integer> additions,
            float inputTemperature,
            boolean immediatelyVisible) {
        double existingWeight = totalWeightGrams();
        double addedWeight = additions.entrySet().stream()
                .mapToDouble(component -> unitWeightGrams(component.getKey(), component.getValue()))
                .sum();
        float safeInputTemperature =
                Float.isFinite(inputTemperature) ? inputTemperature : AMBIENT_TEMPERATURE;
        thermal.mixWith(safeInputTemperature, existingWeight, addedWeight);
        contents.addAll(additions);
        steelmaking.onCompositionChanged();
        if (immediatelyVisible) {
            markVisibleMutation();
        } else {
            markMutation();
        }
    }

    public IFluidHandler externalFluids() {
        return externalFluids;
    }

    public Optional<MaterialDefinition> castIngot() {
        return cast(MaterialPrefixes.INGOT).map(CastTransfer::material);
    }

    public Optional<CastTransfer> cast(MaterialPrefix form) {
        Optional<CastCandidate> candidate = castCandidate(form);
        if (candidate.isEmpty()
                || thermal.authoritativeTemperature() < candidate.get().meltingPoint()) {
            return Optional.empty();
        }
        if (!contents.containsAtLeast(candidate.get().cost())) {
            return Optional.empty();
        }
        contents.removeAll(candidate.get().cost());
        markVisibleMutation();
        return Optional.of(new CastTransfer(
                candidate.get().material(),
                form,
                candidate.get().outputCount(),
                thermal.authoritativeTemperature()));
    }

    public float temperature() {
        return thermal.temperature(level != null && level.isClientSide);
    }

    /** Display helper; temperature is already Celsius. */
    public float temperatureCelsius() {
        return temperature();
    }

    public boolean isMolten() {
        if (contents.composition().isEmpty() || contents.unknownMaterials()) {
            return false;
        }
        if (currentSteelmakingBatch().isPresent()) {
            return temperature()
                    >= MaterialCatalog.require(SteelmakingProcess.IRON).thermal().meltingPoint();
        }
        return temperature() >= contents.moltenThreshold();
    }

    public float fillFraction() {
        return Math.min(1.0f, totalUnits() / (float) maxUnits());
    }

    public Map<String, Integer> composition() {
        return contents.composition();
    }

    public int totalUnits() {
        return contents.totalUnits();
    }

    public int moltenColor() {
        return contents.moltenColor();
    }

    public long storedAir() {
        return steelmaking.storedAir();
    }

    public String casingMaterialId() {
        return casing.materialId();
    }

    public void setCasingMaterialId(String materialId) {
        if (casing.setMaterialId(materialId)) {
            setChanged();
            syncToClient();
            checkpoint.synced();
        }
    }

    public int casingTier() {
        return casing.materialTier();
    }

    public int processingTier() {
        return casing.processingTier();
    }

    public float casingMaxTemperature() {
        return casing.maxTemperature();
    }

    public boolean steelmakingActive() {
        return steelmaking.active();
    }

    private Optional<Batch> currentSteelmakingBatch() {
        return steelmaking.currentBatch(contents.composition());
    }

    private Optional<CastCandidate> castCandidate(MaterialPrefix form) {
        if (hasUnknownMaterials()) {
            return Optional.empty();
        }
        Optional<AlloyMatch> alloy = contents.alloy();
        if (alloy.isPresent()) {
            if (!MaterialCatalog.isFormRegistered(alloy.get().result(), form)) {
                return Optional.empty();
            }
            return MoldCastingRules.smallestBatch(alloy.get().costPerIngot(), form).map(batch -> new CastCandidate(
                    alloy.get().result(),
                    batch.cost(),
                    batch.outputCount(),
                    alloy.get().result().thermal().meltingPoint()));
        }
        if (contents.resolvedMaterial().isEmpty()) {
            return Optional.empty();
        }

        MaterialDefinition material = contents.resolvedMaterial().orElseThrow();
        if (!MaterialCatalog.isFormRegistered(material, form)) {
            return Optional.empty();
        }
        return MoldCastingRules.smallestBatch(
                        Map.of(material.id(), MaterialPrefixes.INGOT.units()),
                        form)
                .map(batch -> new CastCandidate(
                        material,
                        batch.cost(),
                        batch.outputCount(),
                        material.thermal().meltingPoint()));
    }

    public boolean hasUnknownMaterials() {
        return contents.unknownMaterials();
    }

    private static double unitWeightGrams(String materialId, int units) {
        return MaterialCatalog.require(materialId).thermal().density() * CM3_PER_UNIT * units;
    }

    private double totalWeightGrams() {
        return casing.massGrams() + contents.contentsWeightGrams(CM3_PER_UNIT);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        restoreState(tag, false);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
        readClientTag(tag, registries);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            readClientTag(tag, registries);
        }
    }

    private void readClientTag(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        restoreState(tag, true);
    }

    private void restoreState(CompoundTag tag, boolean clientUpdate) {
        contents.clear();
        if (tag.contains("composition", Tag.TAG_COMPOUND)) {
            CompoundTag savedComposition = tag.getCompound("composition");
            for (String material : savedComposition.getAllKeys()) {
                int units = savedComposition.getInt(material);
                if (units > 0) {
                    contents.setUnits(material, units);
                }
            }
        } else {
            migrateLegacyContents(tag);
        }
        casing.setMaterialId(tag.contains("casing_material_id", Tag.TAG_STRING)
                ? tag.getString("casing_material_id")
                : MachineMaterialRules.DEFAULT_CRUCIBLE_MATERIAL);
        int savedCooldownTicks = tag.contains("cooldown_ticks")
                ? tag.getInt("cooldown_ticks")
                : CrucibleThermalModel.HOT_BUFFER_TICKS;
        thermal.restore(
                tag.contains("temperature") ? tag.getFloat("temperature") : AMBIENT_TEMPERATURE,
                tag.getLong("cached_energy_per_tick"),
                tag.getLong("stored_energy"),
                savedCooldownTicks,
                clientUpdate);
        long savedAir = tag.contains("stored_air", Tag.TAG_ANY_NUMERIC)
                ? tag.getLong("stored_air")
                : tag.getInt("air_ticks");
        steelmaking.restore(
                savedAir,
                tag.getInt("steel_batch_iron_units"),
                tag.getInt("steel_reaction_ticks"));
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag savedComposition = new CompoundTag();
        contents.composition().forEach(savedComposition::putInt);
        tag.put("composition", savedComposition);
        tag.putString("casing_material_id", casing.materialId());
        tag.putFloat("temperature", thermal.authoritativeTemperature());
        tag.putLong("cached_energy_per_tick", thermal.pendingHeat());
        tag.putLong("stored_energy", thermal.storedEnergy());
        tag.putInt("cooldown_ticks", thermal.cooldownTicks());
        tag.putLong("stored_air", steelmaking.storedAir());
        tag.putInt("steel_batch_iron_units", steelmaking.batchIronUnits());
        tag.putInt("steel_reaction_ticks", steelmaking.reactionTicks());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return writeClientTag();
    }

    /**
     * Client contract: renderer/Jade need composition, casing and temperature;
     * steel_batch_iron_units keeps isMolten() correct during an active blow.
     * Server-only heat buffers, air and reaction progress deliberately stay out.
     */
    private CompoundTag writeClientTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag savedComposition = new CompoundTag();
        contents.composition().forEach(savedComposition::putInt);
        tag.put("composition", savedComposition);
        tag.putString("casing_material_id", casing.materialId());
        tag.putFloat("temperature", thermal.authoritativeTemperature());
        tag.putInt("steel_batch_iron_units", steelmaking.batchIronUnits());
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    private void markMutation() {
        setChanged();
        checkpoint.checkpointed();
        checkpoint.markSyncPending();
    }

    private void markVisibleMutation() {
        setChanged();
        checkpoint.checkpointed();
        syncToClient();
        checkpoint.synced();
    }

    private Optional<DrainCandidate> drainCandidate(int requested) {
        if (requested <= 0 || !isMolten() || fluidTransferBlocked()) {
            return Optional.empty();
        }
        Optional<MaterialDefinition> resolvedMaterial = contents.resolvedMaterial();
        if (resolvedMaterial.isEmpty()
                || ModFluids.molten(resolvedMaterial.get().id()).isEmpty()) {
            return Optional.empty();
        }
        MaterialDefinition material = resolvedMaterial.get();
        Map<String, Integer> ratio = MaterialCatalog.decompositionRatio(material);
        return MoltenTransferMath.planDrain(contents.composition(), ratio, requested)
                .map(plan -> new DrainCandidate(material, plan));
    }

    private boolean fluidTransferBlocked() {
        return hasUnknownMaterials() || steelmaking.blocksFluidTransfer();
    }

    private FluidStack executeDrain(DrainCandidate candidate, IFluidHandler.FluidAction action) {
        FluidStack result = new FluidStack(
                ModFluids.molten(candidate.material().id()).orElseThrow().source().get(),
                candidate.plan().amount());
        if (action.execute()) {
            contents.removeAll(candidate.plan().removals());
            markMutation();
        }
        return result;
    }

    private void migrateLegacyContents(CompoundTag tag) {
        int copper = tag.getInt("copper_units");
        int tin = tag.getInt("tin_units");
        if (copper > 0) {
            contents.setUnits("copper", copper);
        }
        if (tin > 0) {
            contents.setUnits("tin", tin);
        }
    }

    private record CastCandidate(
            MaterialDefinition material,
            Map<String, Integer> cost,
            int outputCount,
            double meltingPoint) {}

    private record DrainCandidate(
            MaterialDefinition material,
            MoltenTransferMath.DrainPlan plan) {}

    public record CastTransfer(
            MaterialDefinition material,
            MaterialPrefix form,
            int count,
            float temperature) {}

    public static int maxUnits() {
        return MaterialPrefixes.INGOT.units() * MAX_INGOTS;
    }

    public enum AirInjectionResult {
        STARTED,
        CONTINUED,
        TOO_COLD,
        INVALID_CHARGE
    }

    private final class CrucibleFluidHandler implements IFluidHandler {
        @Override
        public int getTanks() {
            return 1;
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            if (tank != 0) {
                return FluidStack.EMPTY;
            }
            return drainCandidate(Integer.MAX_VALUE)
                    .map(candidate -> new FluidStack(
                            ModFluids.molten(candidate.material().id()).orElseThrow().source().get(),
                            candidate.plan().amount()))
                    .orElse(FluidStack.EMPTY);
        }

        @Override
        public int getTankCapacity(int tank) {
            return tank == 0 ? maxUnits() : 0;
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return tank == 0
                    && !stack.isEmpty()
                    && ModFluids.material(stack.getFluid()).isPresent();
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return 0;
            }
            Optional<MaterialDefinition> material = ModFluids.material(resource.getFluid());
            if (material.isEmpty()) {
                return 0;
            }
            Optional<FillPlan> plan = CrucibleTransferCoordinator.planFill(
                    material.get(),
                    resource.getAmount(),
                    totalUnits(),
                    maxUnits(),
                    casing.materialId(),
                    fluidTransferBlocked());
            if (plan.isEmpty()) {
                return 0;
            }
            if (action.execute()) {
                applyAdditions(
                        plan.get().additions(),
                        plan.get().inputTemperature(),
                        false);
            }
            return plan.get().accepted();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            Optional<MaterialDefinition> requestedMaterial = ModFluids.material(resource.getFluid());
            if (requestedMaterial.isEmpty()) {
                return FluidStack.EMPTY;
            }
            return drainCandidate(resource.getAmount())
                    .filter(candidate -> candidate.material().id().equals(requestedMaterial.get().id())
                            && resource.getFluid() == ModFluids.molten(candidate.material().id())
                                    .orElseThrow()
                                    .source()
                                    .get())
                    .map(candidate -> executeDrain(candidate, action))
                    .orElse(FluidStack.EMPTY);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            return drainCandidate(maxDrain)
                    .map(candidate -> executeDrain(candidate, action))
                    .orElse(FluidStack.EMPTY);
        }
    }
}
