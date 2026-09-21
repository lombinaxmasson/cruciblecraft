package com.masson.cruciblecraft.energy.vondagraagg;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.StoneLayerStoneBlock;
import com.masson.cruciblecraft.content.blockentity.MachineCoverHostBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.worldgen.StoneLayerStones;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityVonDaGraagg}: EU in, square spawn-inhibition
 * range {@code bind8(min(energy,4096)/16)}, except mossy cobble.
 */
public final class VonDaGraaggBlockEntity extends MachineCoverHostBlockEntity
        implements IEnergyHandler {
    private static final CopyOnWriteArrayList<VonDaGraaggBlockEntity> ALL =
            new CopyOnWriteArrayList<>();
    private final Set<BlockPos> boundWalls = new LinkedHashSet<>();
    private long energy;
    private int range;
    private boolean formed;

    public VonDaGraaggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.VON_DA_GRAAGG.get(), pos, state);
    }

    public boolean formed() {
        return formed;
    }

    public int range() {
        return range;
    }

    @Override
    public long energyStored() {
        return energy;
    }

    @Override
    public long energyCapacity() {
        return VonDaGraaggLogic.CAPACITY;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            VonDaGraaggBlockEntity graagg) {
        graagg.tickMountedCovers();
        VonDaGraaggStructure.Check check = VonDaGraaggStructure.check(level, pos);
        if (check.formed()) {
            graagg.formed = true;
            graagg.bindWalls(check);
        } else {
            graagg.formed = false;
            graagg.unbindWalls();
        }
        graagg.range = VonDaGraaggLogic.range(graagg.energy, graagg.formed);
        graagg.energy = VonDaGraaggLogic.afterDrain(graagg.energy);
        graagg.setChanged();
    }

    public boolean inhibits(Level level, BlockPos spawn) {
        if (!formed || range <= 0 || level != this.level) {
            return false;
        }
        if (Math.abs(spawn.getX() - worldPosition.getX()) > range
                || Math.abs(spawn.getZ() - worldPosition.getZ()) > range) {
            return false;
        }
        for (int i = -5; i <= 5; i++) {
            var state = level.getBlockState(spawn.offset(0, i, 0));
            if (state.is(Blocks.MOSSY_COBBLESTONE)
                    || (state.getBlock() instanceof StoneLayerStoneBlock stone
                            && stone.role() == StoneLayerStones.Role.MOSSY_COBBLE)) {
                return false;
            }
        }
        return true;
    }

    public static Iterable<VonDaGraaggBlockEntity> all() {
        return ALL;
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
        unbindWalls();
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        ALL.remove(this);
        super.onChunkUnloaded();
    }

    private void bindWalls(VonDaGraaggStructure.Check check) {
        if (level == null) {
            return;
        }
        Set<BlockPos> desired = new LinkedHashSet<>();
        for (VonDaGraaggStructure.Port port : check.energyIn()) {
            desired.add(port.pos());
            if (level.hasChunkAt(port.pos())
                    && level.getBlockEntity(port.pos())
                            instanceof MteInPlaceBlockEntity wall) {
                wall.bind(
                        worldPosition,
                        VonDaGraaggStructure.STRUCTURE_ID,
                        port.type());
                boundWalls.add(port.pos().immutable());
            }
        }
        for (BlockPos previous : List.copyOf(boundWalls)) {
            if (!desired.contains(previous)) {
                unbindOne(previous);
            }
        }
    }

    private void unbindWalls() {
        for (BlockPos previous : List.copyOf(boundWalls)) {
            unbindOne(previous);
        }
    }

    private void unbindOne(BlockPos pos) {
        boundWalls.remove(pos);
        if (level != null
                && level.hasChunkAt(pos)
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity wall) {
            wall.unbind(worldPosition);
        }
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.ELECTRIC || amount <= 0L) {
            return 0L;
        }
        // GT6's custom doInject reports the whole amperage and stores the
        // packet without a separate capacity clamp; range calculation clamps
        // the useful value back to 4096.
        if (!simulate) {
            energy += Math.abs(size) * amount;
            setChanged();
        }
        return amount;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.ELECTRIC ? energy : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC ? VonDaGraaggLogic.CAPACITY : 0L;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("energy", energy);
        tag.putInt("range", range);
        tag.putBoolean("formed", formed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = tag.getLong("energy");
        range = tag.getInt("range");
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
