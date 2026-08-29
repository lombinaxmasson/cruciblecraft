package com.masson.cruciblecraft.recipe.gt;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** Stable policy and publication identity for one compact recipe group. */
public record PublicationGroupKey(
        ResourceLocation targetMap,
        ResourceLocation publicationGroup)
        implements Comparable<PublicationGroupKey> {

    public PublicationGroupKey {
        Objects.requireNonNull(targetMap, "targetMap");
        Objects.requireNonNull(publicationGroup, "publicationGroup");
    }

    @Override
    public int compareTo(PublicationGroupKey other) {
        Objects.requireNonNull(other, "other");
        int targetOrder = targetMap.toString().compareTo(other.targetMap.toString());
        if (targetOrder != 0) {
            return targetOrder;
        }
        return publicationGroup.toString().compareTo(
                other.publicationGroup.toString());
    }
}
