package com.masson.cruciblecraft.steam;

import java.util.Objects;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Moves fluid across a capability boundary with exact simulation/execution checks.
 *
 * <p>The source is committed before the target so a contract violation can lose
 * fluid but cannot duplicate it. Capability APIs provide no rollback.
 */
public final class ExactFluidTransfer {
    private ExactFluidTransfer() {}

    public static int move(IFluidHandler source, IFluidHandler target, int limit) {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(target, "target");
        if (limit <= 0) {
            return 0;
        }

        FluidStack offered = source.drain(limit, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return 0;
        }
        if (offered.getAmount() > limit) {
            throw new IllegalStateException(
                    endpoint("source simulation", source)
                            + " returned " + offered.getAmount()
                            + " mB for a request of " + limit + " mB");
        }
        int accepted = target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        requireBoundedFill(target, offered.getAmount(), accepted);
        if (accepted == 0) {
            return 0;
        }

        FluidStack transfer = offered.copyWithAmount(accepted);
        requireExactDrain(
                source,
                transfer,
                source.drain(transfer, IFluidHandler.FluidAction.SIMULATE),
                "simulation");

        // No rollback exists. Committing the source first makes an endpoint
        // violation fail toward fluid loss rather than duplication.
        requireExactDrain(
                source,
                transfer,
                source.drain(transfer, IFluidHandler.FluidAction.EXECUTE),
                "execution");
        int executed = target.fill(transfer, IFluidHandler.FluidAction.EXECUTE);
        if (executed != accepted) {
            throw new IllegalStateException(
                    endpoint("target execution", target)
                            + " returned " + executed
                            + " mB after simulating " + accepted + " mB");
        }
        return accepted;
    }

    private static void requireBoundedFill(
            IFluidHandler target,
            int offered,
            int accepted) {
        if (accepted < 0 || accepted > offered) {
            throw new IllegalStateException(
                    endpoint("target simulation", target)
                            + " returned " + accepted
                            + " mB for an offer of " + offered + " mB");
        }
    }

    private static void requireExactDrain(
            IFluidHandler source,
            FluidStack expected,
            FluidStack actual,
            String operation) {
        if (!FluidStack.isSameFluidSameComponents(expected, actual)
                || expected.getAmount() != actual.getAmount()) {
            throw new IllegalStateException(
                    endpoint("source " + operation, source)
                            + " returned " + description(actual)
                            + " after simulating " + description(expected));
        }
    }

    private static String endpoint(String operation, IFluidHandler handler) {
        return operation + " using " + handler.getClass().getName();
    }

    private static String description(FluidStack stack) {
        return stack.isEmpty()
                ? "empty"
                : stack.getFluid().getFluidType().getDescriptionId()
                        + " x" + stack.getAmount() + " mB";
    }
}
