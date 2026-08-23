package com.masson.cruciblecraft.census;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.server.level.ServerLevel;

/**
 * Bidirectional runtime registry equality gate for T35 census. Uses the
 * committed classpath fixture derived from {@code tools/t35_runtime_registry.json}.
 * Test evidence only; not part of player runtime.
 */
public final class T35RuntimeRegistryGate {
    private T35RuntimeRegistryGate() {}

    public static List<T35RuntimeRegistryEnumerator.CategoryDiff> verify(
            ServerLevel level) {
        T35RuntimeRegistryGateFixture expected =
                T35RuntimeRegistryGateFixture.load();
        List<T35RuntimeRegistryEnumerator.CategoryDiff> diffs =
                new ArrayList<>();
        for (String category : expected.categories().keySet()) {
            List<String> actual = T35RuntimeRegistryEnumerator.enumerate(
                    level,
                    category,
                    expected.namespace());
            diffs.add(T35RuntimeRegistryEnumerator.compareCategory(
                    category,
                    expected.expectedIds(category),
                    actual));
        }
        return List.copyOf(diffs);
    }
}
