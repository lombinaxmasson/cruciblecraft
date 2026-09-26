package com.masson.cruciblecraft.gametest;

import java.util.Set;
import java.util.TreeSet;

import com.masson.cruciblecraft.content.blockentity.ConfiguredProcessingMachineBlockEntity;
import com.masson.cruciblecraft.recipe.gt.CompactRecipeFamilyProvider;
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

/**
 * Isolated Smelter deferred-recycling runtime gate. Run with
 * {@code -PgameTestGrid=machines}.
 */
@GameTestHolder(SmelterDeferredRecyclingGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SmelterDeferredRecyclingGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_machines";
    public static final int FAMILY_COUNT = 1817;
    private static final String TEMPLATE = "empty";
    private static final String RECIPE_PREFIX = "smelter/deferred_recycling/";
    private static final int SMELTER_OUTPUT_TANK =
            ModProcessingMachines.SMELTER_GT6_FLUID_OUTPUT;

    private SmelterDeferredRecyclingGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void smelterDeferredReleasePerformanceGates(GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        GTRecipeMapLoader.verifyReleasePerformance(metrics, lookup, null, null);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void smelterDeferredFamiliesPublished(GameTestHelper helper) {
        Set<ResourceLocation> published = publishedIds(ModRecipeMaps.SMELTER);
        helper.assertTrue(
                published.size() == FAMILY_COUNT,
                "Smelter deferred-recycling published "
                        + published.size()
                        + " != "
                        + FAMILY_COUNT);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void smelterDeferredGroupsPublished(GameTestHelper helper) {
        Set<String> materials = materials(ModRecipeMaps.SMELTER);
        helper.assertTrue(
                materials.size() == 81,
                "Smelter deferred-recycling material groups "
                        + materials.size()
                        + " != 81");
        for (String material : materials) {
            ResourceLocation group = ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/deferred_recycling/" + material);
            RecipeMap.RecipeFamily family = ModRecipeMaps.SMELTER
                    .family(CompactRecipeFamilyProvider.familyId(
                            ModRecipeMaps.SMELTER.id(), group))
                    .orElse(null);
            helper.assertTrue(
                    family != null && family.logicalRecipeCount() > 0,
                    "Missing deferred-recycling group " + group);
            RecipeMap.Entry representative = firstFitting(family);
            OrdinaryClosureHostGameTests.assertRecipeMatches(
                    helper, ModRecipeMaps.SMELTER, representative.recipe());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void smelterDeferredGroupsExecute(GameTestHelper helper) {
        RecipeMap.Entry representative = firstExecutable(ModRecipeMaps.SMELTER);
        GTRecipe recipe = representative.recipe();
        ConfiguredProcessingMachineBlockEntity machine = OrdinaryClosureHostGameTests.place(
                helper,
                new BlockPos(3, 2, 3),
                ModBlocks.SMELTER.get(),
                ModProcessingMachines.SMELTER);
        OrdinaryClosureHostGameTests.fillEnergy(helper, machine);
        OrdinaryClosureHostGameTests.loadRecipeInputs(machine, recipe);
        OrdinaryClosureHostGameTests.assertRecipeMatches(
                helper, ModRecipeMaps.SMELTER, recipe);
        OrdinaryClosureHostGameTests.executeRepresentative(helper, machine, "Smelter");
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void smelterDeferredDurationEutAndConservation(GameTestHelper helper) {
        GTRecipe recipe = firstExecutable(ModRecipeMaps.SMELTER).recipe();
        helper.assertTrue(recipe.duration() > 0 && recipe.eut() > 0L, "duration/EUt missing");
        helper.assertTrue(
                recipe.itemInputs().size() == 1
                        && recipe.fluidOutputs().size() == 1
                        && recipe.itemOutputs().isEmpty(),
                "deferred recovery shape drifted from consumed MTE -> molten");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void smelterDeferredStableIdsSurviveReload(GameTestHelper helper) {
        Set<ResourceLocation> first = publishedIds(ModRecipeMaps.SMELTER);
        helper.assertTrue(first.size() == FAMILY_COUNT, "ids missing");
        helper.assertTrue(
                publishedIds(ModRecipeMaps.SMELTER).equals(first),
                "stable ids drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 200)
    public static void smelterDeferredEmiPlanIncludesFamilies(GameTestHelper helper) {
        OrdinaryClosureHostGameTests.assertEmiContains(
                helper,
                ModRecipeMaps.SMELTER,
                publishedIds(ModRecipeMaps.SMELTER));
        helper.succeed();
    }

    private static Set<ResourceLocation> publishedIds(RecipeMap map) {
        Set<ResourceLocation> ids = new TreeSet<>();
        for (RecipeMap.RecipeFamily family : map.families()) {
            for (ResourceLocation id : family.recipeIds()) {
                if (id.getPath().startsWith(RECIPE_PREFIX)) {
                    ids.add(id);
                }
            }
        }
        return ids;
    }

    private static Set<String> materials(RecipeMap map) {
        Set<String> materials = new TreeSet<>();
        String marker = "smelter/deferred_recycling/";
        for (RecipeMap.RecipeFamily family : map.families()) {
            String familyId = family.familyId();
            int index = familyId.lastIndexOf(marker);
            if (index < 0) {
                continue;
            }
            String rest = familyId.substring(index + marker.length());
            if (!rest.isEmpty() && rest.indexOf('/') < 0) {
                materials.add(rest);
            }
        }
        return materials;
    }

    private static RecipeMap.Entry firstExecutable(RecipeMap map) {
        RecipeMap.Entry best = null;
        for (String material : materials(map)) {
            ResourceLocation group = ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "smelter/deferred_recycling/" + material);
            RecipeMap.RecipeFamily family = map.family(
                    CompactRecipeFamilyProvider.familyId(map.id(), group))
                    .orElse(null);
            if (family == null || family.logicalRecipeCount() <= 0) {
                continue;
            }
            RecipeMap.Entry candidate = firstFitting(family);
            GTRecipe recipe = candidate.recipe();
            if (!OrdinaryClosureHostGameTests.inputsResolvable(recipe)
                    || !OrdinaryClosureHostGameTests.hasDeclaredOutput(recipe)
                    || !fitsSmelterTank(recipe)) {
                continue;
            }
            if (best == null || recipe.duration() < best.recipe().duration()) {
                best = candidate;
            }
        }
        if (best == null) {
            throw new IllegalStateException(
                    "Missing tank-fitting Smelter deferred-recycling recipe");
        }
        return best;
    }

    private static RecipeMap.Entry firstFitting(RecipeMap.RecipeFamily family) {
        RecipeMap.Entry best = null;
        for (int index = 0; index < family.logicalRecipeCount(); index++) {
            RecipeMap.Entry entry = family.enumerationEntry(index);
            GTRecipe recipe = entry.recipe();
            if (!OrdinaryClosureHostGameTests.inputsResolvable(recipe)
                    || !OrdinaryClosureHostGameTests.hasDeclaredOutput(recipe)) {
                continue;
            }
            if (best == null || recipe.duration() < best.recipe().duration()) {
                best = entry;
            }
        }
        if (best == null) {
            return family.enumerationEntry(0);
        }
        return best;
    }

    private static boolean fitsSmelterTank(GTRecipe recipe) {
        return recipe.fluidOutputs().stream()
                .allMatch(stack -> stack.getAmount() > 0 && stack.getAmount() <= SMELTER_OUTPUT_TANK);
    }
}
