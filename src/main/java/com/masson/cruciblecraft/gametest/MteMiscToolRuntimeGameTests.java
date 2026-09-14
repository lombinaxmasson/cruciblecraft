package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

@GameTestHolder(MteMiscToolRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteMiscToolRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_misc_tool_runtime";
    private static final String TEMPLATE = "empty";

    private MteMiscToolRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void gt6AnvilIsNotVanillaAnvil(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "stone/anvil", MteInPlaceKind.MISC_TOOL);
        helper.assertTrue(
                MteInPlaceGameTestSupport.item(
                        "stone/anvil") != Items.ANVIL
                        && ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                                "stone/anvil")).getBlock()
                                != ModBlocks.ANVIL.get()
                        && ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                                "stone/anvil")).getBlock()
                                != Blocks.ANVIL,
                "stone/anvil aliased an anvil");
        helper.succeed();
    }
}
