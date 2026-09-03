package com.masson.cruciblecraft.logistics.itemnet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Item cover network: identity ∩ loaded item-pipe component.
 * DESIGN_POLICY: not a world broadcast and not adjacent conveyor transfer.
 */
public final class ItemLogisticsNetwork {
    private static final Direction[] DIRECTIONS = Direction.values();

    private ItemLogisticsNetwork() {}

    public static int tickTransfer(
            PipeCover cover,
            CoverDefinition definition,
            Level level,
            BlockPos pipePos,
            Direction side) {
        if (level == null
                || level.isClientSide
                || pipePos == null
                || side == null
                || cover == null
                || definition == null) {
            return 0;
        }
        Optional<ItemNetworkKinds.TransferDirection> direction =
                ItemNetworkKinds.direction(cover.definitionId());
        if (direction.isEmpty()) {
            return 0;
        }
        int networkId = ItemNetworkKinds.networkId(cover);
        if (!ItemNetworkKinds.isJoined(networkId)) {
            return 0;
        }
        IItemHandler adjacent = inventoryAt(level, pipePos, side);
        if (adjacent == null) {
            return 0;
        }
        List<StorageEndpoint> storages = discoverStorage(
                level, pipePos, networkId).endpoints();
        if (storages.isEmpty()) {
            return 0;
        }
        CoverDefinition.Values values = definition.resolve(cover.config());
        int amount = values.rate() > 0
                ? values.rate()
                : ItemNetworkLimits.DEFAULT_RATE;
        Predicate<ItemStack> match = stack -> matches(
                cover.config().matchId(), stack);
        return switch (direction.orElseThrow()) {
            case EXPORT -> exportItems(
                    level, adjacent, storages, amount, match);
            case IMPORT -> importItems(
                    level, adjacent, storages, amount, match);
        };
    }

    public static Discovery discoverStorage(
            Level level, BlockPos start, int networkId) {
        ArrayList<StorageEndpoint> endpoints = new ArrayList<>();
        if (level == null
                || level.isClientSide
                || start == null
                || !ItemNetworkKinds.isJoined(networkId)
                || !level.hasChunkAt(start)) {
            return Discovery.empty();
        }
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(start.immutable());
        while (!open.isEmpty()
                && visited.size() < ItemNetworkLimits.MAX_VISITED_PIPES
                && endpoints.size()
                        < ItemNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT) {
            BlockPos pos = open.remove();
            if (!visited.add(pos)) {
                continue;
            }
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof ItemPipeBlock)
                    || !(level.getBlockEntity(pos)
                            instanceof ItemPipeBlockEntity pipe)
                    || !pipe.offersNetworkDiscovery()) {
                continue;
            }
            collectStorage(level, pipe, pos, networkId, endpoints);
            for (Direction direction : DIRECTIONS) {
                if (!AbstractPipeBlock.isConnected(state, direction)) {
                    continue;
                }
                BlockPos next = pos.relative(direction);
                if (visited.contains(next) || !level.hasChunkAt(next)) {
                    continue;
                }
                if (level.getBlockState(next).getBlock()
                        instanceof ItemPipeBlock) {
                    open.add(next.immutable());
                }
            }
        }
        return new Discovery(List.copyOf(endpoints), visited.size());
    }

    public static boolean isVisibleStorage(
            Level level,
            BlockPos start,
            int networkId,
            BlockPos storagePipe,
            Direction storageSide) {
        for (StorageEndpoint endpoint : discoverStorage(
                level, start, networkId).endpoints()) {
            if (endpoint.pipe().equals(storagePipe)
                    && endpoint.side() == storageSide) {
                return true;
            }
        }
        return false;
    }

    private static void collectStorage(
            Level level,
            ItemPipeBlockEntity pipe,
            BlockPos pipePos,
            int networkId,
            List<StorageEndpoint> endpoints) {
        for (var entry : pipe.coverSnapshot().entrySet()) {
            PipeCover cover = entry.getValue();
            if (cover == null
                    || !ItemNetworkKinds.isStorage(cover.definitionId())
                    || ItemNetworkKinds.networkId(cover) != networkId) {
                continue;
            }
            Direction side = entry.getKey();
            IItemHandler inventory = inventoryAt(level, pipePos, side);
            if (inventory == null) {
                continue;
            }
            endpoints.add(new StorageEndpoint(
                    pipePos.immutable(),
                    side,
                    pipePos.relative(side).immutable()));
            if (endpoints.size()
                    >= ItemNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT) {
                return;
            }
        }
    }

    private static IItemHandler inventoryAt(
            Level level, BlockPos pipePos, Direction side) {
        BlockPos target = pipePos.relative(side);
        if (!level.hasChunkAt(target)) {
            return null;
        }
        try {
            return level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    target,
                    side.getOpposite());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static IItemHandler inventoryOf(
            Level level, StorageEndpoint endpoint) {
        return inventoryAt(level, endpoint.pipe(), endpoint.side());
    }

    private static int exportItems(
            Level level,
            IItemHandler source,
            List<StorageEndpoint> storages,
            int amount,
            Predicate<ItemStack> match) {
        ItemStack extracted = extractMatching(source, amount, match, true);
        if (extracted.isEmpty()) {
            return 0;
        }
        int insertable = 0;
        for (StorageEndpoint endpoint : storages) {
            IItemHandler dest = inventoryOf(level, endpoint);
            if (dest == null) {
                continue;
            }
            ItemStack leftover = insertAll(dest, extracted, true);
            insertable += extracted.getCount() - leftover.getCount();
            if (insertable >= extracted.getCount()) {
                break;
            }
        }
        if (insertable <= 0) {
            return 0;
        }
        int take = Math.min(extracted.getCount(), insertable);
        ItemStack moved = extractMatching(source, take, match, false);
        if (moved.isEmpty()) {
            return 0;
        }
        ItemStack remaining = moved.copy();
        for (StorageEndpoint endpoint : storages) {
            if (remaining.isEmpty()) {
                break;
            }
            IItemHandler dest = inventoryOf(level, endpoint);
            if (dest == null) {
                continue;
            }
            remaining = insertAll(dest, remaining, false);
        }
        if (!remaining.isEmpty()) {
            insertAll(source, remaining, false);
        }
        return moved.getCount() - remaining.getCount();
    }

    private static int importItems(
            Level level,
            IItemHandler dest,
            List<StorageEndpoint> storages,
            int amount,
            Predicate<ItemStack> match) {
        int remainingNeed = amount;
        int moved = 0;
        for (StorageEndpoint endpoint : storages) {
            if (remainingNeed <= 0) {
                break;
            }
            IItemHandler source = inventoryOf(level, endpoint);
            if (source == null) {
                continue;
            }
            ItemStack extracted = extractMatching(
                    source, remainingNeed, match, true);
            if (extracted.isEmpty()) {
                continue;
            }
            ItemStack leftover = insertAll(dest, extracted, true);
            int insertable = extracted.getCount() - leftover.getCount();
            if (insertable <= 0) {
                continue;
            }
            ItemStack taken = extractMatching(
                    source, insertable, match, false);
            if (taken.isEmpty()) {
                continue;
            }
            ItemStack after = insertAll(dest, taken, false);
            if (!after.isEmpty()) {
                insertAll(source, after, false);
            }
            int accepted = taken.getCount() - after.getCount();
            moved += accepted;
            remainingNeed -= accepted;
        }
        return moved;
    }

    private static boolean matches(Optional<String> expected, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (expected.isEmpty()) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return expected.orElseThrow().equals(key.toString());
    }

    private static ItemStack extractMatching(
            IItemHandler handler,
            int amount,
            Predicate<ItemStack> match,
            boolean simulate) {
        if (handler == null || amount <= 0) {
            return ItemStack.EMPTY;
        }
        ItemStack taken = ItemStack.EMPTY;
        for (int slot = 0;
                slot < handler.getSlots() && taken.getCount() < amount;
                slot++) {
            ItemStack slotStack = handler.getStackInSlot(slot);
            if (slotStack.isEmpty() || !match.test(slotStack)) {
                continue;
            }
            if (!taken.isEmpty()
                    && !ItemStack.isSameItemSameComponents(taken, slotStack)) {
                continue;
            }
            ItemStack extracted = handler.extractItem(
                    slot, amount - taken.getCount(), simulate);
            if (extracted.isEmpty()) {
                continue;
            }
            if (taken.isEmpty()) {
                taken = extracted.copy();
            } else {
                taken.grow(extracted.getCount());
            }
        }
        return taken;
    }

    private static ItemStack insertAll(
            IItemHandler handler, ItemStack stack, boolean simulate) {
        if (handler == null || stack.isEmpty()) {
            return stack;
        }
        ItemStack remaining = stack.copy();
        for (int slot = 0;
                slot < handler.getSlots() && !remaining.isEmpty();
                slot++) {
            remaining = handler.insertItem(slot, remaining, simulate);
        }
        return remaining;
    }

    public record StorageEndpoint(
            BlockPos pipe, Direction side, BlockPos inventory) {}

    public record Discovery(List<StorageEndpoint> endpoints, int visits) {
        public Discovery {
            endpoints = List.copyOf(endpoints);
            if (visits < 0) {
                throw new IllegalArgumentException("visits");
            }
        }

        public static Discovery empty() {
            return new Discovery(List.of(), 0);
        }
    }
}
