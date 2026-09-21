package com.masson.cruciblecraft.energy.lightningrod;

import java.util.concurrent.CopyOnWriteArrayList;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.blockentity.MachineCoverHostBlockEntity;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * GT6 {@code MultiTileEntityLightningRod}: weather EU collector, not a
 * recipe machine.
 */
public final class LightningRodBlockEntity extends MachineCoverHostBlockEntity
        implements IEnergyHandler {
    private static final CopyOnWriteArrayList<LightningRodBlockEntity> ALL =
            new CopyOnWriteArrayList<>();

    private long energy;
    private long tickTimer;
    private int size;
    private boolean formed;

    public LightningRodBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LIGHTNING_ROD.get(), pos, state);
    }

    public boolean formed() {
        return formed;
    }

    public int size() {
        return size;
    }

    @Override
    public long energyStored() {
        return energy;
    }

    @Override
    public long energyCapacity() {
        return LightningRodLogic.CAPACITY;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LightningRodBlockEntity rod) {
        rod.tickMountedCovers();
        boolean due = LightningRodLogic.tickDue(++rod.tickTimer);
        LightningRodStructure.Check check = LightningRodStructure.check(level, pos);
        rod.formed = check.formed();
        rod.size = check.formed() ? check.size() : 0;
        boolean weather = level.isRaining() || level.isThundering();
        if (!rod.formed
                || rod.size >= LightningRodLogic.MAX_ACTIVE_SIZE
                || !due
                || !weather) {
            rod.energy = 0L;
            rod.setChanged();
            return;
        }
        if (rod.energy >= LightningRodLogic.PACKET) {
            long packets = EnergyEmitter.pushToSide(
                    level,
                    pos,
                    EnergyType.ELECTRIC,
                    LightningRodLogic.PACKET,
                    LightningRodLogic.AMPS,
                    Direction.DOWN);
            rod.energy = LightningRodLogic.emitDrain(rod.energy, packets);
        } else {
            rod.energy = 0L;
            rod.tryStrike(level);
        }
        rod.setChanged();
    }

    private void tryStrike(Level level) {
        if (!(level instanceof ServerLevel server)
                || size <= 0
                || size >= LightningRodLogic.MAX_ACTIVE_SIZE
                || !LightningRodLogic.sourceHeightHighEnough(
                        worldPosition.getY(), size)
                || !LightningRodLogic.weatherRoll(
                        level.random,
                        size,
                        level.isThundering(),
                        level.isRaining())) {
            return;
        }
        int count = 1;
        for (LightningRodBlockEntity other : ALL) {
            if (other != this
                    && other.formed
                    && other.size > 0
                    && other.level == level
                    && Math.abs(other.worldPosition.getX() - worldPosition.getX())
                            < LightningRodLogic.COMPETITION_RANGE
                    && Math.abs(other.worldPosition.getZ() - worldPosition.getZ())
                            < LightningRodLogic.COMPETITION_RANGE) {
                count++;
            }
        }
        if (!LightningRodLogic.winsCompetition(level.random, count)) {
            return;
        }
        BlockPos tip = LightningRodStructure.tip(worldPosition, size);
        int maxY = level.getMaxBuildHeight();
        for (int y = tip.getY() + 1; y < maxY; y++) {
            if (!level.getBlockState(new BlockPos(tip.getX(), y, tip.getZ())).isAir()) {
                return;
            }
        }
        LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(server);
        if (bolt != null) {
            bolt.moveTo(Vec3.atBottomCenterOf(tip));
            server.addFreshEntity(bolt);
        }
        energy = LightningRodLogic.CAPACITY;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && !ALL.contains(this)) {
            ALL.add(this);
        }
    }

    @Override
    public void setRemoved() {
        ALL.remove(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        ALL.remove(this);
        super.onChunkUnloaded();
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC
                && (side == null || side == Direction.DOWN);
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return handles(type, side) && energy >= LightningRodLogic.PACKET
                ? LightningRodLogic.PACKET
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)
                || size != LightningRodLogic.PACKET
                || energy < LightningRodLogic.PACKET) {
            return 0L;
        }
        long packets = Math.min(LightningRodLogic.AMPS, Math.min(maxAmount, energy / LightningRodLogic.PACKET));
        if (!simulate) {
            energy = LightningRodLogic.emitDrain(energy, packets);
            setChanged();
        }
        return packets;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? energy : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC ? LightningRodLogic.CAPACITY : 0L;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", energy);
        tag.putLong("tick_timer", tickTimer);
        tag.putInt("size", size);
        tag.putBoolean("formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getLong("energy");
        tickTimer = tag.getLong("tick_timer");
        size = tag.getInt("size");
        formed = tag.getBoolean("formed");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag, HolderLookup.Provider registries) {
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
}
