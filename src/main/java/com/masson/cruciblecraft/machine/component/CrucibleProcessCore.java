package com.masson.cruciblecraft.machine.component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.content.block.SmelteryHosts;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
import com.masson.cruciblecraft.content.mold.MoldHost;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.FillPlan;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.fluid.MoltenTransferMath;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.machine.MachineMaterialRules;
import com.masson.cruciblecraft.machine.MachineMaterialRules.Device;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.AlloyIndex.AlloyMatch;
import com.masson.cruciblecraft.recipe.AlloyIndex.Conversion;
import com.masson.cruciblecraft.recipe.SteelmakingProcess;
import com.masson.cruciblecraft.recipe.SteelmakingProcess.Batch;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Block-independent crucible process: composition, heat, steelmaking and
 * casting. Single-block and large-crucible adapters supply isolation,
 * ports and world effects.
 */
public final class CrucibleProcessCore {
    public static final int SINGLE_BLOCK_MAX_INGOTS = 16;
    public static final int LARGE_MAX_INGOTS = 432;
    public static final float AMBIENT_TEMPERATURE = 20.0f;
    public static final long HEAT_DISPLAY_CAPACITY = 64L;
    public static final double SINGLE_HEAT_RESISTANCE = 1.25;
    public static final double LARGE_HEAT_RESISTANCE = 1.10;
    private static final double SINGLE_CASING_VOLUME_CM3 = 800.0;
    private static final double LARGE_CASING_VOLUME_CM3 = 800.0 * 100.0 / 7.0;
    private static final double CM3_PER_UNIT = 0.15;
    private static final int RAIN_INTERVAL_TICKS = 600;

    private final int maxIngots;
    private final double heatResistanceBonus;
    private final CompositionTank contents;
    private final MachineCasing casing;
    private final ThermalComponent thermal = new ThermalComponent(AMBIENT_TEMPERATURE);
    private final SteelmakingController steelmaking = new SteelmakingController();
    private final IFluidHandler fluids = new CoreFluidHandler();
    private boolean frozen;
    private float previousTemperature = AMBIENT_TEMPERATURE;
    private Runnable onMutation = () -> {};

    public CrucibleProcessCore(int maxIngots) {
        this(maxIngots, SINGLE_CASING_VOLUME_CM3, SINGLE_HEAT_RESISTANCE);
    }

    private CrucibleProcessCore(
            int maxIngots, double casingVolumeCm3, double heatResistanceBonus) {
        if (maxIngots <= 0) {
            throw new IllegalArgumentException("maxIngots must be positive");
        }
        this.maxIngots = maxIngots;
        this.heatResistanceBonus = heatResistanceBonus;
        this.contents = new CompositionTank();
        this.casing = new MachineCasing(Device.CRUCIBLE, casingVolumeCm3);
    }

    public static CrucibleProcessCore singleBlock() {
        return new CrucibleProcessCore(
                SINGLE_BLOCK_MAX_INGOTS,
                SINGLE_CASING_VOLUME_CM3,
                SINGLE_HEAT_RESISTANCE);
    }

    public static CrucibleProcessCore large() {
        return new CrucibleProcessCore(
                LARGE_MAX_INGOTS,
                LARGE_CASING_VOLUME_CM3,
                LARGE_HEAT_RESISTANCE);
    }

    public int maxIngots() {
        return maxIngots;
    }

    public int maxUnits() {
        return MaterialPrefixes.INGOT.units() * maxIngots;
    }

    public void setFrozen(boolean frozen) {
        this.frozen = frozen;
    }

    public void setOnMutation(Runnable onMutation) {
        this.onMutation = onMutation == null ? () -> {} : onMutation;
    }

    public boolean frozen() {
        return frozen || casing.quarantined();
    }

    public TickOutcome advance(long incomingEnergy, boolean authoritative) {
        if (frozen()) {
            return TickOutcome.IDLE;
        }
        boolean unknownMaterials = hasUnknownMaterials();
        boolean boiled = false;
        boolean steelMutated = false;
        boolean acidDestroyed = false;
        float explodeRadius = 0.0F;
        float temperature = thermal.authoritativeTemperature();
        if (authoritative && !unknownMaterials) {
            Optional<Conversion> alloy = MaterialCatalog.alloys()
                    .preferredCrucibleConversion(contents.composition(), temperature);
            if (alloy.isPresent() && contents.applyAlloy(alloy.get())) {
                steelMutated = true;
            }
            var reaction = contents.react(
                    temperature,
                    previousTemperature,
                    steelMutated,
                    SmelteryHosts.acidProof(casing.materialId())
                            || LargeCrucibleHosts.acidProof(casing.materialId()));
            boiled = reaction.boiled();
            acidDestroyed = reaction.acidDestroyed();
            if (reaction.exploded()) {
                explodeRadius = Math.max(
                        0.5F,
                        (maxIngots == LARGE_MAX_INGOTS ? 8.0F : 6.0F)
                                * reaction.explodedUnits()
                                / (float) Math.max(1, maxUnits()));
            }
            if (!acidDestroyed && explodeRadius <= 0.0F) {
                steelMutated = steelmaking.tick(contents, temperature).immediateMutation()
                        || steelMutated;
            }
        }
        previousTemperature = thermal.authoritativeTemperature();
        if (acidDestroyed || explodeRadius > 0.0F) {
            contents.clear();
            return new TickOutcome(false, true, boiled, acidDestroyed, explodeRadius);
        }
        thermal.advance(
                unknownMaterials ? 0L : incomingEnergy,
                totalWeightGrams());
        if (thermal.authoritativeTemperature() > casingMaxTemperature()) {
            if (authoritative) {
                contents.clear();
            }
            return new TickOutcome(true, true, boiled, false, 0.0F);
        }
        return new TickOutcome(false, boiled || steelMutated, boiled, false, 0.0F);
    }

    public void driftTowardAmbient() {
        if (frozen()) {
            return;
        }
        previousTemperature = thermal.authoritativeTemperature();
        thermal.advance(0L, totalWeightGrams());
    }

    public boolean addRainWater(long gameTime, float rainfall, boolean thunder) {
        if (frozen()
                || rainfall <= 0.0F
                || gameTime % RAIN_INTERVAL_TICKS != 10L
                || !MaterialCatalog.contains("water")) {
            return false;
        }
        int amount = Math.max(1, Math.round(rainfall * 100.0F)) * (thunder ? 2 : 1);
        int divisor = maxIngots == LARGE_MAX_INGOTS ? 100 : 1000;
        int units = Math.max(1, MaterialPrefixes.INGOT.units() * amount / divisor);
        if (totalUnits() > maxUnits() - units) {
            return false;
        }
        applyAdditions(Map.of("water", units), AMBIENT_TEMPERATURE);
        return true;
    }

    public boolean isThermallyQuiescent() {
        return thermal.isQuiescent() && !steelmaking.hasState();
    }

    public InsertResult insert(MaterialUnits.Entry entry, float inputTemperature) {
        if (frozen()) {
            return InsertResult.INVALID_MATERIAL;
        }
        var plan = CrucibleTransferCoordinator.planInsertion(
                entry,
                totalUnits(),
                maxUnits(),
                casing.materialId());
        if (plan.result() != InsertResult.SUCCESS) {
            return plan.result();
        }
        applyAdditions(plan.additions(), inputTemperature);
        return InsertResult.SUCCESS;
    }

    public SteelmakingController.InjectionResult insertAir(long air) {
        if (frozen()) {
            return SteelmakingController.InjectionResult.INVALID_CHARGE;
        }
        return steelmaking.insertAir(
                air,
                contents.composition(),
                thermal.authoritativeTemperature());
    }

    public SteelmakingController.InjectionResult previewAirInjection() {
        if (frozen()) {
            return SteelmakingController.InjectionResult.INVALID_CHARGE;
        }
        return steelmaking.previewInjection(
                contents.composition(),
                thermal.authoritativeTemperature());
    }

    public Optional<CastTransfer> cast(MaterialPrefix form) {
        if (frozen()) {
            return Optional.empty();
        }
        Optional<CastCandidate> candidate = castCandidate(form);
        if (candidate.isEmpty()
                || thermal.authoritativeTemperature() < candidate.get().meltingPoint()) {
            return Optional.empty();
        }
        if (!contents.containsAtLeast(candidate.get().cost())) {
            return Optional.empty();
        }
        contents.removeAll(candidate.get().cost());
        onMutation.run();
        return Optional.of(new CastTransfer(
                candidate.get().material(),
                form,
                candidate.get().outputCount(),
                thermal.authoritativeTemperature()));
    }

    /**
     * GT6 {@code ITileEntityCrucible.fillMoldAtSide}: first molten catalog
     * stack whose smelting target is itself, then an exact-ratio alloy product.
     */
    public boolean fillMoldAtSide(MoldHost mold, Direction moldSide) {
        if (frozen() || mold == null) {
            return false;
        }
        float temperature = authoritativeTemperature();
        for (var entry : List.copyOf(contents.composition().entrySet())) {
            if (!MaterialCatalog.contains(entry.getKey()) || entry.getValue() <= 0) {
                continue;
            }
            MaterialDefinition material = MaterialCatalog.require(entry.getKey());
            if (!pourableMelt(material, temperature)) {
                continue;
            }
            int consumed = mold.fillMold(
                    material.id(),
                    entry.getValue(),
                    temperature,
                    moldSide);
            if (consumed <= 0) {
                continue;
            }
            contents.setUnits(material.id(), entry.getValue() - consumed);
            onMutation.run();
            return true;
        }
        return pourAlloy(mold, moldSide, temperature);
    }

    public Optional<CastTransfer> previewCast(MaterialPrefix form) {
        if (frozen()) {
            return Optional.empty();
        }
        Optional<CastCandidate> candidate = castCandidate(form);
        if (candidate.isEmpty()
                || thermal.authoritativeTemperature() < candidate.get().meltingPoint()) {
            return Optional.empty();
        }
        if (!contents.containsAtLeast(candidate.get().cost())) {
            return Optional.empty();
        }
        return Optional.of(new CastTransfer(
                candidate.get().material(),
                form,
                candidate.get().outputCount(),
                thermal.authoritativeTemperature()));
    }

    public Optional<MaterialDefinition> castIngot() {
        return cast(MaterialPrefixes.INGOT).map(CastTransfer::material);
    }

    public IFluidHandler fluids() {
        return fluids;
    }

    public CompositionTank contents() {
        return contents;
    }

    public ThermalComponent thermal() {
        return thermal;
    }

    public SteelmakingController steelmaking() {
        return steelmaking;
    }

    public MachineCasing casing() {
        return casing;
    }

    public Map<String, Integer> composition() {
        return contents.composition();
    }

    public int totalUnits() {
        return contents.totalUnits();
    }

    public float temperature(boolean client) {
        return thermal.temperature(client);
    }

    public float authoritativeTemperature() {
        return thermal.authoritativeTemperature();
    }

    public boolean isMolten() {
        if (contents.composition().isEmpty() || contents.unknownMaterials()) {
            return false;
        }
        if (currentSteelmakingBatch().isPresent()) {
            return authoritativeTemperature()
                    >= MaterialCatalog.require(SteelmakingProcess.IRON)
                            .thermal()
                            .meltingPoint();
        }
        return authoritativeTemperature() >= contents.moltenThreshold();
    }

    public float fillFraction() {
        return Math.min(1.0f, totalUnits() / (float) maxUnits());
    }

    public int moltenColor() {
        return contents.moltenColor(authoritativeTemperature());
    }

    public boolean hasUnknownMaterials() {
        return contents.unknownMaterials();
    }

    public boolean steelmakingActive() {
        return steelmaking.active();
    }

    public long storedAir() {
        return steelmaking.storedAir();
    }

    public float casingMaxTemperature() {
        return casing.maxTemperature(heatResistanceBonus);
    }

    public double heatResistanceBonus() {
        return heatResistanceBonus;
    }

    public void save(CompoundTag tag) {
        CompoundTag savedComposition = new CompoundTag();
        contents.composition().forEach(savedComposition::putInt);
        tag.put("composition", savedComposition);
        tag.putString("casing_material_id", casing.persistedMaterialId());
        tag.putFloat("temperature", thermal.authoritativeTemperature());
        tag.putFloat("old_temperature", previousTemperature);
        tag.putLong("cached_energy_per_tick", thermal.pendingHeat());
        tag.putLong("stored_energy", thermal.storedEnergy());
        tag.putInt("cooldown_ticks", thermal.cooldownTicks());
        tag.putLong("stored_air", steelmaking.storedAir());
        tag.putInt("steel_batch_iron_units", steelmaking.batchIronUnits());
        tag.putInt("steel_reaction_ticks", steelmaking.reactionTicks());
    }

    public CompoundTag clientTag() {
        CompoundTag tag = new CompoundTag();
        CompoundTag savedComposition = new CompoundTag();
        contents.composition().forEach(savedComposition::putInt);
        tag.put("composition", savedComposition);
        tag.putString("casing_material_id", casing.persistedMaterialId());
        tag.putFloat("temperature", thermal.authoritativeTemperature());
        tag.putInt("steel_batch_iron_units", steelmaking.batchIronUnits());
        return tag;
    }

    public void restore(CompoundTag tag, boolean clientUpdate) {
        contents.clear();
        if (tag.contains("composition", Tag.TAG_COMPOUND)) {
            CompoundTag savedComposition = tag.getCompound("composition");
            for (String material : savedComposition.getAllKeys()) {
                int units = savedComposition.getInt(material);
                if (units > 0) {
                    contents.setUnits(material, units);
                }
            }
        }
        casing.restoreMaterialId(tag.contains("casing_material_id", Tag.TAG_STRING)
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
        previousTemperature = tag.contains("old_temperature")
                ? tag.getFloat("old_temperature")
                : thermal.authoritativeTemperature();
        steelmaking.restore(
                tag.getLong("stored_air"),
                tag.getInt("steel_batch_iron_units"),
                tag.getInt("steel_reaction_ticks"));
    }

    public void applyAdditions(Map<String, Integer> additions, float inputTemperature) {
        double existingWeight = totalWeightGrams();
        double addedWeight = additions.entrySet().stream()
                .mapToDouble(component ->
                        unitWeightGrams(component.getKey(), component.getValue()))
                .sum();
        float safeInputTemperature =
                Float.isFinite(inputTemperature) ? inputTemperature : AMBIENT_TEMPERATURE;
        thermal.mixWith(safeInputTemperature, existingWeight, addedWeight);
        contents.addAll(additions);
        steelmaking.onCompositionChanged();
        onMutation.run();
    }

    public Optional<ScrapTake> takeScrap(int maxCount) {
        if (frozen() || maxCount <= 0) {
            return Optional.empty();
        }
        Optional<String> lightest = contents.lightestId();
        if (lightest.isEmpty()) {
            return Optional.empty();
        }
        MaterialDefinition material = MaterialCatalog.require(lightest.get());
        if (authoritativeTemperature() >= material.thermal().meltingPoint()) {
            return Optional.empty();
        }
        Optional<MaterialPrefix> scrap = MaterialPrefixCatalog.find("scrap");
        int quantum = scrap.map(MaterialPrefix::units).orElse(MaterialPrefixes.INGOT.units() / 9);
        int available = contents.units(material.id());
        if (available < quantum) {
            contents.setUnits(material.id(), 0);
            onMutation.run();
            return Optional.of(new ScrapTake(material, 0, available, true));
        }
        int count = Math.min(maxCount, available / quantum);
        contents.setUnits(material.id(), available - count * quantum);
        onMutation.run();
        return Optional.of(new ScrapTake(material, count, count * quantum, false));
    }

    public Optional<MaterialDefinition> lightestSolid() {
        Optional<String> lightest = contents.lightestId();
        if (lightest.isEmpty()) {
            return Optional.empty();
        }
        MaterialDefinition material = MaterialCatalog.require(lightest.get());
        return authoritativeTemperature() < material.thermal().meltingPoint()
                ? Optional.of(material)
                : Optional.empty();
    }

    private Optional<Batch> currentSteelmakingBatch() {
        return steelmaking.currentBatch(contents.composition());
    }

    private boolean pourAlloy(MoldHost mold, Direction moldSide, float temperature) {
        Optional<AlloyMatch> alloy = contents.alloy();
        if (alloy.isEmpty()) {
            return false;
        }
        MaterialDefinition result = alloy.get().result();
        if (temperature < result.thermal().meltingPoint()) {
            return false;
        }
        int required = mold.moldRequiredMaterialUnits();
        Map<String, Integer> cost = scaleAlloyCost(alloy.get().costPerIngot(), required);
        if (cost == null || !contents.containsAtLeast(cost)) {
            return false;
        }
        int consumed = mold.fillMold(result.id(), required, temperature, moldSide);
        if (consumed <= 0) {
            return false;
        }
        contents.removeAll(cost);
        onMutation.run();
        return true;
    }

    private static Map<String, Integer> scaleAlloyCost(
            Map<String, Integer> costPerIngot, int requiredUnits) {
        if (requiredUnits <= 0) {
            return null;
        }
        int ingot = MaterialPrefixes.INGOT.units();
        Map<String, Integer> scaled = new LinkedHashMap<>();
        for (var component : costPerIngot.entrySet()) {
            long numerator = (long) component.getValue() * requiredUnits;
            if (numerator % ingot != 0L) {
                return null;
            }
            int units = Math.toIntExact(numerator / ingot);
            if (units <= 0) {
                return null;
            }
            scaled.put(component.getKey(), units);
        }
        return scaled;
    }

    private static boolean pourableMelt(MaterialDefinition material, float temperature) {
        if (temperature < material.thermal().meltingPoint()) {
            return false;
        }
        return material.gt6Metadata()
                .map(metadata -> metadata.processingTargets().get("smelting"))
                .map(target -> target.material().equals(material.id()))
                .orElse(true);
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
            return MoldCastingRules.smallestBatch(alloy.get().costPerIngot(), form)
                    .map(batch -> new CastCandidate(
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

    private static double unitWeightGrams(String materialId, int units) {
        return MaterialCatalog.require(materialId).thermal().density()
                * CM3_PER_UNIT
                * units;
    }

    private double totalWeightGrams() {
        return casing.massGrams() + contents.contentsWeightGrams(CM3_PER_UNIT);
    }

    private boolean fluidTransferBlocked() {
        return hasUnknownMaterials() || steelmaking.blocksFluidTransfer();
    }

    private Optional<DrainCandidate> drainCandidate(int requested) {
        if (frozen() || requested <= 0 || !isMolten() || fluidTransferBlocked()) {
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

    private FluidStack executeDrain(DrainCandidate candidate, IFluidHandler.FluidAction action) {
        FluidStack result = new FluidStack(
                ModFluids.molten(candidate.material().id()).orElseThrow().source().get(),
                candidate.plan().amount());
        if (action.execute()) {
            contents.removeAll(candidate.plan().removals());
            onMutation.run();
        }
        return result;
    }

    public record TickOutcome(
            boolean meltedDown,
            boolean mutated,
            boolean boiled,
            boolean acidDestroyed,
            float explodeRadius) {
        private static final TickOutcome IDLE =
                new TickOutcome(false, false, false, false, 0.0F);

        public boolean exploded() {
            return explodeRadius > 0.0F;
        }

        public boolean destroysHost() {
            return meltedDown || acidDestroyed || exploded();
        }
    }

    public record ScrapTake(
            MaterialDefinition material,
            int count,
            int unitsRemoved,
            boolean discardedRemainder) {}

    public record CastTransfer(
            MaterialDefinition material,
            MaterialPrefix form,
            int count,
            float temperature) {}

    private record CastCandidate(
            MaterialDefinition material,
            Map<String, Integer> cost,
            int outputCount,
            double meltingPoint) {}

    private record DrainCandidate(
            MaterialDefinition material,
            MoltenTransferMath.DrainPlan plan) {}

    private final class CoreFluidHandler implements IFluidHandler {
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
                            ModFluids.molten(candidate.material().id())
                                    .orElseThrow()
                                    .source()
                                    .get(),
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
            if (frozen() || resource.isEmpty()) {
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
                applyAdditions(plan.get().additions(), plan.get().inputTemperature());
            }
            return plan.get().accepted();
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            if (resource.isEmpty()) {
                return FluidStack.EMPTY;
            }
            Optional<MaterialDefinition> requestedMaterial =
                    ModFluids.material(resource.getFluid());
            if (requestedMaterial.isEmpty()) {
                return FluidStack.EMPTY;
            }
            return drainCandidate(resource.getAmount())
                    .filter(candidate -> candidate.material().id()
                            .equals(requestedMaterial.get().id())
                            && resource.getFluid()
                                    == ModFluids.molten(candidate.material().id())
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
