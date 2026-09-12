package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.RotationalAxleBlock;
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

/** One-packet loaded-only RU hop constrained to a straight axle axis. */
public final class RotationalAxleBlockEntity extends BlockEntity
        implements IEnergyHandler {
    public static final long MAX_PACKET = 64L;
    public static final long MAX_POWER = 4L;
    private final PerTickEnergyBudget outputBudget =
            new PerTickEnergyBudget();
    private long stored;
    private long packetSize;
    private long transferredThisTick;
    private long transferredLast;
    private Direction receivedFrom;
    private boolean overloaded;

    public RotationalAxleBlockEntity(
            BlockPos pos, BlockState state) {
        super(ModBlockEntities.ROTATIONAL_AXLE.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            RotationalAxleBlockEntity axle) {
        axle.rollTransfer();
        Direction output = axle.outputSide();
        if (output != null) {
            RotationalEnergyTransfer.emit(
                    level,
                    pos,
                    axle,
                    output);
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.KINETIC_ROTATION
                && side != null
                && getBlockState().getValue(
                                RotationalAxleBlock.AXIS)
                        == side.getAxis();
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || amount <= 0L) {
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
        if ((stored > 0L && packetSize != magnitude)
                || receivedFrom != null && receivedFrom != side) {
            return 0L;
        }
        long storedPower = stored == 0L
                ? 0L
                : stored / packetSize;
        long room = MAX_POWER - storedPower;
        if (amount > room) {
            if (!simulate) {
                overloaded = true;
                stored = 0L;
                packetSize = 0L;
                receivedFrom = null;
                setChanged();
            }
            return amount;
        }
        long accepted = Math.min(amount, room);
        if (!simulate && accepted > 0L) {
            stored += EnergyPackets.units(magnitude, accepted);
            packetSize = magnitude;
            receivedFrom = side;
            setChanged();
        }
        return accepted;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side)
                        && side == outputSide()
                        && stored >= packetSize
                        && packetSize > 0L
                        && outputBudget.claim(
                                        gameTime(),
                                        MAX_POWER,
                                        MAX_POWER,
                                        true)
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
                || side != outputSide()
                || size != packetSize
                || maximum <= 0L
                || stored < packetSize
                || outputBudget.claim(
                                gameTime(),
                                MAX_POWER,
                                maximum,
                                true)
                        <= 0L) {
            return 0L;
        }
        long available = Math.min(
                maximum,
                Math.min(
                        stored / packetSize,
                        outputBudget.claim(
                                gameTime(),
                                MAX_POWER,
                                maximum,
                                true)));
        if (simulate || level == null || level.isClientSide) {
            return available;
        }
        if (outputBudget.claim(
                        gameTime(),
                        MAX_POWER,
                        available,
                        false)
                != available) {
            throw new IllegalStateException(
                    "Axle output budget changed after simulation");
        }
        stored -= EnergyPackets.units(packetSize, available);
        transferredThisTick += EnergyPackets.units(packetSize, available);
        if (stored == 0L) {
            packetSize = 0L;
            receivedFrom = null;
        }
        setChanged();
        return available;
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
                ? MAX_PACKET * MAX_POWER
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

    private Direction outputSide() {
        return receivedFrom == null
                ? null
                : receivedFrom.getOpposite();
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
        if (receivedFrom != null) {
            tag.putString("received_from", receivedFrom.getName());
        }
        tag.putBoolean("overloaded", overloaded);
        tag.putLong("transferred_last", transferredLast);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = Math.max(
                0L,
                Math.min(
                        MAX_PACKET * MAX_POWER,
                        tag.getLong("stored_ru")));
        packetSize = Math.max(
                0L, Math.min(MAX_PACKET, tag.getLong("packet_size")));
        receivedFrom = Direction.byName(
                tag.getString("received_from"));
        overloaded = tag.getBoolean("overloaded");
        transferredLast = Math.max(0L, tag.getLong("transferred_last"));
        if (stored == 0L || packetSize == 0L) {
            stored = 0L;
            packetSize = 0L;
            receivedFrom = null;
        }
    }
}
