package com.masson.cruciblecraft.scale;

import com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Reload and lookup budgets live on the measurement grid. A cold JVM can miss
 * the wall-clock budget without saying anything about recipe counts.
 */
@GameTestHolder(ScaleGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PublicationReloadBudgetGameTests {
    private static final String TEMPLATE = "empty";

    private PublicationReloadBudgetGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 1200)
    public static void publicationReloadStaysInsideVerificationBudget(
            GameTestHelper helper) {
        var metrics = GTRecipeMapLoader.lastPublicationMetrics();
        var lookup = GTRecipeMapLoader.benchmarkCompactLoadLookupsForVerification();
        var onlineGate = GTRecipeMapLoader.evaluateCompactLoadOnlineBudgetGate(
                metrics,
                lookup,
                ModProcessingMachines.VERIFICATION_RECIPE_RELOAD_BUDGET_MS,
                ModProcessingMachines.VERIFICATION_RECIPE_LOOKUP_P95_BUDGET_NS);
        helper.assertTrue(
                metrics.reloadMillis()
                                <= ModProcessingMachines
                                        .VERIFICATION_RECIPE_RELOAD_BUDGET_MS
                        && onlineGate.sideReload(),
                "Recipe reload exceeded the verification budget: reloadMillis="
                        + metrics.reloadMillis()
                        + " sideReload="
                        + onlineGate.sideReload()
                        + " budget="
                        + ModProcessingMachines.VERIFICATION_RECIPE_RELOAD_BUDGET_MS);
        helper.assertTrue(
                metrics.indexMillis()
                                <= ModProcessingMachines.RECIPE_INDEX_BUILD_BUDGET_MS
                        && lookup.timingSamples() == 61
                        && lookup.operations() == lookup.timingSamples() * 32
                        && onlineGate.sideIndex()
                        && onlineGate.sync()
                        && onlineGate.lookupP95()
                        && onlineGate.lookupCandidates(),
                "Recipe publication performance budget exceeded: indexMs="
                        + metrics.indexMillis()
                        + " sideIndex=" + onlineGate.sideIndex()
                        + " sync=" + onlineGate.sync()
                        + " lookupP95=" + onlineGate.lookupP95()
                        + " lookupCandidates=" + onlineGate.lookupCandidates());
        helper.succeed();
    }
}
