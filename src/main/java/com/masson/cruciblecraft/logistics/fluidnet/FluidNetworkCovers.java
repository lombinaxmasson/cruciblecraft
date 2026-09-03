package com.masson.cruciblecraft.logistics.fluidnet;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.fluids.FluidStack;

/** Registers fluid-network behaviors without touching registerBuiltin. */
public final class FluidNetworkCovers {
    private static boolean registered;

    private FluidNetworkCovers() {}

    public static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        CoverBehaviorRegistry.register(
                FluidNetworkKinds.STORAGE_BEHAVIOR, storageBehavior());
        CoverBehaviorRegistry.register(
                FluidNetworkKinds.TRANSFER_BEHAVIOR, transferBehavior());
        registered = true;
    }

    private static CoverBehavior storageBehavior() {
        return new CoverBehavior() {
            @Override
            public boolean allowsIncoming(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access) {
                return false;
            }

            @Override
            public boolean allowsOutgoing(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access) {
                return false;
            }

            @Override
            public boolean matchesFluid(
                    PipeCover cover,
                    CoverDefinition definition,
                    FluidStack stack) {
                return matches(cover, stack);
            }
        };
    }

    private static CoverBehavior transferBehavior() {
        return new CoverBehavior() {
            @Override
            public boolean allowsIncoming(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access) {
                return false;
            }

            @Override
            public boolean allowsOutgoing(
                    PipeCover cover,
                    CoverDefinition definition,
                    Access access) {
                return false;
            }

            @Override
            public boolean matchesFluid(
                    PipeCover cover,
                    CoverDefinition definition,
                    FluidStack stack) {
                return matches(cover, stack);
            }

            @Override
            public void tick(
                    PipeCover cover,
                    CoverDefinition definition,
                    TransferContext context) {
                if (context.world() == null || context.hostPos() == null) {
                    return;
                }
                FluidLogisticsNetwork.tickTransfer(
                        cover,
                        definition,
                        context.world(),
                        context.hostPos(),
                        context.side());
            }
        };
    }

    private static boolean matches(PipeCover cover, FluidStack stack) {
        var expected = cover.config().matchId();
        if (expected.isEmpty()) {
            return true;
        }
        if (stack.isEmpty()) {
            return false;
        }
        return expected.orElseThrow().equals(
                BuiltInRegistries.FLUID.getKey(stack.getFluid()).toString());
    }
}
