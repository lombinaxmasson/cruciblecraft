package com.masson.cruciblecraft.census;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;

/**
 * Bidirectional runtime registry equality gate for recipe census. Uses the
 * committed classpath fixture derived from the tooling runtime-registry artifact.
 * Test evidence only; not part of player runtime.
 */
public final class RecipeCensusRuntimeRegistryGate {
    private RecipeCensusRuntimeRegistryGate() {}

    public static List<RecipeCensusRuntimeRegistryEnumerator.CategoryDiff> verify(
            ServerLevel level) {
        RecipeCensusRuntimeRegistryGateFixture expected =
                RecipeCensusRuntimeRegistryGateFixture.load();
        List<RecipeCensusRuntimeRegistryEnumerator.CategoryDiff> diffs =
                new ArrayList<>();
        for (String category : expected.categories().keySet()) {
            List<String> actual = RecipeCensusRuntimeRegistryEnumerator.enumerate(
                    level,
                    category,
                    expected.namespace());
            diffs.add(RecipeCensusRuntimeRegistryEnumerator.compareCategory(
                    category,
                    expected.expectedIds(category),
                    actual));
        }
        return List.copyOf(diffs);
    }
}
