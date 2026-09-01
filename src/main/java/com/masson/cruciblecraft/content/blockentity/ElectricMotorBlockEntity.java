package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.ElectricMotorBlock;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.energy.rotation.RotationalEnergyTransfer;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Electric motor: 32 EU input work produces one 16 RU packet. */
public final class ElectricMotorBlockEntity extends BlockEntity
        implements IEnergyHandler {
    public static final long INPUT_NOMINAL = 32L;
    public static final long INPUT_MAXIMUM = 64L;
    public static final long OUTPUT_SIZE = 16L;
    private static final long CAPACITY = 1_024L;

    private final MachineEnergyBuffer electric =
            new MachineEnergyBuffer(CAPACITY, INPUT_MAXIMUM);
    private final MachineEnergyBuffer rotational =
            new MachineEnergyBuffer(CAPACITY, OUTPUT_SIZE);
    private final PerTickEnergyBudget outputBudget =
            new PerTickEnergyBudget();
    private boolean overcharged;

    public ElectricMotorBlockEntity(
            BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_MOTOR.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ElectricMotorBlockEntity motor) {
        if (motor.electric.canConsume(INPUT_NOMINAL)
                && motor.rotational.capacity()
                                - motor.rotational.stored()
                        >= OUTPUT_SIZE) {
            if (!motor.electric.consume(INPUT_NOMINAL)) {
                throw new IllegalStateException(
                        "Motor EU changed after simulation");
            }
            if (motor.rotational.insert(
                            OUTPUT_SIZE, 1L, false)
                    != 1L) {
                throw new IllegalStateException(
                        "Motor RU output rejected after simulation");
            }
            motor.setChanged();
        }
        RotationalEnergyTransfer.emit(
                level,
                pos,
                motor,
                state.getValue(ElectricMotorBlock.FACING));
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        return front != null
                && side != null
                && (type == EnergyType.ELECTRIC
                                && side == front.getOpposite()
                        || type == EnergyType.KINETIC_ROTATION
                                && side == front);
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.ELECTRIC
                || !handles(type, side)
                || amount <= 0L) {
            return 0L;
        }
        if (size == Long.MIN_VALUE
                || Math.abs(size) > INPUT_MAXIMUM) {
            if (!simulate) {
                overcharged = true;
                setChanged();
            }
            return amount;
        }
        long accepted = electric.insert(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return type == EnergyType.KINETIC_ROTATION
                        && handles(type, side)
                        && rotational.stored() >= OUTPUT_SIZE
                        && outputBudget.claim(
                                        gameTime(), 1L, 1L, true)
                                > 0L
                ? OUTPUT_SIZE
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.KINETIC_ROTATION
                || !handles(type, side)
                || size != OUTPUT_SIZE
                || maximum <= 0L
                || rotational.stored() < OUTPUT_SIZE
                || outputBudget.claim(
                                gameTime(), 1L, maximum, true)
                        <= 0L) {
            return 0L;
        }
        if (simulate || level == null || level.isClientSide) {
            return 1L;
        }
        if (outputBudget.claim(gameTime(), 1L, 1L, false)
                        != 1L
                || !rotational.consume(OUTPUT_SIZE)) {
            throw new IllegalStateException(
                    "Motor RU changed after simulation");
        }
        setChanged();
        return 1L;
    }

    @Override
    public long stored(EnergyType type) {
        return switch (type) {
            case ELECTRIC -> electric.stored();
            case KINETIC_ROTATION -> rotational.stored();
            default -> 0L;
        };
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC
                        || type == EnergyType.KINETIC_ROTATION
                ? CAPACITY
                : 0L;
    }

    public boolean overcharged() {
        return overcharged;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("electric", electric.stored());
        tag.putLong("rotational", rotational.stored());
        tag.putBoolean("overcharged", overcharged);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        electric.restore(tag.getLong("electric"));
        rotational.restore(tag.getLong("rotational"));
        overcharged = tag.getBoolean("overcharged");
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(ElectricMotorBlock.FACING)
                ? state.getValue(ElectricMotorBlock.FACING)
                : null;
    }
}
