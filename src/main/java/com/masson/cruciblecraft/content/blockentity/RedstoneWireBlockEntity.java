package com.masson.cruciblecraft.content.blockentity;

import java.util.Arrays;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireKind;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireNetwork;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireSinks;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ports {@code MultiTileEntityWireRedstoneInsulated} redstone loss and
 * {@code MultiTileEntityWireRedstone} visual/light state.
 */
public final class RedstoneWireBlockEntity extends BlockEntity {
    private static final byte RECEIVED_NONE = (byte) -1;

    private final RedstoneWireKind kind;
    private final int[] vanillaSides = new int[Direction.values().length];
    private long redstone;
    private byte received = RECEIVED_NONE;
    private byte mode;

    public RedstoneWireBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_WIRE.get(), pos, state);
        this.kind = state.getBlock() instanceof RedstoneWireBlock wire
                ? wire.kind()
                : RedstoneWireKind.RED_ALLOY;
        Arrays.fill(vanillaSides, -1);
    }

    public RedstoneWireKind kind() {
        if (getBlockState().getBlock() instanceof RedstoneWireBlock wire) {
            return wire.kind();
        }
        return kind;
    }

    public long redstoneValue() {
        return redstone;
    }

    public long minusLoss() {
        return redstone - kind().loss();
    }

    public int vanillaSideCache(Direction side) {
        return vanillaSides[side.get3DDataValue()];
    }

    @Nullable
    public Direction received() {
        return received == RECEIVED_NONE
                ? null
                : Direction.from3DDataValue(received);
    }

    public int visual() {
        return RedstoneWireNetwork.visual(redstone);
    }

    public int comparator() {
        return RedstoneWireNetwork.comparator(redstone);
    }

    public boolean canEmitToWire(Direction side) {
        return RedstoneWireBlock.isConnected(getBlockState(), side);
    }

    public boolean canAcceptFromWire(Direction side) {
        return RedstoneWireBlock.isConnected(getBlockState(), side);
    }

    public void onNeighborChanged() {
        if (level != null && !level.isClientSide && updateRedstone()) {
            RedstoneWireNetwork.propagate(this);
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            RedstoneWireBlockEntity wire) {
        Arrays.fill(wire.vanillaSides, -1);
        if (wire.updateRedstone()) {
            RedstoneWireNetwork.propagate(wire);
        }
    }

    public boolean updateRedstone() {
        if (level == null || level.isClientSide) {
            return false;
        }
        long previous = redstone;
        byte previousReceived = received;
        long floor = mode * RedstoneWireNetwork.MAX_RANGE - kind().loss();
        long best = redstoneAt(received());
        byte bestSide = received;
        if (best <= floor) {
            best = floor;
            bestSide = RECEIVED_NONE;
        }
        for (Direction side : Direction.values()) {
            if (previousReceived != RECEIVED_NONE
                    && side.get3DDataValue() == previousReceived) {
                continue;
            }
            long candidate = redstoneAt(side);
            if (candidate > best) {
                best = candidate;
                bestSide = (byte) side.get3DDataValue();
            }
        }
        redstone = best;
        received = bestSide;
        boolean changed = redstone != previous;
        if (changed || visual() != getBlockState().getValue(RedstoneWireBlock.POWER)) {
            syncPower();
        }
        if (changed && connectedToNonWire()) {
            causeBlockUpdate();
        }
        return changed;
    }

    private long redstoneAt(@Nullable Direction side) {
        if (side == null
                || !RedstoneWireBlock.isConnected(getBlockState(), side)) {
            return 0L;
        }
        BlockPos neighborPos = worldPosition.relative(side);
        BlockEntity neighbor = level.getBlockEntity(neighborPos);
        if (neighbor instanceof RedstoneWireBlockEntity other) {
            if (!other.canEmitToWire(side.getOpposite())) {
                return 0L;
            }
            return other.minusLoss();
        }
        BlockState neighborState = level.getBlockState(neighborPos);
        if (RedstoneWireSinks.isSink(neighborState.getBlock())) {
            return 0L;
        }
        int incoming = vanillaIncoming(side, neighborPos);
        if (incoming <= 0) {
            return 0L;
        }
        return RedstoneWireNetwork.MAX_RANGE * incoming - kind().loss();
    }

    private int vanillaIncoming(Direction side, BlockPos neighborPos) {
        int index = side.get3DDataValue();
        if (vanillaSides[index] < 0) {
            vanillaSides[index] = Math.max(
                    level.getSignal(neighborPos, side.getOpposite()),
                    level.getDirectSignal(neighborPos, side.getOpposite()));
        }
        return vanillaSides[index];
    }

    private boolean connectedToNonWire() {
        for (Direction side : Direction.values()) {
            if (!RedstoneWireBlock.isConnected(getBlockState(), side)) {
                continue;
            }
            if (!(level.getBlockEntity(worldPosition.relative(side))
                    instanceof RedstoneWireBlockEntity)) {
                return true;
            }
        }
        return false;
    }

    private void syncPower() {
        int visual = visual();
        BlockState state = getBlockState();
        if (state.getValue(RedstoneWireBlock.POWER) != visual) {
            level.setBlock(
                    worldPosition,
                    state.setValue(RedstoneWireBlock.POWER, visual),
                    Block.UPDATE_CLIENTS);
        }
        setChanged();
        level.sendBlockUpdated(
                worldPosition, state, getBlockState(), Block.UPDATE_CLIENTS);
    }

    private void causeBlockUpdate() {
        BlockState state = getBlockState();
        level.updateNeighborsAt(worldPosition, state.getBlock());
        for (Direction side : Direction.values()) {
            if (RedstoneWireBlock.isConnected(state, side)) {
                level.updateNeighborsAt(
                        worldPosition.relative(side), state.getBlock());
            }
        }
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("redstone", redstone);
        tag.putByte("received", received);
        tag.putByte("mode", mode);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        redstone = tag.getLong("redstone");
        received = tag.getByte("received");
        mode = tag.getByte("mode");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            loadAdditional(packet.getTag(), registries);
        }
    }
}
