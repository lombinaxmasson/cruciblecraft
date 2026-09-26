package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MteFurnitureScaffoldRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteFurnitureScaffoldRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private MteFurnitureScaffoldRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadScaffoldIsLiveBlock(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "lead/scaffold", MteInPlaceKind.SCAFFOLD);
        helper.succeed();
    }
}
