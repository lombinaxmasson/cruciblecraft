package com.masson.cruciblecraft.gametest;

import java.util.List;

import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Identity gate for the seven GT6 fluids that were still missing after the
 * translator repair. Production recipes stay blocked; this test only checks
 * that each fluid is registered and can sit in a tank.
 */
@GameTestHolder(Gt6MissingFluidsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class Gt6MissingFluidsGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_fluid_gt6_missing_fluids";
    private static final String TEMPLATE = "empty";

    private Gt6MissingFluidsGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void registeredFluidsFillATank(GameTestHelper helper) {
        List<Fluid> fluids = List.of(
                ModFluids.molten("plastic").orElseThrow().source().get(),
                ModFluids.chemical("ice").orElseThrow().source().get(),
                ModFluids.chemical("petrotheum").orElseThrow().source().get(),
                ModFluids.named("charged_matter").orElseThrow().source().get(),
                ModFluids.named("fiery_blood").orElseThrow().source().get(),
                ModFluids.named("fiery_tears").orElseThrow().source().get(),
                ModFluids.named("blueberry_juice").orElseThrow().source().get());
        helper.assertTrue(fluids.size() == 7, "expected seven missing-fluid identities");
        for (Fluid fluid : fluids) {
            FluidType type = fluid.getFluidType();
            FluidTank tank = new FluidTank(1_000);
            int filled = tank.fill(new FluidStack(fluid, 250), net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
            helper.assertTrue(
                    filled == 250 && tank.getFluid().getFluid() == fluid,
                    "tank rejected " + type);
            helper.assertTrue(
                    ModFluids.chemical("matter_neutral").orElseThrow().source().get() != fluid,
                    "charged matter must not be neutral matter");
        }
        helper.succeed();
    }
}
