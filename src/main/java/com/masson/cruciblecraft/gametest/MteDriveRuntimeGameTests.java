package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModBlocks;
import net.minecraft.core.Direction;
import net.minecraft.core.BlockPos;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MteDriveRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteDriveRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_drive_runtime";
    private static final String TEMPLATE = "empty";

    private MteDriveRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void axleIsNotKuRotationalAxle(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "drive/small_brass_axle", MteInPlaceKind.AXLE);
        helper.assertTrue(
                ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                        "drive/small_brass_axle")).getBlock()
                        != ModBlocks.ROTATIONAL_AXLE.get(),
                "drive/small_brass_axle aliased KU rotational_axle");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void drivePushesKineticToAdjacentDrive(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "drive/small_brass_axle", MteInPlaceKind.AXLE);
        BlockPos sourcePos = new BlockPos(1, 2, 2);
        BlockPos destPos = new BlockPos(2, 2, 2);
        MteInPlaceBlock axle = ModBlocks.mteInPlaceBlocksById()
                .get(MteInPlaceGameTestSupport.id(
                        "drive/small_brass_axle"))
                .get();
        helper.setBlock(
                sourcePos,
                axle.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
        helper.setBlock(
                destPos,
                axle.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
        MteInPlaceBlockEntity source = helper.getBlockEntity(
                sourcePos);
        helper.assertTrue(
                source.insert(
                        EnergyType.KINETIC_ROTATION,
                        1L,
                        128L,
                        Direction.EAST,
                        false) == 128L,
                "drive source rejected kinetic energy");
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    MteInPlaceBlockEntity dest =
                            helper.getBlockEntity(destPos);
                    helper.assertTrue(
                            dest.stored(
                                    EnergyType.KINETIC_ROTATION) > 0L,
                            "drive did not push kinetic to neighbor");
                })
                .thenSucceed();
    }
}
