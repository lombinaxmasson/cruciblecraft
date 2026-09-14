package com.masson.cruciblecraft.machine.component;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.air.AirOutputModel;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.mold.MoldCastingRules;
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
import com.masson.cruciblecraft.recipe.SteelmakingProcess;
import com.masson.cruciblecraft.recipe.SteelmakingProcess.Batch;
import com.masson.cruciblecraft.registry.ModFluids;

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
    private static final double CASING_VOLUME_CM3 = 800.0;
    private static final double CM3_PER_UNIT = 0.15;

    private final int maxIngots;
    private final CompositionTank contents = new CompositionTank();
    private final MachineCasing casing = new MachineCasing(Device.CRUCIBLE, CASING_VOLUME_CM3);
    private final ThermalComponent thermal = new ThermalComponent(AMBIENT_TEMPERATURE);
    private final SteelmakingController steelmaking = new SteelmakingController();
    private final IFluidHandler fluids = new CoreFluidHandler();
    private boolean frozen;
    private Runnable onMutation = () -> {};

    public CrucibleProcessCore(int maxIngots) {
        if (maxIngots <= 0) {
            throw new IllegalArgumentException("maxIngots must be positive");
        }
        this.maxIngots = maxIngots;
    }

    public static CrucibleProcessCore singleBlock() {
        return new CrucibleProcessCore(SINGLE_BLOCK_MAX_INGOTS);
    }

    public static CrucibleProcessCore large() {
        return new CrucibleProcessCore(LARGE_MAX_INGOTS);
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
        if (authoritative && !unknownMaterials) {
            boiled = contents.removeBoiling(thermal.authoritativeTemperature());
            steelMutated = steelmaking.tick(
                    contents, thermal.authoritativeTemperature()).immediateMutation();
        }
        thermal.advance(
                unknownMaterials ? 0L : incomingEnergy,
                totalWeightGrams());
        if (thermal.authoritativeTemperature() > casingMaxTemperature()) {
            if (authoritative) {
                contents.clear();
            }
            return new TickOutcome(true, true, boiled);
        }
        return new TickOutcome(false, boiled || steelMutated, boiled);
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
        return contents.moltenColor();
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
        return casing.maxTemperature();
    }

    public void save(CompoundTag tag) {
        CompoundTag savedComposition = new CompoundTag();
        contents.composition().forEach(savedComposition::putInt);
        tag.put("composition", savedComposition);
        tag.putString("casing_material_id", casing.persistedMaterialId());
        tag.putFloat("temperature", thermal.authoritativeTemperature());
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

    public record TickOutcome(boolean meltedDown, boolean mutated, boolean boiled) {
        private static final TickOutcome IDLE = new TickOutcome(false, false, false);
    }

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
