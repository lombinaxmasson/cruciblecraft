package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.ElectricEngineBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Runtime projection of GT6 MultiTileEntityEngineElectric. */
public final class ElectricEngineBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final EnergyConverterProfile profile;
    private final long inputNominal;
    private final long inputMaximum;
    private final long outputNominal;
    private final MachineEnergyBuffer electric;

    private int state = 15;
    private int piston;
    private long cycleInput;
    private long cycleOutput;
    private long cycleOutputSigned;
    private boolean cyclePrepared;
    private boolean cycleConsumed;
    private boolean active;
    private boolean emitsEnergy;
    private boolean stopped;
    private boolean overcharged;
    private String status = "no_eu";

    public ElectricEngineBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_ENGINE.get(), pos, state);
        if (!(state.getBlock() instanceof EnergyConverterHost host)) {
            throw new IllegalArgumentException(
                    "Electric engine requires a catalog block");
        }
        profile = host.converterProfile();
        inputNominal = profile.inputPacket().size();
        inputMaximum = profile.inputWindow().maximum();
        outputNominal = profile.outputPacket().size();
        electric = new MachineEnergyBuffer(inputMaximum, inputMaximum);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState blockState,
            ElectricEngineBlockEntity engine) {
        if (engine.active
                && level.getGameTime() % (32L - engine.state) == 0L) {
            engine.piston = (engine.piston + 1) & 3;
        }
        engine.prepareCycle();
        long delivered = 0L;
        if (engine.active) {
            delivered = EnergyEmitter.pushToSide(
                    level,
                    pos,
                    EnergyType.KINETIC_PUSH,
                    engine.cycleOutputSigned,
                    1L,
                    blockState.getValue(ElectricEngineBlock.FACING));
            if (!engine.cycleConsumed) {
                engine.consumeCycle();
            }
            engine.emitsEnergy = delivered > 0L;
            engine.status = delivered > 0L ? "running" : "blocked";
        } else {
            engine.status = engine.stopped
                    ? "stopped"
                    : engine.electric.stored() > 0L
                            ? "underpowered"
                            : "no_eu";
        }
        engine.setLit(level, pos, blockState, engine.active);
        if (engine.cycleInput > 0L || delivered != 0L) {
            engine.setChanged();
        }
    }

    private void prepareCycle() {
        cyclePrepared = true;
        cycleConsumed = false;
        emitsEnergy = false;
        cycleInput = ceilDiv(inputNominal * (state + 1L), 16L);
        cycleOutput = (outputNominal * (state + 1L)) / 16L;
        cycleOutputSigned = piston > 1 ? -cycleOutput : cycleOutput;
        active = !stopped
                && cycleOutput > 0L
                && electric.stored() >= cycleInput;
    }

    private void consumeCycle() {
        if (!cyclePrepared || cycleConsumed || cycleInput <= 0L) {
            return;
        }
        if (!electric.consume(cycleInput)) {
            throw new IllegalStateException(
                    "Electric engine EU changed after conversion simulation");
        }
        cycleConsumed = true;
        cyclePrepared = false;
    }

    private void setLit(
            Level level,
            BlockPos pos,
            BlockState blockState,
            boolean lit) {
        if (blockState.hasProperty(ElectricEngineBlock.LIT)
                && blockState.getValue(ElectricEngineBlock.LIT) != lit) {
            level.setBlock(
                    pos,
                    blockState.setValue(ElectricEngineBlock.LIT, lit),
                    3);
        }
    }

    public void cycleState() {
        state = (state + 1) & 31;
        setChanged();
    }

    public int state() {
        return state;
    }

    public int piston() {
        return piston;
    }

    public long currentInput() {
        return ceilDiv(inputNominal * (state + 1L), 16L);
    }

    public long currentOutput() {
        return (outputNominal * (state + 1L)) / 16L;
    }

    public long stored() {
        return electric.stored();
    }

    public boolean active() {
        return active;
    }

    public boolean emitsEnergy() {
        return emitsEnergy;
    }

    public boolean stopped() {
        return stopped;
    }

    public boolean overcharged() {
        return overcharged;
    }

    public String status() {
        return status;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        return front != null
                && side != null
                && !stopped
                && ((type == EnergyType.ELECTRIC
                                && side == front.getOpposite())
                        || (type == EnergyType.KINETIC_PUSH
                                && side == front));
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
                || EnergyPackets.magnitude(size) > inputMaximum) {
            if (!simulate) {
                overcharged = true;
                electric.restore(0L);
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
        return type == EnergyType.KINETIC_PUSH
                        && handles(type, side)
                        && cyclePrepared
                        && active
                        && !cycleConsumed
                ? cycleOutputSigned
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.KINETIC_PUSH
                || !handles(type, side)
                || size != cycleOutputSigned
                || maximum <= 0L
                || !cyclePrepared
                || !active
                || cycleConsumed) {
            return 0L;
        }
        long extracted = Math.min(maximum, 1L);
        if (!simulate && level != null && !level.isClientSide) {
            consumeCycle();
            emitsEnergy = extracted > 0L;
            setChanged();
        }
        return extracted;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? electric.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC ? electric.capacity() : 0L;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("electric", electric.stored());
        tag.putInt("state", state);
        tag.putInt("piston", piston);
        tag.putBoolean("active", active);
        tag.putBoolean("emits_energy", emitsEnergy);
        tag.putBoolean("stopped", stopped);
        tag.putBoolean("overcharged", overcharged);
        tag.putString("status", status);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        electric.restore(tag.getLong("electric"));
        state = Math.max(0, Math.min(31, tag.getInt("state")));
        piston = tag.getInt("piston") & 3;
        active = tag.getBoolean("active");
        emitsEnergy = tag.getBoolean("emits_energy");
        stopped = tag.getBoolean("stopped");
        overcharged = tag.getBoolean("overcharged");
        status = tag.getString("status");
        if (status.isBlank()) {
            status = stopped
                    ? "stopped"
                    : electric.stored() > 0L ? "ready" : "no_eu";
        }
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(ElectricEngineBlock.FACING)
                ? state.getValue(ElectricEngineBlock.FACING)
                : null;
    }

    private static long ceilDiv(long dividend, long divisor) {
        return (dividend + divisor - 1L) / divisor;
    }
}
