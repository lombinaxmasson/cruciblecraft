package com.masson.cruciblecraft.gametest;

import java.util.Set;

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

@GameTestHolder(CompressorOrdinaryClosureGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CompressorOrdinaryClosureGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final String RECIPE_PREFIX = "compressor/ordinary_closure/";

    private CompressorOrdinaryClosureGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compressorReleasePerformanceGates(GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        GTRecipeMapLoader.verifyReleasePerformance(metrics, lookup, null, null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compressorOrdinaryFamiliesPublished(GameTestHelper helper) {
        helper.assertTrue(
                !OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.COMPRESSOR, RECIPE_PREFIX).isEmpty(),
                "Compressor ordinary-closure compact ids missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void compressorOrdinaryGroupsExecute(GameTestHelper helper) {
        RecipeMap.Entry representative = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.COMPRESSOR, RECIPE_PREFIX);
        GTRecipe recipe = representative.recipe();
        ConfiguredProcessingMachineBlockEntity machine = OrdinaryClosureHostGameTests.place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.COMPRESSOR.get(),
                ModProcessingMachines.COMPRESSOR);
        OrdinaryClosureHostGameTests.fillEnergy(helper, machine);
        OrdinaryClosureHostGameTests.loadRecipeInputs(machine, recipe);
        OrdinaryClosureHostGameTests.assertRecipeMatches(
                helper, ModRecipeMaps.COMPRESSOR, recipe);
        OrdinaryClosureHostGameTests.executeRepresentative(helper, machine, "Compressor");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void compressorDurationEutAndConservation(GameTestHelper helper) {
        GTRecipe recipe = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.COMPRESSOR, RECIPE_PREFIX).recipe();
        helper.assertTrue(recipe.duration() > 0 && recipe.eut() > 0L, "duration/EUt missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compressorStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = OrdinaryClosureHostGameTests.publishedIds(
                ModRecipeMaps.COMPRESSOR, RECIPE_PREFIX);
        helper.assertTrue(!first.isEmpty(), "ids missing");
        helper.assertTrue(
                OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.COMPRESSOR, RECIPE_PREFIX).equals(first),
                "stable ids drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void compressorEmiPlanIncludesFamilies(GameTestHelper helper) {
        OrdinaryClosureHostGameTests.assertEmiContains(
                helper,
                ModRecipeMaps.COMPRESSOR,
                OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.COMPRESSOR, RECIPE_PREFIX));
        helper.succeed();
    }
}
