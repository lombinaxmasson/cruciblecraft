package com.masson.cruciblecraft.logistics.pipe.item;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferDiagnostics;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverItemFilters;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 {@code CoverRetrieverItem}: pull from inventories on the connected item
 * pipe graph into the inventory in front of the retriever.
 */
public final class ItemRetrieverRequests {
    private static final Direction[] DIRECTIONS = Direction.values();

    private ItemRetrieverRequests() {}

    public static int pull(
            Level level,
            BlockPos host,
            Direction coverSide,
            PipeCover cover,
            CoverDefinition definition) {
        if (level == null
                || level.isClientSide
                || host == null
                || coverSide == null
                || cover == null
                || definition == null
                || !level.hasChunkAt(host)) {
            return 0;
        }
        CoverDefinition.Values values = definition.resolve(cover.config());
        int amount = values.exactCount() > 0
                ? values.exactCount()
                : values.rate();
        if (amount <= 0) {
            return 0;
        }
        BlockPos targetPos = host.relative(coverSide);
        if (!level.hasChunkAt(targetPos)) {
            return 0;
        }
        IItemHandler target = capability(
                level, targetPos, coverSide.getOpposite());
        if (target == null) {
            return 0;
        }
        Optional<String> matchId = cover.config().matchId();
        boolean invert = CoverItemFilters.inverted(cover.config());
        for (BlockPos pipePos : scanPipes(level, host)) {
            if (!level.hasChunkAt(pipePos)
                    || !(level.getBlockEntity(pipePos)
                            instanceof ItemPipeBlockEntity pipe)) {
                continue;
            }
            BlockState state = pipe.getBlockState();
            for (Direction direction : DIRECTIONS) {
                if ((pipePos.equals(host) && direction == coverSide)
                        || !AbstractPipeBlock.isConnected(state, direction)) {
                    continue;
                }
                BlockPos sourcePos = pipePos.relative(direction);
                if (sourcePos.equals(targetPos)
                        || !level.hasChunkAt(sourcePos)
                        || level.getBlockEntity(sourcePos)
                                instanceof ItemPipeBlockEntity) {
                    continue;
                }
                IItemHandler source = capability(
                        level, sourcePos, direction.getOpposite());
                if (source == null || source == target) {
                    continue;
                }
                int moved = move(
                        source,
                        target,
                        amount,
                        matchId,
                        invert,
                        values.mode());
                if (moved > 0) {
                    return moved;
                }
            }
        }
        return 0;
    }

    private static List<BlockPos> scanPipes(Level level, BlockPos start) {
        ArrayList<BlockPos> ordered = new ArrayList<>();
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(start.immutable());
        visited.add(start.immutable());
        while (!open.isEmpty()
                && visited.size() < ItemPipeNetworkTraversal.MAX_VISITED_PIPES) {
            BlockPos pos = open.removeFirst();
            if (!(level.getBlockState(pos).getBlock() instanceof ItemPipeBlock)
                    || !(level.getBlockEntity(pos)
                            instanceof ItemPipeBlockEntity)) {
                continue;
            }
            ordered.add(pos);
            BlockState state = level.getBlockState(pos);
            for (Direction direction : DIRECTIONS) {
                if (!AbstractPipeBlock.isConnected(state, direction)) {
                    continue;
                }
                BlockPos next = pos.relative(direction).immutable();
                if (!visited.add(next) || !level.hasChunkAt(next)) {
                    continue;
                }
                if (level.getBlockState(next).getBlock()
                        instanceof ItemPipeBlock) {
                    open.add(next);
                }
            }
        }
        return List.copyOf(ordered);
    }

    private static IItemHandler capability(
            Level level, BlockPos pos, Direction side) {
        try {
            return level.getCapability(
                    Capabilities.ItemHandler.BLOCK, pos, side);
        } catch (RuntimeException failure) {
            PipeTransferDiagnostics.warnOnce(
                    "item retriever discovery",
                    null,
                    "Item retriever discovery failed at " + pos,
                    failure);
            return null;
        }
    }

    private static int move(
            IItemHandler source,
            IItemHandler target,
            int amount,
            Optional<String> matchId,
            boolean invert,
            CoverDefinition.TransferMode mode) {
        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack simulated = source.extractItem(slot, amount, true);
            if (simulated.isEmpty()
                    || !CoverItemFilters.matches(matchId, invert, simulated)
                    || (mode == CoverDefinition.TransferMode.EXACT
                            && simulated.getCount() != amount)) {
                continue;
            }
            ItemStack leftover = insertAll(target, simulated, true);
            int accepted = simulated.getCount() - leftover.getCount();
            if (accepted <= 0
                    || (mode == CoverDefinition.TransferMode.EXACT
                            && accepted != amount)) {
                continue;
            }
            ItemStack extracted = source.extractItem(slot, accepted, false);
            if (extracted.isEmpty()) {
                return 0;
            }
            ItemStack remainder = insertAll(target, extracted, false);
            if (!remainder.isEmpty()) {
                ItemStack bounced = source.insertItem(slot, remainder, false);
                if (!bounced.isEmpty()) {
                    PipeTransferDiagnostics.warnOnce(
                            "item retriever execution",
                            source,
                            "Item retriever could not return a rejected stack");
                }
            }
            return extracted.getCount() - remainder.getCount();
        }
        return 0;
    }

    private static ItemStack insertAll(
            IItemHandler handler, ItemStack stack, boolean simulate) {
        ItemStack remaining = stack.copy();
        for (int slot = 0;
                slot < handler.getSlots() && !remaining.isEmpty();
                slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }
}
