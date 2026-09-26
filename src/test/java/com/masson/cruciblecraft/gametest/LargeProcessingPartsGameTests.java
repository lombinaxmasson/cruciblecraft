package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated large-processing parts/maps gate. Run with
 * {@code -PgameTestGrid=multiblock}.
 */
@GameTestHolder(LargeProcessingPartsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeProcessingPartsGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_multiblock";
    private static final String TEMPLATE = "empty";

    private LargeProcessingPartsGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void specializedProcessingPartsAreLive(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "multiblock/electrolyzer_part", MteInPlaceKind.MULTIBLOCK_PART);
        MteInPlaceGameTestSupport.assertLive(
                helper, "multiblock/crusher_wheels", MteInPlaceKind.MULTIBLOCK_PART);
        MteInPlaceGameTestSupport.assertLive(
                helper, "multiblock/shredder_blades", MteInPlaceKind.MULTIBLOCK_PART);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fermenterAndImplosionMapsExist(GameTestHelper helper) {
        helper.assertTrue(
                ModRecipeMaps.FERMENTER != null
                        && "fermenter".equals(ModRecipeMaps.FERMENTER.id().getPath()),
                "fermenter RecipeMap is missing");
        helper.assertTrue(
                ModRecipeMaps.IMPLOSION_COMPRESSOR != null
                        && "implosion_compressor".equals(
                                ModRecipeMaps.IMPLOSION_COMPRESSOR.id().getPath()),
                "implosion_compressor RecipeMap is missing");
        helper.succeed();
    }
}
