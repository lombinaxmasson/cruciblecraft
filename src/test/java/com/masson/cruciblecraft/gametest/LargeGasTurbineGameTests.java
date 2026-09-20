package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineBlockEntity;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated GT6 LargeTurbineGas 17231–17234 gate. Not kTFRUAddon 10000–10006. */
@GameTestHolder(LargeGasTurbineGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeGasTurbineGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_energy_large_gas_turbine";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(1, 2, 2);
    private static final Direction FACING = Direction.WEST;

    private LargeGasTurbineGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fourHousingsAreLive(GameTestHelper helper) {
        helper.assertTrue(
                LargeGasTurbineCatalog.profiles().size() == 4,
                "Large gas turbines drifted from 4");
        int[] sourceIds = {17231, 17232, 17233, 17234};
        int index = 0;
        for (LargeGasTurbineCatalog.Profile profile
                : LargeGasTurbineCatalog.profiles()) {
            helper.assertTrue(
                    profile.sourceId() == sourceIds[index],
                    profile.id() + " source id drifted");
            MteInPlaceGameTestSupport.assertLive(
                    helper, profile.id().getPath(), MteInPlaceKind.GAS_TURBINE);
            index++;
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHousingsRequireStructure(GameTestHelper helper) {
        LargeGasTurbineCatalog.Profile profile = magnalium();
        MteInPlaceBlock turbine = ModBlocks.mteInPlaceBlocksById()
                .get(profile.id())
                .get();
        helper.setBlock(
                CONTROLLER,
                turbine.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, FACING));
        LargeGasTurbineBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing large gas turbine");
        tick(helper, be);
        helper.assertTrue(
                !be.formed(),
                "Bare large gas turbine reported a formed housing");
        helper.assertTrue(
                be.stored(EnergyType.HEAT) == 0L,
                "Unformed large gas turbine stored HU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void methaneStoresHuAndExhaust(GameTestHelper helper) {
        LargeGasTurbineBlockEntity turbine = placeFormed(helper);
        helper.assertTrue(turbine.formed(), "3x3x4 large gas turbine did not form");
        helper.assertTrue(
                turbine.fillInput(methane(5)),
                "Could not fill large gas turbine with methane");
        tick(helper, turbine);
        helper.assertTrue(
                turbine.stored(EnergyType.HEAT) == 1_920L,
                "5 mB methane did not store 64 HU/t * 30 t = 1920 HU");
        helper.assertTrue(
                turbine.outputAmount(0) == 6 && turbine.outputAmount(1) == 3,
                "Methane exhaust was not 6 water + 3 CO2");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void exhaustAtHalfBlocksFuel(GameTestHelper helper) {
        LargeGasTurbineBlockEntity turbine = placeFormed(helper);
        int half = turbine.profile().outputCapacityMb() / 2;
        helper.assertTrue(
                turbine.fillOutput(0, water(half)),
                "Could not fill exhaust to half capacity");
        helper.assertTrue(
                turbine.fillInput(methane(5)),
                "Could not fill fuel before exhaust block");
        tick(helper, turbine);
        helper.assertTrue(
                turbine.inputAmount() == 5
                        && turbine.stored(EnergyType.HEAT) == 0L
                        && turbine.outputAmount(0) == half,
                "Half-full exhaust consumed methane or stored HU");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void plungerDumpsTanks(GameTestHelper helper) {
        LargeGasTurbineBlockEntity turbine = placeFormed(helper);
        helper.assertTrue(
                turbine.fillOutput(0, water(6))
                        && turbine.fillOutput(1, carbonDioxide(3))
                        && turbine.fillOutput(2, water(1))
                        && turbine.fillInput(methane(5)),
                "Could not fill tanks before plunger");
        helper.assertTrue(turbine.trashWithPlunger(), "plunger missed exhaust 0");
        helper.assertTrue(
                turbine.outputAmount(0) == 0 && turbine.outputAmount(1) == 3,
                "plunger did not dump exhaust 0 first");
        helper.assertTrue(turbine.trashWithPlunger(), "plunger missed exhaust 1");
        helper.assertTrue(
                turbine.outputAmount(1) == 0 && turbine.outputAmount(2) == 1,
                "plunger did not dump exhaust 1 second");
        helper.assertTrue(turbine.trashWithPlunger(), "plunger missed exhaust 2");
        helper.assertTrue(
                turbine.outputAmount(2) == 0 && turbine.inputAmount() == 5,
                "plunger did not dump exhaust 2 third");
        helper.assertTrue(turbine.trashWithPlunger(), "plunger missed fuel");
        helper.assertTrue(
                turbine.inputAmount() == 0,
                "plunger did not dump the fuel tank last");
        helper.succeed();
    }

    private static LargeGasTurbineBlockEntity placeFormed(GameTestHelper helper) {
        LargeGasTurbineCatalog.Profile profile = magnalium();
        MteInPlaceBlock turbine = ModBlocks.mteInPlaceBlocksById()
                .get(profile.id())
                .get();
        Block wall = ModBlocks.mteInPlaceBlocksById().get(profile.wallId()).get();
        helper.setBlock(
                CONTROLLER,
                turbine.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, FACING));
        int minX = CONTROLLER.getX();
        int maxX = CONTROLLER.getX() + 3;
        int minY = CONTROLLER.getY() - 1;
        int maxY = CONTROLLER.getY() + 1;
        int minZ = CONTROLLER.getZ() - 1;
        int maxZ = CONTROLLER.getZ() + 1;
        int walls = 0;
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (pos.equals(CONTROLLER)) {
                        continue;
                    }
                    helper.setBlock(pos, wall.defaultBlockState());
                    walls++;
                }
            }
        }
        helper.assertTrue(walls == 35, "GameTest wall count drifted from 35");
        LargeGasTurbineBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing formed large gas turbine");
        tick(helper, be);
        return be;
    }

    private static void tick(
            GameTestHelper helper, LargeGasTurbineBlockEntity turbine) {
        LargeGasTurbineBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                turbine);
    }

    private static LargeGasTurbineCatalog.Profile magnalium() {
        return LargeGasTurbineCatalog.require(
                MteInPlaceGameTestSupport.id("magnalium/gas_turbine_main_housing"));
    }

    private static FluidStack methane(int amount) {
        return new FluidStack(
                ModFluids.materialFluid("methane").orElseThrow(), amount);
    }

    private static FluidStack water(int amount) {
        return new FluidStack(Fluids.WATER, amount);
    }

    private static FluidStack carbonDioxide(int amount) {
        return new FluidStack(
                ModFluids.materialFluid("carbon_dioxide").orElseThrow(), amount);
    }
}
