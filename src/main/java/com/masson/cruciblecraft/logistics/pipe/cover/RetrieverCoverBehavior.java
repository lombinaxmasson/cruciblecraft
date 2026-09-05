package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.logistics.pipe.item.ItemRetrieverRequests;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * GT6 item retriever: request from the pipe graph into the front inventory.
 * Invert is a visual/filter flag, not a Display CPU.
 */
public final class RetrieverCoverBehavior implements CoverBehavior {
    static final int INTERVAL_TICKS = 20;

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
        return CoverItemFilters.matches(
                cover.config().matchId(),
                CoverItemFilters.inverted(cover.config()),
                stack);
    }

    @Override
    public boolean matchesFluid(
            PipeCover cover,
            CoverDefinition definition,
            FluidStack stack) {
        return false;
    }

    @Override
    public void tick(
            PipeCover cover,
            CoverDefinition definition,
            TransferContext context) {
        if (context.medium() != CoverDefinition.Medium.ITEM
                || context.world() == null
                || context.hostPos() == null
                || !CoverTransferTiming.due(
                        context.world(), INTERVAL_TICKS)) {
            return;
        }
        ItemRetrieverRequests.pull(
                context.world(),
                context.hostPos(),
                context.side(),
                cover,
                definition);
    }
}
