package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

/** Pure persistence/checkpoint decisions used directly by the processing host. */
final class ProcessingHostDecisions {
    private ProcessingHostDecisions() {}

    record IdleTransition(boolean persistentChange) {}

    static IdleTransition idle(
            boolean selectionCleared,
            boolean runtimeReset,
            long previousPowerDemand) {
        return new IdleTransition(
                selectionCleared || runtimeReset || previousPowerDemand != 0L);
    }

    enum SelectionPersistence {
        REUSE_RESTORED_ROLL,
        RESET_AND_REROLL
    }

    static SelectionPersistence selection(
            boolean restoredValid,
            String restoredRecipeId,
            String restoredFingerprint,
            String candidateRecipeId,
            Optional<String> candidateFingerprint) {
        return restoredValid
                && restoredRecipeId.equals(candidateRecipeId)
                && candidateFingerprint.isPresent()
                && restoredFingerprint.equals(candidateFingerprint.get())
                        ? SelectionPersistence.REUSE_RESTORED_ROLL
                        : SelectionPersistence.RESET_AND_REROLL;
    }
}
