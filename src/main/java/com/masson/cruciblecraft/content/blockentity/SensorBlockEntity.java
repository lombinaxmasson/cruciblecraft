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
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class SensorBlockEntity extends BlockEntity {
    private static final int MAX_AVERAGING_VALUES = Short.MAX_VALUE;
    private final SensorKind kind;
    private Direction probe = Direction.SOUTH;
    private SensorMode mode = SensorMode.DISPLAY;
    private boolean hexadecimal;
    private int setNumber;
    private int[] averagedValues = new int[] {0};
    private int averageIndex;
    private long currentValue;
    private long currentMax;
    private int redstone;
    private long lastWallMs;
    private long tpsValue;
    private long lastSyncedDisplay = Long.MIN_VALUE;
    private SensorMode lastSyncedMode;
    private boolean lastSyncedHexadecimal;
    private int lastSyncedSetNumber = Integer.MIN_VALUE;
    private int lastSyncedAverageWindow = Integer.MIN_VALUE;

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

    public boolean hexadecimal() {
        return hexadecimal;
    }

    public int setNumber() {
        return setNumber;
    }

    public int averageWindow() {
        return averagedValues.length;
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

    /**
     * The number shown by the GT6 display for the current mode.
     *
     * <p>Comparison modes show the configured threshold, while the full
     * modes show the fixed {@code 100} payload used by the original display.
     */
    public long displayedNumber() {
        return switch (mode) {
            case DISPLAY -> currentValue;
            case PERCENT -> currentMax > 0L
                    ? Math.min(100L, Math.max(0L, currentValue * 100L / currentMax))
                    : 0L;
            case GREATER, EQUAL, SMALLER, SCALE -> setNumber;
            case FULL, NOT_FULL -> 100L;
        };
    }

    public void setProbe(Direction probe) {
        this.probe = probe;
        setChanged();
        if (level != null && !level.isClientSide) {
            SensorBlock.notifyNeighbors(level, worldPosition);
            syncToClient();
        }
    }

    public void setMode(SensorMode mode) {
        this.mode = mode;
        redstone = 0;
        setChanged();
        if (level != null && !level.isClientSide) {
            SensorBlock.notifyNeighbors(level, worldPosition);
            syncToClient();
        }
    }

    public SensorMode cycleMode() {
        setMode(mode.next());
        return mode;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = bindSet(setNumber);
        setChanged();
        if (level != null && !level.isClientSide) {
            syncToClient();
        }
    }

    public void adjustSetNumber(int delta) {
        if (!mode.usesSetNumber()) {
            return;
        }
        long maximum = hexadecimal ? 0xFFFFL : 9999L;
        long next = Math.max(0L, Math.min(maximum, setNumber + (long) delta));
        setSetNumber((int) next);
    }

    public void toggleHexadecimal() {
        hexadecimal = !hexadecimal;
        setChanged();
        if (level != null && !level.isClientSide) {
            syncToClient();
        }
    }

    public void adjustAverageWindow(int delta) {
        int next = Math.max(
                1,
                Math.min(
                        MAX_AVERAGING_VALUES,
                        averagedValues.length + delta));
        if (next == averagedValues.length) {
            return;
        }
        averagedValues = new int[next];
        averageIndex = 0;
        setChanged();
        if (level != null && !level.isClientSide) {
            syncToClient();
        }
    }

    public void reset() {
        mode = SensorMode.DISPLAY;
        hexadecimal = false;
        setNumber = 0;
        averagedValues = new int[] {0};
        averageIndex = 0;
        currentValue = 0L;
        currentMax = 0L;
        redstone = 0;
        tpsValue = 0L;
        lastWallMs = 0L;
        setChanged();
        if (level != null && !level.isClientSide) {
            SensorBlock.notifyNeighbors(level, worldPosition);
            syncToClient();
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
        sensor.averageIndex = (sensor.averageIndex + 1)
                % sensor.averagedValues.length;
        sensor.averagedValues[sensor.averageIndex] = bindInt(sample.value());
        sensor.currentValue = average(sensor.averagedValues);
        sensor.currentMax = sample.max();
        int next = SensorReading.redstone(
                sensor.mode,
                sensor.currentValue,
                sensor.currentMax,
                sensor.setNumber,
                sensor.redstone);
        boolean redstoneChanged = next != sensor.redstone;
        if (redstoneChanged) {
            sensor.redstone = next;
            SensorBlock.notifyNeighbors(level, pos);
        }
        long display = sensor.displayedNumber();
        boolean displayChanged = sensor.lastSyncedDisplay == Long.MIN_VALUE
                || Math.abs(display - sensor.lastSyncedDisplay) > 49L;
        boolean metadataChanged = sensor.lastSyncedMode != sensor.mode
                || sensor.lastSyncedHexadecimal != sensor.hexadecimal
                || sensor.lastSyncedSetNumber != sensor.setNumber
                || sensor.lastSyncedAverageWindow != sensor.averageWindow();
        // GT6 sends large display changes immediately and refreshes the
        // display at least once per second for small changes.
        if (redstoneChanged
                || displayChanged
                || metadataChanged
                || level.getGameTime() % 20L == 0L) {
            sensor.syncToClient();
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        probe = Direction.from3DDataValue(tag.getInt("probe"));
        int modeBits = tag.getInt("mode") & 0xFF;
        mode = SensorMode.fromOrdinal(modeBits & 0x7F);
        hexadecimal = (modeBits & 0x80) != 0;
        setNumber = bindSet(tag.getInt("set_number"));
        int averageWindow = Math.max(
                1,
                Math.min(
                        MAX_AVERAGING_VALUES,
                        tag.getInt("average_window")));
        averagedValues = new int[averageWindow];
        int[] savedValues = tag.getIntArray("average_values");
        System.arraycopy(
                savedValues,
                0,
                averagedValues,
                0,
                Math.min(savedValues.length, averagedValues.length));
        averageIndex = Math.floorMod(
                tag.getInt("average_index"),
                averagedValues.length);
        currentValue = tag.getLong("value");
        currentMax = tag.getLong("max");
        redstone = Math.max(0, Math.min(15, tag.getInt("redstone")));
        tpsValue = tag.getLong("tps");
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("probe", probe.get3DDataValue());
        tag.putByte("mode", (byte) (mode.ordinal() | (hexadecimal ? 0x80 : 0)));
        tag.putInt("set_number", setNumber);
        tag.putInt("average_window", averageWindow());
        tag.putInt("average_index", averageIndex);
        tag.putIntArray("average_values", averagedValues);
        tag.putLong("value", currentValue);
        tag.putLong("max", currentMax);
        tag.putInt("redstone", redstone);
        tag.putLong("tps", tpsValue);
    }

    private static int bindSet(int value) {
        if (value < 0) {
            return 0;
        }
        return Math.min(0xFFFF, value);
    }

    private void syncToClient() {
        setChanged();
        if (level == null || level.isClientSide) {
            return;
        }
        lastSyncedDisplay = displayedNumber();
        lastSyncedMode = mode;
        lastSyncedHexadecimal = hexadecimal;
        lastSyncedSetNumber = setNumber;
        lastSyncedAverageWindow = averageWindow();
        level.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                Block.UPDATE_CLIENTS);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        tag.remove("average_values");
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
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

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private static int bindInt(long value) {
        if (value <= 0L) {
            return 0;
        }
        return (int) Math.min(Integer.MAX_VALUE, value);
    }

    private static long average(int[] values) {
        long total = 0L;
        for (int value : values) {
            total += value;
        }
        return total / values.length;
    }
}
