package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MteMultiblockRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteMultiblockRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_multiblock_runtime";
    private static final String TEMPLATE = "empty";

    private MteMultiblockRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gt6CokeOvenMteIsNotNamedCokeOven(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "coal_coke/oven", MteInPlaceKind.MULTIBLOCK_PART);
        helper.assertTrue(
                ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                        "coal_coke/oven")).getBlock()
                        != ModBlocks.COKE_OVEN.get(),
                "coal_coke/oven aliased named coke_oven");
        helper.succeed();
    }
}
