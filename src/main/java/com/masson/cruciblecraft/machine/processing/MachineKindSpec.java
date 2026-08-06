package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;

import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;

/**
 * Behavior shared by every tier of one machine kind.
 *
 * <p>The wrapped legacy spec is a temporary compatibility carrier for the
 * already mature slot/tank/UI policies. Numeric energy capability is replaced
 * by {@link TierProfile} when a {@link MachineVariant} is created.
 */
public record MachineKindSpec(
        ResourceLocation id,
        ProcessingMachineSpec behavior,
        OverclockPolicy overclockPolicy,
        boolean parallelDuration) {

    public MachineKindSpec {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(behavior, "behavior");
        Objects.requireNonNull(overclockPolicy, "overclockPolicy");
    }

    public RecipeMap requireRecipeMap() {
        return behavior.requireRecipeMap();
    }

    public ResourceLocation recipeMapId() {
        return behavior.recipeMapId();
    }

    public enum OverclockPolicy {
        LEGACY_TICKS,
        STANDARD,
        CHEAP
    }
}
