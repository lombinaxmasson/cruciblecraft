package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ReactorCoreBlock;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.item.ReactorRodItem;
import com.masson.cruciblecraft.nuclear.ReactorRodCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/** Runtime gates for GT6 fission cores. Track C stays unstarted. */
@GameTestHolder(NuclearFissionGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class NuclearFissionGameTests {
    public static final String NAMESPACE = "cruciblecraft_nuclear_fission";
    private static final String TEMPLATE = "empty";

    private NuclearFissionGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void fortySixRodsAreRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ReactorRodCatalog.entries().size() == 46,
                "Reactor rod catalog drifted from 46 GT6 identities");
        for (ReactorRodCatalog.Entry entry : ReactorRodCatalog.entries()) {
            helper.assertTrue(
                    ModItems.reactorRod(entry.id().getPath()).get()
                            instanceof ReactorRodItem,
                    "Missing reactor rod " + entry.id());
        }
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("reactor_core_1x1"))
                        .isEmpty(),
                "1x1 reactor core recipe must stay blocked");
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("reactor_core_2x2"))
                        .isEmpty(),
                "2x2 reactor core recipe must stay blocked");
        helper.assertTrue(
                helper.getLevel().getRecipeManager()
                        .byKey(id("uranium238_fuel_rod"))
                        .isEmpty(),
                "Canner rod recipes must stay blocked");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void insertingFuelRodStopsReactor(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        ReactorCoreBlockEntity core = helper.getBlockEntity(pos);
        helper.assertTrue(core.stopped(), "Fresh reactor core must start stopped");
        core.setStopped(false);
        ItemStack rod = ModItems.reactorRod("uranium238_fuel_rod")
                .get()
                .defaultStack();
        helper.assertTrue(
                core.insertRod(0, rod, null),
                "U-238 rod was rejected");
        helper.assertTrue(core.stopped(), "Inserting a rod must stop the core");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void unstoppedCoreEmitsNeutronsAtTick19(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        ReactorCoreBlockEntity core = helper.getBlockEntity(pos);
        fillDistilledWater(core, 1_000);
        core.insertRod(
                0,
                ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack(),
                null);
        core.setStopped(false);
        core.onTick(19L);
        helper.assertTrue(
                core.neutrons(0) > 0,
                "U-238 rod did not emit on the 20-tick boundary");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reflectorNeighborReturnsNeutrons(GameTestHelper helper) {
        BlockPos fuelPos = new BlockPos(2, 1, 2);
        BlockPos reflectorPos = fuelPos.north();
        helper.setBlock(
                fuelPos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        helper.setBlock(
                reflectorPos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.SOUTH));
        ReactorCoreBlockEntity fuel = helper.getBlockEntity(fuelPos);
        ReactorCoreBlockEntity reflector = helper.getBlockEntity(reflectorPos);
        fillDistilledWater(fuel, 1_000);
        fillDistilledWater(reflector, 1_000);
        fuel.insertRod(
                0,
                ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack(),
                null);
        reflector.insertRod(
                0,
                ModItems.reactorRod("neutron_reflector_rod").get().defaultStack(),
                null);
        fuel.setStopped(false);
        reflector.setStopped(false);
        fuel.onTick(19L);
        helper.assertTrue(
                fuel.neutrons(0) > 4,
                "Reflector neighbor did not bounce neutrons back");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void distilledWaterProducesSteam(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 1, 2);
        helper.setBlock(
                pos,
                ModBlocks.REACTOR_CORE_1X1.get().defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        ReactorCoreBlockEntity core = helper.getBlockEntity(pos);
        fillDistilledWater(core, 10_000);
        core.insertRod(
                0,
                ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack(),
                null);
        core.setStopped(false);
        for (long tick = 0L; tick < 200L; tick++) {
            core.onTick(tick);
        }
        helper.assertTrue(
                core.outputTank().getFluidAmount() > 0,
                "Reactor heat did not convert distilled water into steam");
        helper.assertTrue(
                core.outputTank().getFluid().is(ModFluids.STEAM_SOURCE.get()),
                "Reactor hot output is not steam");
        helper.succeed();
    }

    private static void fillDistilledWater(ReactorCoreBlockEntity core, int amount) {
        var fluid = ModFluids.materialFluid("water_distilled").orElseThrow();
        core.coolantTank().fill(
                new FluidStack(fluid, amount),
                IFluidHandler.FluidAction.EXECUTE);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
