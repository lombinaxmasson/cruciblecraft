package com.masson.cruciblecraft.gametest.support;

import java.util.LinkedHashMap;
import java.util.Map;

import com.masson.cruciblecraft.recipe.gt.CompactPublicationPolicyDefinition;
import com.masson.cruciblecraft.registry.ModRecipes;

import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;

/**
 * Live compact-publication {@code relation_count} values. GameTests compare
 * family rows to these instead of copying the number into the test source.
 */
public final class PublicationPolicyCounts {
    private PublicationPolicyCounts() {}

    public static int relationCount(
            GameTestHelper helper, ResourceLocation publicationGroup) {
        CompactPublicationPolicyDefinition definition =
                index(helper).get(publicationGroup);
        if (definition == null) {
            throw new IllegalStateException(
                    "Missing compact publication policy " + publicationGroup);
        }
        return count(definition);
    }

    public static int relationCountSum(
            GameTestHelper helper, ResourceLocation... publicationGroups) {
        int sum = 0;
        for (ResourceLocation publicationGroup : publicationGroups) {
            sum += relationCount(helper, publicationGroup);
        }
        return sum;
    }

    public static int relationCountForMap(
            GameTestHelper helper, ResourceLocation targetMap) {
        int sum = 0;
        int matches = 0;
        for (CompactPublicationPolicyDefinition definition : index(helper).values()) {
            if (definition.targetMap().equals(targetMap)) {
                sum += count(definition);
                matches++;
            }
        }
        if (matches == 0) {
            throw new IllegalStateException(
                    "No compact publication policy for " + targetMap);
        }
        return sum;
    }

    public static int groupCount(GameTestHelper helper, String pathPrefix) {
        int matches = 0;
        for (ResourceLocation group : index(helper).keySet()) {
            if (group.getPath().startsWith(pathPrefix)) {
                matches++;
            }
        }
        if (matches == 0) {
            throw new IllegalStateException(
                    "No compact publication policy under " + pathPrefix);
        }
        return matches;
    }

    public static int relationCountWithPrefix(
            GameTestHelper helper, String pathPrefix) {
        int sum = 0;
        int matches = 0;
        for (CompactPublicationPolicyDefinition definition : index(helper).values()) {
            if (definition.publicationGroup().getPath().startsWith(pathPrefix)) {
                sum += count(definition);
                matches++;
            }
        }
        if (matches == 0) {
            throw new IllegalStateException(
                    "No compact publication policy under " + pathPrefix);
        }
        return sum;
    }

    private static int count(CompactPublicationPolicyDefinition definition) {
        if (definition.relationCount().isEmpty()) {
            throw new IllegalStateException(
                    "Publication policy has no relation_count: "
                            + definition.publicationGroup());
        }
        return definition.relationCount().get();
    }

    private static Map<ResourceLocation, CompactPublicationPolicyDefinition> index(
            GameTestHelper helper) {
        Map<ResourceLocation, CompactPublicationPolicyDefinition> indexed =
                new LinkedHashMap<>();
        helper.getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipes.COMPACT_PUBLICATION_POLICY_TYPE.get())
                .forEach(holder -> {
                    CompactPublicationPolicyDefinition definition =
                            holder.value().definition();
                    CompactPublicationPolicyDefinition previous = indexed.put(
                            definition.publicationGroup(), definition);
                    if (previous != null) {
                        throw new IllegalStateException(
                                "Duplicate compact publication policy "
                                        + definition.publicationGroup());
                    }
                });
        return indexed;
    }
}
