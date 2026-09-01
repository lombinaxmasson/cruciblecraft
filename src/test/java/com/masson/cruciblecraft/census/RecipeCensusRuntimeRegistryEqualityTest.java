package com.masson.cruciblecraft.census;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RecipeCensusRuntimeRegistryEqualityTest {
    @Test
    void bidirectionalSetEqualityPassesWhenSetsMatch() {
        List<String> expected = List.of(
                "cruciblecraft:alpha",
                "cruciblecraft:beta");
        var diff = RecipeCensusRuntimeRegistryEnumerator.compareCategory(
                "items",
                expected,
                List.of("cruciblecraft:beta", "cruciblecraft:alpha"));
        assertTrue(diff.ok());
        assertEquals(0, diff.missingCount());
        assertEquals(0, diff.extraCount());
        assertEquals(0, diff.duplicateCount());
    }

    @Test
    void frozenSubsetAllowsExtrasButNotMissingIds() {
        var extraOnly = RecipeCensusRuntimeRegistryEnumerator.compareCategory(
                "blocks",
                List.of("cruciblecraft:a"),
                List.of("cruciblecraft:a", "cruciblecraft:b"));
        assertFalse(extraOnly.ok());
        assertTrue(extraOnly.frozenSubsetOk());
        var missing = RecipeCensusRuntimeRegistryEnumerator.compareCategory(
                "blocks",
                List.of("cruciblecraft:a", "cruciblecraft:b"),
                List.of("cruciblecraft:a"));
        assertFalse(missing.frozenSubsetOk());
    }

    @Test
    void missingExtraAndDuplicateIdsFailClosed() {
        var diff = RecipeCensusRuntimeRegistryEnumerator.compareCategory(
                "blocks",
                List.of("cruciblecraft:a", "cruciblecraft:b"),
                List.of(
                        "cruciblecraft:a",
                        "cruciblecraft:c",
                        "cruciblecraft:c"));
        assertFalse(diff.ok());
        assertEquals(1, diff.missingCount());
        assertEquals(1, diff.extraCount());
        assertEquals(1, diff.duplicateCount());
        assertEquals(List.of("cruciblecraft:b"), diff.missingSample());
        assertEquals(List.of("cruciblecraft:c"), diff.extraSample());
        assertEquals(List.of("cruciblecraft:c"), diff.duplicateSample());
    }
}
