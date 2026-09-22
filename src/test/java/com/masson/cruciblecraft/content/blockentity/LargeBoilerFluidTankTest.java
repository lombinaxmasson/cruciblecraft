package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.fluids.FluidStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;

class LargeBoilerFluidTankTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void keepsAdamantiumCapacityAndAmountBeyondNeoForgeIntSurface() {
        LargeBoilerFluidTank tank = new LargeBoilerFluidTank(
                3_000_000_000L,
                stack -> stack.getFluid() == Fluids.WATER);

        tank.addUnsafe(new FluidStack(Fluids.WATER, 1), 2_500_000_000L);

        assertEquals(3_000_000_000L, tank.longCapacity());
        assertEquals(2_500_000_000L, tank.longAmount());
        assertEquals(Integer.MAX_VALUE, tank.getFluidAmount());
        assertEquals(Integer.MAX_VALUE, tank.getFluid().getAmount());
    }

    @Test
    void normalCapabilityTransfersRemainBounded() {
        LargeBoilerFluidTank tank = new LargeBoilerFluidTank(
                128_000L,
                stack -> stack.getFluid() == Fluids.WATER);

        assertEquals(
                1_000,
                tank.fill(
                        new FluidStack(Fluids.WATER, 1_000),
                        net.neoforged.neoforge.fluids.capability.IFluidHandler
                                .FluidAction.EXECUTE));
        assertEquals(
                250,
                tank.drain(
                        250,
                        net.neoforged.neoforge.fluids.capability.IFluidHandler
                                .FluidAction.EXECUTE)
                        .getAmount());
        assertEquals(750L, tank.longAmount());
    }

    @Test
    void longAwareTransfersExposeFullCapacity() {
        LargeBoilerFluidTank tank = new LargeBoilerFluidTank(
                3_000_000_000L,
                stack -> stack.getFluid() == Fluids.WATER);

        assertEquals(
                3_000_000_000L,
                tank.fillLong(
                        new FluidStack(Fluids.WATER, 1),
                        3_000_000_000L,
                        net.neoforged.neoforge.fluids.capability.IFluidHandler
                                .FluidAction.EXECUTE));
        var simulated = tank.drainLong(
                2_500_000_000L,
                net.neoforged.neoforge.fluids.capability.IFluidHandler
                        .FluidAction.SIMULATE);

        assertEquals(2_500_000_000L, simulated.amount());
        assertEquals(3_000_000_000L, tank.longAmount());
        assertEquals(
                2_500_000_000L,
                tank.drainLong(
                                2_500_000_000L,
                                net.neoforged.neoforge.fluids.capability
                                        .IFluidHandler.FluidAction.EXECUTE)
                        .amount());
        assertEquals(500_000_000L, tank.longAmount());
    }
}
