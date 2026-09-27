package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidBarrelBlockEntity;
import com.masson.cruciblecraft.content.fluidbarrel.FluidBarrelProfile;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerBlock;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerBlockEntity;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerCatalog;
import com.masson.cruciblecraft.energy.largeheatexchanger.LargeHeatExchangerStructure;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Isolated 17197 large heat-exchanger gate. */
@GameTestHolder(LargeHeatExchangerGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class LargeHeatExchangerGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_multiblock";
    private static final String TEMPLATE = "empty";
    private static final BlockPos CONTROLLER = new BlockPos(2, 1, 2);

    private LargeHeatExchangerGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void largeHexFormsAndEmitsHu(GameTestHelper helper) {
        LargeHeatExchangerBlockEntity exchanger = placeFormed(helper);
        helper.assertTrue(exchanger.formed(), "3x3x2 large HEX did not form");
        helper.assertTrue(
                exchanger.fillInput(hotTin(64)),
                "Could not fill large HEX with hot tin");
        LargeHeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                exchanger);
        helper.assertTrue(
                exchanger.energyStored() > 0L && exchanger.outputAmount() > 0,
                "Large HEX did not buffer HU from hot tin");
        Vec3i transmitter = LargeHeatExchangerStructure.transmitters()[0];
        BlockPos sink = CONTROLLER.offset(transmitter).above();
        helper.setBlock(sink, ModBlocks.steelSmeltingCrucible().get().defaultBlockState());
        long before = exchanger.energyStored();
        LargeHeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                exchanger);
        CrucibleBlockEntity crucible = helper.getBlockEntity(sink);
        helper.assertTrue(
                exchanger.energyStored() < before
                        || (crucible != null
                                && crucible.stored(
                                        com.masson.cruciblecraft.api.energy
                                                .EnergyType.HEAT)
                                        > 0L),
                "Large HEX did not emit HU upward from a transmitter");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHexReloadPreservesTanks(GameTestHelper helper) {
        LargeHeatExchangerBlockEntity exchanger = placeFormed(helper);
        helper.assertTrue(
                exchanger.fillInput(hotTin(8)),
                "Could not fill tanks before reload");
        LargeHeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                exchanger);
        long stored = exchanger.energyStored();
        int input = exchanger.inputAmount();
        int output = exchanger.outputAmount();
        helper.assertTrue(stored > 0L && output > 0, "Need HU before reload");
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = exchanger.saveWithoutMetadata(registries);
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_HEAT_EXCHANGER.get().defaultBlockState()
                        .setValue(LargeHeatExchangerBlock.FACING, Direction.NORTH));
        LargeHeatExchangerBlockEntity reloaded = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(reloaded != null, "Missing reloaded large HEX");
        reloaded.loadWithComponents(saved, registries);
        helper.assertTrue(
                reloaded.energyStored() == stored
                        && reloaded.inputAmount() == input
                        && reloaded.outputAmount() == output,
                "Large HEX tanks or HU did not survive reload");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHexIsSurvivalCraftable(GameTestHelper helper) {
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("large_heat_exchanger"))
                        .isPresent(),
                "Missing large heat exchanger survival recipe");
        helper.assertTrue(
                LargeHeatExchangerCatalog.profile().sourceId() == 17197,
                "Large HEX source id drifted from 17197");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHexBottomRingAcceptsHotFluid(GameTestHelper helper) {
        LargeHeatExchangerBlockEntity exchanger = placeFormed(helper);
        int filled = 0;
        for (Vec3i offset : LargeHeatExchangerStructure.bottomWalls()) {
            IFluidHandler hatch = fluidsAt(helper, CONTROLLER.offset(offset));
            helper.assertTrue(
                    hatch != null,
                    "Bottom 18024 was not an input hatch at " + offset);
            helper.assertTrue(
                    hatch.fill(hotTin(1), IFluidHandler.FluidAction.EXECUTE) == 1,
                    "Bottom 18024 rejected hot tin at " + offset);
            helper.assertTrue(
                    hatch.drain(1, IFluidHandler.FluidAction.EXECUTE).isEmpty(),
                    "Bottom 18024 drained fluid at " + offset);
            filled++;
        }
        helper.assertTrue(filled == 8, "Bottom ring is not eight input hatches");
        helper.assertTrue(
                exchanger.inputAmount() == 8,
                "Bottom-ring fills did not land in the controller input tank");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHexStructureOnlyCellsRejectFluid(GameTestHelper helper) {
        placeFormed(helper);
        helper.assertTrue(
                fluidsAt(helper, CONTROLLER.offset(LargeHeatExchangerStructure.TOP_CENTER))
                        == null,
                "Top-center 18024 accepted fluid; GT6 binds it as NOTHING");
        for (Vec3i offset : LargeHeatExchangerStructure.transmitters()) {
            helper.assertTrue(
                    fluidsAt(helper, CONTROLLER.offset(offset)) == null,
                    "Heat transmitter accepted fluid at " + offset);
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHexExhaustLeavesControllerBottom(GameTestHelper helper) {
        LargeHeatExchangerBlockEntity exchanger = placeFormed(helper);
        helper.assertTrue(
                exchanger.fillInput(hotTin(16)),
                "Could not fill hot tin before exhaust push");
        tick(helper, exchanger);
        helper.assertTrue(
                exchanger.outputAmount() > 0,
                "Large HEX produced no waste fluid");
        IFluidHandler down = fluidsAt(helper, CONTROLLER, Direction.DOWN);
        IFluidHandler side = fluidsAt(helper, CONTROLLER, Direction.NORTH);
        helper.assertTrue(
                down != null
                        && down.drain(1, IFluidHandler.FluidAction.SIMULATE).getAmount()
                                == 1,
                "Controller bottom did not offer waste fluid");
        helper.assertTrue(
                side == null
                        || side.drain(1, IFluidHandler.FluidAction.SIMULATE).isEmpty(),
                "Controller side drained waste fluid");
        int before = exchanger.outputAmount();
        BlockPos below = CONTROLLER.below();
        BlockPos aside = below.east();
        Block drum = ModBlocks.fluidBarrelBlocksById()
                .get(FluidBarrelProfile.id("fluid_barrel/bronze_drum"))
                .get();
        helper.setBlock(below, drum.defaultBlockState());
        helper.setBlock(aside, drum.defaultBlockState());
        tick(helper, exchanger);
        FluidBarrelBlockEntity under = helper.getBlockEntity(below);
        FluidBarrelBlockEntity neighbor = helper.getBlockEntity(aside);
        helper.assertTrue(under != null && neighbor != null, "Missing exhaust drums");
        helper.assertTrue(
                under.getFluidInTank(0).getAmount() > 0,
                "Waste fluid did not leave through the controller bottom");
        helper.assertTrue(
                neighbor.getFluidInTank(0).isEmpty(),
                "Waste fluid left through a face other than the controller bottom");
        helper.assertTrue(
                exchanger.outputAmount() < before,
                "Controller kept all waste fluid after the bottom push");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void largeHexBrokenWallUnbindsInput(GameTestHelper helper) {
        LargeHeatExchangerBlockEntity exchanger = placeFormed(helper);
        Vec3i[] bottoms = LargeHeatExchangerStructure.bottomWalls();
        BlockPos broken = CONTROLLER.offset(bottoms[0]);
        BlockPos kept = CONTROLLER.offset(bottoms[1]);
        helper.setBlock(broken, Blocks.AIR.defaultBlockState());
        tick(helper, exchanger);
        helper.assertTrue(!exchanger.formed(), "Large HEX stayed formed with a missing wall");
        helper.assertTrue(
                fluidsAt(helper, kept) == null,
                "Remaining 18024 stayed an input hatch after the structure broke");
        helper.succeed();
    }

    private static LargeHeatExchangerBlockEntity placeFormed(GameTestHelper helper) {
        var profile = LargeHeatExchangerCatalog.profile();
        Block wall = ModBlocks.mteInPlaceBlocksById().get(profile.wallId()).get();
        Block transmitter = ModBlocks.mteInPlaceBlocksById()
                .get(profile.transmitterId())
                .get();
        helper.setBlock(
                CONTROLLER,
                ModBlocks.LARGE_HEAT_EXCHANGER.get().defaultBlockState()
                        .setValue(LargeHeatExchangerBlock.FACING, Direction.NORTH));
        for (Vec3i offset : LargeHeatExchangerStructure.bottomWalls()) {
            helper.setBlock(
                    CONTROLLER.offset(offset),
                    wall.defaultBlockState());
        }
        helper.setBlock(
                CONTROLLER.offset(LargeHeatExchangerStructure.TOP_CENTER),
                wall.defaultBlockState());
        for (Vec3i offset : LargeHeatExchangerStructure.transmitters()) {
            helper.setBlock(
                    CONTROLLER.offset(offset),
                    transmitter.defaultBlockState());
        }
        LargeHeatExchangerBlockEntity exchanger = helper.getBlockEntity(CONTROLLER);
        helper.assertTrue(exchanger != null, "Missing large HEX controller");
        tick(helper, exchanger);
        return exchanger;
    }

    private static void tick(
            GameTestHelper helper, LargeHeatExchangerBlockEntity exchanger) {
        LargeHeatExchangerBlockEntity.serverTick(
                helper.getLevel(),
                helper.absolutePos(CONTROLLER),
                helper.getBlockState(CONTROLLER),
                exchanger);
    }

    private static IFluidHandler fluidsAt(GameTestHelper helper, BlockPos pos) {
        return fluidsAt(helper, pos, Direction.UP);
    }

    private static IFluidHandler fluidsAt(
            GameTestHelper helper, BlockPos pos, Direction side) {
        return helper.getLevel().getCapability(
                Capabilities.FluidHandler.BLOCK,
                helper.absolutePos(pos),
                side);
    }

    private static FluidStack hotTin(int amount) {
        return new FluidStack(
                ModFluids.hotSource("hot_molten_tin").orElseThrow(), amount);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
