package com.masson.cruciblecraft.content.blockentity;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.logistics.pipe.PipeTopology;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferDiagnostics;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferPhase;
import com.masson.cruciblecraft.logistics.itemnet.ItemNetworkKinds;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverSet;
import com.masson.cruciblecraft.logistics.pipe.item
        .ItemPipeNetworkTraversal;
import com.masson.cruciblecraft.logistics.pipe.item
        .ItemPipeNetworkTraversal.Route;
import com.masson.cruciblecraft.logistics.pipe.item
        .ItemPipeTransferPlan;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Cached-route item pipe with source-first active pump covers. */
public final class ItemPipeBlockEntity extends BlockEntity {
    public static final int TRANSFER_INTERVAL =
            PipeTransferPhase.INTERVAL;

    private final PipeCoverSet covers = new PipeCoverSet();
    private final EnumMap<Direction, IItemHandler> sidedHandlers =
            new EnumMap<>(Direction.class);
    private final ItemPipeRouteCache routeCache =
            new ItemPipeRouteCache();
    private long cachedTopologyVersion = Long.MIN_VALUE;
    private long windowStart = Long.MIN_VALUE;
    private int consumedThisWindow;
    private int deliveredThisWindow;
    private long totalDelivered;
    private long clogEvents;
    private int nextRoute;
    private int recoveredInvalidCoverRows;
    private boolean recoveryWarningLogged;
    private ItemStack recoveryBuffer = ItemStack.EMPTY;
    private Direction recoveryIngress;
    private boolean networkChunkLoaded = true;

    public ItemPipeBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ITEM_PIPE.get(), pos, state);
        for (Direction side : Direction.values()) {
            sidedHandlers.put(side, new SidedHandler(side));
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        networkChunkLoaded = true;
        if (!recoveryWarningLogged
                && recoveredInvalidCoverRows > 0
                && level != null
                && !level.isClientSide) {
            recoveryWarningLogged = true;
            CrucibleCraft.LOGGER.warn(
                    "Recovered {} invalid item-pipe cover row(s) at {} {}; "
                            + "invalid faces were quarantined fail-closed",
                    recoveredInvalidCoverRows,
                    level.dimension().location(),
                    worldPosition);
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ItemPipeBlockEntity pipe) {
        pipe.rollWindow(level.getGameTime());
        if (PipeTransferPhase.isDue(level.getGameTime(), pos)) {
            pipe.tickCovers(level);
        }
    }

    public IItemHandler itemHandler(Direction side) {
        return side == null ? null : sidedHandlers.get(side);
    }

    public boolean acceptsIncoming(
            Direction side, ItemStack stack) {
        return covers.allowsIncoming(
                        side,
                        CoverDefinition.Medium.ITEM,
                        0,
                        0)
                && covers.matches(side, stack);
    }

    public boolean allowsOutgoing(
            Direction side, ItemStack stack) {
        return covers.allowsOutgoing(
                        side,
                        CoverDefinition.Medium.ITEM,
                        0,
                        0)
                && covers.matches(side, stack);
    }

    public int availableItems() {
        rollWindow(level == null ? 0L : level.getGameTime());
        return Math.max(0, itemLimit() - consumedThisWindow);
    }

    public int deliveredThisWindow() {
        rollWindow(level == null ? 0L : level.getGameTime());
        return deliveredThisWindow;
    }

    public long clogEvents() {
        return clogEvents;
    }

    public long totalDelivered() {
        return totalDelivered;
    }

    public Map<Direction, PipeCover> coverSnapshot() {
        return covers.snapshot();
    }

    public ItemStack recoveryBuffer() {
        return recoveryBuffer.copy();
    }

    public void setRecoveryForTest(ItemStack stack, Direction side) {
        recoveryBuffer = stack.copy();
        recoveryIngress = side;
        setChanged();
    }

    public boolean ejectRecovery() {
        if (recoveryBuffer.isEmpty()) {
            return false;
        }
        ItemStack ejected = recoveryBuffer.copy();
        recoveryBuffer = ItemStack.EMPTY;
        recoveryIngress = null;
        setChanged();
        if (level != null && !level.isClientSide) {
            Block.popResource(level, worldPosition, ejected);
        }
        return true;
    }

    public int routeCacheEntries() {
        return routeCache.size();
    }

    public String coverSummary() {
        return covers.boundedSummary();
    }

    @Override
    public void onChunkUnloaded() {
        networkChunkLoaded = false;
        super.onChunkUnloaded();
    }

    public boolean offersNetworkDiscovery() {
        return networkChunkLoaded && !isRemoved();
    }

    public void dropCovers() {
        if (level == null || level.isClientSide) {
            return;
        }
        for (ItemStack stack : covers.removeAllAsItems()) {
            Block.popResource(level, worldPosition, stack);
        }
        invalidateRoutes();
        setChanged();
        if (!level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
        }
    }

    public boolean removeCover(Direction side, net.minecraft.world.entity.player.Player player) {
        var taken = covers.take(side);
        if (taken.isEmpty()) {
            return false;
        }
        ItemStack stack = com.masson.cruciblecraft.logistics.pipe.cover
                .PipeCoverItems.stackFor(taken.orElseThrow());
        if (!stack.isEmpty()) {
            if (player == null || !player.addItem(stack)) {
                if (level != null && !level.isClientSide) {
                    Block.popResource(level, worldPosition, stack);
                }
            }
        }
        invalidateRoutes();
        setChanged();
        if (level != null && !level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
            notifyCoverRedstone(side);
        }
        return true;
    }

    public boolean setCover(Direction side, PipeCover cover) {
        if (cover != null
                && !cover.supports(CoverDefinition.Medium.ITEM)) {
            return false;
        }
        if (!covers.set(side, cover)) {
            return false;
        }
        invalidateRoutes();
        setChanged();
        if (level != null && !level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
            notifyCoverRedstone(side);
        }
        return true;
    }

    public boolean replaceCoverQuiet(Direction side, PipeCover cover) {
        if (cover != null
                && !cover.supports(CoverDefinition.Medium.ITEM)) {
            return false;
        }
        if (!covers.set(side, cover)) {
            return false;
        }
        setChanged();
        if (level != null && !level.isClientSide) {
            syncToClient();
            notifyCoverRedstone(side);
        }
        return true;
    }

    public boolean configureCover(
            Direction side,
            CoverDefinition.ConfigField field,
            int value) {
        if (!covers.configure(side, field, value)) {
            return false;
        }
        invalidateRoutes();
        setChanged();
        if (level != null && !level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
        }
        return true;
    }

    public boolean toggleCoverInvert(Direction side) {
        if (!covers.toggleInvert(side)) {
            return false;
        }
        invalidateRoutes();
        setChanged();
        if (level != null && !level.isClientSide) {
            PipeTopology.invalidate(level, worldPosition);
            syncToClient();
            notifyCoverRedstone(side);
        }
        return true;
    }

    private void notifyCoverRedstone(Direction side) {
        if (level == null || level.isClientSide) {
            return;
        }
        net.minecraft.world.level.block.Block block =
                getBlockState().getBlock();
        level.updateNeighborsAt(worldPosition, block);
        if (side != null) {
            level.updateNeighborsAt(worldPosition.relative(side), block);
        }
    }

    public void recordTransferred(int consumed, int delivered) {
        if (consumed < 0 || delivered < 0 || delivered > consumed) {
            throw new IllegalArgumentException(
                    "Invalid item pipe transfer accounting");
        }
        rollWindow(level == null ? 0L : level.getGameTime());
        consumedThisWindow = Math.min(
                Integer.MAX_VALUE, consumedThisWindow + consumed);
        deliveredThisWindow = Math.min(
                Integer.MAX_VALUE, deliveredThisWindow + delivered);
        totalDelivered = Math.addExact(totalDelivered, delivered);
        if (consumed > 0) {
            setChanged();
        }
    }

    private void tickCovers(Level level) {
        flushRecoveryBuffer();
        if (!recoveryBuffer.isEmpty()) {
            return;
        }
        for (Direction side : Direction.values()) {
            if (!AbstractPipeBlock.isConnected(liveState(), side)) {
                continue;
            }
            Optional<PipeCover> cover = covers.get(side);
            if (cover.isEmpty()) {
                continue;
            }
            boolean logistics = ItemNetworkKinds.isLogistics(
                    cover.orElseThrow().definitionId());
            boolean retriever = cover.orElseThrow().definition()
                    .map(CoverDefinition::behaviorId)
                    .map(net.minecraft.resources.ResourceLocation::getPath)
                    .filter("retriever_item"::equals)
                    .isPresent();
            if (!logistics && !retriever && availableItems() <= 0) {
                continue;
            }
            covers.tick(side, new ItemCoverContext(level, side));
        }
    }

    private int pumpOne(
            IItemHandler source,
            Direction side,
            int limit,
            Optional<String> matchId,
            CoverDefinition.TransferMode mode) {
        try {
            int requested = Math.min(availableItems(), limit);
            for (int slot = 0;
                    slot < source.getSlots() && requested > 0;
                    slot++) {
                ItemStack simulated = source.extractItem(
                        slot, requested, true);
                if (simulated.isEmpty()
                        || !matches(matchId, simulated)
                        || !covers.matches(side, simulated)
                        || (mode == CoverDefinition.TransferMode.EXACT
                                && simulated.getCount() != requested)) {
                    continue;
                }
                ItemStack simulatedRemainder = insert(
                        side, simulated, true);
                int accepted = simulated.getCount()
                        - simulatedRemainder.getCount();
                if (accepted <= 0
                        || (mode == CoverDefinition.TransferMode.EXACT
                                && accepted != requested)) {
                    continue;
                }
                ItemStack extracted = source.extractItem(
                        slot, accepted, false);
                if (extracted.isEmpty()
                        || extracted.getCount() > accepted
                        || !ItemStack.isSameItemSameComponents(
                                extracted, simulated)) {
                    PipeTransferDiagnostics.warnOnce(
                            "item pump execution",
                            source,
                            "Item pump source violated simulated extraction");
                    return 0;
                }
                ItemStack remainder = insert(side, extracted, false);
                if (!remainder.isEmpty()) {
                    ItemStack unrecovered = returnToSource(
                            source, slot, remainder);
                    if (!unrecovered.isEmpty()) {
                        holdForRetry(side, unrecovered);
                    }
                    return extracted.getCount() - unrecovered.getCount();
                }
                return extracted.getCount();
            }
        } catch (RuntimeException failure) {
            PipeTransferDiagnostics.warnOnce(
                    "item pump", source, "Item pump transfer failed", failure);
        }
        return 0;
    }

    private void flushRecoveryBuffer() {
        if (recoveryBuffer.isEmpty() || recoveryIngress == null) {
            return;
        }
        recoveryBuffer = insert(
                recoveryIngress, recoveryBuffer, false).copy();
        if (recoveryBuffer.isEmpty()) {
            recoveryIngress = null;
        }
        setChanged();
    }

    private void holdForRetry(Direction side, ItemStack stack) {
        if (!recoveryBuffer.isEmpty()) {
            throw new IllegalStateException(
                    "Item pipe recovery buffer was not drained");
        }
        recoveryBuffer = stack.copy();
        recoveryIngress = side;
        clogEvents = Math.addExact(clogEvents, stack.getCount());
        setChanged();
    }

    private static boolean matches(
            Optional<String> expected, ItemStack stack) {
        return expected.isEmpty()
                || expected.orElseThrow().equals(
                        net.minecraft.core.registries.BuiltInRegistries.ITEM
                                .getKey(stack.getItem()).toString());
    }

    private ItemStack returnToSource(
            IItemHandler source, int slot, ItemStack remainder) {
        try {
            ItemStack returned = source.insertItem(
                    slot, remainder, false);
            if (returned == null
                    || returned.getCount() < 0
                    || returned.getCount() > remainder.getCount()
                    || (!returned.isEmpty()
                            && !ItemStack.isSameItemSameComponents(
                                    remainder, returned))) {
                PipeTransferDiagnostics.warnOnce(
                        "item pump recovery",
                        source,
                        "Item source returned invalid recovery remainder");
                return remainder;
            }
            return returned;
        } catch (RuntimeException failure) {
            PipeTransferDiagnostics.warnOnce(
                    "item pump recovery",
                    source,
                    "Item source rejected route-failure recovery",
                    failure);
            return remainder;
        }
    }

    private ItemStack insert(
            Direction ingress, ItemStack stack, boolean simulate) {
        if (stack.isEmpty()
                || !AbstractPipeBlock.isConnected(
                        liveState(), ingress)
                || !acceptsIncoming(ingress, stack)
                || level == null
                || level.isClientSide
                || availableItems() <= 0) {
            return stack;
        }
        List<Route> routes = routes(ingress, stack);
        ItemPipeTransferPlan plan = ItemPipeTransferPlan.plan(
                level, stack, routes, availableItems());
        if (simulate) {
            return stack.copyWithCount(stack.getCount() - plan.accepted());
        }
        ItemPipeTransferPlan.Execution execution = plan.execute(level);
        int consumed = Math.min(stack.getCount(), execution.consumed());
        if (execution.delivered() > 0 && !routes.isEmpty()) {
            nextRoute = Math.floorMod(nextRoute + 1, routes.size());
            setChanged();
        }
        if (consumed < plan.accepted()) {
            clogEvents += plan.accepted() - consumed;
            setChanged();
        }
        return consumed == stack.getCount()
                ? ItemStack.EMPTY
                : stack.copyWithCount(stack.getCount() - consumed);
    }

    private List<Route> routes(
            Direction ingress, ItemStack stack) {
        long version = PipeTopology.version(level, worldPosition);
        if (version != cachedTopologyVersion) {
            routeCache.clear();
            cachedTopologyVersion = version;
        }
        List<Route> cached = routeCache.getOrDiscover(
                ingress,
                stack.getItem(),
                () -> ItemPipeNetworkTraversal.discover(
                        level, worldPosition, ingress, stack));
        if (cached.size() < 2) {
            return cached;
        }
        long bestCost = cached.getFirst().cost();
        int equalPriority = 1;
        while (equalPriority < cached.size()
                && cached.get(equalPriority).cost() == bestCost) {
            equalPriority++;
        }
        int offset = Math.floorMod(nextRoute, equalPriority);
        if (offset == 0) {
            return cached;
        }
        java.util.ArrayList<Route> rotated =
                new java.util.ArrayList<>(cached.size());
        rotated.addAll(cached.subList(offset, equalPriority));
        rotated.addAll(cached.subList(0, offset));
        rotated.addAll(cached.subList(equalPriority, cached.size()));
        return List.copyOf(rotated);
    }

    private void invalidateRoutes() {
        routeCache.clear();
        cachedTopologyVersion = Long.MIN_VALUE;
    }

    private int itemLimit() {
        long value = (long) pipe().pipe().item().stacksPerSecond() * 64L;
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, value));
    }

    private void rollWindow(long tick) {
        long start = tick - Math.floorMod(tick, 20L);
        if (windowStart != start) {
            windowStart = start;
            consumedThisWindow = 0;
            deliveredThisWindow = 0;
        }
    }

    private ItemPipeBlock pipe() {
        if (liveState().getBlock() instanceof ItemPipeBlock pipe) {
            return pipe;
        }
        throw new IllegalStateException(
                "Item pipe block entity has non-pipe state");
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        covers.save(tag, registries);
        tag.putLong("window_start", windowStart);
        tag.putInt("consumed_window", consumedThisWindow);
        tag.putInt("delivered_window", deliveredThisWindow);
        tag.putLong("total_delivered", totalDelivered);
        tag.putLong("clog_events", clogEvents);
        tag.putInt("next_route", nextRoute);
        if (!recoveryBuffer.isEmpty() && recoveryIngress != null) {
            tag.put("cover_recovery_item", recoveryBuffer.save(registries));
            tag.putString(
                    "cover_recovery_ingress", recoveryIngress.getName());
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        recoveredInvalidCoverRows = covers.load(tag, registries);
        windowStart = tag.getLong("window_start");
        consumedThisWindow = tag.getInt("consumed_window");
        deliveredThisWindow = tag.getInt("delivered_window");
        totalDelivered = tag.getLong("total_delivered");
        clogEvents = tag.getLong("clog_events");
        nextRoute = Math.max(0, tag.getInt("next_route"));
        recoveryBuffer = tag.contains("cover_recovery_item")
                ? ItemStack.parseOptional(
                        registries,
                        tag.getCompound("cover_recovery_item"))
                : ItemStack.EMPTY;
        recoveryIngress = Direction.byName(
                tag.getString("cover_recovery_ingress"));
        if (recoveryBuffer.isEmpty() || recoveryIngress == null) {
            recoveryBuffer = ItemStack.EMPTY;
            recoveryIngress = null;
        }
        invalidateRoutes();
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        covers.save(tag, registries);
        tag.putInt("consumed_window", consumedThisWindow);
        tag.putInt("delivered_window", deliveredThisWindow);
        tag.putLong("total_delivered", totalDelivered);
        tag.putLong("clog_events", clogEvents);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        covers.load(tag, registries);
        consumedThisWindow = tag.getInt("consumed_window");
        deliveredThisWindow = tag.getInt("delivered_window");
        totalDelivered = tag.getLong("total_delivered");
        clogEvents = tag.getLong("clog_events");
        invalidateRoutes();
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag(), registries);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            BlockState state = liveState();
            level.sendBlockUpdated(
                    worldPosition,
                    state,
                    state,
                    Block.UPDATE_CLIENTS);
        }
    }

    private BlockState liveState() {
        return level == null
                ? getBlockState()
                : level.getBlockState(worldPosition);
    }

    private final class ItemCoverContext
            implements CoverBehavior.TransferContext {
        private final Level world;
        private final Direction side;

        private ItemCoverContext(Level world, Direction side) {
            this.world = world;
            this.side = side;
        }

        @Override
        public CoverDefinition.Medium medium() {
            return CoverDefinition.Medium.ITEM;
        }

        @Override
        public Direction side() {
            return side;
        }

        @Override
        public int storedAmount() {
            return recoveryBuffer.getCount();
        }

        @Override
        public int capacity() {
            return itemLimit();
        }

        @Override
        public int transferItems(
                int amount,
                Optional<String> matchId,
                CoverDefinition.TransferMode mode) {
            if (amount <= 0 || !recoveryBuffer.isEmpty()) {
                return 0;
            }
            BlockPos sourcePos = worldPosition.relative(side);
            if (!world.hasChunkAt(sourcePos)) {
                return 0;
            }
            IItemHandler source;
            try {
                source = world.getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        sourcePos,
                        side.getOpposite());
            } catch (RuntimeException failure) {
                PipeTransferDiagnostics.warnOnce(
                        "item cover discovery",
                        null,
                        "Item cover discovery failed at " + sourcePos,
                        failure);
                return 0;
            }
            return source == null
                    ? 0
                    : pumpOne(source, side, amount, matchId, mode);
        }

        @Override
        public int transferFluids(
                int amount,
                Optional<String> matchId,
                CoverDefinition.TransferMode mode) {
            return 0;
        }

        @Override
        public Level world() {
            return world;
        }

        @Override
        public BlockPos hostPos() {
            return worldPosition;
        }
    }

    private final class SidedHandler implements IItemHandler {
        private final Direction side;

        private SidedHandler(Direction side) {
            this.side = side;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(
                int slot, ItemStack stack, boolean simulate) {
            return insert(side, stack, simulate);
        }

        @Override
        public ItemStack extractItem(
                int slot, int amount, boolean simulate) {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot) {
            return itemLimit();
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return acceptsIncoming(side, stack);
        }
    }
}
