package com.masson.cruciblecraft.energy.flux;

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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * GT6 flux converters: RF/FE in to HU/KU/RU/MU/LU, or RU in to RF/FE.
 * Heaters, motors, magnets and lasers follow
 * {@code TileEntityBase10EnergyConverter} with {@code NBT_WASTE_ENERGY}.
 * Engines follow {@code MultiTileEntityEngineFlux}.
 */
public final class FluxBlockEntity extends BlockEntity implements IEnergyHandler {
    private final FluxProfile profile;
    private final MachineEnergyBuffer buffer;
    private final IEnergyStorage fluxInput = new FluxInput();
    private final IEnergyStorage fluxOutput = new FluxOutput();

    private int mode;
    private int engineState = 15;
    private int piston;
    private long tickCount;
    private long cycleOutput;
    private long cycleSigned;
    private boolean canEmit;
    private boolean emitsEnergy;
    private boolean overcharged;
    private boolean stopped;
    private boolean counterClockwise;
    private boolean fast;
    private long lastPushed;
    private String status = "idle";

    public FluxBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLUX_CONVERTER.get(), pos, state);
        if (!(state.getBlock() instanceof FluxBlock block)) {
            throw new IllegalArgumentException(
                    "Flux block entity requires a configured block");
        }
        profile = block.profile();
        buffer = new MachineEnergyBuffer(
                profile.energyCapacity(),
                profile.dynamo() ? profile.inputMaximum() : 1L);
        status = profile.dynamo() ? "no_ru" : "no_fe";
    }

    public FluxProfile profile() {
        return profile;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FluxBlockEntity flux) {
        flux.tickCount++;
        if (flux.profile.engine()) {
            flux.convertEngine(level, pos);
        } else {
            flux.convert(level, pos);
        }
        flux.setLit(level, pos, state, flux.canEmit);
        flux.setChanged();
    }

    private void convert(Level level, BlockPos pos) {
        lastPushed = 0L;
        emitsEnergy = false;
        long stored = buffer.stored();
        cycleOutput = FluxMath.convertUnits(
                stored, profile.nbtInput(), profile.nbtOutput(), false);
        if (mode > 0) {
            cycleOutput = Math.min(
                    cycleOutput,
                    FluxMath.convertUnits(
                            profile.outputMaximum(),
                            16L,
                            16L - mode,
                            false));
        }
        canEmit = cycleOutput >= profile.outputMinimum();
        fast = cycleOutput > profile.nbtOutput();
        if (canEmit && cycleOutput > profile.outputMaximum()) {
            overcharged = true;
            buffer.restore(0L);
            canEmit = false;
            cycleOutput = 0L;
            cycleSigned = 0L;
            status = "overloaded";
            return;
        }
        cycleSigned = signedOutput(cycleOutput);
        Direction front = front();
        if (canEmit && front != null) {
            lastPushed = emitConverted(level, pos, front);
            emitsEnergy = lastPushed > 0L;
            status = emitsEnergy ? "running" : "blocked";
        } else if (stored > 0L) {
            status = "underpowered";
        } else {
            status = profile.dynamo() ? "no_ru" : "no_fe";
        }
        long waste = FluxMath.convertUnits(
                profile.inputMaximum(), 16L, 16L - mode, true);
        long remaining = Math.max(0L, stored - waste);
        if (remaining != stored) {
            buffer.restore(remaining);
        }
    }

    private void convertEngine(Level level, BlockPos pos) {
        lastPushed = 0L;
        emitsEnergy = false;
        if (canEmit && tickCount % (32L - engineState) == 0L) {
            piston = (piston + 1) & 3;
        }
        long stored = buffer.stored();
        long tOutput = (profile.nbtOutput() * (engineState + 1L)) / 16L;
        long tInput = FluxMath.ceilDiv(
                profile.nbtInput() * (engineState + 1L), 16L);
        canEmit = stored >= tInput && tOutput > 0L;
        cycleOutput = tOutput;
        cycleSigned = piston > 1 ? -tOutput : tOutput;
        fast = false;
        Direction front = front();
        if (canEmit) {
            if (front != null) {
                lastPushed = EnergyEmitter.pushToSide(
                        level,
                        pos,
                        EnergyType.KINETIC_PUSH,
                        cycleSigned,
                        1L,
                        front);
            }
            emitsEnergy = lastPushed > 0L;
            buffer.restore(stored - tInput);
            status = emitsEnergy ? "running" : "blocked";
        } else if (stored > 0L) {
            status = "underpowered";
        } else {
            status = "no_fe";
        }
    }

    private long emitConverted(Level level, BlockPos pos, Direction front) {
        if (profile.dynamo()) {
            return FluxEnergy.pushToSide(
                    level,
                    pos,
                    front,
                    cycleOutput > Integer.MAX_VALUE
                            ? Integer.MAX_VALUE
                            : (int) cycleOutput);
        }
        EnergyType type = profile.gregOutputType();
        if (type == null) {
            return 0L;
        }
        if (profile.magnet()) {
            long pushed = EnergyEmitter.pushToSide(
                    level, pos, type, cycleOutput, 1L, front);
            pushed += EnergyEmitter.pushToSide(
                    level, pos, type, -cycleOutput, 1L, front.getOpposite());
            return pushed;
        }
        if (profile.sizeIrrelevantOutput()) {
            return EnergyEmitter.pushToSide(
                    level, pos, type, 1L, cycleOutput, front);
        }
        return EnergyEmitter.pushToSide(
                level, pos, type, cycleSigned, 1L, front);
    }

    private long signedOutput(long magnitude) {
        if (profile.motor() && counterClockwise) {
            return -magnitude;
        }
        return magnitude;
    }

    public IEnergyStorage fluxStorage(Direction side) {
        if (profile.fluxOutput() && isFront(side)) {
            return fluxOutput;
        }
        if (profile.fluxInput() && isInput(side)) {
            return fluxInput;
        }
        return null;
    }

    public void cycleMode(boolean reverse) {
        if (profile.engine()) {
            engineState = reverse
                    ? (engineState + 31) % 32
                    : (engineState + 1) % 32;
            setChanged();
            return;
        }
        if (!profile.converterMode()) {
            return;
        }
        mode = reverse ? (mode + 15) % 16 : (mode + 1) % 16;
        setChanged();
    }

    public void setMode(int nextMode) {
        if (profile.engine()) {
            engineState = Math.floorMod(nextMode, 32);
            setChanged();
            return;
        }
        if (!profile.converterMode()) {
            return;
        }
        mode = Math.floorMod(nextMode, 16);
        setChanged();
    }

    public void toggleReverse() {
        if (!profile.motor()) {
            return;
        }
        buffer.restore(0L);
        counterClockwise = !counterClockwise;
        setChanged();
    }

    public void toggleStopped() {
        stopped = !stopped;
        setChanged();
    }

    public void applyContactDamage(Entity entity) {
        if (level == null
                || level.isClientSide
                || !profile.heater()
                || !canEmit) {
            return;
        }
        entity.hurt(
                level.damageSources().hotFloor(),
                Math.min(10.0F, profile.nbtOutput() / 10.0F));
    }

    public void applyWalkOver(LivingEntity living) {
        if (!profile.motor() || !canEmit) {
            return;
        }
        float delta = (counterClockwise ? -5.0F : 5.0F) * (fast ? 2.0F : 1.0F);
        living.setYRot(living.getYRot() + delta);
        living.setYHeadRot(living.getYHeadRot() + delta);
    }

    public int mode() {
        return profile.engine() ? engineState : mode;
    }

    public long stored() {
        return buffer.stored();
    }

    public long lastPushed() {
        return lastPushed;
    }

    public boolean canEmit() {
        return canEmit;
    }

    public boolean emitsEnergy() {
        return emitsEnergy;
    }

    public boolean overcharged() {
        return overcharged;
    }

    public boolean counterClockwise() {
        return counterClockwise;
    }

    public boolean fast() {
        return fast;
    }

    public boolean stopped() {
        return stopped;
    }

    public String status() {
        return status;
    }

    public long cycleOutput() {
        return cycleOutput;
    }

    public long cycleSigned() {
        return cycleSigned;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        if (side == null) {
            return false;
        }
        EnergyType output = profile.gregOutputType();
        if (output != null && type == output && isOutput(side)) {
            return true;
        }
        EnergyType input = profile.gregInputType();
        return input != null && type == input && isInput(side);
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        EnergyType input = profile.gregInputType();
        if (input == null
                || type != input
                || !handles(type, side)
                || stopped
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
        if (!handles(type, side) || !canEmit) {
            return 0L;
        }
        if (profile.sizeIrrelevantOutput()) {
            return 1L;
        }
        if (profile.magnet()) {
            if (isFront(side)) {
                return cycleOutput;
            }
            if (isBack(side)) {
                return -cycleOutput;
            }
            return 0L;
        }
        return cycleSigned;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || maximum <= 0L || !canEmit) {
            return 0L;
        }
        if (profile.sizeIrrelevantOutput()) {
            return size == 1L ? Math.min(maximum, cycleOutput) : 0L;
        }
        if (size != outputSize(type, side)) {
            return 0L;
        }
        return Math.min(maximum, 1L);
    }

    @Override
    public long stored(EnergyType type) {
        EnergyType input = profile.gregInputType();
        return input != null && type == input ? buffer.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        EnergyType input = profile.gregInputType();
        return input != null && type == input ? profile.energyCapacity() : 0L;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", buffer.stored());
        tag.putInt("mode", mode);
        tag.putInt("engineState", engineState);
        tag.putInt("piston", piston);
        tag.putLong("tickCount", tickCount);
        tag.putBoolean("overcharged", overcharged);
        tag.putBoolean("stopped", stopped);
        tag.putBoolean("counterClockwise", counterClockwise);
        tag.putString("status", status);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        buffer.restore(tag.getLong("energy"));
        mode = Math.floorMod(tag.getInt("mode"), 16);
        if (!profile.converterMode()) {
            mode = 0;
        }
        engineState = tag.contains("engineState")
                ? Math.floorMod(tag.getInt("engineState"), 32)
                : 15;
        piston = tag.getInt("piston") & 3;
        tickCount = tag.getLong("tickCount");
        overcharged = tag.getBoolean("overcharged");
        stopped = tag.getBoolean("stopped");
        counterClockwise = tag.getBoolean("counterClockwise");
        status = tag.getString("status");
        if (status.isBlank()) {
            status = buffer.stored() > 0L
                    ? "ready"
                    : profile.dynamo() ? "no_ru" : "no_fe";
        }
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(FluxBlock.FACING)
                ? state.getValue(FluxBlock.FACING)
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
        if (side == null) {
            return false;
        }
        if (profile.dynamo() || profile.engine()) {
            return isBack(side);
        }
        if (profile.magnet()) {
            return !isFront(side) && !isBack(side);
        }
        return !isFront(side);
    }

    private boolean isOutput(Direction side) {
        if (profile.magnet()) {
            return isFront(side) || isBack(side);
        }
        return isFront(side);
    }

    private void setLit(
            Level level, BlockPos pos, BlockState state, boolean lit) {
        if (state.hasProperty(FluxBlock.LIT)
                && state.getValue(FluxBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(FluxBlock.LIT, lit), Block.UPDATE_CLIENTS);
        }
    }

    private final class FluxInput implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            if (!profile.fluxInput() || stopped || maxReceive <= 0) {
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
            return profile.fluxInput() && !stopped;
        }
    }

    private final class FluxOutput implements IEnergyStorage {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            if (!profile.fluxOutput() || !canEmit || maxExtract <= 0) {
                return 0;
            }
            long offered = Math.min(maxExtract, cycleOutput);
            return offered > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) offered;
        }

        @Override
        public int getEnergyStored() {
            return canEmit && cycleOutput <= Integer.MAX_VALUE
                    ? (int) cycleOutput
                    : canEmit ? Integer.MAX_VALUE : 0;
        }

        @Override
        public int getMaxEnergyStored() {
            long capacity = profile.outputMaximum();
            return capacity > Integer.MAX_VALUE
                    ? Integer.MAX_VALUE
                    : (int) capacity;
        }

        @Override
        public boolean canExtract() {
            return profile.fluxOutput();
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    }
}
