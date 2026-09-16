package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.energy.steam.SteamTurbineCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated steam-turbine STEAM→RU gate. */
@GameTestHolder(SteamTurbineGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class SteamTurbineGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_energy_steam_turbine";
    private static final String TEMPLATE = "empty";

    private SteamTurbineGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void fifteenSinglesConvertSteamToRu(GameTestHelper helper) {
        helper.assertTrue(
                SteamTurbineCatalog.profiles().stream()
                        .filter(profile -> !profile.large())
                        .count()
                        == 15,
                "Steam turbine singles drifted from 15");
        helper.assertTrue(
                SteamTurbineCatalog.profiles().stream()
                        .filter(SteamTurbineCatalog.Profile::large)
                        .count()
                        == 4,
                "Large steam turbines drifted from 4");
        MteInPlaceGameTestSupport.assertLive(
                helper, "steam/turbine_bronze", MteInPlaceKind.STEAM_TURBINE);
        runSingle(helper, "steam/turbine_bronze");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHousingsRequireStructure(GameTestHelper helper) {
        SteamTurbineCatalog.Profile large = SteamTurbineCatalog.profiles().stream()
                .filter(SteamTurbineCatalog.Profile::large)
                .findFirst()
                .orElseThrow();
        BlockPos turbinePos = new BlockPos(2, 2, 2);
        MteInPlaceBlock turbine = ModBlocks.mteInPlaceBlocksById()
                .get(large.id())
                .get();
        helper.setBlock(
                turbinePos,
                turbine.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
        MteInPlaceBlockEntity be = helper.getBlockEntity(turbinePos);
        helper.assertTrue(be != null, "Missing large steam turbine");
        MteInPlaceBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(turbinePos),
                helper.getBlockState(turbinePos),
                be);
        helper.assertTrue(
                !be.formed(),
                "Bare large steam turbine reported a formed housing");
        helper.assertTrue(
                be.stored(EnergyType.KINETIC_ROTATION) == 0L,
                "Unformed large steam turbine produced RU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void distilledWaterRecovery(GameTestHelper helper) {
        BlockPos sourcePos = new BlockPos(1, 2, 2);
        BlockPos turbinePos = new BlockPos(2, 2, 2);
        placeBronze(helper, sourcePos, turbinePos, 200);
        helper.startSequence()
                .thenExecuteAfter(2, () -> {
                    MteInPlaceBlockEntity be = helper.getBlockEntity(turbinePos);
                    helper.assertTrue(
                            be.stored(EnergyType.KINETIC_ROTATION) > 0L,
                            "steam turbine stored no rotational energy");
                    IFluidHandler fluids = be.fluidHandler(Direction.EAST);
                    helper.assertTrue(fluids != null, "Missing turbine fluid handler");
                    var distilled = ModFluids.chemical("water_distilled")
                            .orElseThrow();
                    FluidStack drained = fluids.drain(
                            new FluidStack(distilled.source().get(), 1),
                            IFluidHandler.FluidAction.SIMULATE);
                    helper.assertTrue(
                            !drained.isEmpty(),
                            "200 steam did not recover distilled water");
                })
                .thenSucceed();
    }

    private static void runSingle(GameTestHelper helper, String path) {
        BlockPos sourcePos = new BlockPos(1, 2, 2);
        BlockPos turbinePos = new BlockPos(2, 2, 2);
        placeBronze(helper, sourcePos, turbinePos, 200);
        MteInPlaceBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(turbinePos),
                helper.getBlockState(turbinePos),
                helper.getBlockEntity(turbinePos));
        MteInPlaceBlockEntity be = helper.getBlockEntity(turbinePos);
        helper.assertTrue(
                be.stored(EnergyType.KINETIC_ROTATION) > 0L,
                path + " stored no rotational energy");
    }

    private static void placeBronze(
            GameTestHelper helper,
            BlockPos sourcePos,
            BlockPos turbinePos,
            int steam) {
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
                .get(id("steam/turbine_bronze"))
                .get();
        helper.setBlock(
                turbinePos,
                turbine.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
        helper.assertTrue(
                source.fillInternal(
                        new FluidStack(ModFluids.STEAM_SOURCE.get(), steam),
                        IFluidHandler.FluidAction.EXECUTE)
                        == steam,
                "source pipe rejected steam");
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
