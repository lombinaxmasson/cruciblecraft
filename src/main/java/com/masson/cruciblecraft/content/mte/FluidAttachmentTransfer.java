package com.masson.cruciblecraft.content.mte;

import java.util.Objects;

import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.steam.ExactFluidTransfer;

import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Phase/proof filtering shared by the four GT6 fluid attachments.
 *
 * <p>GT6 hosts can expose several tanks. The filtered view deliberately scans
 * those tanks instead of relying on {@link IFluidHandler#drain(int,
 * IFluidHandler.FluidAction)} choosing the correct one.
 */
public final class FluidAttachmentTransfer {
    private FluidAttachmentTransfer() {}

    public static int move(
            IFluidHandler source,
            IFluidHandler target,
            MteFluidAttachmentProfile profile,
            int limit) {
        if (source == null || target == null || limit <= 0) {
            return 0;
        }
        return ExactFluidTransfer.move(
                filtered(source, profile),
                filtered(target, profile),
                limit);
    }

    public static IFluidHandler filtered(
            IFluidHandler delegate,
            MteFluidAttachmentProfile profile) {
        return delegate == null
                ? null
                : new FilteredHandler(
                        Objects.requireNonNull(delegate, "delegate"),
                        Objects.requireNonNull(profile, "profile"));
    }

    public static boolean accepted(
            FluidStack stack,
            MteFluidAttachmentProfile profile) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        boolean gas = isGas(stack);
        if ((profile.phase() == MteFluidAttachmentProfile.Phase.GAS) != gas) {
            return false;
        }
        return profile.acidProof() || !isAcid(stack);
    }

    public static boolean isGas(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        try {
            if (ModFluids.chemicalState(stack.getFluid())
                    .filter(state -> state
                            == com.masson.cruciblecraft.material
                                    .ChemicalFluidRegistrationGate.State.GAS)
                    .isPresent()) {
                return true;
            }
        } catch (IllegalStateException ignored) {
            // Common setup may not have published the chemical reverse index.
        }
        if (stack.is(ModFluids.STEAM_SOURCE.get())
                || stack.is(ModFluids.STEAM_FLOWING.get())) {
            return true;
        }
        return ModFluids.material(stack.getFluid())
                .flatMap(MaterialDefinition::gt6Metadata)
                .map(metadata -> "gas".equals(metadata.state()))
                .orElse(false);
    }

    public static boolean isAcid(FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        return ModFluids.material(stack.getFluid())
                .map(material -> material.hasMaterialTag("PROPERTIES.ACID"))
                .orElse(false);
    }

    private static final class FilteredHandler implements IFluidHandler {
        private final IFluidHandler delegate;
        private final MteFluidAttachmentProfile profile;

        private FilteredHandler(
                IFluidHandler delegate,
                MteFluidAttachmentProfile profile) {
            this.delegate = delegate;
            this.profile = profile;
        }

        @Override
        public int getTanks() {
            return delegate.getTanks();
        }

        @Override
        public FluidStack getFluidInTank(int tank) {
            FluidStack fluid = delegate.getFluidInTank(tank);
            return accepted(fluid, profile) ? fluid : FluidStack.EMPTY;
        }

        @Override
        public int getTankCapacity(int tank) {
            FluidStack fluid = delegate.getFluidInTank(tank);
            return !fluid.isEmpty() && !accepted(fluid, profile)
                    ? 0
                    : delegate.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, FluidStack stack) {
            return accepted(stack, profile)
                    && delegate.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action) {
            return accepted(resource, profile)
                    ? delegate.fill(resource, action)
                    : 0;
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action) {
            return accepted(resource, profile)
                    ? delegate.drain(resource, action)
                    : FluidStack.EMPTY;
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action) {
            if (maxDrain <= 0) {
                return FluidStack.EMPTY;
            }
            for (int tank = 0; tank < delegate.getTanks(); tank++) {
                FluidStack fluid = delegate.getFluidInTank(tank);
                if (!accepted(fluid, profile)) {
                    continue;
                }
                FluidStack request = fluid.copyWithAmount(
                        Math.min(maxDrain, fluid.getAmount()));
                FluidStack drained = delegate.drain(request, action);
                if (!drained.isEmpty() && accepted(drained, profile)) {
                    return drained;
                }
            }
            return FluidStack.EMPTY;
        }
    }
}
