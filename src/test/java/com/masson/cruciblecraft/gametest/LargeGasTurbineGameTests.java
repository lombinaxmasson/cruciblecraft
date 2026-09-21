package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineBlockEntity;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineCatalog;
import com.masson.cruciblecraft.energy.largegasturbine.LargeTurbineHatchRole;
import com.masson.cruciblecraft.energy.largegasturbine.LargeTurbineHatches;
import com.masson.cruciblecraft.energy.largegasturbine.LargeTurbineWalls;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
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

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wallHatchRolesMatchGt6(GameTestHelper helper) {
        helper.assertTrue(
                LargeTurbineHatches.counts(CONTROLLER, FACING).matchesGt6Horizontal(),
                "Large turbine hatch counts drifted from GT6 checkStructure2");
        helper.assertTrue(
                SteamTurbineStructure.energyOut(CONTROLLER, FACING)
                        .equals(CONTROLLER.relative(Direction.EAST, 3)),
                "Energy-out wall is not 3 blocks OPOS[facing]");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wallHatchesForwardIo(GameTestHelper helper) {
        LargeGasTurbineBlockEntity turbine = placeFormed(helper);
        BlockPos frontTop = CONTROLLER.above();
        BlockPos farEnergy = SteamTurbineStructure.energyOut(CONTROLLER, FACING);
        BlockPos farTop = farEnergy.above();
        BlockPos sideBottom = CONTROLLER.relative(Direction.EAST).below();
        helper.assertTrue(
                fillWall(helper, frontTop, methane(5)) == 5,
                "Frontal 3x3 wall did not forward methane into the fuel tank");
        helper.assertTrue(
                turbine.inputAmount() == 5,
                "Frontal fill did not land in the controller fuel tank");
        helper.assertTrue(
                fillWall(helper, farTop, methane(5)) == 0,
                "NOTHING wall accepted fuel");
        helper.assertTrue(
                fillWall(helper, farEnergy, methane(5)) == 0,
                "Energy-out wall accepted fuel");
        helper.assertTrue(
                fluids(helper, farEnergy) == null,
                "Energy-out wall exposed a fluid handler");
        helper.assertTrue(
                turbine.fillOutput(0, water(6)),
                "Could not fill exhaust before wall drain");
        IFluidHandler bottom = fluids(helper, sideBottom);
        helper.assertTrue(
                bottom != null && bottom.drain(6, IFluidHandler.FluidAction.EXECUTE).getAmount() == 6,
                "Bottom non-frontal wall did not drain exhaust");
        helper.assertTrue(
                turbine.outputAmount(0) == 0,
                "Bottom drain did not empty controller exhaust");
        IEnergyHandler energy = helper.getLevel().getCapability(
                ModCapabilities.ENERGY,
                helper.absolutePos(farEnergy),
                Direction.EAST);
        helper.assertTrue(
                energy != null
                        && energy.handles(EnergyType.KINETIC_ROTATION, Direction.EAST),
                "Far-wall energy-out hatch did not expose RU");
        MteInPlaceBlockEntity nothing = helper.getBlockEntity(farTop);
        helper.assertTrue(
                nothing != null
                        && nothing.gasTurbineRole() == LargeTurbineHatchRole.NOTHING
                        && nothing.boundGasTurbine() == turbine,
                "NOTHING wall was not bound for tool relay");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void energyOutPushesRu(GameTestHelper helper) {
        LargeGasTurbineBlockEntity turbine = placeFormed(helper);
        BlockPos farEnergy = SteamTurbineStructure.energyOut(CONTROLLER, FACING);
        BlockPos sink = farEnergy.relative(Direction.EAST);
        MteInPlaceBlock axle = ModBlocks.mteInPlaceBlocksById()
                .get(MteInPlaceGameTestSupport.id("drive/small_brass_axle"))
                .get();
        helper.setBlock(
                sink,
                axle.defaultBlockState().setValue(MteInPlaceBlock.FACING, Direction.EAST));
        helper.assertTrue(
                turbine.fillInput(methane(10)),
                "Could not fill 10 mB methane for an RU packet");
        tick(helper, turbine);
        MteInPlaceBlockEntity dest = helper.getBlockEntity(sink);
        helper.assertTrue(
                dest != null && dest.stored(EnergyType.KINETIC_ROTATION) > 0L,
                "Energy-out wall did not push RU into the adjacent axle");
        helper.assertTrue(
                turbine.stored(EnergyType.HEAT) == 0L,
                "Emitted RU did not consume the 3840 HU from two methane parallels");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void survivalRecipesArePresent(GameTestHelper helper) {
        for (LargeGasTurbineCatalog.Profile profile
                : LargeGasTurbineCatalog.profiles()) {
            helper.assertTrue(
                    helper.getLevel().getRecipeManager()
                            .byKey(profile.id())
                            .isPresent(),
                    "Missing survival recipe " + profile.id());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void controllerSidesFillAndDrain(GameTestHelper helper) {
        LargeGasTurbineBlockEntity turbine = placeFormed(helper);
        BlockPos abs = helper.absolutePos(CONTROLLER);
        IFluidHandler south = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, abs, Direction.SOUTH);
        IFluidHandler up = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, abs, Direction.UP);
        IFluidHandler facing = helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK, abs, FACING);
        helper.assertTrue(
                south != null && up != null && facing != null,
                "Controller missing fluids on a side");
        helper.assertTrue(
                south.fill(methane(5), IFluidHandler.FluidAction.EXECUTE) == 5,
                "Controller did not accept fuel on a non-front side");
        helper.assertTrue(
                turbine.inputAmount() == 5,
                "Non-front fill did not land in the fuel tank");
        helper.assertTrue(
                up.fill(methane(5), IFluidHandler.FluidAction.EXECUTE) == 5,
                "Controller did not accept fuel on a second side");
        helper.assertTrue(
                turbine.inputAmount() == 10,
                "Second-side fill did not add to the fuel tank");
        helper.assertTrue(
                turbine.fillOutput(0, water(6)),
                "Could not fill exhaust before controller drain");
        helper.assertTrue(
                facing.drain(6, IFluidHandler.FluidAction.EXECUTE).getAmount() == 6,
                "Controller did not drain exhaust from the front");
        helper.assertTrue(
                turbine.fillOutput(0, water(6)),
                "Could not refill exhaust before side drain");
        helper.assertTrue(
                south.drain(6, IFluidHandler.FluidAction.EXECUTE).getAmount() == 6,
                "Controller did not drain exhaust from a non-front side");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void farWallUsesDesign3(GameTestHelper helper) {
        placeFormed(helper);
        BlockPos farEnergy = SteamTurbineStructure.energyOut(CONTROLLER, FACING);
        helper.assertTrue(
                LargeTurbineWalls.outlet(helper.getBlockState(farEnergy)),
                "Far-wall energy-out hatch is not GT6 design 3");
        helper.assertTrue(
                !LargeTurbineWalls.outlet(helper.getBlockState(CONTROLLER.above())),
                "Frontal wall used far-wall design 3");
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

    private static IFluidHandler fluids(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(pos),
                Direction.WEST);
    }

    private static int fillWall(
            GameTestHelper helper, BlockPos pos, FluidStack stack) {
        IFluidHandler fluids = fluids(helper, pos);
        return fluids == null
                ? 0
                : fluids.fill(stack, IFluidHandler.FluidAction.EXECUTE);
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
