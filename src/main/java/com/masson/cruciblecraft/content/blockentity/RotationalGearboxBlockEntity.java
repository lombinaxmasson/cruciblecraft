package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.RotationalGearboxBlock;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.energy.rotation.RotationalEnergyTransfer;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Loaded-only 1:1 directional RU routing proven separately from KU/EU. */
public final class RotationalGearboxBlockEntity extends BlockEntity
        implements IEnergyHandler {
    public static final long MAX_PACKET = 64L;
    private final PerTickEnergyBudget outputBudget =
            new PerTickEnergyBudget();
    private long stored;
    private long packetSize;
    private long transferredThisTick;
    private long transferredLast;
    private boolean overloaded;

    public RotationalGearboxBlockEntity(
            BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROTATIONAL_GEARBOX.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            RotationalGearboxBlockEntity gearbox) {
        gearbox.rollTransfer();
        RotationalEnergyTransfer.emit(
                level,
                pos,
                gearbox,
                state.getValue(RotationalGearboxBlock.FACING));
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        return type == EnergyType.KINETIC_ROTATION
                && front != null
                && side != null
                && (side == front || side == front.getOpposite());
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        Direction front = front();
        if (!handles(type, side)
                || side != front.getOpposite()
                || amount <= 0L) {
            return 0L;
        }
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude == 0L || magnitude > MAX_PACKET) {
            if (!simulate) {
                overloaded = true;
                setChanged();
            }
            return amount;
        }
        if (stored > 0L && packetSize != magnitude) {
            return 0L;
        }
        long accepted = stored == 0L ? 1L : 0L;
        if (!simulate && accepted > 0L) {
            stored = magnitude;
            packetSize = magnitude;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && side == front()
                        && packetSize > 0L
                        && stored >= packetSize
                        && outputBudget.claim(
                                        gameTime(), 1L, 1L, true)
                                > 0L
                ? packetSize
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || side != front()
                || size != packetSize
                || maximum <= 0L
                || stored < packetSize
                || outputBudget.claim(
                                gameTime(), 1L, maximum, true)
                        <= 0L) {
            return 0L;
        }
        if (simulate || level == null || level.isClientSide) {
            return 1L;
        }
        if (outputBudget.claim(gameTime(), 1L, 1L, false)
                != 1L) {
            throw new IllegalStateException(
                    "Gearbox output budget changed after simulation");
        }
        stored = 0L;
        packetSize = 0L;
        transferredThisTick += EnergyPackets.units(size, 1L);
        setChanged();
        return 1L;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.KINETIC_ROTATION
                ? stored
                : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.KINETIC_ROTATION
                ? MAX_PACKET
                : 0L;
    }

    public boolean overloaded() {
        return overloaded;
    }

    public long transferredLast() {
        return transferredLast;
    }

    private void rollTransfer() {
        transferredLast = transferredThisTick;
        transferredThisTick = 0L;
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(RotationalGearboxBlock.FACING)
                ? state.getValue(RotationalGearboxBlock.FACING)
                : null;
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("stored_ru", stored);
        tag.putLong("packet_size", packetSize);
        tag.putBoolean("overloaded", overloaded);
        tag.putLong("transferred_last", transferredLast);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = Math.max(
                0L, Math.min(MAX_PACKET, tag.getLong("stored_ru")));
        packetSize = Math.max(
                0L, Math.min(MAX_PACKET, tag.getLong("packet_size")));
        overloaded = tag.getBoolean("overloaded");
        transferredLast = Math.max(0L, tag.getLong("transferred_last"));
        if (stored == 0L || packetSize == 0L) {
            stored = 0L;
            packetSize = 0L;
        }
    }
}
