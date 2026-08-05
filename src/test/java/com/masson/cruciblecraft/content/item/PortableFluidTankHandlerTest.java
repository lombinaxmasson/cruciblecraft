package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

class PortableFluidTankHandlerTest {
    private static DataComponentType<SimpleFluidContent> fluidComponent;

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        fluidComponent = DataComponentType.<SimpleFluidContent>builder()
                .persistent(SimpleFluidContent.CODEC)
                .networkSynchronized(SimpleFluidContent.STREAM_CODEC)
                .build();
    }

    @Test
    void fillSimulationDoesNotMutateAndExecutionCapsAtCapacity() {
        ItemStack stack = new ItemStack(Items.BUCKET);
        FluidHandlerItemStack handler = handler(stack);
        FluidStack water = new FluidStack(Fluids.WATER, 70_000);

        assertEquals(
                PortableFluidTankItem.CAPACITY,
                handler.getTankCapacity(0));
        assertEquals(
                PortableFluidTankItem.CAPACITY,
                handler.fill(water, IFluidHandler.FluidAction.SIMULATE));
        assertFalse(stack.has(fluidComponent));
        assertTrue(handler.getFluidInTank(0).isEmpty());

        assertEquals(
                PortableFluidTankItem.CAPACITY,
                handler.fill(water, IFluidHandler.FluidAction.EXECUTE));
        assertEquals(
                PortableFluidTankItem.CAPACITY,
                handler.getFluidInTank(0).getAmount());
        assertTrue(handler.getFluidInTank(0).is(Fluids.WATER));
        assertEquals(
                0,
                handler.fill(
                        new FluidStack(Fluids.LAVA, 1_000),
                        IFluidHandler.FluidAction.EXECUTE));
    }

    @Test
    void drainSimulationAndTypedDrainPreserveFluidIdentity() {
        ItemStack stack = new ItemStack(Items.BUCKET);
        FluidHandlerItemStack handler = handler(stack);
        handler.fill(
                new FluidStack(Fluids.WATER, 5_000),
                IFluidHandler.FluidAction.EXECUTE);

        FluidStack simulated = handler.drain(
                2_000, IFluidHandler.FluidAction.SIMULATE);
        assertEquals(2_000, simulated.getAmount());
        assertTrue(simulated.is(Fluids.WATER));
        assertEquals(5_000, handler.getFluidInTank(0).getAmount());

        assertTrue(handler.drain(
                new FluidStack(Fluids.LAVA, 5_000),
                IFluidHandler.FluidAction.EXECUTE).isEmpty());
        FluidStack drained = handler.drain(
                new FluidStack(Fluids.WATER, 3_000),
                IFluidHandler.FluidAction.EXECUTE);
        assertEquals(3_000, drained.getAmount());
        assertTrue(drained.is(Fluids.WATER));
        assertEquals(2_000, handler.getFluidInTank(0).getAmount());

        handler.drain(2_000, IFluidHandler.FluidAction.EXECUTE);
        assertFalse(stack.has(fluidComponent));
        assertTrue(handler.getFluidInTank(0).isEmpty());
    }

    private static FluidHandlerItemStack handler(ItemStack stack) {
        return new FluidHandlerItemStack(
                () -> fluidComponent,
                stack,
                PortableFluidTankItem.CAPACITY);
    }
}
