package com.masson.cruciblecraft.recipe.gt;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** Authored compact family handle passed into {@link CompactRecipeFamilyProvider}. */
public record CompactRecipeFamilySource(
        ResourceLocation id,
        CompactGTRecipeFamilyDefinition definition) {
    public CompactRecipeFamilySource {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(definition, "definition");
    }
}
