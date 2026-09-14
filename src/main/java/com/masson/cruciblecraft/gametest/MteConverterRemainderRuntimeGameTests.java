package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

@GameTestHolder(MteConverterRemainderRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteConverterRemainderRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_converter_remainder_runtime";
    private static final String TEMPLATE = "empty";

    private MteConverterRemainderRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void luvBatteryBoxIsNotDummy(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "boxwood/battery_luv", MteInPlaceKind.BATTERY_BOX);
        helper.assertTrue(
                MteInPlaceGameTestSupport.item("boxwood/battery_luv")
                        != ModItems.BRONZE_STEAM_ENGINE.get(),
                "LuV battery box aliased a converter host");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void steamTurbineConsumesSteam(GameTestHelper helper) {
        MteInPlaceGameTestSupport.assertLive(
                helper, "steam/turbine_bronze", MteInPlaceKind.STEAM_TURBINE);
        BlockPos sourcePos = new BlockPos(1, 2, 2);
        BlockPos turbinePos = new BlockPos(2, 2, 2);
        FluidPipeBlock pipe = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(
                sourcePos,
                pipe.defaultBlockState().setValue(
                        AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.EAST),
                        true));
        FluidPipeBlockEntity source = helper.getBlockEntity(sourcePos);
        MteInPlaceBlock turbine = ModBlocks.mteInPlaceBlocksById()
                .get(MteInPlaceGameTestSupport.id("steam/turbine_bronze"))
                .get();
        helper.setBlock(
                turbinePos,
                turbine.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.WEST));
        helper.assertTrue(
                source.fillInternal(
                        new FluidStack(ModFluids.STEAM_SOURCE.get(), 200),
                        IFluidHandler.FluidAction.EXECUTE)
                        == 200,
                "source pipe rejected steam");
        helper.assertTrue(
                ((CatalogNamedBlockItem) MteInPlaceGameTestSupport.item(
                        "steam/turbine_bronze")).getBlock()
                        != ModBlocks.BRONZE_STEAM_ENGINE.get(),
                "bronze steam turbine aliased the converter steam engine");
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    MteInPlaceBlockEntity be = helper.getBlockEntity(turbinePos);
                    helper.assertTrue(
                            be.stored(EnergyType.KINETIC_ROTATION) > 0L,
                            "steam turbine stored no rotational energy");
                })
                .thenSucceed();
    }
}
