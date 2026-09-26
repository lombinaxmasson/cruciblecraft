package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.DirectedWasteConverterBlock;
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
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code TileEntityBase10EnergyConverter} with {@code NBT_WASTE_ENERGY},
 * plus {@code TileEntityBase11Bipolar.doBipolar} for electromagnets.
 *
 * <p>Laser and laser absorber emit one packet whose size is the converted
 * amount. Electromagnets emit that packet out the front and the negated
 * packet out the back. Waste then discards up to the input maximum even
 * when the output side accepts nothing. A selector cover mode above 0 caps
 * both the emitted size and that waste at {@code (16 - mode) / 16} of the
 * GT6 maximum, matching {@code TE_Behavior_Energy_Converter}.
 */
public final class DirectedWasteConverterBlockEntity
        extends MachineCoverHostBlockEntity
        implements IEnergyHandler {
    private final EnergyConverterProfile profile;
    private final EnergyType inputType;
    private final EnergyType outputType;
    private final boolean bipolar;
    private final boolean backInput;
    private final long inputNominal;
    private final long inputMinimum;
    private final long inputMaximum;
    private final long outputNominal;
    private final long outputMinimum;
    private final long outputMaximum;
    private final MachineEnergyBuffer buffer;

    private boolean active;
    private boolean emitsEnergy;
    private boolean overcharged;
    private String status = "idle";

    public DirectedWasteConverterBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DIRECTED_WASTE_CONVERTER.get(), pos, state);
        if (!(state.getBlock() instanceof EnergyConverterHost host)) {
            throw new IllegalArgumentException(
                    "Directed waste converter requires a catalog block");
        }
        profile = host.converterProfile();
        inputType = energyType(profile.inputPacket().identity());
        outputType = energyType(profile.outputPacket().identity());
        bipolar = "magnet_electric".equals(profile.runtimeBinding());
        backInput = "laser_absorber".equals(profile.runtimeBinding());
        inputNominal = profile.inputPacket().size();
        inputMinimum = profile.inputWindow().minimum();
        inputMaximum = profile.inputWindow().maximum();
        outputNominal = profile.outputPacket().size();
        outputMinimum = Math.max(1L, outputNominal / 2L);
        outputMaximum = Math.multiplyExact(outputNominal, 2L);
        buffer = new MachineEnergyBuffer(inputMaximum, inputMaximum);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            DirectedWasteConverterBlockEntity converter) {
        converter.tickCovers();
        converter.convert(level, pos, state);
    }

    /**
     * GT6 {@code UT.Code.units(maximum, 16, 16 - mode, roundUp)}. Mode 0 is
     * the uncapped maximum.
     */
    public static long selectorLimit(long maximum, int mode, boolean roundUp) {
        if (mode <= 0) {
            return maximum;
        }
        int kept = 16 - Math.min(mode, 15);
        if (maximum <= 0L || kept <= 0) {
            return 0L;
        }
        long product = maximum * kept;
        long quotient = product / 16L;
        if (roundUp && product % 16L > 0L) {
            quotient++;
        }
        return Math.max(0L, quotient);
    }

    private void convert(Level level, BlockPos pos, BlockState state) {
        long stored = buffer.stored();
        long converted = inputNominal <= 0L
                ? 0L
                : stored * outputNominal / inputNominal;
        if (converted > outputMaximum) {
            buffer.restore(0L);
            overcharged = true;
            active = false;
            emitsEnergy = false;
            status = "overloaded";
            setLit(level, pos, state, false);
            setChanged();
            return;
        }
        int mode = selectorMode();
        if (mode > 0) {
            converted = Math.min(
                    converted, selectorLimit(outputMaximum, mode, false));
        }
        active = converted >= outputMinimum && stored >= inputMinimum;
        long delivered = 0L;
        if (active) {
            Direction front = front();
            if (bipolar) {
                delivered += EnergyEmitter.pushToSide(
                        level, pos, outputType, converted, 1L, front);
                delivered += EnergyEmitter.pushToSide(
                        level,
                        pos,
                        outputType,
                        -converted,
                        1L,
                        front.getOpposite());
            } else {
                delivered += EnergyEmitter.pushToSide(
                        level, pos, outputType, converted, 1L, front);
            }
            emitsEnergy = delivered > 0L;
            status = delivered > 0L ? "running" : "blocked";
        } else if (stored > 0L) {
            emitsEnergy = false;
            status = "underpowered";
        } else {
            emitsEnergy = false;
            status = "idle";
        }
        if (stored > 0L) {
            long drain = selectorLimit(inputMaximum, selectorMode(), true);
            buffer.consume(Math.min(stored, drain));
        }
        setLit(level, pos, state, active);
        if (stored > 0L || delivered > 0L) {
            setChanged();
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        if (front == null || side == null) {
            return false;
        }
        if (type == inputType && isInput(front, side)) {
            return true;
        }
        return type == outputType && isOutput(front, side);
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (type != inputType || !handles(type, side) || amount <= 0L) {
            return 0L;
        }
        if (size == Long.MIN_VALUE
                || EnergyPackets.magnitude(size) > inputMaximum) {
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
        return 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        return 0L;
    }

    @Override
    public long stored(EnergyType type) {
        return type == inputType ? buffer.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == inputType ? buffer.capacity() : 0L;
    }

    public long stored() {
        return buffer.stored();
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

    @Override
    public boolean runningActively() {
        return active;
    }

    @Override
    public boolean runningSuccessfully() {
        return emitsEnergy;
    }

    @Override
    public long energyStored() {
        return buffer.stored();
    }

    @Override
    public long energyCapacity() {
        return buffer.capacity();
    }

    public String status() {
        return status;
    }

    public EnergyConverterProfile profile() {
        return profile;
    }

    private boolean isInput(Direction front, Direction side) {
        if (backInput) {
            return side == front.getOpposite();
        }
        if (bipolar) {
            return side.getAxis() != front.getAxis();
        }
        return side != front;
    }

    private boolean isOutput(Direction front, Direction side) {
        if (bipolar) {
            return side.getAxis() == front.getAxis();
        }
        return side == front;
    }

    private void setLit(
            Level level,
            BlockPos pos,
            BlockState state,
            boolean lit) {
        if (state.hasProperty(DirectedWasteConverterBlock.LIT)
                && state.getValue(DirectedWasteConverterBlock.LIT) != lit) {
            level.setBlock(
                    pos,
                    state.setValue(DirectedWasteConverterBlock.LIT, lit),
                    3);
        }
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(DirectedWasteConverterBlock.FACING)
                ? state.getValue(DirectedWasteConverterBlock.FACING)
                : null;
    }

    private static EnergyType energyType(String identity) {
        return switch (identity) {
            case "EU" -> EnergyType.ELECTRIC;
            case "LU" -> EnergyType.LU;
            case "MU" -> EnergyType.MU;
            case "QU" -> EnergyType.QUANTUM;
            default -> throw new IllegalStateException(
                    "Unsupported converter energy " + identity);
        };
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("stored", buffer.stored());
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
        buffer.restore(tag.getLong("stored"));
        active = tag.getBoolean("active");
        emitsEnergy = tag.getBoolean("emits_energy");
        overcharged = tag.getBoolean("overcharged");
        status = tag.getString("status");
        if (status.isBlank()) {
            status = buffer.stored() > 0L ? "ready" : "idle";
        }
    }
}
