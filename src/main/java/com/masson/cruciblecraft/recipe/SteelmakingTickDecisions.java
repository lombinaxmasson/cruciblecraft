package com.masson.cruciblecraft.recipe;

import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.recipe.SteelmakingProcess.Batch;

/** Pure authoritative-tick decisions for validating persisted steel batches. */
public final class SteelmakingTickDecisions {
    private SteelmakingTickDecisions() {}

    public static Decision evaluate(
            Map<String, Integer> composition,
            int steelBatchIronUnits,
            long storedAir) {
        if (steelBatchIronUnits <= 0) {
            return new Decision(Action.INACTIVE, Optional.empty());
        }
        Optional<Batch> batch = SteelmakingProcess.resume(steelBatchIronUnits)
                .filter(candidate -> matches(composition, candidate));
        if (batch.isEmpty()) {
            return new Decision(Action.RESET_STALE, Optional.empty());
        }
        return new Decision(
                storedAir >= 1L ? Action.PROCESS : Action.WAIT_FOR_AIR,
                batch);
    }

    public static boolean matches(Map<String, Integer> composition, Batch batch) {
        if (composition.size() != 2
                || composition.getOrDefault(SteelmakingProcess.IRON, 0) != batch.ironUnits()) {
            return false;
        }
        int carbon = composition.getOrDefault(SteelmakingProcess.CARBON, 0);
        return carbon >= batch.minimumSteelCarbonUnits()
                && carbon <= batch.ironUnits() / 3;
    }

    public record Decision(Action action, Optional<Batch> batch) {}

    public enum Action {
        INACTIVE,
        RESET_STALE,
        WAIT_FOR_AIR,
        PROCESS
    }
}
