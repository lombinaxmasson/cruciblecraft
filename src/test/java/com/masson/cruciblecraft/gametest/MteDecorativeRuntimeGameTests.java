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

@GameTestHolder(MteDecorativeRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteDecorativeRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private MteDecorativeRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steelRopeIsNotVanillaLead(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "steel/rope", MteInPlaceKind.ROPE);
        helper.assertTrue(
                MteInPlaceGameTestSupport.item("steel/rope")
                        != Items.LEAD,
                "steel/rope aliased vanilla lead");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void woodPanelIsLiveBlock(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "panel/wood_0", MteInPlaceKind.WOOD_PANEL);
        helper.succeed();
    }
}
