package com.masson.cruciblecraft.machine.processing;

import java.util.Optional;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/** Adopts legacy Crusher output-item progress only after matching current input. */
public final class CrusherLegacyMigration {
    private CrusherLegacyMigration() {}

    public record Hint(
            String outputItemId,
            int progress,
            int duration,
            String status) {
        public Hint {
            outputItemId = outputItemId == null ? "" : outputItemId;
            progress = Math.max(0, progress);
            duration = Math.max(0, duration);
            progress = Math.min(progress, duration);
            status = status == null ? "idle" : status;
        }
    }

    public record Adoption(
            String recipeId,
            int progress,
            int duration,
            String status,
            boolean preservedProgress) {}

    public record BoundHint(Hint hint, String inputFingerprint) {
        public BoundHint {
            if (hint == null || inputFingerprint == null || inputFingerprint.isBlank()) {
                throw new IllegalArgumentException("Bound legacy hint requires an input hash");
            }
        }
    }

    public static Optional<BoundHint> bind(
            Hint hint,
            ItemStack loadedInput,
            HolderLookup.Provider registries) {
        return GTRecipeFingerprint.input(loadedInput, registries)
                .map(fingerprint -> new BoundHint(hint, fingerprint));
    }

    public static boolean inputUnchanged(
            BoundHint bound,
            ItemStack currentInput,
            HolderLookup.Provider registries) {
        return GTRecipeFingerprint.input(currentInput, registries)
                .map(bound.inputFingerprint()::equals)
                .orElse(false);
    }

    public static Adoption adopt(
            BoundHint bound,
            ResourceLocation recipeId,
            int recipeDuration,
            ItemStack candidateOutput) {
        Hint hint = bound.hint();
        ResourceLocation legacyOutput = ResourceLocation.tryParse(hint.outputItemId());
        boolean matches = legacyOutput != null
                && !candidateOutput.isEmpty()
                && legacyOutput.equals(BuiltInRegistries.ITEM.getKey(candidateOutput.getItem()));
        return matches
                ? new Adoption(
                        recipeId.toString(),
                        Math.min(hint.progress(), Math.max(0, recipeDuration)),
                        Math.max(0, recipeDuration),
                        hint.status(),
                        true)
                : new Adoption(recipeId.toString(), 0, 0, "idle", false);
    }
}
