package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ReactorCoreBlock;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.nuclear.ReactorCoolant;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.verification.PlayerCompleteSmoke;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated fission hot-fluid gate. Run with
 * {@code -PgameTestGrid=energy}.
 */
@GameTestHolder(NuclearFissionHotFluidsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class NuclearFissionHotFluidsGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_energy";
    private static final String TEMPLATE = "empty";
    private static final String CAPABILITY = "energy/nuclear-fission-hot-fluids";
    private static final BlockPos POS = new BlockPos(2, 1, 2);

    private NuclearFissionHotFluidsGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                ModFluids.hotFluids().size() == 8,
                "Hot-fluid catalog drifted from 8 GT6 identities");
        helper.assertTrue(
                ModItems.REACTOR_CORE_1X1.get() != null
                        && ModItems.REACTOR_CORE_2X2.get() != null,
                "Reactor core items are missing");
        for (ModFluids.HotFluidEntry entry : ModFluids.hotFluids()) {
            helper.assertTrue(
                    entry.source().get() != null && entry.flowing().get() != null,
                    "Missing hot fluid pair " + entry.id());
            helper.assertTrue(
                    entry.source().get() != entry.flowing().get(),
                    "Hot fluid source aliases flowing " + entry.id());
            helper.assertTrue(
                    ModFluids.hotId(entry.source().get()).orElse("").equals(entry.id())
                            && ModFluids.hotId(entry.flowing().get())
                                    .orElse("")
                                    .equals(entry.id()),
                    "Hot-fluid reverse lookup missed " + entry.id());
            helper.assertTrue(
                    ModFluids.material(entry.source().get()).isEmpty(),
                    "Hot fluid resolved as a materialFluid " + entry.id());
            ModFluids.materialFluid(entry.sourceMaterialId()).ifPresent(cold ->
                    helper.assertTrue(
                            cold != entry.source().get(),
                            "Hot fluid aliases cold materialFluid " + entry.id()));
        }
        PlayerCompleteSmoke.writeIfConfigured("gameTestServer", CAPABILITY);
        com.google.gson.JsonObject snapshot =
                PlayerCompleteSmoke.snapshot("gameTestServer", CAPABILITY);
        helper.assertTrue(
                snapshot.get("status").getAsString().equals("PASS"),
                "Player-complete registry snapshot failed: " + snapshot);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void carbonDioxideProducesHotCarbonDioxide(
            GameTestHelper helper) {
        convertAndBackpressure(helper, ReactorCoolant.CARBON_DIOXIDE);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void distilledWaterProducesSteam(GameTestHelper helper) {
        ReactorCoreBlockEntity core = place(helper, POS, false);
        convertAndBackpressure(helper, core, ReactorCoolant.DISTILLED_WATER);
        drainOutput(core);
        fillTank(
                core.outputTank(),
                ReactorCoolant.DISTILLED_WATER.inputFluid().orElseThrow(),
                1_000);
        long heat = injectHeat(core, ReactorCoolant.DISTILLED_WATER, 10);
        int coolant = core.coolantTank().getFluidAmount();
        core.onTick(3L);
        helper.assertTrue(
                core.heat() == heat
                        && core.coolantTank().getFluidAmount() == coolant
                        && core.outputTank()
                                .getFluid()
                                .is(ReactorCoolant.DISTILLED_WATER.inputFluid()
                                        .orElseThrow()),
                "Cold output identity was reinterpreted as steam");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void emptyCoolantDestroysRods(GameTestHelper helper) {
        ReactorCoreBlockEntity core = place(helper, POS, false);
        helper.assertTrue(
                core.insertRod(
                        0,
                        ModItems.reactorRod("uranium238_fuel_rod")
                                .get()
                                .defaultStack(),
                        null),
                "U-238 rod was rejected");
        core.setStopped(false);
        core.onTick(19L);
        helper.assertTrue(
                core.rod(0).isEmpty() && core.heat() > 0L,
                "Empty coolant with lastHeat did not destroy rods");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void heavyWaterProducesHotHeavyWater(GameTestHelper helper) {
        convertAndBackpressure(helper, ReactorCoolant.HEAVY_WATER);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void heliumProducesHotHelium(GameTestHelper helper) {
        convertAndBackpressure(helper, ReactorCoolant.HELIUM);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void hotFluidsDoNotAliasMaterialFluids(GameTestHelper helper) {
        for (ModFluids.HotFluidEntry entry : ModFluids.hotFluids()) {
            helper.assertTrue(
                    ModFluids.hotSource(entry.id()).orElseThrow()
                            != ModFluids.materialFluid(entry.sourceMaterialId())
                                    .orElse(null),
                    "Hot identity aliases materialFluid " + entry.id());
            helper.assertTrue(
                    !ModFluids.isHotFluid(
                            ModFluids.materialFluid(entry.sourceMaterialId())
                                    .orElse(ModFluids.STEAM_SOURCE.get())),
                    "Cold coolant is registered as a hot fluid " + entry.id());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void insufficientCoolantDestroysRods(GameTestHelper helper) {
        ReactorCoreBlockEntity core = place(helper, POS, false);
        fillCoolant(core, ReactorCoolant.DISTILLED_WATER, 1);
        helper.assertTrue(
                core.insertRod(
                        0,
                        ModItems.reactorRod("uranium238_fuel_rod")
                                .get()
                                .defaultStack(),
                        null),
                "U-238 rod was rejected");
        core.setStopped(false);
        core.addHeat(800L);
        core.onTick(0L);
        helper.assertTrue(
                core.rod(0).isEmpty()
                        && core.heat() == 800L
                        && core.coolantTank().getFluidAmount() == 1
                        && core.outputTank().getFluidAmount() == 0,
                "Insufficient coolant did not keep heat/input while destroying rods");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void moltenLiclProducesHotLicl(GameTestHelper helper) {
        convertAndBackpressure(helper, ReactorCoolant.MOLTEN_LICL);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void moltenSodiumProducesHotSodium(GameTestHelper helper) {
        convertAndBackpressure(helper, ReactorCoolant.MOLTEN_SODIUM);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void moltenTinProducesHotTin(GameTestHelper helper) {
        convertAndBackpressure(helper, ReactorCoolant.MOLTEN_TIN);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void oneByOneAndTwoByTwoShareConversionContract(
            GameTestHelper helper) {
        ReactorCoreBlockEntity one = place(helper, new BlockPos(1, 1, 2), false);
        ReactorCoreBlockEntity two = place(helper, new BlockPos(4, 1, 2), true);
        convertOnce(helper, one, ReactorCoolant.DISTILLED_WATER);
        convertOnce(helper, two, ReactorCoolant.DISTILLED_WATER);
        helper.assertTrue(
                one.outputTank().getFluidAmount()
                                == two.outputTank().getFluidAmount()
                        && one.outputTank().getFluidAmount() == 1_600
                        && one.outputTank()
                                .getFluid()
                                .is(ModFluids.STEAM_SOURCE.get())
                        && two.outputTank()
                                .getFluid()
                                .is(ModFluids.STEAM_SOURCE.get())
                        && one.coolantTank().getFluidAmount()
                                == two.coolantTank().getFluidAmount(),
                "1x1 and 2x2 conversion contracts drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void reloadPreservesHeatContract(GameTestHelper helper) {
        ReactorCoreBlockEntity core = place(helper, POS, false);
        fillCoolant(core, ReactorCoolant.DISTILLED_WATER, 10_000);
        helper.assertTrue(
                core.insertRod(
                        0,
                        ModItems.reactorRod("uranium238_fuel_rod")
                                .get()
                                .defaultStack(),
                        null),
                "U-238 rod was rejected");
        core.setStopped(false);
        core.onTick(19L);
        helper.assertTrue(core.lastHeat() > 0L, "Expected lastHeat after emit");
        var registries = helper.getLevel().registryAccess();
        CompoundTag saved = core.saveWithoutMetadata(registries);
        long heat = core.heat();
        long lastHeat = core.lastHeat();
        int coolant = core.coolantTank().getFluidAmount();
        int output = core.outputTank().getFluidAmount();
        boolean stopped = core.stopped();
        boolean running = core.running();
        core.addHeat(99_999L);
        core.setStopped(true);
        core.loadWithComponents(saved, registries);
        helper.assertTrue(
                core.heat() == heat
                        && core.lastHeat() == lastHeat
                        && core.coolantTank().getFluidAmount() == coolant
                        && core.outputTank().getFluidAmount() == output
                        && core.stopped() == stopped
                        && core.running() == running,
                "Heat contract did not survive BlockEntity reload");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void semiheavyWaterProducesHotSemiheavyWater(
            GameTestHelper helper) {
        convertAndBackpressure(helper, ReactorCoolant.SEMI_HEAVY_WATER);
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void tinDividerIsThreeAndSodiumDividerIsSix(
            GameTestHelper helper) {
        ReactorCoreBlockEntity water = place(helper, new BlockPos(1, 1, 1), false);
        ReactorCoreBlockEntity tin = place(helper, new BlockPos(3, 1, 1), false);
        ReactorCoreBlockEntity sodium = place(helper, new BlockPos(5, 1, 1), false);
        fillCoolant(water, ReactorCoolant.DISTILLED_WATER, 1_000);
        fillCoolant(tin, ReactorCoolant.MOLTEN_TIN, 1_000);
        fillCoolant(sodium, ReactorCoolant.MOLTEN_SODIUM, 1_000);
        insertFuel(water);
        insertFuel(tin);
        insertFuel(sodium);
        water.setStopped(false);
        tin.setStopped(false);
        sodium.setStopped(false);
        water.onTick(19L);
        tin.onTick(19L);
        sodium.onTick(19L);
        helper.assertTrue(
                water.lastHeat() == 4L
                        && tin.lastHeat() == 2L
                        && sodium.lastHeat() == 1L,
                "Tin/sodium heat dividers drifted from GT6 3/6");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void tritiatedWaterProducesHotTritiatedWater(
            GameTestHelper helper) {
        convertAndBackpressure(helper, ReactorCoolant.TRITIATED_WATER);
        helper.succeed();
    }

    private static void convertAndBackpressure(
            GameTestHelper helper, ReactorCoolant kind) {
        convertAndBackpressure(helper, place(helper, POS, false), kind);
    }

    private static void convertAndBackpressure(
            GameTestHelper helper,
            ReactorCoreBlockEntity core,
            ReactorCoolant kind) {
        Fluid hot = convertOnce(helper, core, kind);
        int space = core.outputTank().getCapacity()
                - core.outputTank().getFluidAmount();
        if (space > 0) {
            fillTank(core.outputTank(), hot, space);
        }
        long heat = injectHeat(core, kind, 10);
        int coolant = core.coolantTank().getFluidAmount();
        int output = core.outputTank().getFluidAmount();
        core.onTick(1L);
        helper.assertTrue(
                core.heat() == heat
                        && core.coolantTank().getFluidAmount() == coolant
                        && core.outputTank().getFluidAmount() == output,
                "Backpressure consumed heat or input for " + kind.hotOutputId());
    }

    private static Fluid convertOnce(
            GameTestHelper helper,
            ReactorCoreBlockEntity core,
            ReactorCoolant kind) {
        Fluid hot = hotFluid(kind);
        fillCoolant(core, kind, 1_000);
        int produced = kind.outputAmount(10);
        injectHeat(core, kind, 10);
        core.onTick(0L);
        helper.assertTrue(
                core.heat() == 0L
                        && core.coolantTank().getFluidAmount() == 990
                        && core.outputTank().getFluid().is(hot)
                        && core.outputTank().getFluidAmount() == produced,
                "Conversion drifted for " + kind.hotOutputId());
        return hot;
    }

    private static ReactorCoreBlockEntity place(
            GameTestHelper helper, BlockPos pos, boolean twoByTwo) {
        helper.setBlock(
                pos,
                (twoByTwo
                        ? ModBlocks.REACTOR_CORE_2X2
                        : ModBlocks.REACTOR_CORE_1X1)
                        .get()
                        .defaultBlockState()
                        .setValue(ReactorCoreBlock.FACING, Direction.NORTH));
        return helper.getBlockEntity(pos);
    }

    private static void insertFuel(ReactorCoreBlockEntity core) {
        core.insertRod(
                0,
                ModItems.reactorRod("uranium238_fuel_rod").get().defaultStack(),
                null);
    }

    private static void fillCoolant(
            ReactorCoreBlockEntity core, ReactorCoolant kind, int amount) {
        fillTank(core.coolantTank(), kind.inputFluid().orElseThrow(), amount);
    }

    private static void fillTank(FluidTank tank, Fluid fluid, int amount) {
        tank.fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    private static void drainOutput(ReactorCoreBlockEntity core) {
        core.outputTank().drain(
                core.outputTank().getCapacity(),
                IFluidHandler.FluidAction.EXECUTE);
    }

    private static long injectHeat(
            ReactorCoreBlockEntity core, ReactorCoolant kind, int units) {
        core.addHeat((long) kind.euPerUnit() * units);
        return core.heat();
    }

    private static Fluid hotFluid(ReactorCoolant kind) {
        if (kind.steamOutput()) {
            return ModFluids.STEAM_SOURCE.get();
        }
        return ModFluids.hotSource(kind.hotOutputId()).orElseThrow();
    }
}
