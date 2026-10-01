package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Opening invariant for the recipe capacity card. The sync ceiling stays
 * 64 MiB. The lazy ceiling is still the previous gate until the 600k
 * measurement passes.
 */
@GameTestHolder(RecipeCapacityExpansionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RecipeCapacityExpansionGameTests {
    public static final String NAMESPACE = "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private RecipeCapacityExpansionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void syncBudgetStaysAt64MiB(GameTestHelper helper) {
        helper.assertTrue(
                ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES == 64L * 1_024L * 1_024L,
                "sync budget stays 64 MiB");
        helper.succeed();
    }
}
