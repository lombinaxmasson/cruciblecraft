package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.energy.EnergyTransferDiagnostics;
import com.masson.cruciblecraft.energy.cable.CableLoadState;
import com.masson.cruciblecraft.energy.cable.CableNetworkTraversal;
import com.masson.cruciblecraft.energy.cable.CableTransferPlan;
import com.masson.cruciblecraft.energy.cable.ElectricalShock;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/** Transient cable endpoint; only load/burn observability is persisted. */
public final class CableBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private static final int SYNC_INTERVAL = 20;

    private final CableLoadState load = new CableLoadState();
    private final CheckpointTracker checkpoint = new CheckpointTracker();
    private long cachedPlanTick = Long.MIN_VALUE;
    private PlanKey cachedPlanKey;
    private CableTransferPlan cachedPlan;

    public CableBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CABLE.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CableBlockEntity cable) {
        long tick = level.getGameTime();
        if (cable.load.shouldBurn(tick)) {
            level.playSound(
                    null,
                    pos,
                    SoundEvents.FIRECHARGE_USE,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F);
            level.gameEvent(
                    GameEvent.BLOCK_DESTROY,
                    pos,
                    GameEvent.Context.of(state));
            level.setBlock(
                    pos,
                    Blocks.FIRE.defaultBlockState(),
                    Block.UPDATE_ALL);
            return;
        }
        CableLoadState.Change change = cable.load.advance(tick);
        if (change.persistenceChanged()) {
            cable.setChanged();
        }
        if (change.observableChanged()) {
            cable.checkpoint.markSyncPending();
        }
        long phase = CheckpointDecisions.phaseKey(
                pos.getX(), pos.getY(), pos.getZ());
        if (cable.checkpoint.shouldSync(
                false, tick, phase, SYNC_INTERVAL)) {
            cable.syncToClient();
            cable.checkpoint.synced();
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC
                && side != null
                && getBlockState().getBlock() instanceof CableBlock
                && CableBlock.isConnected(getBlockState(), side);
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || size == 0L
                || amount <= 0L
                || level == null
                || level.isClientSide) {
            return 0L;
        }
        try {
            long tick = level.getGameTime();
            PlanKey key = new PlanKey(side, size, amount);
            if (cachedPlanTick != tick) {
                clearPlanCache();
                cachedPlanTick = tick;
            }
            CableTransferPlan plan;
            if (key.equals(cachedPlanKey) && cachedPlan != null) {
                plan = cachedPlan;
            } else {
                plan = CableNetworkTraversal.plan(
                        level, worldPosition, side, size, amount);
                cachedPlanKey = key;
                cachedPlan = plan;
            }
            if (simulate) {
                return plan.acceptedAmperes();
            }
            clearPlanCache();
            return plan.execute(level);
        } catch (RuntimeException failure) {
            clearPlanCache();
            EnergyTransferDiagnostics.warnOnce(
                    "cable injection",
                    this,
                    "Cable transfer failed at " + worldPosition
                            + " side " + side,
                    failure);
            return 0L;
        }
    }

    @Override
    public void invalidateSimulationCache() {
        clearPlanCache();
    }

    public CableLoadState.Snapshot loadSnapshot(long tick) {
        return load.snapshot(tick);
    }

    public boolean loadWouldOverload(
            long tick, long postLossSize, long amperes) {
        return load.wouldOverload(
                tick,
                postLossSize,
                amperes,
                conductor().conductor().electrical());
    }

    public void applyLoad(
            long tick,
            long postLossSize,
            long amperes,
            boolean overloaded) {
        boolean effectiveOverloaded = load.wouldOverload(
                tick,
                postLossSize,
                amperes,
                conductor().conductor().electrical());
        CableLoadState.Change change = load.record(
                tick,
                postLossSize,
                amperes,
                effectiveOverloaded);
        if (change.persistenceChanged()) {
            setChanged();
        }
        if (change.observableChanged()) {
            checkpoint.markSyncPending();
        }
        clearPlanCache();
    }

    public long transferredAmperes() {
        return load.snapshot(gameTime()).amperesThisTick();
    }

    public long transferredWattage() {
        return load.snapshot(gameTime()).wattageThisTick();
    }

    public long wattageLast() {
        return load.wattageLast(gameTime());
    }

    public int burnCounter() {
        return load.burnCounter(gameTime());
    }

    public void applyContactDamage(Entity entity) {
        CableBlock cable = conductor();
        if (!cable.conductor().bareWire()
                || !cable.conductor().electrical().contactDamage()
                || level == null
                || level.isClientSide) {
            return;
        }
        long wattage = load.wattageLast(level.getGameTime());
        if (wattage > 0L) {
            ElectricalShock.apply(level, entity, wattage);
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        writeLoad(tag, load.persistedSnapshot());
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load.restore(readLoad(tag));
        clearPlanCache();
    }

    @Override
    public CompoundTag getUpdateTag(
            HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        writeLoad(tag, load.snapshot(gameTime()));
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        load.restore(readLoad(tag));
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            load.restore(readLoad(packet.getTag()));
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private CableBlock conductor() {
        if (getBlockState().getBlock() instanceof CableBlock cable) {
            return cable;
        }
        throw new IllegalStateException(
                "Cable block entity has non-cable state");
    }

    private long gameTime() {
        return level == null ? 0L : level.getGameTime();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void clearPlanCache() {
        cachedPlanKey = null;
        cachedPlan = null;
    }

    private static void writeLoad(
            CompoundTag tag, CableLoadState.Snapshot snapshot) {
        tag.putBoolean("load_tick_initialized", snapshot.tickInitialized());
        tag.putLong("load_tick", snapshot.currentTick());
        tag.putLong("transferred_amperes", snapshot.amperesThisTick());
        tag.putLong("transferred_wattage", snapshot.wattageThisTick());
        tag.putLong("wattage_last", snapshot.wattageLast());
        tag.putInt("burn_counter", snapshot.burnCounter());
        tag.putLong("burn_next_decay", snapshot.nextDecayTick());
        tag.putLong("burn_at_tick", snapshot.burnAtTick());
    }

    private static CableLoadState.Snapshot readLoad(CompoundTag tag) {
        return new CableLoadState.Snapshot(
                tag.getBoolean("load_tick_initialized"),
                tag.getLong("load_tick"),
                tag.getLong("transferred_amperes"),
                tag.getLong("transferred_wattage"),
                tag.getLong("wattage_last"),
                tag.getInt("burn_counter"),
                tag.contains("burn_next_decay")
                        ? tag.getLong("burn_next_decay")
                        : -1L,
                tag.contains("burn_at_tick")
                        ? tag.getLong("burn_at_tick")
                        : -1L);
    }

    private record PlanKey(
            Direction ingress,
            long packetSize,
            long amperes) {}
}
