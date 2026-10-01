package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.recipe.gt.GTRecipeMapLoader;
import com.masson.cruciblecraft.recipe.gt.RecipeCapacityWireProbe;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Cold publication with the temporary capacity rows installed. The rows
 * are not player recipes. This GameTest server is not the dedicated client.
 */
@GameTestHolder(RecipeCapacityLiveWireGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RecipeCapacityLiveWireGameTests {
    public static final String NAMESPACE = "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private RecipeCapacityLiveWireGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 20 * 60 * 10)
    public static void liveWireAnd600kProjection(GameTestHelper helper) throws Exception {
        RecipeCapacityWireProbe.Report report = RecipeCapacityWireProbe.measure(
                helper.getLevel().getServer());
        RecipeCapacityWireProbe.write(report);
        helper.assertTrue(
                report.lazyLogicalRecipes() >= 600_000,
                "lazy rows " + report.lazyLogicalRecipes());
        helper.assertTrue(
                report.livePacketBytes()
                        <= ModProcessingMachines.RECIPE_SYNC_BUDGET_BYTES,
                "login packet bytes " + report.livePacketBytes());
        helper.assertTrue(
                report.livePacketBytes()
                        == GTRecipeMapLoader.lastPublicationMetrics().loginSyncBytes(),
                "loader login bytes "
                        + GTRecipeMapLoader.lastPublicationMetrics().loginSyncBytes());
        helper.assertTrue(
                report.reloadMillis() <= 15_000L,
                "reload ms " + report.reloadMillis());
        helper.assertTrue(
                report.lookupP95Nanos()
                        <= ModProcessingMachines
                                .VERIFICATION_RECIPE_LOOKUP_P95_BUDGET_NS,
                "lookup p95 ns " + report.lookupP95Nanos());
        helper.succeed();
    }
}
