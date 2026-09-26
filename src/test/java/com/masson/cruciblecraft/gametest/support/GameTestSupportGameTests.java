package com.masson.cruciblecraft.gametest.support;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;

import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder("cruciblecraft_default_grid")
@PrefixGameTestTemplate(false)
public final class GameTestSupportGameTests {
    private static final String TEMPLATE = "empty";

    private GameTestSupportGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void requireBlockEntityReadsAPlacedChest(GameTestHelper helper) {
        BlockPos pos = new BlockPos(1, 2, 1);
        helper.setBlock(pos, Blocks.CHEST.defaultBlockState());
        ChestBlockEntity chest = GameTestRequirements.requireBlockEntity(
                helper,
                pos,
                ChestBlockEntity.class);
        helper.assertTrue(chest != null, "chest block entity missing");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void requireRegisteredIronIngotAndCopperPipe(GameTestHelper helper) {
        helper.assertTrue(
                !GameTestFixtures.requireMaterialStack(
                        helper,
                        "iron",
                        MaterialPrefixes.INGOT,
                        1).isEmpty(),
                "iron ingot stack missing");
        PipeCatalog.Entry pipe = GameTestFixtures.requirePipe(
                helper,
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID);
        helper.assertTrue(
                "copper".equals(pipe.materialId()),
                "copper tiny fluid pipe was not the registered entry");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void requirePipeRejectsTinBecauseGt6HasNone(GameTestHelper helper) {
        boolean rejected = false;
        // hygiene-negative: GT6 registers no tin fluid pipe; this asserts rejection.
        try {
            GameTestFixtures.requirePipe(
                    "tin",
                    MaterialPrefixes.TINY_FLUID_PIPE,
                    PipeCatalog.Kind.FLUID);
        } catch (IllegalArgumentException failure) {
            rejected = failure.getMessage().contains("tin")
                    && failure.getMessage().contains("tiny_fluid_pipe");
        }
        helper.assertTrue(rejected, "tin tiny fluid pipe must fail as missing");
        helper.succeed();
    }
}
