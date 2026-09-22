package com.masson.cruciblecraft.api.fluid;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;

/**
 * Long-capacity fluid endpoint for machines whose GT6 tanks exceed the
 * NeoForge {@code int} capability surface.
 *
 * <p>The regular {@code IFluidHandler} capability remains available for
 * vanilla and NeoForge integrations. CC-aware callers can use this endpoint
 * to inspect and move the full long-sized tank without treating a saturated
 * {@code int} view as the real capacity.</p>
 */
public interface LongFluidHandler {
    int tanks();

    FluidStack fluid(int tank);

    long amount(int tank);

    long capacity(int tank);

    boolean isFluidValid(int tank, FluidStack stack);

    long fill(int tank, FluidStack resource, long maxFill, FluidAction action);

    LongFluidStack drain(int tank, long maxDrain, FluidAction action);

    record LongFluidStack(FluidStack fluid, long amount) {
        public static LongFluidStack empty() {
            return new LongFluidStack(FluidStack.EMPTY, 0L);
        }

        public boolean isEmpty() {
            return fluid.isEmpty() || amount <= 0L;
        }
    }
}
