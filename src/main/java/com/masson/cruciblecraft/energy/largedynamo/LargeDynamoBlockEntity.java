package com.masson.cruciblecraft.energy.largedynamo;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.blockentity.MachineCoverHostBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.steam.SteamTurbinePresentation;
import com.masson.cruciblecraft.energy.steam.SteamTurbineStructure;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Level.ExplosionInteraction;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityLargeDynamo}: RU in on the facing, EU out of the
 * far wall, waste {@code inputMax} each tick.
 */
public final class LargeDynamoBlockEntity extends MachineCoverHostBlockEntity
        implements IEnergyHandler {
    private final LargeDynamoCatalog.Profile profile;
    private final Set<BlockPos> boundHatches = new LinkedHashSet<>();
    private long storedRu;
    private long euPacket;
    private long tickTimer;
    private int overloadPrevention;
    private boolean overcharged;
    private boolean stopped;
    private boolean formed;

    public LargeDynamoBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.LARGE_DYNAMO.get(), pos, state);
        this.profile = LargeDynamoCatalog.require(specOf(state).id());
    }

    @Override
    public long energyStored() {
        return storedRu;
    }

    @Override
    public long energyCapacity() {
        return profile.capacity();
    }

    public LargeDynamoCatalog.Profile profile() {
        return profile;
    }

    public boolean formed() {
        return formed;
    }

    public boolean overcharged() {
        return overcharged;
    }

    public boolean getStateOnOff() {
        return !stopped;
    }

    public boolean setStateOnOff(boolean on) {
        stopped = !on;
        setChanged();
        return !stopped;
    }

    private static MteInPlaceSpec specOf(BlockState state) {
        return ((MteInPlaceBlock) state.getBlock()).spec();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LargeDynamoBlockEntity dynamo) {
        dynamo.tickMountedCovers();
        long timer = ++dynamo.tickTimer;
        Direction facing = state.getValue(MteInPlaceBlock.FACING);
        if (!SteamTurbineStructure.aabbLoaded(level, pos, facing)) {
            if (dynamo.formed) {
                dynamo.convert(facing);
            }
            return;
        }
        boolean ok = LargeDynamoStructure.check(
                level, pos, facing, dynamo.profile.wallId());
        if (ok) {
            dynamo.formed = true;
            dynamo.bindHatch(facing);
            dynamo.convert(facing);
        } else if (dynamo.formed) {
            dynamo.formed = false;
            dynamo.euPacket = 0L;
            dynamo.unbindHatch();
        } else {
            dynamo.unbindHatch();
        }
        if (timer % 600L == 5L
                && dynamo.euPacket == 0L
                && dynamo.overloadPrevention > 0) {
            dynamo.overloadPrevention--;
        }
    }

    @Override
    public void setRemoved() {
        unbindHatch();
        super.setRemoved();
    }

    private void bindHatch(Direction facing) {
        if (level == null) {
            return;
        }
        BlockPos far = SteamTurbineStructure.energyOut(worldPosition, facing);
        if (level.hasChunkAt(far)
                && level.getBlockEntity(far) instanceof MteInPlaceBlockEntity wall) {
            wall.bindLargeDynamo(worldPosition, facing.getOpposite());
            boundHatches.add(far.immutable());
        }
        for (BlockPos previous : List.copyOf(boundHatches)) {
            if (!previous.equals(far)) {
                unbindOne(previous);
            }
        }
    }

    private void unbindHatch() {
        for (BlockPos previous : List.copyOf(boundHatches)) {
            unbindOne(previous);
        }
    }

    private void unbindOne(BlockPos pos) {
        boundHatches.remove(pos);
        if (level != null
                && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity wall) {
            wall.unbindLargeDynamo();
        }
    }

    private void convert(Direction facing) {
        if (level == null || level.isClientSide) {
            return;
        }
        LargeDynamoConversion.Tick tick =
                LargeDynamoConversion.emit(storedRu, profile);
        if (tick.overloaded()) {
            handleOverload(storedRu);
            return;
        }
        euPacket = tick.packetSize();
        if (tick.canEmit()) {
            long pushed = EnergyEmitter.pushToSide(
                    level,
                    SteamTurbineStructure.energyOut(worldPosition, facing),
                    EnergyType.ELECTRIC,
                    tick.packetSize(),
                    1L,
                    facing.getOpposite());
            // A directly accepted packet is consumed by the GT6-style push.
            // Keep it only when the modern pull bridge still needs to expose it.
            if (pushed > 0L) {
                euPacket = 0L;
            }
        }
        storedRu = tick.storedAfterWaste();
        setChanged();
    }

    public long hatchOutputSize(EnergyType type, Direction side) {
        if (type != EnergyType.ELECTRIC || !formed || euPacket <= 0L) {
            return 0L;
        }
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        if (side != null && side != facing.getOpposite()) {
            return 0L;
        }
        return euPacket;
    }

    public long hatchExtract(
            EnergyType type,
            long size,
            long maxAmount,
            boolean simulate) {
        if (type != EnergyType.ELECTRIC || euPacket <= 0L || size != euPacket) {
            return 0L;
        }
        long packets = Math.min(1L, maxAmount);
        if (!simulate && packets > 0L) {
            euPacket = 0L;
            setChanged();
        }
        return packets;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        Direction facing = getBlockState().getValue(MteInPlaceBlock.FACING);
        return type == EnergyType.KINETIC_ROTATION
                && !stopped
                && (side == null || side == facing);
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || amount <= 0L) {
            return 0L;
        }
        long magnitude = Math.abs(size);
        if (magnitude == 0L) {
            return 0L;
        }
        if (magnitude > profile.inputMax()) {
            if (!simulate) {
                handleOverload(magnitude);
            }
            // GT6 consumes an oversize packet before triggering overload.
            return amount;
        }
        long units = magnitude * amount;
        long room = Math.max(0L, profile.capacity() - storedRu);
        long accepted = Math.min(units, room);
        if (!simulate && accepted > 0L) {
            storedRu += accepted;
            setChanged();
        }
        return accepted <= 0L
                ? 0L
                : Math.min(
                        amount,
                        accepted / magnitude
                                + (accepted % magnitude == 0L ? 0L : 1L));
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.KINETIC_ROTATION ? storedRu : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.KINETIC_ROTATION ? profile.capacity() : 0L;
    }

    private void handleOverload(long size) {
        storedRu = 0L;
        euPacket = 0L;
        if (overloadPrevention < SteamTurbinePresentation.OVERLOAD_EXPLOSION_THRESHOLD) {
            overloadPrevention++;
            setChanged();
            return;
        }
        overcharged = true;
        if (level != null) {
            BlockPos pos = worldPosition;
            level.removeBlock(pos, false);
            if (level instanceof ServerLevel server) {
                server.explode(
                        null,
                        pos.getX() + 0.5,
                        pos.getY() + 0.5,
                        pos.getZ() + 0.5,
                        SteamTurbinePresentation.overchargeExplosionStrength(size),
                        ExplosionInteraction.TNT);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("stored_ru", storedRu);
        tag.putLong("tick_timer", tickTimer);
        tag.putBoolean("formed", formed);
        tag.putLong("eu_packet", euPacket);
        tag.putInt("overload_prevention", overloadPrevention);
        tag.putBoolean("overcharged", overcharged);
        tag.putBoolean("stopped", stopped);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        storedRu = tag.getLong("stored_ru");
        tickTimer = tag.getLong("tick_timer");
        formed = tag.getBoolean("formed");
        euPacket = tag.getLong("eu_packet");
        overloadPrevention = tag.getInt("overload_prevention");
        overcharged = tag.getBoolean("overcharged");
        stopped = tag.getBoolean("stopped");
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
