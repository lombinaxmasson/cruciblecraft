package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteFoundryTanks;
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
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private MteCrucibleFoundryRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foundryCrucibleIsNotCeramicCrucible(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "foundry/smelting_crucible_invar", MteInPlaceKind.CRUCIBLE_FOUNDRY);
        helper.assertTrue(
                MteInPlaceGameTestSupport.item("foundry/smelting_crucible_invar")
                        instanceof CatalogNamedBlockItem,
                "foundry/smelting_crucible_invar is not a unique catalog item");
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.mteInPlaceBlocksById()
                        .get(MteInPlaceGameTestSupport.id(
                                "foundry/smelting_crucible_invar"))
                        .get()
                        .defaultBlockState());
        CrucibleBlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(
                be instanceof CrucibleBlockEntity
                        && CrucibleBlockEntity.maxUnits() == MteFoundryTanks.SMELTERY_MB,
                "foundry smeltery is not a 16-ingot crucible");
        helper.succeed();
    }
}
