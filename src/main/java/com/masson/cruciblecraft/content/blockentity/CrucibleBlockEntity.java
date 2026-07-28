package com.masson.cruciblecraft.content.blockentity;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.air.AirIntakeCoordinator;
import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.fluid.MoltenTransferMath;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.FillPlan;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.recipe.AlloyIndex.AlloyMatch;
import com.masson.cruciblecraft.recipe.SteelmakingProcess;
import com.masson.cruciblecraft.recipe.SteelmakingProcess.Batch;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
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

public class CrucibleBlockEntity extends BlockEntity {
    public static final int MAX_UNITS = MaterialForm.INGOT.units() * 8;
    public static final float AMBIENT_TEMPERATURE = 20.0f;
    private static final double CASING_VOLUME_CM3 = 800.0;
    private static final double CM3_PER_UNIT = 0.15;
    private final Map<String, Integer> composition = new HashMap<>();
    private String casingMaterialId = MachineMaterialRules.DEFAULT_CRUCIBLE_MATERIAL;
    private float temperature = AMBIENT_TEMPERATURE;
    private float cachedEnergyPerTick;
    private double storedEnergy;
    private int cooldownTicks = CrucibleThermalModel.HOT_BUFFER_TICKS;
    private float storedAir;
    private int steelBatchIronUnits;
    private int steelReactionTicks;
    private final IFluidHandler externalFluids = new CrucibleFluidHandler();
    private boolean dirtySinceCheckpoint;
    private Map<String, Integer> cachedComposition = Map.of();
    private Optional<AlloyMatch> cachedAlloy = Optional.empty();
    private Optional<MaterialDefinition> cachedResolvedMaterial = Optional.empty();
    private boolean cachedUnknownMaterials;
    private int cachedTotalUnits;
    private double cachedMoltenThreshold = Double.POSITIVE_INFINITY;
    private int cachedMoltenColor = 0xFFFFFF;
    private boolean compositionCacheValid;
    private float displayTemperature = AMBIENT_TEMPERATURE;
    private float displayTargetTemperature = AMBIENT_TEMPERATURE;
    private int displayInterpolationTicks;
    private boolean displayInitialized;

    public CrucibleBlockEntity(BlockPos pos, BlockState blockState) {
        super(ModBlockEntities.CRUCIBLE.get(), pos, blockState);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CrucibleBlockEntity crucible) {
        crucible.cachedEnergyPerTick = pullHeat(level, pos, false);
        crucible.pullAdjacentAir(level, pos);

        float previousTemperature = crucible.temperature;
        double previousStoredEnergy = crucible.storedEnergy;
        int previousCooldown = crucible.cooldownTicks;
        float previousAir = crucible.storedAir;
        int previousReactionTicks = crucible.steelReactionTicks;
        boolean meltedDown = crucible.isThermallyQuiescent()
                ? false
                : crucible.advance(crucible.cachedEnergyPerTick, true);
        if (meltedDown) {
            level.destroyBlock(pos, false);
            return;
        }
        boolean processChanged = Float.compare(previousTemperature, crucible.temperature) != 0
                || Double.compare(previousStoredEnergy, crucible.storedEnergy) != 0
                || previousCooldown != crucible.cooldownTicks
                || Float.compare(previousAir, crucible.storedAir) != 0
                || previousReactionTicks != crucible.steelReactionTicks;
        crucible.dirtySinceCheckpoint |= processChanged;
        if (CheckpointDecisions.shouldCheckpoint(
                crucible.dirtySinceCheckpoint,
                level.getGameTime(),
                20)) {
            crucible.setChanged();
            crucible.dirtySinceCheckpoint = false;
        }
        if (CheckpointDecisions.shouldSync(
                crucible.isActiveProcess(),
                level.getGameTime(),
                100)) {
            crucible.syncToClient();
        }
    }

    public static void clientTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CrucibleBlockEntity crucible) {
        if (crucible.displayInterpolationTicks > 0) {
            crucible.displayTemperature = CrucibleThermalModel.interpolateDisplay(
                    crucible.displayTemperature,
                    crucible.displayTargetTemperature,
                    crucible.displayInterpolationTicks);
            crucible.displayInterpolationTicks--;
        } else {
            crucible.displayTemperature = crucible.displayTargetTemperature;
        }
    }

    private static float pullHeat(Level level, BlockPos pos, boolean simulate) {
        var source = level.getCapability(
                ModCapabilities.HEAT_SOURCE,
                pos.below(),
                Direction.UP);
        if (source == null) {
            return 0.0F;
        }
        float rate = source.outputRate();
        if (!Float.isFinite(rate) || rate <= 0.0F) {
            return 0.0F;
        }
        double extracted = source.extractHeat(rate, simulate);
        return Double.isFinite(extracted) && extracted > 0.0
                ? (float) extracted
                : 0.0F;
    }

    private boolean advance(float incomingEnergy, boolean authoritative) {
        if (hasUnknownMaterials()) {
            return false;
        }
        if (authoritative) {
            boolean boiled = composition.keySet().removeIf(id ->
                    CrucibleThermalModel.shouldBoil(
                            temperature,
                            MaterialCatalog.require(id).thermal().boilingPoint()));
            if (boiled) {
                invalidateCompositionCache();
                markImmediateMutation();
                emitBoilingEffects();
            }
            processSteelmaking();
        }

        CrucibleThermalModel.StepResult result = CrucibleThermalModel.step(
                temperature,
                storedEnergy,
                cooldownTicks,
                incomingEnergy,
                totalWeightGrams(),
                AMBIENT_TEMPERATURE);
        temperature = result.temperature();
        storedEnergy = result.storedEnergy();
        cooldownTicks = result.cooldownTicks();

        if (temperature > casingMaxTemperature()) {
            if (authoritative) {
                composition.clear();
            }
            return true;
        }
        return false;
    }

    private boolean isThermallyQuiescent() {
        return cachedEnergyPerTick <= 0.0F
                && Float.compare(temperature, AMBIENT_TEMPERATURE) == 0
                && storedEnergy == 0.0
                && storedAir == 0.0F
                && steelBatchIronUnits == 0;
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

    private void pullAdjacentAir(Level level, BlockPos pos) {
        if (!level.getBlockState(pos.above()).isAir()
                || airAcceptanceResult() != AirInjectionResult.ACCEPTABLE) {
            return;
        }
        float room = AirOutputModel.MAX_STORED_AIR - storedAir;
        if (room <= 0.0F) {
            return;
        }
        float incomingAir = 0.0F;
        for (Direction direction : Direction.values()) {
            if (room <= 0.0F) {
                break;
            }
            var source = level.getCapability(
                    ModCapabilities.AIR_SOURCE,
                    pos.relative(direction),
                    direction.getOpposite());
            if (source == null) {
                continue;
            }
            float request = AirIntakeCoordinator.request(
                    true,
                    true,
                    room,
                    source.outputRate());
            float simulated = source.extractAir(request, true);
            if (!Float.isFinite(simulated) || simulated <= 0.0F) {
                continue;
            }
            float extracted = source.extractAir(Math.min(request, simulated), false);
            if (Float.isFinite(extracted) && extracted > 0.0F) {
                float accepted = Math.min(room, extracted);
                incomingAir += accepted;
                room -= accepted;
            }
        }
        if (incomingAir > 0.0F) {
            injectAir(incomingAir);
        }
    }

    public AirInjectionResult injectAir(float air) {
        AirInjectionResult acceptance = airAcceptanceResult();
        if (acceptance != AirInjectionResult.ACCEPTABLE) {
            return acceptance;
        }

        Optional<Batch> batch = steelBatchIronUnits > 0
                ? SteelmakingProcess.resume(steelBatchIronUnits)
                : SteelmakingProcess.begin(composition);
        boolean continuing = steelBatchIronUnits > 0;
        steelBatchIronUnits = batch.orElseThrow().ironUnits();
        storedAir = AirOutputModel.addToBuffer(storedAir, air);
        dirtySinceCheckpoint = true;
        return continuing ? AirInjectionResult.CONTINUED : AirInjectionResult.STARTED;
    }

    private AirInjectionResult airAcceptanceResult() {
        if (hasUnknownMaterials()) {
            return AirInjectionResult.INVALID_CHARGE;
        }
        if (temperature
                < MaterialCatalog.require(SteelmakingProcess.IRON).thermal().meltingPoint()) {
            return AirInjectionResult.TOO_COLD;
        }
        Optional<Batch> batch = steelBatchIronUnits > 0
                ? SteelmakingProcess.resume(steelBatchIronUnits)
                : SteelmakingProcess.begin(composition);
        if (batch.isEmpty() || !matchesActiveSteelBatch(batch.get())) {
            return AirInjectionResult.INVALID_CHARGE;
        }
        return AirInjectionResult.ACCEPTABLE;
    }

    private void processSteelmaking() {
        if (storedAir < 1.0F || steelBatchIronUnits <= 0) {
            return;
        }
        storedAir = AirOutputModel.consumeProcessingTick(storedAir);

        Optional<Batch> batch = SteelmakingProcess.resume(steelBatchIronUnits);
        if (batch.isEmpty() || !matchesActiveSteelBatch(batch.get())) {
            resetSteelmaking();
            return;
        }
        if (temperature
                < MaterialCatalog.require(SteelmakingProcess.IRON).thermal().meltingPoint()) {
            return;
        }

        steelReactionTicks++;
        if (steelReactionTicks < SteelmakingProcess.REACTION_INTERVAL_TICKS) {
            return;
        }
        steelReactionTicks = 0;

        int carbon = composition.getOrDefault(SteelmakingProcess.CARBON, 0);
        var carbonStep = SteelmakingProcess.consumeCarbon(batch.get(), carbon);
        if (carbonStep.steelRangeReached()) {
            composition.remove(SteelmakingProcess.IRON);
            composition.remove(SteelmakingProcess.CARBON);
            // Steel is intentionally no_decompose: the residual 1-2 carbon
            // units per ingot are absorbed into one steel grade. Output units
            // therefore equal the iron units, with no per-stack metadata.
            composition.merge(
                    SteelmakingProcess.STEEL,
                    batch.get().ironUnits(),
                    Integer::sum);
            resetSteelmaking();
        } else {
            composition.put(SteelmakingProcess.CARBON, carbonStep.remainingCarbonUnits());
        }
        invalidateCompositionCache();
        markImmediateMutation();
    }

    private boolean matchesActiveSteelBatch(Batch batch) {
        if (composition.size() != 2
                || composition.getOrDefault(SteelmakingProcess.IRON, 0) != batch.ironUnits()) {
            return false;
        }
        int carbon = composition.getOrDefault(SteelmakingProcess.CARBON, 0);
        return carbon >= batch.minimumSteelCarbonUnits()
                && carbon <= batch.ironUnits() / 3;
    }

    private void resetSteelmaking() {
        storedAir = 0.0F;
        steelBatchIronUnits = 0;
        steelReactionTicks = 0;
    }

    public InsertResult insert(MaterialUnits.Entry entry, float inputTemperature) {
        var plan = CrucibleTransferCoordinator.planInsertion(
                entry,
                totalUnits(),
                MAX_UNITS,
                casingMaterialId);
        if (plan.result() != InsertResult.SUCCESS) {
            return plan.result();
        }
        applyAdditions(plan.additions(), inputTemperature);
        return InsertResult.SUCCESS;
    }

    private void applyAdditions(Map<String, Integer> additions, float inputTemperature) {
        double existingWeight = totalWeightGrams();
        double addedWeight = additions.entrySet().stream()
                .mapToDouble(component -> unitWeightGrams(component.getKey(), component.getValue()))
                .sum();
        float safeInputTemperature =
                Float.isFinite(inputTemperature) ? inputTemperature : AMBIENT_TEMPERATURE;
        temperature = CrucibleThermalModel.mixTemperature(
                temperature,
                existingWeight,
                safeInputTemperature,
                addedWeight);
        resetSteelmaking();
        additions.forEach((material, units) -> composition.merge(material, units, Integer::sum));
        invalidateCompositionCache();
        markImmediateMutation();
    }

    public IFluidHandler externalFluids() {
        return externalFluids;
    }

    public Optional<MaterialDefinition> castIngot() {
        return cast(MaterialForm.INGOT).map(CastTransfer::material);
    }

    public Optional<CastTransfer> cast(MaterialForm form) {
        Optional<CastCandidate> candidate = castCandidate(form);
        if (candidate.isEmpty() || temperature < candidate.get().meltingPoint()) {
            return Optional.empty();
        }
        if (candidate.get().cost().entrySet().stream()
                .anyMatch(entry -> composition.getOrDefault(entry.getKey(), 0) < entry.getValue())) {
            return Optional.empty();
        }
        candidate.get().cost().forEach((material, units) ->
                composition.computeIfPresent(material, (ignored, amount) -> {
                    int remainder = amount - units;
                    return remainder == 0 ? null : remainder;
                }));
        invalidateCompositionCache();
        markImmediateMutation();
        return Optional.of(new CastTransfer(
                candidate.get().material(),
                form,
                candidate.get().outputCount(),
                temperature));
    }

    public float temperature() {
        return level != null && level.isClientSide ? displayTemperature : temperature;
    }

    /** Display helper; temperature is already Celsius. */
    public float temperatureCelsius() {
        return temperature();
    }

    public boolean isMolten() {
        ensureCompositionCache();
        if (cachedComposition.isEmpty() || cachedUnknownMaterials) {
            return false;
        }
        if (currentSteelmakingBatch().isPresent()) {
            return temperature()
                    >= MaterialCatalog.require(SteelmakingProcess.IRON).thermal().meltingPoint();
        }
        return temperature() >= cachedMoltenThreshold;
    }

    public float fillFraction() {
        return Math.min(1.0f, totalUnits() / (float) MAX_UNITS);
    }

    public Map<String, Integer> composition() {
        ensureCompositionCache();
        return cachedComposition;
    }

    public int totalUnits() {
        ensureCompositionCache();
        return cachedTotalUnits;
    }

    public int moltenColor() {
        ensureCompositionCache();
        return cachedMoltenColor;
    }

    public float storedAir() {
        return storedAir;
    }

    public String casingMaterialId() {
        return casingMaterialId;
    }

    public void setCasingMaterialId(String materialId) {
        String sanitized = MachineMaterialRules.sanitize(Device.CRUCIBLE, materialId);
        if (!sanitized.equals(casingMaterialId)) {
            casingMaterialId = sanitized;
            setChanged();
            syncToClient();
        }
    }

    public int casingTier() {
        return MachineMaterialRules.materialTier(casingMaterialId);
    }

    public int processingTier() {
        return MachineMaterialRules.processingTier(Device.CRUCIBLE, casingMaterialId);
    }

    public float casingMaxTemperature() {
        return MachineMaterialRules.crucibleMaxTemperature(casingMaterialId);
    }

    public boolean steelmakingActive() {
        return steelBatchIronUnits > 0;
    }

    private Optional<Batch> currentSteelmakingBatch() {
        Optional<Batch> batch = steelBatchIronUnits > 0
                ? SteelmakingProcess.resume(steelBatchIronUnits)
                : SteelmakingProcess.begin(composition);
        return batch.filter(this::matchesActiveSteelBatch);
    }

    private Optional<CastCandidate> castCandidate(MaterialForm form) {
        if (hasUnknownMaterials()) {
            return Optional.empty();
        }
        ensureCompositionCache();
        Optional<AlloyMatch> alloy = cachedAlloy;
        if (alloy.isPresent()) {
            if (!alloy.get().result().forms().contains(form)) {
                return Optional.empty();
            }
            return MoldCastingRules.smallestBatch(alloy.get().costPerIngot(), form).map(batch -> new CastCandidate(
                    alloy.get().result(),
                    batch.cost(),
                    batch.outputCount(),
                    alloy.get().result().thermal().meltingPoint()));
        }
        if (cachedResolvedMaterial.isEmpty()) {
            return Optional.empty();
        }

        MaterialDefinition material = cachedResolvedMaterial.get();
        if (!material.forms().contains(form)) {
            return Optional.empty();
        }
        return MoldCastingRules.smallestBatch(
                        Map.of(material.id(), MaterialForm.INGOT.units()),
                        form)
                .map(batch -> new CastCandidate(
                        material,
                        batch.cost(),
                        batch.outputCount(),
                        material.thermal().meltingPoint()));
    }

    private double allMaterialsMeltingPoint() {
        return composition.keySet().stream()
                .map(MaterialCatalog::require)
                .mapToDouble(material -> material.thermal().meltingPoint())
                .max()
                .orElse(Double.POSITIVE_INFINITY);
    }

    public boolean hasUnknownMaterials() {
        ensureCompositionCache();
        return cachedUnknownMaterials;
    }

    private void ensureCompositionCache() {
        if (compositionCacheValid) {
            return;
        }
        cachedComposition = Map.copyOf(composition);
        cachedUnknownMaterials = composition.keySet().stream().anyMatch(id -> !MaterialCatalog.contains(id));
        cachedTotalUnits = composition.values().stream().mapToInt(Integer::intValue).sum();
        cachedAlloy = cachedUnknownMaterials
                ? Optional.empty()
                : MaterialCatalog.alloys().match(cachedComposition);
        cachedResolvedMaterial = cachedUnknownMaterials
                ? Optional.empty()
                : CrucibleTransferCoordinator.resolveCurrentMaterial(cachedComposition);
        cachedMoltenThreshold = cachedResolvedMaterial
                .map(material -> material.thermal().meltingPoint())
                .orElseGet(this::allMaterialsMeltingPoint);

        long total = 0L;
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        if (!cachedUnknownMaterials) {
            for (var entry : composition.entrySet()) {
                int color = parseMaterialColor(MaterialCatalog.require(entry.getKey()).color());
                int units = entry.getValue();
                total += units;
                red += (long) ((color >> 16) & 0xFF) * units;
                green += (long) ((color >> 8) & 0xFF) * units;
                blue += (long) (color & 0xFF) * units;
            }
        }
        cachedMoltenColor = total == 0L
                ? 0xFFFFFF
                : ((int) (red / total) << 16)
                        | ((int) (green / total) << 8)
                        | (int) (blue / total);
        compositionCacheValid = true;
    }

    private static int parseMaterialColor(String color) {
        try {
            return Integer.parseInt(color.startsWith("#") ? color.substring(1) : color, 16) & 0xFFFFFF;
        } catch (NumberFormatException ignored) {
            return 0xFFFFFF;
        }
    }

    private void invalidateCompositionCache() {
        compositionCacheValid = false;
    }

    private static double unitWeightGrams(String materialId, int units) {
        return MaterialCatalog.require(materialId).thermal().density() * CM3_PER_UNIT * units;
    }

    private double totalWeightGrams() {
        double weight = casingWeightGrams();
        for (var entry : composition.entrySet()) {
            weight += unitWeightGrams(entry.getKey(), entry.getValue());
        }
        return weight;
    }

    private double casingWeightGrams() {
        return MachineMaterialRules.casingMassGrams(casingMaterialId, CASING_VOLUME_CM3);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        composition.clear();
        if (tag.contains("composition", Tag.TAG_COMPOUND)) {
            CompoundTag savedComposition = tag.getCompound("composition");
            for (String material : savedComposition.getAllKeys()) {
                int units = savedComposition.getInt(material);
                if (units > 0) {
                    composition.put(material, units);
                }
            }
        } else {
            migrateLegacyContents(tag);
        }
        casingMaterialId = MachineMaterialRules.sanitize(
                Device.CRUCIBLE,
                tag.contains("casing_material_id", Tag.TAG_STRING)
                        ? tag.getString("casing_material_id")
                        : MachineMaterialRules.DEFAULT_CRUCIBLE_MATERIAL);
        temperature = tag.contains("temperature") ? tag.getFloat("temperature") : AMBIENT_TEMPERATURE;
        if (!Float.isFinite(temperature)) {
            temperature = AMBIENT_TEMPERATURE;
        }
        cachedEnergyPerTick = tag.getFloat("cached_energy_per_tick");
        storedEnergy = tag.getDouble("stored_energy");
        if (!Float.isFinite(cachedEnergyPerTick) || cachedEnergyPerTick < 0.0f) {
            cachedEnergyPerTick = 0.0f;
        }
        if (!Double.isFinite(storedEnergy) || storedEnergy < 0.0) {
            storedEnergy = 0.0;
        }
        cooldownTicks = tag.contains("cooldown_ticks")
                ? tag.getInt("cooldown_ticks")
                : CrucibleThermalModel.HOT_BUFFER_TICKS;
        cooldownTicks = Math.max(
                0,
                Math.min(CrucibleThermalModel.HOT_BUFFER_TICKS, cooldownTicks));
        float savedAir = tag.contains("stored_air", Tag.TAG_ANY_NUMERIC)
                ? tag.getFloat("stored_air")
                : tag.getInt("air_ticks");
        storedAir = AirOutputModel.clampStoredAir(savedAir);
        steelBatchIronUnits = tag.getInt("steel_batch_iron_units");
        if (SteelmakingProcess.resume(steelBatchIronUnits).isEmpty()) {
            steelBatchIronUnits = 0;
        }
        steelReactionTicks = Math.max(
                0,
                Math.min(
                        SteelmakingProcess.REACTION_INTERVAL_TICKS - 1,
                        tag.getInt("steel_reaction_ticks")));
        invalidateCompositionCache();
        displayTargetTemperature = temperature;
        if (!displayInitialized || level == null || !level.isClientSide) {
            displayTemperature = temperature;
            displayInterpolationTicks = 0;
            displayInitialized = true;
        } else {
            displayInterpolationTicks = 10;
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        CompoundTag savedComposition = new CompoundTag();
        composition.forEach(savedComposition::putInt);
        tag.put("composition", savedComposition);
        tag.putString("casing_material_id", casingMaterialId);
        tag.putFloat("temperature", temperature);
        tag.putFloat("cached_energy_per_tick", cachedEnergyPerTick);
        tag.putDouble("stored_energy", storedEnergy);
        tag.putInt("cooldown_ticks", cooldownTicks);
        tag.putFloat("stored_air", storedAir);
        tag.putInt("steel_batch_iron_units", steelBatchIronUnits);
        tag.putInt("steel_reaction_ticks", steelReactionTicks);
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

    private void markImmediateMutation() {
        setChanged();
        dirtySinceCheckpoint = false;
        syncToClient();
    }

    private Optional<DrainCandidate> drainCandidate(int requested) {
        if (requested <= 0 || !isMolten() || fluidTransferBlocked()) {
            return Optional.empty();
        }
        ensureCompositionCache();
        if (cachedResolvedMaterial.isEmpty()
                || ModFluids.molten(cachedResolvedMaterial.get().id()).isEmpty()) {
            return Optional.empty();
        }
        MaterialDefinition material = cachedResolvedMaterial.get();
        int quantum = MaterialCatalog.decompositionQuantum(material);
        Map<String, Integer> ratio = MaterialCatalog.decompose(material, quantum);
        return MoltenTransferMath.planDrain(composition, ratio, requested)
                .map(plan -> new DrainCandidate(material, plan));
    }

    private boolean fluidTransferBlocked() {
        return hasUnknownMaterials()
                || steelBatchIronUnits > 0
                || storedAir > 0.0F
                || steelReactionTicks > 0;
    }

    private FluidStack executeDrain(DrainCandidate candidate, IFluidHandler.FluidAction action) {
        FluidStack result = new FluidStack(
                ModFluids.molten(candidate.material().id()).orElseThrow().source().get(),
                candidate.plan().amount());
        if (action.execute()) {
            candidate.plan().removals().forEach((material, units) ->
                    composition.computeIfPresent(material, (ignored, amount) -> {
                        int remainder = amount - units;
                        return remainder == 0 ? null : remainder;
                    }));
            invalidateCompositionCache();
            markImmediateMutation();
        }
        return result;
    }

    private void migrateLegacyContents(CompoundTag tag) {
        int copper = tag.getInt("copper_units");
        int tin = tag.getInt("tin_units");
        if (copper > 0) {
            composition.put("copper", copper);
        }
        if (tin > 0) {
            composition.put("tin", tin);
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
            MaterialForm form,
            int count,
            float temperature) {}

    public enum AirInjectionResult {
        ACCEPTABLE,
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
            return tank == 0 ? MAX_UNITS : 0;
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
                    MAX_UNITS,
                    casingMaterialId,
                    fluidTransferBlocked());
            if (plan.isEmpty()) {
                return 0;
            }
            if (action.execute()) {
                applyAdditions(plan.get().additions(), plan.get().inputTemperature());
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
