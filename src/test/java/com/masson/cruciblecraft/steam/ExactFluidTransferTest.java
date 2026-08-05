package com.masson.cruciblecraft.steam;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ExactFluidTransferTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void partialTransferConservesFluid() {
        FluidTank source = tank(1_000, 1_000);
        FluidTank target = new FluidTank(600);

        assertEquals(600, ExactFluidTransfer.move(source, target, 1_280));
        assertEquals(400, source.getFluidAmount());
        assertEquals(600, target.getFluidAmount());
    }

    @Test
    void oversizedSimulationFailsBeforeSourceMutation() {
        FluidTank source = tank(1_000, 1_000);
        FluidTank target = new FluidTank(2_000) {
            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return action.simulate()
                        ? resource.getAmount() + 1
                        : super.fill(resource, action);
            }
        };

        assertThrows(
                IllegalStateException.class,
                () -> ExactFluidTransfer.move(source, target, 1_280));
        assertEquals(1_000, source.getFluidAmount());
        assertEquals(0, target.getFluidAmount());
    }

    @Test
    void executionMismatchFailsAfterSourceCommitWithoutDuplication() {
        FluidTank source = tank(1_000, 1_000);
        FluidTank target = new FluidTank(2_000) {
            @Override
            public int fill(FluidStack resource, FluidAction action) {
                return action.simulate() ? resource.getAmount() : 0;
            }
        };

        assertThrows(
                IllegalStateException.class,
                () -> ExactFluidTransfer.move(source, target, 1_280));
        assertEquals(0, source.getFluidAmount());
        assertEquals(0, target.getFluidAmount());
    }

    private static FluidTank tank(int capacity, int amount) {
        FluidTank tank = new FluidTank(capacity);
        tank.setFluid(new FluidStack(Fluids.WATER, amount));
        return tank;
    }
}
