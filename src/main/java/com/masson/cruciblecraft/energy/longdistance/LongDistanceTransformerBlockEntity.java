package com.masson.cruciblecraft.energy.longdistance;

import java.util.ArrayDeque;
import java.util.HashSet;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Same-voltage EU endpoint. Facing is the local EU port; the opposite face
 * must touch long-distance wire. Loss is {@code max(64, distance/8)}.
 */
public final class LongDistanceTransformerBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private static final int MAX_SCAN = 256;
    private final LongDistanceTransformerProfile profile;
    private final MachineEnergyBuffer buffer;
    private BlockPos targetPos;
    private BlockPos senderPos;
    private long distance;
    private long throughput;

    public LongDistanceTransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LONG_DISTANCE_TRANSFORMER.get(), pos, state);
        this.profile = profileOf(state);
        this.buffer = new MachineEnergyBuffer(profile.capacity(), profile.voltage());
    }

    public LongDistanceTransformerProfile profile() {
        return profile;
    }

    public Direction facing() {
        return getBlockState().getValue(LongDistanceTransformerBlock.FACING);
    }

    public Direction wireSide() {
        return facing().getOpposite();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LongDistanceTransformerBlockEntity endpoint) {
        endpoint.ensureLink();
        LongDistanceTransformerBlockEntity target = endpoint.target();
        if (target != null && endpoint.buffer.stored() >= endpoint.profile.voltage()) {
            long packets = EnergyPackets.packetsForUnits(
                    endpoint.profile.voltage(), endpoint.buffer.stored());
            long lost = endpoint.lostSize();
            long accepted = target.buffer.insert(lost, packets, true);
            if (accepted > 0L
                    && endpoint.buffer.consume(
                            EnergyPackets.units(endpoint.profile.voltage(), accepted))) {
                target.buffer.insert(lost, accepted, false);
                target.senderPos = pos.immutable();
                endpoint.setChanged();
                target.setChanged();
            }
        } else if (target == null && endpoint.buffer.stored() >= endpoint.profile.voltage()) {
            EnergyEmitter.emit(
                    level,
                    pos,
                    endpoint,
                    EnergyType.ELECTRIC,
                    endpoint.facing());
        }
        boolean lit = endpoint.buffer.stored() > 0L || endpoint.targetPos != null;
        if (state.getValue(LongDistanceTransformerBlock.LIT) != lit) {
            level.setBlock(
                    pos,
                    state.setValue(LongDistanceTransformerBlock.LIT, lit),
                    Block.UPDATE_CLIENTS);
        }
    }

    public void forceLinkForTest(BlockPos target, long distance) {
        this.targetPos = target.immutable();
        this.senderPos = null;
        this.distance = Math.max(1L, distance);
        this.throughput = profile.voltage();
        setChanged();
    }

    private long lostSize() {
        long loss = Math.max(64L, distance / 8L);
        return Math.max(1L, profile.voltage() - loss);
    }

    private LongDistanceTransformerBlockEntity target() {
        return endpointAt(targetPos);
    }

    private LongDistanceTransformerBlockEntity sender() {
        return endpointAt(senderPos);
    }

    private LongDistanceTransformerBlockEntity endpointAt(BlockPos pos) {
        if (level == null || pos == null) {
            return null;
        }
        BlockEntity be = level.getBlockEntity(pos);
        return be instanceof LongDistanceTransformerBlockEntity endpoint
                ? endpoint
                : null;
    }

    private boolean isReceiver() {
        LongDistanceTransformerBlockEntity sender = sender();
        return sender != null && worldPosition.equals(sender.targetPos);
    }

    private void ensureLink() {
        if (isReceiver() || target() != null) {
            return;
        }
        scanWires();
    }

    private void scanWires() {
        targetPos = null;
        distance = 0L;
        throughput = 0L;
        if (level == null) {
            return;
        }
        BlockPos start = worldPosition.relative(wireSide());
        BlockState startState = level.getBlockState(start);
        if (!(startState.getBlock() instanceof LongDistanceWireBlock wire)
                || wire.voltage() < profile.voltage()) {
            setChanged();
            return;
        }
        throughput = wire.voltage();
        HashSet<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> frontier = new ArrayDeque<>();
        seen.add(worldPosition);
        frontier.add(start);
        seen.add(start);
        long walked = 0L;
        while (!frontier.isEmpty() && seen.size() < MAX_SCAN) {
            int layer = frontier.size();
            walked++;
            for (int i = 0; i < layer; i++) {
                BlockPos current = frontier.removeFirst();
                for (Direction direction : Direction.values()) {
                    BlockPos next = current.relative(direction);
                    if (!seen.add(next)) {
                        continue;
                    }
                    BlockState nextState = level.getBlockState(next);
                    if (nextState.getBlock() == wire) {
                        frontier.add(next);
                        continue;
                    }
                    if (nextState.getBlock() instanceof LongDistanceTransformerBlock
                            && level.getBlockEntity(next)
                                    instanceof LongDistanceTransformerBlockEntity other
                            && other != this
                            && other.wireSide() == direction.getOpposite()
                            && other.profile.voltage() == profile.voltage()) {
                        targetPos = next.immutable();
                        distance = walked;
                        other.senderPos = worldPosition.immutable();
                        other.setChanged();
                        setChanged();
                        return;
                    }
                }
            }
        }
        setChanged();
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC && side != null && side != wireSide();
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || amount <= 0L || side != facing()) {
            return 0L;
        }
        if (size > profile.voltage()) {
            return amount;
        }
        if (!simulate) {
            ensureLink();
        }
        if (target() == null) {
            return 0L;
        }
        if (throughput > 0L && size > throughput) {
            return amount;
        }
        long accepted = buffer.insert(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (!handles(type, side) || side != facing() || target() != null) {
            return 0L;
        }
        return buffer.stored() >= profile.voltage() ? profile.voltage() : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || side != facing()
                || target() != null
                || size != profile.voltage()
                || maxAmount <= 0L) {
            return 0L;
        }
        long packets = Math.min(
                maxAmount,
                EnergyPackets.packetsForUnits(size, buffer.stored()));
        if (!simulate && packets > 0L) {
            buffer.consume(EnergyPackets.units(size, packets));
            setChanged();
        }
        return packets;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? buffer.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC ? buffer.capacity() : 0L;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.eu", buffer.stored());
        tag.putLong("gt.distance", distance);
        tag.putLong("gt.throughput", throughput);
        if (targetPos != null) {
            tag.putLong("gt.target", targetPos.asLong());
        }
        if (senderPos != null) {
            tag.putLong("gt.sender", senderPos.asLong());
        }
    }

    @Override
    public void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.restore(tag.getLong("gt.eu"));
        distance = tag.getLong("gt.distance");
        throughput = tag.getLong("gt.throughput");
        targetPos = tag.contains("gt.target")
                ? BlockPos.of(tag.getLong("gt.target"))
                : null;
        senderPos = tag.contains("gt.sender")
                ? BlockPos.of(tag.getLong("gt.sender"))
                : null;
    }

    private static LongDistanceTransformerProfile profileOf(BlockState state) {
        if (state.getBlock() instanceof LongDistanceTransformerBlock block) {
            return block.profile();
        }
        throw new IllegalArgumentException("Long-distance transformer block required");
    }
}
