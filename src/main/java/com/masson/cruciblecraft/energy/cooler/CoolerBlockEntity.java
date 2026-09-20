package com.masson.cruciblecraft.energy.cooler;

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
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * GT6 {@code TileEntityBase11Twotypes#doTwinType}: EU or FE in, CU front,
 * HU back, {@code NBT_WASTE_ENERGY}.
 */
public final class CoolerBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final CoolerProfile profile;
    private final MachineEnergyBuffer buffer;
    private final IEnergyStorage fluxInput = new FluxInput();

    private int mode;
    private long cycleOutput;
    private boolean canEmit;
    private boolean overcharged;
    private long lastPushedCu;
    private long lastPushedHu;
    private String status = "idle";

    public CoolerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.COOLER.get(), pos, state);
        if (!(state.getBlock() instanceof CoolerBlock block)) {
            throw new IllegalArgumentException(
                    "Cooler block entity requires a configured block");
        }
        profile = block.profile();
        buffer = new MachineEnergyBuffer(
                profile.energyCapacity(),
                profile.electric() ? profile.inputMaximum() : 1L);
        status = profile.electric() ? "no_eu" : "no_fe";
    }

    public CoolerProfile profile() {
        return profile;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            CoolerBlockEntity cooler) {
        cooler.convert(level, pos, state);
        cooler.setLit(level, pos, state, cooler.canEmit);
        cooler.setChanged();
    }

    private void convert(Level level, BlockPos pos, BlockState state) {
        lastPushedCu = 0L;
        lastPushedHu = 0L;
        long stored = buffer.stored();
        cycleOutput = convertUnits(
                stored, profile.nbtInput(), profile.nbtOutput(), false);
        if (mode > 0) {
            cycleOutput = Math.min(
                    cycleOutput,
                    convertUnits(
                            profile.outputMaximum(),
                            16L,
                            16L - mode,
                            false));
        }
        canEmit = cycleOutput >= profile.outputMinimum();
        if (canEmit && cycleOutput > profile.outputMaximum()) {
            overcharged = true;
            buffer.restore(0L);
            canEmit = false;
            cycleOutput = 0L;
            status = "overloaded";
            return;
        }
        Direction front = front();
        if (canEmit && front != null) {
            lastPushedCu = EnergyEmitter.pushToSide(
                    level, pos, EnergyType.CU, 1L, cycleOutput, front);
            lastPushedHu = EnergyEmitter.pushToSide(
                    level,
                    pos,
                    EnergyType.HEAT,
                    1L,
                    cycleOutput,
                    front.getOpposite());
            status = lastPushedCu > 0L || lastPushedHu > 0L
                    ? "running"
                    : "blocked";
        } else if (stored > 0L) {
            status = "underpowered";
        } else {
            status = profile.electric() ? "no_eu" : "no_fe";
        }
        long waste = convertUnits(
                profile.inputMaximum(), 16L, 16L - mode, true);
        long remaining = Math.max(0L, stored - waste);
        if (remaining != stored) {
            buffer.restore(remaining);
        }
    }

    public IEnergyStorage fluxStorage(Direction side) {
        return profile.flux() && isInput(side) ? fluxInput : null;
    }

    public void cycleMode(boolean reverse) {
        if (!profile.switchableMode()) {
            return;
        }
        mode = reverse ? (mode + 15) % 16 : (mode + 1) % 16;
        setChanged();
    }

    public void setMode(int nextMode) {
        if (!profile.switchableMode()) {
            return;
        }
        mode = Math.floorMod(nextMode, 16);
        setChanged();
    }

    public int mode() {
        return mode;
    }

    public long stored() {
        return buffer.stored();
    }

    public long lastPushedCu() {
        return lastPushedCu;
    }

    public long lastPushedHu() {
        return lastPushedHu;
    }

    public boolean canEmit() {
        return canEmit;
    }

    public boolean overcharged() {
        return overcharged;
    }

    public String status() {
        return status;
    }

    public long cycleOutput() {
        return cycleOutput;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        if (side == null) {
            return false;
        }
        if (type == EnergyType.CU) {
            return isFront(side);
        }
        if (type == EnergyType.HEAT) {
            return isBack(side);
        }
        return profile.electric()
                && type == EnergyType.ELECTRIC
                && isInput(side);
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!profile.electric()
                || type != EnergyType.ELECTRIC
                || !handles(type, side)
                || amount <= 0L) {
            return 0L;
        }
        if (size == Long.MIN_VALUE
                || EnergyPackets.magnitude(size) > profile.inputMaximum()) {
            if (!simulate) {
                overcharged = true;
                buffer.restore(0L);
                setChanged();
            }
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
        return handles(type, side) && canEmit ? 1L : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || size != 1L
                || maximum <= 0L
                || !canEmit) {
            return 0L;
        }
        return Math.min(maximum, cycleOutput);
    }

    @Override
    public long stored(EnergyType type) {
        if (profile.electric() && type == EnergyType.ELECTRIC) {
            return buffer.stored();
        }
        return 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        if (profile.electric() && type == EnergyType.ELECTRIC) {
            return profile.energyCapacity();
        }
        return 0L;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", buffer.stored());
        tag.putInt("mode", mode);
        tag.putBoolean("overcharged", overcharged);
        tag.putString("status", status);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.restore(tag.getLong("energy"));
        mode = Math.floorMod(tag.getInt("mode"), 16);
        if (!profile.switchableMode()) {
            mode = 0;
        }
        overcharged = tag.getBoolean("overcharged");
        status = tag.getString("status");
        if (status.isBlank()) {
            status = buffer.stored() > 0L
                    ? "ready"
                    : profile.electric() ? "no_eu" : "no_fe";
        }
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(CoolerBlock.FACING)
                ? state.getValue(CoolerBlock.FACING)
                : null;
    }

    private boolean isFront(Direction side) {
        Direction facing = front();
        return facing != null && side == facing;
    }

    private boolean isBack(Direction side) {
        Direction facing = front();
        return facing != null && side == facing.getOpposite();
    }

    private boolean isInput(Direction side) {
        return side != null && !isFront(side) && !isBack(side);
    }

    private void setLit(
            Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.hasProperty(CoolerBlock.LIT)
                && state.getValue(CoolerBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(CoolerBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    /**
     * GT6 {@code UT.Code.units}.
     */
    static long convertUnits(
            long amount,
            long originalUnit,
            long targetUnit,
            boolean roundUp) {
        if (targetUnit == 0L) {
            return 0L;
        }
        if (originalUnit == targetUnit || originalUnit == 0L) {
            return amount;
        }
        long original = originalUnit;
        long target = targetUnit;
        if (original % target == 0L) {
            original /= target;
            target = 1L;
        } else if (target % original == 0L) {
            target /= original;
            original = 1L;
        }
        long product = amount * target;
        long result = product / original;
        if (roundUp && product % original > 0L) {
            result++;
        }
        return Math.max(0L, result);
    }

    private final class FluxInput implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!profile.flux() || maxReceive <= 0) {
                return 0;
            }
            long accepted = buffer.insert(1L, maxReceive, simulate);
            if (!simulate && accepted > 0L) {
                setChanged();
            }
            return accepted > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) accepted;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0;
        }

        @Override
        public int getEnergyStored() {
            long stored = buffer.stored();
            return stored > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) stored;
        }

        @Override
        public int getMaxEnergyStored() {
            long capacity = profile.energyCapacity();
            return capacity > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) capacity;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return profile.flux();
        }
    }
}
