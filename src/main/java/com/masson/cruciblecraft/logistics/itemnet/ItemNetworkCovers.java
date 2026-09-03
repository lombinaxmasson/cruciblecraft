package com.masson.cruciblecraft.logistics.itemnet;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

/** Registers the two item-network behaviors without touching registerBuiltin. */
public final class ItemNetworkCovers {
    private static boolean registered;

    private ItemNetworkCovers() {}

    public static synchronized void bootstrap() {
        if (registered) {
            return;
        }
        CoverBehaviorRegistry.register(
                ItemNetworkKinds.STORAGE_BEHAVIOR, storageBehavior());
        CoverBehaviorRegistry.register(
                ItemNetworkKinds.TRANSFER_BEHAVIOR, transferBehavior());
        registered = true;
    }

    private static CoverBehavior storageBehavior() {
        return new CoverBehavior() {
            @Override
            public boolean matchesItem(
                    PipeCover cover,
                    CoverDefinition definition,
                    ItemStack stack) {
                return matches(cover, stack);
            }
        };
    }

    private static CoverBehavior transferBehavior() {
        return new CoverBehavior() {
            @Override
            public boolean matchesItem(
                    PipeCover cover,
                    CoverDefinition definition,
                    ItemStack stack) {
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
                ItemLogisticsNetwork.tickTransfer(
                        cover,
                        definition,
                        context.world(),
                        context.hostPos(),
                        context.side());
            }
        };
    }

    private static boolean matches(PipeCover cover, ItemStack stack) {
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
}
