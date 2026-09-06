package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
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
 * Minimal source-backed LU consumer.
 *
 * <p>The source laser engraver accepts LU on its upper face at a 32 LU
 * nominal packet. Recipe-map processing is deliberately deferred until the
 * source recipe identities and tiered circuits are available; this host keeps
 * the real LU capability and persistence contract in the meantime.
 */
public final class LaserEngraverBlockEntity
        extends BlockEntity implements IEnergyHandler {
    public static final long INPUT_PACKET = 32L;
    public static final long INPUT_MINIMUM = 16L;
    public static final long INPUT_MAXIMUM = 64L;
    public static final long ENERGY_CAPACITY = 2_048L;

    private final MachineEnergyBuffer energy =
            new MachineEnergyBuffer(ENERGY_CAPACITY, INPUT_MAXIMUM);

    public LaserEngraverBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LASER_ENGRAVER.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LaserEngraverBlockEntity engraver) {
        engraver.syncIfNeeded();
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.LU && side == Direction.UP;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || size < INPUT_MINIMUM
                || size > INPUT_MAXIMUM) {
            return 0L;
        }
        long accepted = energy.insert(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.LU ? energy.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.LU ? energy.capacity() : 0L;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.lu", energy.stored());
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.restore(tag.getLong("gt.lu"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putLong("gt.lu", energy.stored());
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        energy.restore(tag.getLong("gt.lu"));
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

    private void syncIfNeeded() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                Block.UPDATE_CLIENTS);
    }
}
