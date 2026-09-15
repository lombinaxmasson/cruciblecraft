package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.ElectricHeaterBlock;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Runtime projection of GT6 MultiTileEntityHeaterElectric. */
public final class ElectricHeaterBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final EnergyConverterProfile profile;
    private final long inputNominal;
    private final long inputMinimum;
    private final long inputMaximum;
    private final long outputNominal;
    private final long outputMinimum;
    private final MachineEnergyBuffer electric;

    private long cycleInput;
    private long cycleOutput;
    private boolean cyclePrepared;
    private boolean cycleConsumed;
    private boolean active;
    private boolean emitsEnergy;
    private boolean overcharged;
    private String status = "no_eu";

    public ElectricHeaterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ELECTRIC_HEATER.get(), pos, state);
        if (!(state.getBlock() instanceof EnergyConverterHost host)) {
            throw new IllegalArgumentException(
                    "Electric heater requires a catalog block");
        }
        profile = host.converterProfile();
        inputNominal = profile.inputPacket().size();
        inputMinimum = profile.inputWindow().minimum();
        inputMaximum = profile.inputWindow().maximum();
        outputNominal = profile.outputPacket().maxAmountPerTick();
        outputMinimum = Math.max(1L, outputNominal / 2L);
        electric = new MachineEnergyBuffer(inputMaximum, inputMaximum);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ElectricHeaterBlockEntity heater) {
        heater.prepareCycle();
        long delivered = 0L;
        if (heater.active) {
            delivered = EnergyEmitter.pushToSide(
                    level,
                    pos,
                    EnergyType.HEAT,
                    1L,
                    heater.cycleOutput,
                    state.getValue(ElectricHeaterBlock.FACING));
            if (!heater.cycleConsumed) {
                heater.consumeCycle();
            }
            heater.emitsEnergy = delivered > 0L;
            heater.status = delivered > 0L ? "running" : "blocked";
        } else if (heater.cycleInput > 0L) {
            // GT6 marks WASTE_ENERGY on this family: input is discarded even
            // when the converted output is below the receiver's minimum.
            heater.consumeCycle();
            heater.status = "underpowered";
        } else {
            heater.status = "no_eu";
        }
        heater.setLit(level, pos, state, heater.active);
        if (heater.cycleInput > 0L || delivered > 0L) {
            heater.setChanged();
        }
    }

    private void prepareCycle() {
        cyclePrepared = true;
        cycleConsumed = false;
        emitsEnergy = false;
        cycleInput = Math.min(electric.stored(), inputMaximum);
        if (cycleInput <= 0L) {
            cycleOutput = 0L;
            active = false;
            return;
        }
        cycleOutput = Math.min(
                profile.outputPacket().maxAmountPerTick(),
                (cycleInput * outputNominal) / inputNominal);
        active = cycleOutput >= outputMinimum
                && cycleInput >= inputMinimum;
    }

    private void consumeCycle() {
        if (!cyclePrepared || cycleConsumed || cycleInput <= 0L) {
            return;
        }
        if (!electric.consume(cycleInput)) {
            throw new IllegalStateException(
                    "Heater EU changed after conversion simulation");
        }
        cycleConsumed = true;
        cyclePrepared = false;
    }

    private void setLit(
            Level level,
            BlockPos pos,
            BlockState state,
            boolean lit) {
        if (state.hasProperty(ElectricHeaterBlock.LIT)
                && state.getValue(ElectricHeaterBlock.LIT) != lit) {
            level.setBlock(
                    pos,
                    state.setValue(ElectricHeaterBlock.LIT, lit),
                    3);
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        return front != null
                && side != null
                && ((type == EnergyType.ELECTRIC && side != front)
                        || (type == EnergyType.HEAT && side == front));
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
        return type == EnergyType.HEAT
                        && handles(type, side)
                        && cyclePrepared
                        && active
                        && !cycleConsumed
                ? 1L
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.HEAT
                || !handles(type, side)
                || size != 1L
                || maximum <= 0L
                || !cyclePrepared
                || !active
                || cycleConsumed) {
            return 0L;
        }
        long extracted = Math.min(maximum, cycleOutput);
        if (!simulate && level != null && !level.isClientSide) {
            consumeCycle();
            emitsEnergy = extracted > 0L;
            setChanged();
        }
        return extracted;
    }

    public void applyContactDamage(Entity entity) {
        if (level == null
                || level.isClientSide
                || !active) {
            return;
        }
        entity.hurt(
                level.damageSources().hotFloor(),
                Math.min(10.0F, outputNominal / 10.0F));
    }

    public long stored() {
        return electric.stored();
    }

    public long capacityStored() {
        return electric.capacity();
    }

    public boolean active() {
        return active;
    }

    public boolean emitsEnergy() {
        return emitsEnergy;
    }

    public boolean overcharged() {
        return overcharged;
    }

    public String status() {
        return status;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? electric.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC
                ? electric.capacity()
                : 0L;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("electric", electric.stored());
        tag.putBoolean("active", active);
        tag.putBoolean("emits_energy", emitsEnergy);
        tag.putBoolean("overcharged", overcharged);
        tag.putString("status", status);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        electric.restore(tag.getLong("electric"));
        active = tag.getBoolean("active");
        emitsEnergy = tag.getBoolean("emits_energy");
        overcharged = tag.getBoolean("overcharged");
        status = tag.getString("status");
        if (status.isBlank()) {
            status = electric.stored() > 0L ? "ready" : "no_eu";
        }
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(ElectricHeaterBlock.FACING)
                ? state.getValue(ElectricHeaterBlock.FACING)
                : null;
    }
}
