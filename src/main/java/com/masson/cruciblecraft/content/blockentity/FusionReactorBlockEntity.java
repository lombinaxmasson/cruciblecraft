package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.FusionReactorBlock;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.fusion.FusionRecipeCatalog;
import com.masson.cruciblecraft.fusion.FusionStructure;
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

/**
 * Dual TU/LU fusion host. EU is emitted only while the octagon is formed.
 *
 * <p>Recipe-map execution is not required to close the energy/structure
 * contract; the 18-row map is published independently.
 */
public final class FusionReactorBlockEntity
        extends BlockEntity implements IEnergyHandler {
    public static final long TIME_MINIMUM = 1L;
    public static final long TIME_MAXIMUM = 16_384L;
    public static final long EU_PACKET = 8_192L;
    private static final long LU_CAPACITY =
            FusionRecipeCatalog.entries().getLast().luStart();

    private final MachineEnergyBuffer time =
            new MachineEnergyBuffer(TIME_MAXIMUM * 64L, TIME_MAXIMUM);
    private final MachineEnergyBuffer lu =
            new MachineEnergyBuffer(LU_CAPACITY, Long.MAX_VALUE / 4L);
    private final MachineEnergyBuffer eu =
            new MachineEnergyBuffer(EU_PACKET * 64L, EU_PACKET);
    private boolean forceFormed;
    private boolean formed;

    public FusionReactorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUSION_REACTOR.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            FusionReactorBlockEntity reactor) {
        if (!reactor.forceFormed) {
            reactor.formed = FusionStructure.check(
                    level,
                    pos,
                    state.getValue(FusionReactorBlock.FACING));
        }
        reactor.syncIfNeeded();
    }

    public void forceFormedForTest() {
        forceFormed = true;
        formed = true;
        setChanged();
    }

    public boolean formed() {
        return formed || forceFormed;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        if (!formed()) {
            return false;
        }
        return type == EnergyType.TIME
                || type == EnergyType.LU
                || type == EnergyType.ELECTRIC;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!formed()) {
            return 0L;
        }
        long accepted = switch (type) {
            case TIME -> size < TIME_MINIMUM || size > TIME_MAXIMUM
                    ? 0L
                    : time.insert(size, amount, simulate);
            case LU -> lu.insert(size, amount, simulate);
            default -> 0L;
        };
        if (!simulate && accepted > 0L) {
            setChanged();
        }
        return accepted;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (!formed() || type != EnergyType.ELECTRIC) {
            return 0L;
        }
        return eu.stored() >= EU_PACKET ? EU_PACKET : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!formed()
                || type != EnergyType.ELECTRIC
                || size != EU_PACKET
                || maxAmount <= 0L) {
            return 0L;
        }
        long packets = Math.min(
                maxAmount, EnergyPackets.packetsForUnits(size, eu.stored()));
        if (!simulate && packets > 0L && eu.consume(EnergyPackets.units(size, packets))) {
            setChanged();
        }
        return packets;
    }

    @Override
    public long stored(EnergyType type) {
        return switch (type) {
            case TIME -> time.stored();
            case LU -> lu.stored();
            case ELECTRIC -> eu.stored();
            default -> 0L;
        };
    }

    @Override
    public long capacity(EnergyType type) {
        return switch (type) {
            case TIME -> time.capacity();
            case LU -> lu.capacity();
            case ELECTRIC -> eu.capacity();
            default -> 0L;
        };
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.tu", time.stored());
        tag.putLong("gt.lu", lu.stored());
        tag.putLong("gt.eu", eu.stored());
        tag.putBoolean("gt.formed", formed);
        tag.putBoolean("gt.force_formed", forceFormed);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        time.restore(tag.getLong("gt.tu"));
        lu.restore(tag.getLong("gt.lu"));
        eu.restore(tag.getLong("gt.eu"));
        formed = tag.getBoolean("gt.formed");
        forceFormed = tag.getBoolean("gt.force_formed");
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
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet,
            HolderLookup.Provider registries) {
        if (packet.getTag() != null) {
            handleUpdateTag(packet.getTag(), registries);
        }
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void syncIfNeeded() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                Block.UPDATE_CLIENTS);
    }
}
