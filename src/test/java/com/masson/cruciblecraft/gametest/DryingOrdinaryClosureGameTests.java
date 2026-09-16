package com.masson.cruciblecraft.gametest;

import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(DryingOrdinaryClosureGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class DryingOrdinaryClosureGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_drying_ordinary_closure";
    private static final String TEMPLATE = "empty";
    private static final String RECIPE_PREFIX = "drying/ordinary_closure/";

    private DryingOrdinaryClosureGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dryingReleasePerformanceGates(GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        CrucibleCraft.LOGGER.info(
                "drying/ordinary-closure measured metrics={} lookup={}", metrics, lookup);
        GTRecipeMapLoader.verifyReleasePerformance(metrics, lookup, null, null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dryingOrdinaryFamiliesPublished(GameTestHelper helper) {
        helper.assertTrue(
                !OrdinaryClosureHostGameTests.publishedIds(ModRecipeMaps.DRYING, RECIPE_PREFIX)
                        .isEmpty(),
                "Drying ordinary-closure compact ids missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void dryingOrdinaryGroupsExecute(GameTestHelper helper) {
        RecipeMap.Entry representative = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.DRYING, RECIPE_PREFIX);
        GTRecipe recipe = representative.recipe();
        ConfiguredProcessingMachineBlockEntity machine = OrdinaryClosureHostGameTests.place(
                helper, new BlockPos(3, 2, 3), ModBlocks.DRYING.get(), ModProcessingMachines.DRYING);
        OrdinaryClosureHostGameTests.fillEnergy(helper, machine);
        OrdinaryClosureHostGameTests.loadRecipeInputs(machine, recipe);
        OrdinaryClosureHostGameTests.assertRecipeMatches(helper, ModRecipeMaps.DRYING, recipe);
        OrdinaryClosureHostGameTests.executeRepresentative(helper, machine, "Drying");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void dryingDurationEutAndConservation(GameTestHelper helper) {
        GTRecipe recipe = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.DRYING, RECIPE_PREFIX).recipe();
        helper.assertTrue(recipe.duration() > 0, "Ordinary drying duration is 0");
        helper.assertTrue(recipe.eut() > 0L, "Ordinary drying EUt is 0");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dryingStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = OrdinaryClosureHostGameTests.publishedIds(
                ModRecipeMaps.DRYING, RECIPE_PREFIX);
        helper.assertTrue(!first.isEmpty(), "Ordinary drying ids missing");
        helper.assertTrue(
                OrdinaryClosureHostGameTests.publishedIds(ModRecipeMaps.DRYING, RECIPE_PREFIX)
                        .equals(first),
                "Ordinary drying stable ids drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dryingEmiPlanIncludesFamilies(GameTestHelper helper) {
        OrdinaryClosureHostGameTests.assertEmiContains(
                helper,
                ModRecipeMaps.DRYING,
                OrdinaryClosureHostGameTests.publishedIds(ModRecipeMaps.DRYING, RECIPE_PREFIX));
        helper.succeed();
    }
}
