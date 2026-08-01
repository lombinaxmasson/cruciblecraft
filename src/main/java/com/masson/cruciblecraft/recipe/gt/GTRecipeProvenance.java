package com.masson.cruciblecraft.recipe.gt;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Visible source evidence carried by generated concrete GT recipes. */
public record GTRecipeProvenance(
        String sourceKind,
        Optional<String> selectedSourceRecipe,
        List<String> evidenceHashes) {
    public static final Codec<GTRecipeProvenance> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.fieldOf("source_kind")
                            .forGetter(GTRecipeProvenance::sourceKind),
                    Codec.STRING.optionalFieldOf("selected_source_recipe")
                            .forGetter(GTRecipeProvenance::selectedSourceRecipe),
                    Codec.STRING.listOf().optionalFieldOf("evidence_hashes", List.of())
                            .forGetter(GTRecipeProvenance::evidenceHashes)
            ).apply(instance, GTRecipeProvenance::new));

    public GTRecipeProvenance {
        Objects.requireNonNull(sourceKind, "sourceKind");
        Objects.requireNonNull(selectedSourceRecipe, "selectedSourceRecipe");
        evidenceHashes = List.copyOf(evidenceHashes);
        if (sourceKind.isBlank()) {
            throw new IllegalArgumentException("Provenance source kind must not be blank");
        }
        if (selectedSourceRecipe.stream().anyMatch(String::isBlank)) {
            throw new IllegalArgumentException(
                    "Selected source recipe must not be blank");
        }
        if (evidenceHashes.stream().anyMatch(
                hash -> hash == null || hash.isBlank())) {
            throw new IllegalArgumentException(
                    "Provenance evidence hashes must not be blank");
        }
    }
}
