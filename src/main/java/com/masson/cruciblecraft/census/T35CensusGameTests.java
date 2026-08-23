package com.masson.cruciblecraft.census;

import java.util.List;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated T35 runtime registry equality gate. The holder namespace keeps the
 * daily {@code cruciblecraft} GameTest grid separate from this census probe.
 * Run with {@code -Pt35Census} so only this namespace loads.
 */
@GameTestHolder(T35CensusGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class T35CensusGameTests {
    public static final String NAMESPACE = "cruciblecraft_census";
    private static final String TEMPLATE = "empty";

    private T35CensusGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 20_000)
    public static void t35RuntimeRegistryGate(GameTestHelper helper) {
        List<T35RuntimeRegistryEnumerator.CategoryDiff> diffs =
                T35RuntimeRegistryGate.verify(helper.getLevel());
        String failures = diffs.stream()
                .filter(diff -> !diff.frozenSubsetOk())
                .map(T35RuntimeRegistryEnumerator.CategoryDiff::message)
                .collect(java.util.stream.Collectors.joining("; "));
        helper.assertTrue(failures.isEmpty(), failures);
        helper.succeed();
    }
}
