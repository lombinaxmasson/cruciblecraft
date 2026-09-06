package com.masson.cruciblecraft.energy.transformer;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * EU-only bidirectional transformer. Front is HV in by default; wrench
 * reverse swaps I/O and clears the buffer.
 */
public final class TransformerBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final EnergyTransformerProfile profile;
    private final TransformerEnergyStore store;
    private final TransformerActivity activity = new TransformerActivity();
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();
    private boolean insertedThisTick;

    public TransformerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TRANSFORMER.get(), pos, state);
        this.profile = profileOf(state);
        this.store = new TransformerEnergyStore(profile);
    }

    public EnergyTransformerProfile profile() {
        return profile;
    }

    public boolean reversed() {
        return store.reversed();
    }

    public boolean isInput(Direction side) {
        Direction facing = facing();
        return store.reversed() ? side != facing : side == facing;
    }

    public boolean isOutput(Direction side) {
        Direction facing = facing();
        return store.reversed() ? side == facing : side != facing;
    }

    public long storedEu() {
        return store.stored();
    }

    public long capacityEu() {
        return store.capacity();
    }

    public boolean activityActive() {
        return activity.active();
    }

    public long sideVoltage(Direction side) {
        if (side == null) {
            return 0L;
        }
        return isInput(side)
                ? profile.acceptRec(store.reversed())
                : profile.emitRec(store.reversed());
    }

    public long sidePacketMultiplier(Direction side) {
        if (side == null || !isOutput(side)) {
            return 1L;
        }
        return profile.packetMultiplier(store.reversed());
    }

    public void toggleReversed() {
        store.clear();
        store.setReversed(!store.reversed());
        outputBudget.reset();
        insertedThisTick = false;
        setChanged();
        if (level != null && !level.isClientSide) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(
                    worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            TransformerBlockEntity transformer) {
        boolean inserted = transformer.insertedThisTick;
        transformer.insertedThisTick = false;
        long moved = EnergyEmitter.emit(
                level,
                pos,
                transformer,
                EnergyType.ELECTRIC,
                transformer.outputSides());
        transformer.activity.setActiveThisTick(moved > 0L || inserted);
        if (transformer.activity.check()) {
            BlockState next = state.setValue(
                    TransformerBlock.ACTIVITY,
                    (int) transformer.activity.state());
            level.setBlock(pos, next, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC && side != null;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (!handles(type, side) || !isOutput(side)) {
            return 0L;
        }
        long size = store.outputSize();
        if (size <= 0L) {
            return 0L;
        }
        return outputBudget.claim(
                gameTime(),
                profile.packetMultiplier(store.reversed()),
                1L,
                true) > 0L
                ? size
                : 0L;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || !isInput(side)) {
            return 0L;
        }
        long accepted = store.insert(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            insertedThisTick = true;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || !isOutput(side) || maxAmount <= 0L) {
            return 0L;
        }
        long limit = profile.packetMultiplier(store.reversed());
        long permitted = outputBudget.claim(gameTime(), limit, maxAmount, true);
        long available = store.extract(size, permitted, true);
        boolean effectiveSimulation =
                simulate || level == null || level.isClientSide;
        if (effectiveSimulation || available <= 0L) {
            return available;
        }
        long claimed = outputBudget.claim(
                gameTime(), limit, available, false);
        if (claimed != available) {
            throw new IllegalStateException(
                    "Transformer output budget changed after simulation");
        }
        long extracted = store.extract(size, available, false);
        if (extracted != available) {
            throw new IllegalStateException(
                    "Transformer store changed after simulation");
        }
        setChanged();
        return extracted;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? store.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC ? store.capacity() : 0L;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.energy", store.stored());
        tag.putBoolean("gt.reversed", store.reversed());
        tag.putBoolean("gt.active", activity.active());
        tag.putLong("gt.active_data", activity.data());
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        store.restore(
                tag.getLong("gt.energy"),
                tag.getBoolean("gt.reversed"));
        int visual = 0;
        if (getBlockState().hasProperty(TransformerBlock.ACTIVITY)) {
            visual = getBlockState().getValue(TransformerBlock.ACTIVITY);
        }
        activity.restore(
                tag.getBoolean("gt.active"),
                tag.getLong("gt.active_data"),
                visual);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("gt.energy", store.stored());
        tag.putBoolean("gt.reversed", store.reversed());
        tag.putBoolean("gt.active", activity.active());
        tag.putLong("gt.active_data", activity.data());
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        store.restore(
                tag.getLong("gt.energy"),
                tag.getBoolean("gt.reversed"));
        activity.restore(
                tag.getBoolean("gt.active"),
                tag.getLong("gt.active_data"),
                getBlockState().hasProperty(TransformerBlock.ACTIVITY)
                        ? getBlockState().getValue(TransformerBlock.ACTIVITY)
                        : 0);
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

    private List<Direction> outputSides() {
        Direction facing = facing();
        if (store.reversed()) {
            return List.of(facing);
        }
        List<Direction> sides = new ArrayList<>(5);
        for (Direction side : Direction.values()) {
            if (side != facing) {
                sides.add(side);
            }
        }
        return sides;
    }

    private Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(TransformerBlock.FACING)
                ? state.getValue(TransformerBlock.FACING)
                : Direction.UP;
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    private static EnergyTransformerProfile profileOf(BlockState state) {
        if (state.getBlock() instanceof TransformerBlock transformer) {
            return transformer.profile();
        }
        return EnergyTransformerCatalog.require(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
}
