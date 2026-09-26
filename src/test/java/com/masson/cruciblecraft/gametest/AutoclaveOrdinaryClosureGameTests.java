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

@GameTestHolder(AutoclaveOrdinaryClosureGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class AutoclaveOrdinaryClosureGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final String RECIPE_PREFIX = "autoclave/ordinary_closure/";

    private AutoclaveOrdinaryClosureGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void autoclaveReleasePerformanceGates(GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        GTRecipeMapLoader.verifyReleasePerformance(metrics, lookup, null, null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void autoclaveOrdinaryFamiliesPublished(GameTestHelper helper) {
        helper.assertTrue(
                !OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.AUTOCLAVE, RECIPE_PREFIX).isEmpty(),
                "Autoclave ordinary-closure compact ids missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void autoclaveOrdinaryGroupsExecute(GameTestHelper helper) {
        RecipeMap.Entry representative = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.AUTOCLAVE, RECIPE_PREFIX);
        GTRecipe recipe = representative.recipe();
        ConfiguredProcessingMachineBlockEntity machine = OrdinaryClosureHostGameTests.place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.AUTOCLAVE.get(),
                ModProcessingMachines.AUTOCLAVE);
        OrdinaryClosureHostGameTests.fillEnergy(helper, machine);
        OrdinaryClosureHostGameTests.loadRecipeInputs(machine, recipe);
        OrdinaryClosureHostGameTests.assertRecipeMatches(
                helper, ModRecipeMaps.AUTOCLAVE, recipe);
        OrdinaryClosureHostGameTests.executeRepresentative(helper, machine, "Autoclave");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void autoclaveDurationEutAndConservation(GameTestHelper helper) {
        GTRecipe recipe = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.AUTOCLAVE, RECIPE_PREFIX).recipe();
        helper.assertTrue(recipe.duration() > 0, "duration missing");
        helper.assertTrue(
                recipe.eut() >= 0L,
                "TIME autoclave may be eut=0 but must not be negative");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void autoclaveStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = OrdinaryClosureHostGameTests.publishedIds(
                ModRecipeMaps.AUTOCLAVE, RECIPE_PREFIX);
        helper.assertTrue(!first.isEmpty(), "ids missing");
        helper.assertTrue(
                OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.AUTOCLAVE, RECIPE_PREFIX).equals(first),
                "stable ids drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void autoclaveEmiPlanIncludesFamilies(GameTestHelper helper) {
        OrdinaryClosureHostGameTests.assertEmiContains(
                helper,
                ModRecipeMaps.AUTOCLAVE,
                OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.AUTOCLAVE, RECIPE_PREFIX));
        helper.succeed();
    }
}
