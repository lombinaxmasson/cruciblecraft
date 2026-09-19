package com.masson.cruciblecraft.machine.component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata.MaterialAmount;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.AlloyIndex.AlloyMatch;
import com.masson.cruciblecraft.recipe.AlloyIndex.Conversion;

/** Mutable material-unit storage with derived composition caches. */
public final class CompositionTank {
    public static final float FLAMMABLE_BURN_CELSIUS = 40.0F;

    private final Map<String, Integer> contents = new LinkedHashMap<>();
    private Map<String, Integer> snapshot = Map.of();
    private Optional<AlloyMatch> alloy = Optional.empty();
    private Optional<MaterialDefinition> resolvedMaterial = Optional.empty();
    private boolean unknownMaterials;
    private int totalUnits;
    private double moltenThreshold = Double.POSITIVE_INFINITY;
    private int moltenColor = 0xFFFFFF;
    private boolean cacheValid;
    private long cachedRuntimeRevision = -1L;

    public void replace(Map<String, Integer> replacement) {
        contents.clear();
        replacement.forEach((material, units) -> {
            if (material != null && units != null && units > 0) {
                contents.put(material, units);
            }
        });
        invalidate();
    }

    public void addAll(Map<String, Integer> additions) {
        additions.forEach((material, units) -> {
            if (units != null && units > 0) {
                contents.merge(material, units, Math::addExact);
            }
        });
        invalidate();
    }

    public boolean containsAtLeast(Map<String, Integer> amounts) {
        return amounts.entrySet().stream()
                .allMatch(entry -> contents.getOrDefault(entry.getKey(), 0) >= entry.getValue());
    }

    public boolean removeAll(Map<String, Integer> removals) {
        if (!containsAtLeast(removals)) {
            return false;
        }
        removals.forEach((material, units) -> setUnits(
                material,
                contents.getOrDefault(material, 0) - units));
        invalidate();
        return true;
    }

    public void setUnits(String material, int units) {
        if (units > 0) {
            contents.put(material, units);
        } else {
            contents.remove(material);
        }
        invalidate();
    }

    public void merge(String material, int units) {
        if (units <= 0) {
            return;
        }
        contents.merge(material, units, Math::addExact);
        invalidate();
    }

    public boolean applyAlloy(Conversion conversion) {
        if (conversion == null || !removeAll(conversion.consumption())) {
            return false;
        }
        merge(conversion.recipe().resultId(), conversion.outputUnits());
        return true;
    }

    public boolean removeBoiling(float temperature) {
        if (unknownMaterials()) {
            return false;
        }
        boolean removed = contents.keySet().removeIf(id ->
                CrucibleThermalModel.shouldBoil(
                        temperature,
                        MaterialCatalog.require(id).thermal().boilingPoint()));
        if (removed) {
            invalidate();
        }
        return removed;
    }

    /**
     * GT6 smeltery/crucible content loop: boil or burn, acid, explosives, then
     * smelting/solidifying targets when temperature crosses a melting point.
     */
    public ContentReaction react(
            float temperature,
            float previousTemperature,
            boolean contentChanged,
            boolean acidProof) {
        if (unknownMaterials()) {
            return ContentReaction.NONE;
        }
        boolean boiled = false;
        Map<String, Integer> toAdd = new LinkedHashMap<>();
        var iterator = contents.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            String id = entry.getKey();
            int amount = entry.getValue();
            if (id == null || amount <= 0 || "air".equals(id) || !MaterialCatalog.contains(id)) {
                iterator.remove();
                invalidate();
                continue;
            }
            MaterialDefinition material = MaterialCatalog.require(id);
            if (material.thermal().density() <= 0.0012) {
                iterator.remove();
                invalidate();
                boiled = true;
                continue;
            }
            if (shouldBurnOff(material, temperature)) {
                iterator.remove();
                invalidate();
                boiled = true;
                if (material.hasMaterialTag("PROPERTIES.EXPLOSIVE")) {
                    int exploded = amount;
                    contents.clear();
                    invalidate();
                    return ContentReaction.exploded(exploded);
                }
                continue;
            }
            if (!acidProof && material.hasMaterialTag("PROPERTIES.ACID")) {
                contents.clear();
                invalidate();
                return ContentReaction.ACID;
            }
            boolean nowMolten = temperature >= material.thermal().meltingPoint();
            boolean wasMolten = previousTemperature >= material.thermal().meltingPoint();
            if (nowMolten && (contentChanged || !wasMolten)) {
                Optional<PhaseChange> smelted = phaseChange(material, amount, "smelting");
                if (smelted.isPresent() && !smelted.get().identity()) {
                    iterator.remove();
                    invalidate();
                    toAdd.merge(smelted.get().materialId(), smelted.get().units(), Math::addExact);
                }
            } else if (!nowMolten && (contentChanged || wasMolten)) {
                Optional<PhaseChange> solidified = phaseChange(material, amount, "solidifying");
                if (solidified.isPresent() && !solidified.get().identity()) {
                    iterator.remove();
                    invalidate();
                    toAdd.merge(
                            solidified.get().materialId(),
                            solidified.get().units(),
                            Math::addExact);
                }
            }
        }
        if (!toAdd.isEmpty()) {
            addAll(toAdd);
        }
        return boiled ? ContentReaction.BOILED : ContentReaction.NONE;
    }

    public void clear() {
        if (!contents.isEmpty()) {
            contents.clear();
            invalidate();
        }
    }

    public Map<String, Integer> composition() {
        ensureCache();
        return snapshot;
    }

    public int units(String material) {
        return contents.getOrDefault(material, 0);
    }

    public int totalUnits() {
        ensureCache();
        return totalUnits;
    }

    public boolean unknownMaterials() {
        ensureCache();
        return unknownMaterials;
    }

    public Optional<AlloyMatch> alloy() {
        ensureCache();
        return alloy;
    }

    public Optional<MaterialDefinition> resolvedMaterial() {
        ensureCache();
        return resolvedMaterial;
    }

    public double moltenThreshold() {
        ensureCache();
        return moltenThreshold;
    }

    public int moltenColor() {
        ensureCache();
        return moltenColor;
    }

    public int moltenColor(float temperature) {
        return lightestMolten(temperature)
                .map(MaterialDefinition::colorRgb)
                .orElseGet(this::moltenColor);
    }

    public Optional<String> lightestId() {
        ensureCache();
        if (unknownMaterials || contents.isEmpty()) {
            return Optional.empty();
        }
        String lightest = null;
        double density = Double.POSITIVE_INFINITY;
        for (String id : contents.keySet()) {
            double candidate = MaterialCatalog.require(id).thermal().density();
            if (candidate < density) {
                density = candidate;
                lightest = id;
            }
        }
        return Optional.ofNullable(lightest);
    }

    public Optional<MaterialDefinition> lightestMolten(float temperature) {
        ensureCache();
        if (unknownMaterials) {
            return Optional.empty();
        }
        MaterialDefinition lightest = null;
        double density = Double.POSITIVE_INFINITY;
        for (var entry : contents.entrySet()) {
            if (entry.getValue() <= 0 || !MaterialCatalog.contains(entry.getKey())) {
                continue;
            }
            MaterialDefinition material = MaterialCatalog.require(entry.getKey());
            if (temperature < material.thermal().meltingPoint()) {
                continue;
            }
            if (material.thermal().density() < density) {
                density = material.thermal().density();
                lightest = material;
            }
        }
        return Optional.ofNullable(lightest);
    }

    public double contentsWeightGrams(double cm3PerUnit) {
        if (!Double.isFinite(cm3PerUnit) || cm3PerUnit < 0.0) {
            throw new IllegalArgumentException("Unit volume must be finite and non-negative");
        }
        if (unknownMaterials()) {
            return 0.0;
        }
        double weight = 0.0;
        for (var entry : contents.entrySet()) {
            weight += MaterialCatalog.require(entry.getKey()).thermal().density()
                    * cm3PerUnit
                    * entry.getValue();
        }
        return weight;
    }

    private static boolean shouldBurnOff(MaterialDefinition material, float temperature) {
        if (CrucibleThermalModel.shouldBoil(temperature, material.thermal().boilingPoint())) {
            return true;
        }
        return temperature > FLAMMABLE_BURN_CELSIUS
                && material.hasMaterialTag("PROPERTIES.FLAMMABLE")
                && !material.hasMaterialTag("PROPERTIES.UNBURNABLE")
                && !material.hasMaterialTag("PROCESSING.MELTING");
    }

    private static Optional<PhaseChange> phaseChange(
            MaterialDefinition source, int units, String key) {
        Optional<MaterialAmount> target = source.gt6Metadata()
                .map(metadata -> metadata.processingTargets().get(key));
        if (target.isEmpty() || target.get().ccUnits().isEmpty()) {
            return Optional.empty();
        }
        String materialId = target.get().material();
        int produced = Math.toIntExact(
                ((long) units * target.get().ccUnits().orElseThrow())
                        / MaterialPrefixes.INGOT.units());
        boolean identity = materialId.equals(source.id()) && produced == units;
        return Optional.of(new PhaseChange(materialId, produced, identity));
    }

    private void ensureCache() {
        long runtimeRevision = MaterialCatalog.runtimeRevision();
        if (cacheValid && cachedRuntimeRevision == runtimeRevision) {
            return;
        }
        snapshot = Map.copyOf(contents);
        unknownMaterials = contents.keySet().stream().anyMatch(id -> !MaterialCatalog.contains(id));
        totalUnits = contents.values().stream().mapToInt(Integer::intValue).sum();
        alloy = unknownMaterials ? Optional.empty() : MaterialCatalog.alloys().match(snapshot);
        resolvedMaterial = unknownMaterials
                ? Optional.empty()
                : CrucibleTransferCoordinator.resolveCurrentMaterial(snapshot);
        moltenThreshold = unknownMaterials
                ? Double.POSITIVE_INFINITY
                : resolvedMaterial
                        .map(material -> material.thermal().meltingPoint())
                        .orElseGet(this::allMaterialsMeltingPoint);
        moltenColor = calculateColor();
        cachedRuntimeRevision = runtimeRevision;
        cacheValid = true;
    }

    private double allMaterialsMeltingPoint() {
        return contents.keySet().stream()
                .map(MaterialCatalog::require)
                .mapToDouble(material -> material.thermal().meltingPoint())
                .max()
                .orElse(Double.POSITIVE_INFINITY);
    }

    private int calculateColor() {
        if (unknownMaterials) {
            return 0xFFFFFF;
        }
        long total = 0L;
        long red = 0L;
        long green = 0L;
        long blue = 0L;
        for (var entry : contents.entrySet()) {
            int color = MaterialCatalog.require(
                    entry.getKey()).colorRgb();
            int units = entry.getValue();
            total += units;
            red += (long) ((color >> 16) & 0xFF) * units;
            green += (long) ((color >> 8) & 0xFF) * units;
            blue += (long) (color & 0xFF) * units;
        }
        return total == 0L
                ? 0xFFFFFF
                : ((int) (red / total) << 16)
                        | ((int) (green / total) << 8)
                        | (int) (blue / total);
    }

    private void invalidate() {
        cacheValid = false;
    }

    public record ContentReaction(boolean boiled, boolean acidDestroyed, int explodedUnits) {
        static final ContentReaction NONE = new ContentReaction(false, false, 0);
        static final ContentReaction BOILED = new ContentReaction(true, false, 0);
        static final ContentReaction ACID = new ContentReaction(true, true, 0);

        static ContentReaction exploded(int units) {
            return new ContentReaction(true, false, Math.max(1, units));
        }

        boolean exploded() {
            return explodedUnits > 0;
        }
    }

    private record PhaseChange(String materialId, int units, boolean identity) {}
}
