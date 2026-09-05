package com.masson.cruciblecraft.logistics.genericnet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * Generic cover network: identity ∩ loaded host-pipe component.
 * Host is the item-pipe graph or the fluid-pipe graph, never a mix.
 * DESIGN_POLICY: not a world broadcast and not adjacent conveyor/pump transfer.
 */
public final class GenericLogisticsNetwork {
    private static final Direction[] DIRECTIONS = Direction.values();

    private GenericLogisticsNetwork() {}

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
        Optional<GenericNetworkKinds.TransferDirection> direction =
                GenericNetworkKinds.direction(cover.definitionId());
        if (direction.isEmpty()) {
            return 0;
        }
        int networkId = GenericNetworkKinds.networkId(cover);
        if (!GenericNetworkKinds.isJoined(networkId)) {
            return 0;
        }
        List<StorageEndpoint> storages = discoverStorage(
                level, pipePos, networkId).endpoints();
        if (storages.isEmpty()) {
            return 0;
        }
        CoverDefinition.Values values = definition.resolve(cover.config());
        int itemAmount = values.rate() > 0
                ? values.rate()
                : GenericNetworkLimits.DEFAULT_ITEM_RATE;
        int fluidAmount = GenericNetworkLimits.DEFAULT_FLUID_RATE;
        Predicate<ItemStack> itemMatch = stack -> matchesItem(
                cover.config().matchId(), stack);
        Predicate<FluidStack> fluidMatch = stack -> matchesFluid(
                cover.config().matchId(), stack);
        IItemHandler adjacentItems = inventoryAt(level, pipePos, side);
        IFluidHandler adjacentFluids = tankAt(level, pipePos, side);
        return switch (direction.orElseThrow()) {
            case EXPORT -> exportItems(
                    level, adjacentItems, storages, itemAmount, itemMatch)
                    + exportFluids(
                            level, adjacentFluids, storages, fluidAmount, fluidMatch);
            case IMPORT -> importItems(
                    level, adjacentItems, storages, itemAmount, itemMatch)
                    + importFluids(
                            level, adjacentFluids, storages, fluidAmount, fluidMatch);
        };
    }

    public static Discovery discoverStorage(
            Level level, BlockPos start, int networkId) {
        ArrayList<StorageEndpoint> endpoints = new ArrayList<>();
        if (level == null
                || level.isClientSide
                || start == null
                || !GenericNetworkKinds.isJoined(networkId)
                || !level.hasChunkAt(start)) {
            return Discovery.empty();
        }
        PipeKind kind = pipeKind(level.getBlockState(start));
        if (kind == null) {
            return Discovery.empty();
        }
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(start.immutable());
        while (!open.isEmpty()
                && visited.size() < GenericNetworkLimits.MAX_VISITED_PIPES
                && endpoints.size()
                        < GenericNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT) {
            BlockPos pos = open.remove();
            if (!visited.add(pos)) {
                continue;
            }
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (pipeKind(state) != kind
                    || !offersDiscovery(level, pos, kind)) {
                continue;
            }
            collectStorage(level, pos, networkId, endpoints);
            for (Direction direction : DIRECTIONS) {
                if (!AbstractPipeBlock.isConnected(state, direction)) {
                    continue;
                }
                BlockPos next = pos.relative(direction);
                if (visited.contains(next) || !level.hasChunkAt(next)) {
                    continue;
                }
                if (pipeKind(level.getBlockState(next)) == kind) {
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
            BlockPos pipePos,
            int networkId,
            List<StorageEndpoint> endpoints) {
        BlockEntity be = level.getBlockEntity(pipePos);
        Iterable<java.util.Map.Entry<Direction, PipeCover>> covers;
        if (be instanceof ItemPipeBlockEntity itemPipe) {
            covers = itemPipe.coverSnapshot().entrySet();
        } else if (be instanceof FluidPipeBlockEntity fluidPipe) {
            covers = fluidPipe.coverSnapshot().entrySet();
        } else {
            return;
        }
        for (var entry : covers) {
            PipeCover cover = entry.getValue();
            if (cover == null
                    || !GenericNetworkKinds.isStorage(cover.definitionId())
                    || GenericNetworkKinds.networkId(cover) != networkId) {
                continue;
            }
            Direction side = entry.getKey();
            boolean items = inventoryAt(level, pipePos, side) != null;
            boolean fluids = tankAt(level, pipePos, side) != null;
            if (!items && !fluids) {
                continue;
            }
            endpoints.add(new StorageEndpoint(
                    pipePos.immutable(),
                    side,
                    pipePos.relative(side).immutable(),
                    items,
                    fluids));
            if (endpoints.size()
                    >= GenericNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT) {
                return;
            }
        }
    }

    private static PipeKind pipeKind(BlockState state) {
        if (state.getBlock() instanceof ItemPipeBlock) {
            return PipeKind.ITEM;
        }
        if (state.getBlock() instanceof FluidPipeBlock) {
            return PipeKind.FLUID;
        }
        return null;
    }

    private static boolean offersDiscovery(
            Level level, BlockPos pos, PipeKind kind) {
        BlockEntity be = level.getBlockEntity(pos);
        return switch (kind) {
            case ITEM -> be instanceof ItemPipeBlockEntity item
                    && item.offersNetworkDiscovery();
            case FLUID -> be instanceof FluidPipeBlockEntity fluid
                    && fluid.offersNetworkDiscovery();
        };
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

    private static IFluidHandler tankAt(
            Level level, BlockPos pipePos, Direction side) {
        BlockPos target = pipePos.relative(side);
        if (!level.hasChunkAt(target)) {
            return null;
        }
        try {
            return level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
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

    private static IFluidHandler tankOf(
            Level level, StorageEndpoint endpoint) {
        return tankAt(level, endpoint.pipe(), endpoint.side());
    }

    private static int exportItems(
            Level level,
            IItemHandler source,
            List<StorageEndpoint> storages,
            int amount,
            Predicate<ItemStack> match) {
        if (source == null) {
            return 0;
        }
        ItemStack extracted = extractMatching(source, amount, match, true);
        if (extracted.isEmpty()) {
            return 0;
        }
        int insertable = 0;
        for (StorageEndpoint endpoint : storages) {
            if (!endpoint.items()) {
                continue;
            }
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
            if (!endpoint.items()) {
                continue;
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
        if (dest == null) {
            return 0;
        }
        int remainingNeed = amount;
        int moved = 0;
        for (StorageEndpoint endpoint : storages) {
            if (remainingNeed <= 0) {
                break;
            }
            if (!endpoint.items()) {
                continue;
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

    private static int exportFluids(
            Level level,
            IFluidHandler source,
            List<StorageEndpoint> storages,
            int amount,
            Predicate<FluidStack> match) {
        if (source == null) {
            return 0;
        }
        FluidStack simulated = source.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty() || !match.test(simulated)) {
            return 0;
        }
        int insertable = 0;
        for (StorageEndpoint endpoint : storages) {
            if (!endpoint.fluids()) {
                continue;
            }
            IFluidHandler dest = tankOf(level, endpoint);
            if (dest == null) {
                continue;
            }
            insertable += dest.fill(
                    simulated.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (insertable >= simulated.getAmount()) {
                break;
            }
        }
        if (insertable <= 0) {
            return 0;
        }
        int take = Math.min(simulated.getAmount(), insertable);
        FluidStack drained = source.drain(take, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }
        FluidStack remaining = drained.copy();
        for (StorageEndpoint endpoint : storages) {
            if (remaining.isEmpty()) {
                break;
            }
            if (!endpoint.fluids()) {
                continue;
            }
            IFluidHandler dest = tankOf(level, endpoint);
            if (dest == null) {
                continue;
            }
            int filled = dest.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                remaining = remaining.copyWithAmount(
                        remaining.getAmount() - filled);
            }
        }
        if (!remaining.isEmpty()) {
            source.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
        }
        return drained.getAmount() - remaining.getAmount();
    }

    private static int importFluids(
            Level level,
            IFluidHandler dest,
            List<StorageEndpoint> storages,
            int amount,
            Predicate<FluidStack> match) {
        if (dest == null) {
            return 0;
        }
        int remainingNeed = amount;
        int moved = 0;
        for (StorageEndpoint endpoint : storages) {
            if (remainingNeed <= 0) {
                break;
            }
            if (!endpoint.fluids()) {
                continue;
            }
            IFluidHandler source = tankOf(level, endpoint);
            if (source == null) {
                continue;
            }
            FluidStack simulated = source.drain(
                    remainingNeed, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty() || !match.test(simulated)) {
                continue;
            }
            int insertable = dest.fill(
                    simulated.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (insertable <= 0) {
                continue;
            }
            FluidStack taken = source.drain(
                    insertable, IFluidHandler.FluidAction.EXECUTE);
            if (taken.isEmpty()) {
                continue;
            }
            int accepted = dest.fill(taken, IFluidHandler.FluidAction.EXECUTE);
            if (accepted < taken.getAmount()) {
                FluidStack leftover = taken.copyWithAmount(
                        taken.getAmount() - accepted);
                source.fill(leftover, IFluidHandler.FluidAction.EXECUTE);
            }
            moved += accepted;
            remainingNeed -= accepted;
        }
        return moved;
    }

    private static boolean matchesItem(Optional<String> expected, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (expected.isEmpty()) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.ITEM.getKey(stack.getItem());
        return expected.orElseThrow().equals(key.toString());
    }

    private static boolean matchesFluid(Optional<String> expected, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (expected.isEmpty()) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(stack.getFluid());
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

    private enum PipeKind {
        ITEM,
        FLUID
    }

    public record StorageEndpoint(
            BlockPos pipe,
            Direction side,
            BlockPos adjacent,
            boolean items,
            boolean fluids) {}

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
