package com.masson.cruciblecraft.energy.quantum;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.machine.processing.MachineEnergyBuffer;
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

/** Converts buffered LU into QU at the catalog 1/2 ratio. */
public final class QuantumEnergizerBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final QuantumEnergizerProfile profile;
    private final MachineEnergyBuffer lu;
    private final MachineEnergyBuffer qu;
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();

    public QuantumEnergizerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.QUANTUM_ENERGIZER.get(), pos, state);
        this.profile = profileOf(state);
        this.lu = new MachineEnergyBuffer(
                profile.luCapacity(), profile.luInput());
        this.qu = new MachineEnergyBuffer(
                profile.quCapacity(), profile.quOutput());
    }

    public QuantumEnergizerProfile profile() {
        return profile;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            QuantumEnergizerBlockEntity energizer) {
        energizer.convert();
        if (energizer.qu.stored() >= energizer.profile.quOutput()) {
            EnergyEmitter.emit(
                    level,
                    pos,
                    energizer,
                    EnergyType.QUANTUM,
                    state.getValue(QuantumEnergizerBlock.FACING));
        }
        boolean lit = energizer.lu.stored() > 0L || energizer.qu.stored() > 0L;
        if (state.getValue(QuantumEnergizerBlock.LIT) != lit) {
            level.setBlock(
                    pos,
                    state.setValue(QuantumEnergizerBlock.LIT, lit),
                    Block.UPDATE_CLIENTS);
        }
    }

    private void convert() {
        while (lu.stored() >= profile.luInput()
                && qu.stored() + profile.quOutput() <= profile.quCapacity()) {
            if (!lu.consume(profile.luInput())) {
                break;
            }
            qu.insert(
                    profile.quOutput(),
                    1L,
                    false);
            setChanged();
        }
    }

    public Direction facing() {
        return getBlockState().getValue(QuantumEnergizerBlock.FACING);
    }

    public boolean isOutput(Direction side) {
        return side == facing();
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        if (side == null) {
            return false;
        }
        if (type == EnergyType.LU) {
            return !isOutput(side);
        }
        return type == EnergyType.QUANTUM && isOutput(side);
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || type != EnergyType.LU || amount <= 0L) {
            return 0L;
        }
        long accepted = lu.insert(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (!handles(type, side) || type != EnergyType.QUANTUM) {
            return 0L;
        }
        return qu.stored() >= profile.quOutput() ? profile.quOutput() : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || type != EnergyType.QUANTUM
                || size != profile.quOutput()
                || maxAmount <= 0L
                || qu.stored() < profile.quOutput()) {
            return 0L;
        }
        long packets = Math.min(
                maxAmount,
                EnergyPackets.packetsForUnits(size, qu.stored()));
        packets = outputBudget.claim(
                gameTime(), packets, packets, simulate);
        if (!simulate && packets > 0L
                && qu.consume(EnergyPackets.units(size, packets))) {
            setChanged();
        }
        return packets;
    }

    @Override
    public long stored(EnergyType type) {
        return switch (type) {
            case LU -> lu.stored();
            case QUANTUM -> qu.stored();
            default -> 0L;
        };
    }

    @Override
    public long capacity(EnergyType type) {
        return switch (type) {
            case LU -> profile.luCapacity();
            case QUANTUM -> profile.quCapacity();
            default -> 0L;
        };
    }

    private long gameTime() {
        return level == null ? 0L : level.getGameTime();
    }

    private static QuantumEnergizerProfile profileOf(BlockState state) {
        if (!(state.getBlock() instanceof QuantumEnergizerBlock block)) {
            throw new IllegalStateException(
                    "Quantum energizer entity bound to " + state.getBlock());
        }
        return block.profile();
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.lu", lu.stored());
        tag.putLong("gt.qu", qu.stored());
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        lu.restore(tag.getLong("gt.lu"));
        qu.restore(tag.getLong("gt.qu"));
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag(), registries);
        }
    }
}
