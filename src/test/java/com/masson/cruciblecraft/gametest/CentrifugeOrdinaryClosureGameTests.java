package com.masson.cruciblecraft.gametest;

import java.util.Set;

import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationGroups;
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

@GameTestHolder(CentrifugeOrdinaryClosureGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class CentrifugeOrdinaryClosureGameTests {
    public static final String NAMESPACE = "cruciblecraft_machines";
    private static final String TEMPLATE = "empty";
    private static final String RECIPE_PREFIX = "centrifuge/ordinary_closure/";

    private CentrifugeOrdinaryClosureGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void centrifugeReleasePerformanceGates(GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        GTRecipeMapLoader.verifyReleasePerformance(metrics, lookup, null, null);
        helper.assertTrue(
                CompactPublicationGroups.ENVELOPE_GT6_PANEL.equals(
                        CompactPublicationGroups.executionEnvelope(
                                ResourceLocation.parse(
                                        "cruciblecraft:centrifuge/ordinary_closure/singleton"))),
                "Centrifuge ordinary-closure envelope is not gt6_panel");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void centrifugeOrdinaryFamiliesPublished(GameTestHelper helper) {
        helper.assertTrue(
                !OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.CENTRIFUGE, RECIPE_PREFIX).isEmpty(),
                "Centrifuge ordinary-closure compact ids missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void centrifugeOrdinaryGroupsExecute(GameTestHelper helper) {
        RecipeMap.Entry representative = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.CENTRIFUGE, RECIPE_PREFIX);
        GTRecipe recipe = representative.recipe();
        ConfiguredProcessingMachineBlockEntity machine = OrdinaryClosureHostGameTests.place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.CENTRIFUGE.get(),
                ModProcessingMachines.CENTRIFUGE);
        OrdinaryClosureHostGameTests.fillEnergy(helper, machine);
        OrdinaryClosureHostGameTests.loadRecipeInputs(machine, recipe);
        OrdinaryClosureHostGameTests.assertRecipeMatches(
                helper, ModRecipeMaps.CENTRIFUGE, recipe);
        OrdinaryClosureHostGameTests.executeRepresentative(helper, machine, "Centrifuge");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void centrifugeDurationEutAndConservation(GameTestHelper helper) {
        GTRecipe recipe = OrdinaryClosureHostGameTests.firstOrdinary(
                ModRecipeMaps.CENTRIFUGE, RECIPE_PREFIX).recipe();
        helper.assertTrue(recipe.duration() > 0 && recipe.eut() > 0L, "duration/EUt missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void centrifugeStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = OrdinaryClosureHostGameTests.publishedIds(
                ModRecipeMaps.CENTRIFUGE, RECIPE_PREFIX);
        helper.assertTrue(!first.isEmpty(), "ids missing");
        helper.assertTrue(
                OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.CENTRIFUGE, RECIPE_PREFIX).equals(first),
                "stable ids drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void centrifugeEmiPlanIncludesFamilies(GameTestHelper helper) {
        OrdinaryClosureHostGameTests.assertEmiContains(
                helper,
                ModRecipeMaps.CENTRIFUGE,
                OrdinaryClosureHostGameTests.publishedIds(
                        ModRecipeMaps.CENTRIFUGE, RECIPE_PREFIX));
        helper.succeed();
    }
}
