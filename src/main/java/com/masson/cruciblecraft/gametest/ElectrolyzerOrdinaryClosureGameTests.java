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

@GameTestHolder(ElectrolyzerOrdinaryClosureGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ElectrolyzerOrdinaryClosureGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_electrolyzer_ordinary_closure";
    private static final String TEMPLATE = "empty";
    private static final String RECIPE_PREFIX = "electrolyzer/ordinary_closure/";

    private ElectrolyzerOrdinaryClosureGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electrolyzerReleasePerformanceGates(GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        GTRecipeMapLoader.verifyReleasePerformance(metrics, lookup, null, null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electrolyzerOrdinaryFamiliesPublished(GameTestHelper helper) {
        helper.assertTrue(
                !OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.ELECTROLYZER, RECIPE_PREFIX).isEmpty(),
                "Electrolyzer ordinary-closure compact ids missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void electrolyzerOrdinaryGroupsExecute(GameTestHelper helper) {
        RecipeMap.Entry representative = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.ELECTROLYZER, RECIPE_PREFIX);
        GTRecipe recipe = representative.recipe();
        ConfiguredProcessingMachineBlockEntity machine = OrdinaryClosureHostGameTests.place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.ELECTROLYZER.get(),
                ModProcessingMachines.ELECTROLYZER);
        OrdinaryClosureHostGameTests.fillEnergy(helper, machine);
        OrdinaryClosureHostGameTests.loadRecipeInputs(machine, recipe);
        OrdinaryClosureHostGameTests.assertRecipeMatches(
                helper, ModRecipeMaps.ELECTROLYZER, recipe);
        OrdinaryClosureHostGameTests.executeRepresentative(helper, machine, "Electrolyzer");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void electrolyzerDurationEutAndConservation(GameTestHelper helper) {
        GTRecipe recipe = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.ELECTROLYZER, RECIPE_PREFIX).recipe();
        helper.assertTrue(recipe.duration() > 0 && recipe.eut() > 0L, "duration/EUt missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electrolyzerStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = OrdinaryClosureHostGameTests.publishedIds(
                ModRecipeMaps.ELECTROLYZER, RECIPE_PREFIX);
        helper.assertTrue(!first.isEmpty(), "ids missing");
        helper.assertTrue(
                OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.ELECTROLYZER, RECIPE_PREFIX).equals(first),
                "stable ids drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void electrolyzerEmiPlanIncludesFamilies(GameTestHelper helper) {
        OrdinaryClosureHostGameTests.assertEmiContains(
                helper,
                ModRecipeMaps.ELECTROLYZER,
                OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.ELECTROLYZER, RECIPE_PREFIX));
        helper.succeed();
    }
}
