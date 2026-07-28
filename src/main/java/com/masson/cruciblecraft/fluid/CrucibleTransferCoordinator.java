package com.masson.cruciblecraft.fluid;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.machine.MachineMaterialRules;

/**
 * Pure transfer planning shared by capability simulation, execution and player
 * insertion. Executing callers apply a returned plan without re-validating it.
 */
public final class CrucibleTransferCoordinator {
    private CrucibleTransferCoordinator() {}

    public static InsertionPlan planInsertion(
            MaterialUnits.Entry entry,
            int currentUnits,
            int capacity,
            String casingMaterialId) {
        if (entry == null || entry.units() <= 0 || !MaterialCatalog.contains(entry.material().id())) {
            return InsertionPlan.failure(InsertResult.INVALID_MATERIAL);
        }
        if (!MachineMaterialRules.canCrucibleProcess(casingMaterialId, entry.material())) {
            return InsertionPlan.failure(InsertResult.TIER_TOO_LOW);
        }
        int quantum = MaterialCatalog.decompositionQuantum(entry.material());
        if (entry.units() % quantum != 0) {
            return InsertionPlan.failure(InsertResult.INEXACT_DECOMPOSITION);
        }
        if (currentUnits > capacity - entry.units()) {
            return InsertionPlan.failure(InsertResult.FULL);
        }
        return new InsertionPlan(
                InsertResult.SUCCESS,
                MaterialCatalog.decompose(entry.material(), entry.units()));
    }

    public static Optional<FillPlan> planFill(
            MaterialDefinition material,
            int requested,
            int currentUnits,
            int capacity,
            String casingMaterialId,
            boolean transferBlocked) {
        if (material == null || requested <= 0 || transferBlocked
                || !MachineMaterialRules.canCrucibleProcess(casingMaterialId, material)) {
            return Optional.empty();
        }
        int quantum = MaterialCatalog.decompositionQuantum(material);
        int accepted = MoltenTransferMath.planFill(requested, capacity - currentUnits, quantum);
        if (accepted <= 0) {
            return Optional.empty();
        }
        Map<String, Integer> additions = MaterialCatalog.decompose(material, accepted);
        return Optional.of(new FillPlan(
                material,
                accepted,
                additions,
                (float) material.thermal().meltingPoint()));
    }

    /**
     * Resolves exactly the same current material identity used for casting:
     * exact alloy first, otherwise one pure/no-decompose material only.
     */
    public static Optional<MaterialDefinition> resolveCurrentMaterial(Map<String, Integer> composition) {
        var alloy = MaterialCatalog.alloys().match(composition);
        if (alloy.isPresent()) {
            return Optional.of(alloy.get().result());
        }
        if (composition.size() != 1) {
            return Optional.empty();
        }
        String id = composition.keySet().iterator().next();
        if (!MaterialCatalog.contains(id)) {
            return Optional.empty();
        }
        MaterialDefinition material = MaterialCatalog.require(id);
        return material.composition().isEmpty() || material.noDecompose()
                ? Optional.of(material)
                : Optional.empty();
    }

    public enum InsertResult {
        SUCCESS,
        FULL,
        TIER_TOO_LOW,
        INEXACT_DECOMPOSITION,
        INVALID_MATERIAL
    }

    public record InsertionPlan(InsertResult result, Map<String, Integer> additions) {
        public InsertionPlan {
            additions = Map.copyOf(additions);
        }

        private static InsertionPlan failure(InsertResult result) {
            return new InsertionPlan(result, Map.of());
        }
    }

    public record FillPlan(
            MaterialDefinition material,
            int accepted,
            Map<String, Integer> additions,
            float inputTemperature) {
        public FillPlan {
            additions = Map.copyOf(additions);
        }
    }
}
