package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.DynamoBlock;
import com.masson.cruciblecraft.energy.BronzeDynamoEnergy;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.machine.CheckpointDecisions;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
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

/** Fixed bronze dynamo: kinetic back input, electric front output. */
public final class DynamoBlockEntity extends BlockEntity implements IEnergyHandler {
    private final BronzeDynamoEnergy energy = new BronzeDynamoEnergy();
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();
    private final CheckpointTracker checkpoint = new CheckpointTracker();

    public DynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.DYNAMO.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            DynamoBlockEntity dynamo) {
        if (dynamo.energy.convertOnePacket()) {
            dynamo.markMutation();
        }
        Direction front = state.getValue(DynamoBlock.FACING);
        EnergyEmitter.emit(level, pos, dynamo, EnergyType.ELECTRIC, front);
        long phase = CheckpointDecisions.phaseKey(pos.getX(), pos.getY(), pos.getZ());
        if (dynamo.checkpoint.shouldSync(false, level.getGameTime(), phase, 20)) {
            dynamo.syncToClient();
            dynamo.checkpoint.synced();
        }
    }

    @Override public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        if (front == null || side == null) {
            return false;
        }
        return (type == EnergyType.KINETIC && side == front.getOpposite())
                || (type == EnergyType.ELECTRIC && side == front);
    }

    @Override public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.KINETIC || !handles(type, side)) {
            return 0L;
        }
        long accepted = energy.insertKinetic(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            markMutation();
        }
        return accepted;
    }

    @Override public long outputSize(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC
                        && handles(type, side)
                        && energy.electricStored() >= BronzeDynamoEnergy.PACKET_SIZE
                        && outputBudget.claim(gameTime(), 1L, 1L, true) > 0L
                ? BronzeDynamoEnergy.PACKET_SIZE
                : 0L;
    }

    @Override public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.ELECTRIC
                || !handles(type, side)
                || size != BronzeDynamoEnergy.PACKET_SIZE
                || maxAmount <= 0L) {
            return 0L;
        }
        long permitted = outputBudget.claim(gameTime(), 1L, maxAmount, true);
        long available = energy.extractElectric(size, permitted, true);
        boolean effectiveSimulation = simulate || level == null || level.isClientSide;
        if (effectiveSimulation || available <= 0L) {
            return available;
        }
        long claimed = outputBudget.claim(gameTime(), 1L, available, false);
        if (claimed != available) {
            throw new IllegalStateException("Dynamo output budget changed after simulation");
        }
        long extracted = energy.extractElectric(size, available, false);
        if (extracted != available) {
            throw new IllegalStateException("Dynamo electric buffer changed after simulation");
        }
        markMutation();
        return extracted;
    }

    @Override public long stored(EnergyType type) {
        return switch (type) {
            case KINETIC -> energy.kineticStored();
            case ELECTRIC -> energy.electricStored();
            default -> 0L;
        };
    }

    @Override public long capacity(EnergyType type) {
        return type == EnergyType.KINETIC || type == EnergyType.ELECTRIC
                ? BronzeDynamoEnergy.BUFFER_CAPACITY
                : 0L;
    }

    @Override protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        BronzeDynamoEnergy.State state = energy.snapshot();
        tag.putLong("kinetic", state.kinetic());
        tag.putLong("electric", state.electric());
    }

    @Override protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.restore(new BronzeDynamoEnergy.State(
                tag.getLong("kinetic"), tag.getLong("electric")));
    }

    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return writeClientTag();
    }

    @Override public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        readClientTag(tag);
    }

    @Override public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            readClientTag(packet.getTag());
        }
    }

    @Override public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private CompoundTag writeClientTag() {
        BronzeDynamoEnergy.State state = energy.snapshot();
        CompoundTag tag = new CompoundTag();
        tag.putLong("kinetic", state.kinetic());
        tag.putLong("electric", state.electric());
        return tag;
    }

    private void readClientTag(CompoundTag tag) {
        energy.restore(new BronzeDynamoEnergy.State(
                tag.getLong("kinetic"), tag.getLong("electric")));
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
        return state.hasProperty(DynamoBlock.FACING)
                ? state.getValue(DynamoBlock.FACING)
                : null;
    }

    private void markMutation() {
        setChanged();
        checkpoint.markSyncPending();
    }

    private void syncToClient() {
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(
                    worldPosition,
                    getBlockState(),
                    getBlockState(),
                    Block.UPDATE_CLIENTS);
        }
    }
}
