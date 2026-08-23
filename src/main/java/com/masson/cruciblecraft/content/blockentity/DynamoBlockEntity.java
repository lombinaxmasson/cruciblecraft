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

/** Source-10111 bronze dynamo: RU back input, waste-policy EU front output. */
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
        Direction front = state.getValue(DynamoBlock.FACING);
        long delivered = EnergyEmitter.emit(
                level, pos, dynamo, EnergyType.ELECTRIC, front);
        if (delivered == 0L && dynamo.energy.wasteBlockedInput()) {
            dynamo.markMutation();
        }
        dynamo.updateLitState();
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
        return (type == EnergyType.KINETIC_ROTATION
                        && side == front.getOpposite())
                || (type == EnergyType.ELECTRIC && side == front);
    }

    @Override public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.KINETIC_ROTATION
                || !handles(type, side)) {
            return 0L;
        }
        long accepted = energy.insertKinetic(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            markMutation();
            updateLitState();
        }
        return accepted;
    }

    @Override public long outputSize(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC
                        && handles(type, side)
                        && energy.outputSize() > 0L
                        && outputBudget.claim(gameTime(), 1L, 1L, true) > 0L
                ? energy.outputSize()
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
                || size != energy.outputSize()
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
        updateLitState();
        return extracted;
    }

    @Override public long stored(EnergyType type) {
        return switch (type) {
            case KINETIC_ROTATION -> energy.kineticStored();
            case ELECTRIC -> 0L;
            default -> 0L;
        };
    }

    @Override public long capacity(EnergyType type) {
        return type == EnergyType.KINETIC_ROTATION
                ? BronzeDynamoEnergy.BUFFER_CAPACITY
                : 0L;
    }

    public long kineticConsumed() {
        return energy.kineticConsumed();
    }

    public long electricExtracted() {
        return energy.electricExtracted();
    }

    public long conversionLoss() {
        return energy.conversionLoss();
    }

    public boolean overloaded() {
        return energy.overloaded();
    }

    @Override protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        BronzeDynamoEnergy.State state = energy.snapshot();
        tag.putLong("kinetic", state.kinetic());
        tag.putLong("kinetic_consumed", state.kineticConsumed());
        tag.putLong("electric_extracted", state.electricExtracted());
        tag.putLong("conversion_loss", state.conversionLoss());
        tag.putBoolean("overloaded", state.overloaded());
    }

    @Override protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.restore(new BronzeDynamoEnergy.State(
                tag.getLong("kinetic"),
                tag.getLong("kinetic_consumed"),
                tag.getLong("electric_extracted"),
                tag.getLong("conversion_loss"),
                tag.getBoolean("overloaded")));
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
        tag.putLong("kinetic_consumed", state.kineticConsumed());
        tag.putLong("electric_extracted", state.electricExtracted());
        tag.putLong("conversion_loss", state.conversionLoss());
        tag.putBoolean("overloaded", state.overloaded());
        return tag;
    }

    private void readClientTag(CompoundTag tag) {
        energy.restore(new BronzeDynamoEnergy.State(
                tag.getLong("kinetic"),
                tag.getLong("kinetic_consumed"),
                tag.getLong("electric_extracted"),
                tag.getLong("conversion_loss"),
                tag.getBoolean("overloaded")));
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

    private void updateLitState() {
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = getBlockState();
        boolean lit = energy.kineticStored() > 0L;
        if (state.hasProperty(DynamoBlock.LIT)
                && state.getValue(DynamoBlock.LIT) != lit) {
            level.setBlock(
                    worldPosition,
                    state.setValue(DynamoBlock.LIT, lit),
                    Block.UPDATE_CLIENTS);
        }
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
