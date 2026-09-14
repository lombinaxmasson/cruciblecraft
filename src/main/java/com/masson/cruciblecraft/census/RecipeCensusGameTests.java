package com.masson.cruciblecraft.census;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated frozen-subset registry probe for release or special runs.
 * Ordinary recipe-card closeout does not run this GameTest. Daily coverage
 * is the authored-recipe material-form registration test. Run with
 * {@code -PrecipeCensus} so only this namespace loads.
 */
@GameTestHolder(RecipeCensusGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RecipeCensusGameTests {
    public static final String NAMESPACE = "cruciblecraft_census";
    private static final String TEMPLATE = "empty";

    private RecipeCensusGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 20_000)
    public static void runtimeRegistryGate(GameTestHelper helper) {
        List<RecipeCensusRuntimeRegistryEnumerator.CategoryDiff> diffs =
                RecipeCensusRuntimeRegistryGate.verify(helper.getLevel());
        String failures = diffs.stream()
                .filter(diff -> !diff.frozenSubsetOk())
                .map(RecipeCensusRuntimeRegistryEnumerator.CategoryDiff::message)
                .collect(java.util.stream.Collectors.joining("; "));
        helper.assertTrue(failures.isEmpty(), failures);
        helper.succeed();
    }
}
