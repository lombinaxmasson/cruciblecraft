package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.energy.steam.SteamTurbineCatalog;
import com.masson.cruciblecraft.energy.steam.SteamTurbineHatchRole;
import com.masson.cruciblecraft.energy.steam.SteamTurbineHatches;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModCapabilities;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.steam.SteamConversion;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.capabilities.Capabilities;
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
    private static final BlockPos CONTROLLER = new BlockPos(1, 2, 2);
    private static final Direction FACING = Direction.WEST;

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
        runSingle(helper);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidPipeFeedsSmallSteamTurbine(
            GameTestHelper helper) {
        BlockPos turbinePos = new BlockPos(2, 2, 2);
        BlockPos pipePos = turbinePos.west();
        placeBronze(helper, turbinePos);
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(
                pipePos,
                pipeBlock.defaultBlockState().setValue(
                        AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(
                                Direction.EAST),
                        true));
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        MteInPlaceBlockEntity turbine = helper.getBlockEntity(turbinePos);
        helper.assertTrue(
                pipe != null && turbine != null,
                "Steam turbine or fluid pipe block entity missing");
        helper.assertTrue(
                pipe.fillInternal(
                                steam(100),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 100,
                "Could not prime the steam turbine pipe");
        helper.startSequence()
                .thenIdle(2)
                .thenExecute(() -> {
                    helper.assertTrue(
                            pipe.storedFluid().isEmpty(),
                            "Steam pipe did not transfer into the turbine");
                    helper.assertTrue(
                            turbine.stored(EnergyType.KINETIC_ROTATION) > 0L,
                            "Fluid pipe did not feed the small steam turbine");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHousingsRequireStructure(GameTestHelper helper) {
        SteamTurbineCatalog.Profile large = magnalium();
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
        tick(helper, turbinePos);
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
        MteInPlaceBlockEntity turbine = placeFormed(helper);
        helper.assertTrue(
                turbine.tank().fill(steam(12_288), IFluidHandler.FluidAction.EXECUTE)
                        == 12_288,
                "Could not fill magnalium conversion threshold for DistW");
        tick(helper, CONTROLLER);
        helper.assertTrue(
                turbine.distilledTank().getFluidAmount() == 72
                        && SteamConversion.isDistilledWater(
                                turbine.distilledTank().getFluid()),
                "12288 steam did not recover 72 DistW (170 steam/water)");
        BlockPos sideBottom = CONTROLLER.relative(Direction.EAST).below();
        IFluidHandler bottom = fluids(helper, sideBottom);
        helper.assertTrue(
                bottom != null
                        && bottom.drain(72, IFluidHandler.FluidAction.EXECUTE)
                                .getAmount()
                        == 72,
                "Bottom non-frontal wall did not drain DistW");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void singlePushesDistilledToSides(GameTestHelper helper) {
        BlockPos turbinePos = new BlockPos(2, 2, 2);
        BlockPos catcher = turbinePos.north();
        placeBronze(helper, turbinePos);
        placeCatcher(helper, catcher);
        MteInPlaceBlockEntity be = helper.getBlockEntity(turbinePos);
        helper.assertTrue(
                be.tank().fill(steam(144), IFluidHandler.FluidAction.EXECUTE) == 144,
                "Could not fill first steam dump");
        tick(helper, turbinePos);
        tick(helper, turbinePos);
        helper.assertTrue(
                be.tank().fill(steam(48), IFluidHandler.FluidAction.EXECUTE) == 48,
                "Could not fill second steam dump");
        tick(helper, turbinePos);
        tick(helper, turbinePos);
        helper.assertTrue(
                be.tank().fill(steam(48), IFluidHandler.FluidAction.EXECUTE) == 48,
                "Could not fill third steam dump");
        tick(helper, turbinePos);
        FluidPipeBlockEntity pipe = helper.getBlockEntity(catcher);
        helper.assertTrue(
                pipe != null
                        && SteamConversion.isDistilledWater(pipe.storedFluid())
                        && pipe.storedFluid().getAmount() >= 1,
                "Single steam turbine did not push DistW to a side pipe");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wallHatchRolesMatchGt6(GameTestHelper helper) {
        helper.assertTrue(
                SteamTurbineHatches.counts(CONTROLLER, FACING).matchesGt6Horizontal(),
                "Steam turbine hatch counts drifted from GT6 checkStructure2");
        helper.assertTrue(
                SteamTurbineStructure.energyOut(CONTROLLER, FACING)
                        .equals(CONTROLLER.relative(Direction.EAST, 3)),
                "Energy-out wall is not 3 blocks OPOS[facing]");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void wallHatchesForwardIo(GameTestHelper helper) {
        MteInPlaceBlockEntity turbine = placeFormed(helper);
        BlockPos frontTop = CONTROLLER.above();
        BlockPos farEnergy = SteamTurbineStructure.energyOut(CONTROLLER, FACING);
        BlockPos farTop = farEnergy.above();
        BlockPos sideBottom = CONTROLLER.relative(Direction.EAST).below();
        helper.assertTrue(
                fillWall(helper, frontTop, steam(48)) == 48,
                "Frontal 3x3 wall did not forward steam");
        helper.assertTrue(
                turbine.tank().getFluidAmount() == 48,
                "Frontal fill did not land in the controller steam tank");
        helper.assertTrue(
                fillWall(helper, farTop, steam(48)) == 0,
                "NOTHING wall accepted steam");
        helper.assertTrue(
                fillWall(helper, farEnergy, steam(48)) == 0,
                "Energy-out wall accepted steam");
        helper.assertTrue(
                fluids(helper, farEnergy) == null,
                "Energy-out wall exposed a fluid handler");
        helper.assertTrue(
                turbine.distilledTank().fill(
                                SteamConversion.distilledExhaust(6),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 6,
                "Could not fill DistW before wall drain");
        IFluidHandler bottom = fluids(helper, sideBottom);
        helper.assertTrue(
                bottom != null
                        && bottom.drain(6, IFluidHandler.FluidAction.EXECUTE)
                                .getAmount()
                        == 6,
                "Bottom non-frontal wall did not drain DistW");
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
                        && nothing.steamTurbineRole() == SteamTurbineHatchRole.NOTHING
                        && nothing.boundSteamTurbine() == turbine,
                "NOTHING wall was not bound for tool relay");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fluidPipeFeedsFormedLargeSteamTurbine(
            GameTestHelper helper) {
        MteInPlaceBlockEntity turbine = placeFormed(helper);
        BlockPos wallPos = CONTROLLER.above();
        BlockPos pipePos = wallPos.west();
        FluidPipeBlock pipeBlock = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.TINY_FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(
                pipePos,
                pipeBlock.defaultBlockState().setValue(
                        AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(
                                Direction.EAST),
                        true));
        FluidPipeBlockEntity pipe = helper.getBlockEntity(pipePos);
        helper.assertTrue(
                pipe != null && turbine.tank().getFluidAmount() == 0,
                "Large steam turbine pipe fixture was not empty");
        helper.assertTrue(
                pipe.fillInternal(
                                steam(100),
                                IFluidHandler.FluidAction.EXECUTE)
                        == 100,
                "Could not prime the large steam turbine pipe");
        helper.startSequence()
                .thenIdle(1)
                .thenExecute(() -> {
                    helper.assertTrue(
                            pipe.storedFluid().isEmpty(),
                            "Steam pipe did not transfer to the large turbine wall");
                    helper.assertTrue(
                            turbine.tank().getFluidAmount() == 100,
                            "Large steam turbine wall did not accept pipe steam");
                })
                .thenSucceed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void energyOutPushesRu(GameTestHelper helper) {
        MteInPlaceBlockEntity turbine = placeFormed(helper);
        BlockPos farEnergy = SteamTurbineStructure.energyOut(CONTROLLER, FACING);
        BlockPos sink = farEnergy.relative(Direction.EAST);
        placeAxle(helper, sink);
        helper.assertTrue(
                turbine.tank().fill(steam(12_288), IFluidHandler.FluidAction.EXECUTE)
                        == 12_288,
                "Could not fill magnalium conversion threshold");
        tick(helper, CONTROLLER);
        MteInPlaceBlockEntity dest = helper.getBlockEntity(sink);
        helper.assertTrue(
                dest != null && dest.stored(EnergyType.KINETIC_ROTATION) > 0L,
                "Energy-out wall did not push RU into the adjacent axle");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void softHammerStopsLargeConversion(GameTestHelper helper) {
        MteInPlaceBlockEntity turbine = placeFormed(helper);
        helper.assertTrue(
                turbine.tank().fill(steam(12_288), IFluidHandler.FluidAction.EXECUTE)
                        == 12_288,
                "Could not fill steam before soft hammer");
        helper.assertTrue(
                !turbine.toggleSteamTurbineStopped(),
                "Soft hammer did not stop the large steam turbine");
        tick(helper, CONTROLLER);
        helper.assertTrue(
                turbine.tank().getFluidAmount() == 12_288,
                "Stopped large steam turbine still dumped steam");
        helper.succeed();
    }

    private static void runSingle(GameTestHelper helper) {
        BlockPos turbinePos = new BlockPos(2, 2, 2);
        BlockPos sink = turbinePos.east();
        placeBronze(helper, turbinePos);
        placeAxle(helper, sink);
        MteInPlaceBlockEntity be = helper.getBlockEntity(turbinePos);
        helper.assertTrue(
                be.tank().fill(steam(48), IFluidHandler.FluidAction.EXECUTE) == 48,
                "bronze steam turbine rejected the 48 mB GT6 min dump");
        tick(helper, turbinePos);
        MteInPlaceBlockEntity dest = helper.getBlockEntity(sink);
        helper.assertTrue(
                dest != null && dest.stored(EnergyType.KINETIC_ROTATION) > 0L,
                "steam/turbine_bronze did not push RU to the front");
        helper.assertTrue(
                be.stored(EnergyType.KINETIC_ROTATION) == 0L,
                "bronze steam turbine did not waste the converter capacitor");
        helper.assertTrue(
                helper.getBlockState(turbinePos).getValue(MteInPlaceBlock.LIT),
                "bronze steam turbine did not light overlay_active after RU emit");
    }

    private static void placeBronze(GameTestHelper helper, BlockPos turbinePos) {
        MteInPlaceBlock turbine = ModBlocks.mteInPlaceBlocksById()
                .get(id("steam/turbine_bronze"))
                .get();
        helper.setBlock(
                turbinePos,
                turbine.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
    }

    private static void placeCatcher(GameTestHelper helper, BlockPos pos) {
        FluidPipeBlock pipe = (FluidPipeBlock) ModBlocks.pipeBlock(
                "copper",
                MaterialPrefixes.FLUID_PIPE,
                PipeCatalog.Kind.FLUID).get();
        helper.setBlock(
                pos,
                pipe.defaultBlockState().setValue(
                        AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.SOUTH),
                        true));
        helper.getLevel().setBlock(
                helper.absolutePos(pos),
                helper.getBlockState(pos).setValue(
                        AbstractPipeBlock.PROPERTY_BY_DIRECTION.get(Direction.SOUTH),
                        true),
                2);
    }

    private static void placeAxle(GameTestHelper helper, BlockPos pos) {
        MteInPlaceBlock axle = ModBlocks.mteInPlaceBlocksById()
                .get(MteInPlaceGameTestSupport.id("drive/small_brass_axle"))
                .get();
        helper.setBlock(
                pos,
                axle.defaultBlockState().setValue(
                        MteInPlaceBlock.FACING, Direction.EAST));
    }

    private static MteInPlaceBlockEntity placeFormed(GameTestHelper helper) {
        SteamTurbineCatalog.Profile profile = magnalium();
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
        MteInPlaceBlockEntity be = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(be != null, "Missing formed large steam turbine");
        tick(helper, CONTROLLER);
        return be;
    }

    private static void tick(GameTestHelper helper, BlockPos pos) {
        MteInPlaceBlockEntity be = helper.getBlockEntity(pos);
        helper.assertTrue(be != null, "Missing steam turbine to tick");
        MteInPlaceBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(pos),
                helper.getBlockState(pos),
                be);
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

    private static SteamTurbineCatalog.Profile magnalium() {
        return SteamTurbineCatalog.require(
                MteInPlaceGameTestSupport.id("magnalium/steam_turbine_main_housing"));
    }

    private static FluidStack steam(int amount) {
        return new FluidStack(ModFluids.STEAM_SOURCE.get(), amount);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
