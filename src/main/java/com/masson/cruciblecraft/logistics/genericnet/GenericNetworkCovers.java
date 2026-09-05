package com.masson.cruciblecraft.logistics.genericnet;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Registers generic-network behaviors without touching registerBuiltin. */
public final class GenericNetworkCovers {
    private static boolean registered;

    private GenericNetworkCovers() {}

    public static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        CoverBehaviorRegistry.register(
                GenericNetworkKinds.STORAGE_BEHAVIOR, storageBehavior());
        CoverBehaviorRegistry.register(
                GenericNetworkKinds.TRANSFER_BEHAVIOR, transferBehavior());
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
            public boolean matchesItem(
                    PipeCover cover,
                    CoverDefinition definition,
                    ItemStack stack) {
                return matchesItemId(cover, stack);
            }

            @Override
            public boolean matchesFluid(
                    PipeCover cover,
                    CoverDefinition definition,
                    FluidStack stack) {
                return matchesFluidId(cover, stack);
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
            public boolean matchesItem(
                    PipeCover cover,
                    CoverDefinition definition,
                    ItemStack stack) {
                return matchesItemId(cover, stack);
            }

            @Override
            public boolean matchesFluid(
                    PipeCover cover,
                    CoverDefinition definition,
                    FluidStack stack) {
                return matchesFluidId(cover, stack);
            }

            @Override
            public void tick(
                    PipeCover cover,
                    CoverDefinition definition,
                    TransferContext context) {
                if (context.world() == null || context.hostPos() == null) {
                    return;
                }
                GenericLogisticsNetwork.tickTransfer(
                        cover,
                        definition,
                        context.world(),
                        context.hostPos(),
                        context.side());
            }
        };
    }

    private static boolean matchesItemId(PipeCover cover, ItemStack stack) {
        var expected = cover.config().matchId();
        if (expected.isEmpty()) {
            return true;
        }
        if (stack.isEmpty()) {
            return false;
        }
        return expected.orElseThrow().equals(
                BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
    }

    private static boolean matchesFluidId(PipeCover cover, FluidStack stack) {
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
