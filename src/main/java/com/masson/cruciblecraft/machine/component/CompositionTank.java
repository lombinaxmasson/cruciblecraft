package com.masson.cruciblecraft.machine.component;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator;
import com.masson.cruciblecraft.heat.CrucibleThermalModel;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.AlloyIndex.AlloyMatch;

/** Mutable material-unit storage with derived composition caches. */
public final class CompositionTank {
    private final Map<String, Integer> contents = new HashMap<>();
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
}
