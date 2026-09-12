package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.SensorBlock;
import com.masson.cruciblecraft.content.sensor.SensorKind;
import com.masson.cruciblecraft.content.sensor.SensorMode;
import com.masson.cruciblecraft.content.sensor.SensorReading;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SensorBlockEntity extends BlockEntity {
    private final SensorKind kind;
    private Direction probe = Direction.SOUTH;
    private SensorMode mode = SensorMode.DISPLAY;
    private int setNumber;
    private long currentValue;
    private long currentMax;
    private int redstone;
    private long lastWallMs;
    private long tpsValue;

    public SensorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.SENSOR.get(), pos, state);
        this.kind = state.getBlock() instanceof SensorBlock sensor
                ? sensor.kind()
                : SensorKind.THERMOMETER;
        this.probe = state.getValue(SensorBlock.FACING).getOpposite();
    }

    public SensorKind kind() {
        return kind;
    }

    public Direction probe() {
        return probe;
    }

    public SensorMode mode() {
        return mode;
    }

    public int setNumber() {
        return setNumber;
    }

    public long currentValue() {
        return currentValue;
    }

    public long currentMax() {
        return currentMax;
    }

    public int redstone() {
        return redstone;
    }

    public void setProbe(Direction probe) {
        this.probe = probe;
        setChanged();
        if (level != null && !level.isClientSide) {
            SensorBlock.notifyNeighbors(level, worldPosition);
        }
    }

    public void setMode(SensorMode mode) {
        this.mode = mode;
        redstone = 0;
        setChanged();
        if (level != null && !level.isClientSide) {
            SensorBlock.notifyNeighbors(level, worldPosition);
        }
    }

    public SensorMode cycleMode() {
        setMode(mode.next());
        return mode;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = bindSet(setNumber);
        setChanged();
    }

    public void adjustSetNumber(int delta) {
        if (!mode.usesSetNumber()) {
            return;
        }
        setSetNumber(setNumber + delta);
    }

    public void reset() {
        mode = SensorMode.DISPLAY;
        setNumber = 0;
        currentValue = 0L;
        currentMax = 0L;
        redstone = 0;
        tpsValue = 0L;
        lastWallMs = 0L;
        setChanged();
        if (level != null && !level.isClientSide) {
            SensorBlock.notifyNeighbors(level, worldPosition);
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            SensorBlockEntity sensor) {
        long tickRate = Math.max(1L, sensor.kind.tickRate());
        if (sensor.kind == SensorKind.TPS_METER) {
            long now = System.currentTimeMillis();
            if (sensor.lastWallMs == 0L) {
                sensor.lastWallMs = now;
            } else if (level.getGameTime() % tickRate == 0L) {
                long delta = Math.max(1L, now - sensor.lastWallMs);
                sensor.tpsValue = (tickRate * 100000L) / delta;
                sensor.lastWallMs = now;
            }
        }
        if (level.getGameTime() % tickRate != 0L) {
            return;
        }
        SensorReading.Sample sample = SensorReading.read(
                sensor.kind, level, pos, sensor.probe, sensor.tpsValue);
        sensor.currentValue = sample.value();
        sensor.currentMax = sample.max();
        int next = SensorReading.redstone(
                sensor.mode, sensor.currentValue, sensor.currentMax, sensor.setNumber);
        if (next != sensor.redstone) {
            sensor.redstone = next;
            sensor.setChanged();
            SensorBlock.notifyNeighbors(level, pos);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        probe = Direction.from3DDataValue(tag.getInt("probe"));
        mode = SensorMode.fromOrdinal(tag.getInt("mode"));
        setNumber = bindSet(tag.getInt("set_number"));
        currentValue = tag.getLong("value");
        currentMax = tag.getLong("max");
        redstone = Math.max(0, Math.min(15, tag.getInt("redstone")));
        tpsValue = tag.getLong("tps");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("probe", probe.get3DDataValue());
        tag.putInt("mode", mode.ordinal());
        tag.putInt("set_number", setNumber);
        tag.putLong("value", currentValue);
        tag.putLong("max", currentMax);
        tag.putInt("redstone", redstone);
        tag.putLong("tps", tpsValue);
    }

    private static int bindSet(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(9999, value);
    }
}
