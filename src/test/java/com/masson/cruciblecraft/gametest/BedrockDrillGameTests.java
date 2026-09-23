package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.energy.bedrockdrill.BedrockDrillBlock;
import com.masson.cruciblecraft.energy.bedrockdrill.BedrockDrillBlockEntity;
import com.masson.cruciblecraft.energy.bedrockdrill.BedrockDrillStructure;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;

/** GT6 17999 controller and 18103 drill-head behavior. */
@GameTestHolder(CrucibleCraftGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class BedrockDrillGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(8, 7, 8);

    private BedrockDrillGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bedrockDrillBareControllerDoesNotForm(
            GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.BEDROCK_DRILL.get()
                        .defaultBlockState()
                        .setValue(BedrockDrillBlock.FACING, Direction.UP));
        BedrockDrillBlockEntity drill = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(drill != null, "Bedrock drill controller is missing");
        BedrockDrillBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                drill);
        helper.assertTrue(
                !drill.formed(),
                "Bare bedrock drill controller reported a formed tower");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bedrockDrillExposesGt6PortModes(GameTestHelper helper) {
        BedrockDrillBlockEntity drill = placeFormed(helper);
        helper.assertTrue(
                drill.handles(EnergyType.KINETIC_ROTATION, Direction.UP)
                        && !drill.handles(
                                EnergyType.KINETIC_ROTATION, Direction.NORTH),
                "Bedrock drill energy input is not top-only");
        helper.assertTrue(
                drill.fluids(Direction.UP) != null
                        && drill.fluids(Direction.DOWN) == null,
                "Bedrock drill fluid port direction drifted");
        helper.assertTrue(
                drill.items(Direction.UP) != null
                        && drill.items(Direction.NORTH) == null,
                "Bedrock drill output inventory direction drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bedrockDrillConsumesRuAndLubricantOnce(
            GameTestHelper helper) {
        BedrockDrillBlockEntity drill = placeFormed(helper);
        long accepted = drill.insert(
                EnergyType.KINETIC_ROTATION,
                4_096L,
                8L,
                Direction.UP,
                false);
        var lubricant = ModFluids.chemical("lubricant")
                .map(entry -> entry.source().get())
                .orElseThrow();
        helper.assertTrue(accepted == 8L, "Bedrock drill rejected RU packets");
        helper.assertTrue(
                drill.fillLube(new FluidStack(lubricant, 100)),
                "Bedrock drill rejected lubricant");
        BedrockDrillBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                drill);
        helper.assertTrue(
                !drill.mined().isEmpty()
                        && drill.fluids(Direction.UP)
                                .getFluidInTank(0)
                                .getAmount() == 0,
                "Bedrock drill did not consume one operation of lubricant");
        long ruAfterMine = drill.stored(EnergyType.KINETIC_ROTATION);
        BedrockDrillBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                drill);
        helper.assertTrue(
                drill.stored(EnergyType.KINETIC_ROTATION) == ruAfterMine,
                "Full drill output consumed RU a second time");
        helper.succeed();
    }

    private static BedrockDrillBlockEntity placeFormed(GameTestHelper helper) {
        helper.setBlock(
                CONTROLLER,
                ModBlocks.BEDROCK_DRILL.get()
                        .defaultBlockState()
                        .setValue(BedrockDrillBlock.FACING, Direction.UP));
        Block wall = ModBlocks.mteInPlaceBlocksById()
                .get(BedrockDrillStructure.WALL_ID)
                .get();
        Block head = ModBlocks.BEDROCK_DRILL_HEAD.get();
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                helper.setBlock(
                        CONTROLLER.offset(dx, -5, dz),
                        Blocks.BEDROCK);
                helper.setBlock(
                        CONTROLLER.offset(dx, -4, dz),
                        head);
                for (int dy = -3; dy <= 0; dy++) {
                    if (dx == 0 && dy == 0 && dz == 0) {
                        continue;
                    }
                    helper.setBlock(
                            CONTROLLER.offset(dx, dy, dz),
                            wall);
                }
            }
        }
        BedrockDrillBlockEntity drill = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(drill != null, "Bedrock drill controller is missing");
        BedrockDrillBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                drill);
        helper.assertTrue(drill.formed(), "Bedrock drill tower did not form");
        return drill;
    }
}
