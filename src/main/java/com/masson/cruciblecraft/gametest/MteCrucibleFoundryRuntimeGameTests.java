package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModBlocks;
import net.minecraft.core.BlockPos;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MteCrucibleFoundryRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteCrucibleFoundryRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_crucible_foundry_runtime";
    private static final String TEMPLATE = "empty";

    private MteCrucibleFoundryRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foundryCrucibleIsNotCeramicCrucible(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "foundry/smelting_crucible_invar", MteInPlaceKind.CRUCIBLE_FOUNDRY);
        helper.assertTrue(
                ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                        "foundry/smelting_crucible_invar")).getBlock()
                        != ModBlocks.CRUCIBLE.get(),
                "foundry/smelting_crucible_invar aliased the ceramic crucible");
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id(
                                "foundry/smelting_crucible_invar"))
                        .get()
                        .defaultBlockState());
        MteInPlaceBlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(
                be.tank().getCapacity() == 8000,
                "foundry tank missing");
        helper.succeed();
    }
}
