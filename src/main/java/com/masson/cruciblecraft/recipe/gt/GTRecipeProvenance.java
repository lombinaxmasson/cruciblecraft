package com.masson.cruciblecraft.recipe.gt;

import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Visible source provenance carried by generated concrete GT recipes. */
public record GTRecipeProvenance(
        String sourceKind,
        Optional<String> selectedSourceRecipe) {
    public static final Codec<GTRecipeProvenance> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("source_kind")
                            .forGetter(GTRecipeProvenance::sourceKind),
                    Codec.STRING.optionalFieldOf("selected_source_recipe")
                            .forGetter(GTRecipeProvenance::selectedSourceRecipe)
            ).apply(instance, GTRecipeProvenance::new));

    public GTRecipeProvenance {
        Objects.requireNonNull(sourceKind, "sourceKind");
        Objects.requireNonNull(selectedSourceRecipe, "selectedSourceRecipe");
        if (sourceKind.isBlank()) {
            throw new IllegalArgumentException("Provenance source kind must not be blank");
        }
        if (selectedSourceRecipe.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Selected source recipe must not be blank");
        }
    }
}
