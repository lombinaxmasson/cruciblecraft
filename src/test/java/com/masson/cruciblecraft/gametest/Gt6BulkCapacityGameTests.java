package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.recipe.gt.CompactRecipeShardRouter;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeWireLimits;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Capacity gate. Confirms the global lazy ceiling moved and the per-holder,
 * shard, and cache ceilings did not.
 */
@GameTestHolder(Gt6BulkCapacityGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class Gt6BulkCapacityGameTests {
    public static final String NAMESPACE = "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private Gt6BulkCapacityGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bulkCapacityCeilings(GameTestHelper helper) {
        helper.assertTrue(
                ModProcessingMachines.ALL_LAZY_LOGICAL_RECIPE_HARD_CEILING == 616_572,
                "lazy logical ceiling");
        helper.assertTrue(
                CompactRecipeWireLimits.DECODE_RELATIONS_CEILING == 4_096,
                "per-holder row ceiling");
        helper.assertTrue(
                CompactRecipeShardRouter.HARD_SHARD_CEILING == 128,
                "shard ceiling");
        helper.assertTrue(
                ModProcessingMachines.ALL_LAZY_RECIPE_CACHE_HARD_CEILING == 4_096,
                "cache ceiling");
        helper.assertTrue(
                ModProcessingMachines.TEMPORARY_EAGER_COMPATIBILITY_CEILING == 41_000,
                "eager ceiling stays");
        helper.succeed();
    }
}
