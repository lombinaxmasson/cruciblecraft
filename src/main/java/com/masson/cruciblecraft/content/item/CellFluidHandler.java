package com.masson.cruciblecraft.content.item;

import java.util.function.Supplier;
import java.util.function.Predicate;

import com.masson.cruciblecraft.material.CellContentGate;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.SimpleFluidContent;
import net.neoforged.neoforge.fluids.capability.templates.FluidHandlerItemStack;

/** Single-cell fluid handler with a closed liquid/gas content policy. */
public final class CellFluidHandler extends FluidHandlerItemStack {
    public static final int CAPACITY = 1_000;
    private final Predicate<FluidStack> acceptance;

    public CellFluidHandler(
            Supplier<DataComponentType<SimpleFluidContent>> component,
            ItemStack container,
            CellContentGate.Kind kind) {
        this(
                component,
                container,
                fluid -> CellContentGate.accepts(
                        kind,
                        fluid.getFluid()));
    }

    CellFluidHandler(
            Supplier<DataComponentType<SimpleFluidContent>> component,
            ItemStack container,
            Predicate<FluidStack> acceptance) {
        super(component, container, CAPACITY);
        this.acceptance = acceptance;
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return tank == 0
                && !stack.isEmpty()
                && acceptance.test(stack);
    }

    @Override
    public boolean canFillFluidType(FluidStack fluid) {
        return isFluidValid(0, fluid);
    }
}
