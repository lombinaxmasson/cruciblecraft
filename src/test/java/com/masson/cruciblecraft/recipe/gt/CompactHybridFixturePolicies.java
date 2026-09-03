package com.masson.cruciblecraft.recipe.gt;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

final class CompactHybridFixturePolicies {
    private CompactHybridFixturePolicies() {}

    static CompactRecipeFamilyProvider.MaterializationPolicy assemblerCompactSelector(
            List<CompactRecipeFamilySource> sources) {
        List<String> familyIds = sources.stream()
                .map(source -> source.definition().familyId())
                .sorted()
                .toList();
        Set<String> firstTen = new HashSet<>(
                familyIds.subList(0, Math.min(10, familyIds.size())));
        Set<ResourceLocation> eagerStableIds = new HashSet<>();
        for (CompactRecipeFamilySource source : sources) {
            for (CompactGTRecipeFamilyDefinition.Relation relation
                    : source.authoredRelations()) {
                if (relation.duration() <= 16
                        || firstTen.contains(source.definition().familyId())) {
                    eagerStableIds.add(relation.stableId());
                }
            }
        }
        return CompactRecipeFamilyProvider.MaterializationPolicy.hybrid(
                8,
                (index, relation) -> eagerStableIds.contains(relation.stableId()));
    }
}
