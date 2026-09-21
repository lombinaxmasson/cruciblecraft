package com.masson.cruciblecraft.content.blockentity;

import java.util.Optional;

import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverSounds;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverItems;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverSet;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Shared remainder-cover inventory for energy converters that GT6 hosts
 * covers on. Controller covers drive {@link #setStateOnOff(boolean)} only
 * while present so soft-hammer / vent-stop still work without one.
 */
public abstract class MachineCoverHostBlockEntity extends BlockEntity
        implements MachineCoverHost {
    private final PipeCoverSet covers = new PipeCoverSet();
    private boolean coverEnabled = true;
    private boolean coversStopped;
    private int selectorMode;

    protected MachineCoverHostBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    protected final void tickCovers() {
        if (level != null && !level.isClientSide) {
            MachineCoverBehaviors.tickAll(this);
        }
    }

    public final void tickMountedCovers() {
        tickCovers();
    }

    protected void onHostChanged() {
        setChanged();
    }

    protected final void saveCoverNbt(
            CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("machine_cover_enabled", coverEnabled);
        tag.putBoolean("machine_covers_stopped", coversStopped);
        tag.putInt("machine_selector_mode", selectorMode);
        CompoundTag coverTag = new CompoundTag();
        covers.save(coverTag, registries);
        tag.put("machine_covers", coverTag);
    }

    protected final void loadCoverNbt(
            CompoundTag tag, HolderLookup.Provider registries) {
        coverEnabled = !tag.contains("machine_cover_enabled")
                || tag.getBoolean("machine_cover_enabled");
        coversStopped = tag.getBoolean("machine_covers_stopped");
        selectorMode = Math.max(
                0, Math.min(15, tag.getInt("machine_selector_mode")));
        if (tag.contains("machine_covers")) {
            covers.load(tag.getCompound("machine_covers"), registries);
        }
    }

    private void syncCoverUpdate() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.sendBlockUpdated(
                worldPosition,
                getBlockState(),
                getBlockState(),
                Block.UPDATE_CLIENTS);
        notifyRedstone();
    }

    private void markCoverMutation() {
        onHostChanged();
        syncCoverUpdate();
    }

    @Override
    public final PipeCoverSet covers() {
        return covers;
    }

    @Override
    public final boolean setCover(Direction side, PipeCover cover) {
        if (side == null
                || cover == null
                || !MachineCoverBehaviors.canPlace(this, side, cover)) {
            return false;
        }
        boolean changed = covers.set(side, cover);
        if (changed) {
            markCoverMutation();
        }
        return changed;
    }

    @Override
    public final void replaceCover(Direction side, PipeCover cover) {
        if (side == null) {
            return;
        }
        if (covers.set(side, cover)) {
            markCoverMutation();
        }
    }

    @Override
    public final boolean configureCover(
            Direction side,
            CoverDefinition.ConfigField field,
            int value) {
        if (side == null || field == null) {
            return false;
        }
        if (!covers.configure(side, field, value)) {
            return false;
        }
        markCoverMutation();
        return true;
    }

    @Override
    public final boolean removeCover(Direction side, Player player) {
        if (side == null) {
            return false;
        }
        Optional<PipeCover> taken = covers.take(side);
        if (taken.isEmpty()) {
            return false;
        }
        PipeCover removed = taken.orElseThrow();
        ItemStack stack = PipeCoverItems.stackFor(removed);
        if (!stack.isEmpty()) {
            if (player == null || !player.addItem(stack)) {
                if (level != null && !level.isClientSide) {
                    Block.popResource(level, worldPosition, stack);
                }
            }
        }
        CoverSounds.removed(level, worldPosition, removed);
        if (switchableOnOff()
                && MachineCoverBehaviors.isControllerCover(removed)) {
            setStateOnOff(true);
        }
        markCoverMutation();
        return true;
    }

    @Override
    public final void dropCovers() {
        if (level == null || level.isClientSide) {
            return;
        }
        boolean hadController = false;
        for (Direction side : Direction.values()) {
            PipeCover cover = covers.get(side).orElse(null);
            if (MachineCoverBehaviors.isControllerCover(cover)) {
                hadController = true;
                break;
            }
        }
        for (ItemStack stack : covers.removeAllAsItems()) {
            Block.popResource(level, worldPosition, stack);
        }
        if (hadController && switchableOnOff()) {
            setStateOnOff(true);
        }
        markCoverMutation();
    }

    @Override
    public final boolean coverEnabled() {
        return coverEnabled;
    }

    @Override
    public final void setCoverEnabled(boolean enabled) {
        if (coverEnabled != enabled) {
            coverEnabled = enabled;
            markCoverMutation();
        }
    }

    @Override
    public final boolean coversStopped() {
        return coversStopped;
    }

    @Override
    public final void setCoversStopped(boolean stopped) {
        if (coversStopped != stopped) {
            coversStopped = stopped;
            onHostChanged();
        }
    }

    @Override
    public final boolean canTick() {
        return level != null && !level.isClientSide;
    }

    @Override
    public boolean runningPossible() {
        return true;
    }

    @Override
    public boolean runningPassively() {
        return runningActively();
    }

    @Override
    public boolean runningActively() {
        return false;
    }

    @Override
    public boolean runningSuccessfully() {
        return false;
    }

    @Override
    public final int selectorMode() {
        return selectorMode;
    }

    @Override
    public final void setSelectorMode(int mode) {
        int bounded = Math.max(0, Math.min(15, mode));
        if (selectorMode != bounded) {
            selectorMode = bounded;
            onHostChanged();
        }
    }

    @Override
    public boolean hasEnergyBuffer() {
        return true;
    }

    @Override
    public long energyStored() {
        return 0L;
    }

    @Override
    public long energyCapacity() {
        return 0L;
    }

    @Override
    public int progress() {
        return 0;
    }

    @Override
    public int duration() {
        return 0;
    }

    @Override
    public boolean hasFluidTanks() {
        return false;
    }

    @Override
    public int fillAir(int amount) {
        return 0;
    }

    @Override
    public final int incomingRedstone(Direction side) {
        if (level == null || side == null) {
            return 0;
        }
        BlockPos neighbor = worldPosition.relative(side);
        return Math.max(
                level.getSignal(neighbor, side.getOpposite()),
                level.getDirectSignal(neighbor, side.getOpposite()));
    }

    @Override
    public final void notifyRedstone() {
        if (level == null || level.isClientSide) {
            return;
        }
        level.updateNeighborsAt(worldPosition, getBlockState().getBlock());
        for (Direction side : Direction.values()) {
            level.updateNeighborsAt(
                    worldPosition.relative(side),
                    getBlockState().getBlock());
        }
    }

    @Override
    public final long gameTime() {
        return level == null ? 0L : level.getGameTime();
    }

    @Override
    public final Level level() {
        return level;
    }

    @Override
    public final BlockPos hostPos() {
        return worldPosition;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        saveCoverNbt(tag, registries);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        loadCoverNbt(tag, registries);
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveCoverNbt(tag, registries);
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        loadCoverNbt(tag, registries);
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

    protected static boolean alongFacingAxis(
            Direction front, Direction side) {
        return front != null
                && side != null
                && front.getAxis() == side.getAxis();
    }
}
