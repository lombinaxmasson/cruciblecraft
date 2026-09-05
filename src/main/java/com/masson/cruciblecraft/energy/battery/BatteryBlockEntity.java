package com.masson.cruciblecraft.energy.battery;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.PerTickEnergyBudget;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Shared battery runtime: charge NBT, packet window, EU or LU only. */
public final class BatteryBlockEntity extends BlockEntity implements IEnergyHandler {
    private static final List<Direction> SIDES = List.of(Direction.values());
    private final EnergyBatteryProfile profile;
    private final BatteryEnergyStore store;
    private final PerTickEnergyBudget outputBudget = new PerTickEnergyBudget();
    private byte displayedEnergy;

    public BatteryBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.BATTERY.get(), pos, state);
        this.profile = profileOf(state);
        this.store = new BatteryEnergyStore(profile);
    }

    public EnergyBatteryProfile profile() {
        return profile;
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            BatteryBlockEntity battery) {
        EnergyEmitter.emit(
                level, pos, battery, battery.profile.energyType(), SIDES);
        battery.syncDisplayed();
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == profile.energyType() && side != null;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        if (!handles(type, side)) {
            return 0L;
        }
        long size = store.outputSize();
        if (size <= 0L) {
            return 0L;
        }
        return outputBudget.claim(gameTime(), size, 1L, true) > 0L ? size : 0L;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side)) {
            return 0L;
        }
        long accepted = store.insert(size, amount, simulate);
        if (!simulate && accepted > 0L) {
            markMutation();
        }
        return accepted;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maxAmount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || maxAmount <= 0L) {
            return 0L;
        }
        long permitted = outputBudget.claim(
                gameTime(), profile.inputSize(), maxAmount, true);
        long available = store.extract(size, permitted, true);
        boolean effectiveSimulation = simulate || level == null || level.isClientSide;
        if (effectiveSimulation || available <= 0L) {
            return available;
        }
        long claimed = outputBudget.claim(
                gameTime(), profile.inputSize(), available, false);
        if (claimed != available) {
            throw new IllegalStateException(
                    "Battery output budget changed after simulation");
        }
        long extracted = store.extract(size, available, false);
        if (extracted != available) {
            throw new IllegalStateException(
                    "Battery store changed after simulation");
        }
        markMutation();
        return extracted;
    }

    @Override
    public long stored(EnergyType type) {
        return type == profile.energyType() ? store.stored() : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == profile.energyType() ? store.capacity() : 0L;
    }

    public void readFromItem(ItemStack stack) {
        store.restore(BatteryCharge.get(stack));
        displayedEnergy = profile.displayedEnergy(store.stored());
        setChanged();
    }

    public void writeToItem(ItemStack stack) {
        BatteryCharge.set(stack, store.stored(), store.capacity());
    }

    public byte displayedEnergy() {
        return displayedEnergy;
    }

    public int lightLevel() {
        if (profile.energyType() != EnergyType.LU || displayedEnergy <= 0) {
            return 0;
        }
        return Math.min(15, 1 + (displayedEnergy * 14) / profile.displayScaleMax());
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.energy", store.stored());
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        store.restore(tag.getLong("gt.energy"));
        displayedEnergy = profile.displayedEnergy(store.stored());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putByte("displayed", displayedEnergy);
        tag.putLong("gt.energy", store.stored());
        return tag;
    }

    @Override
    public void handleUpdateTag(
            CompoundTag tag, HolderLookup.Provider registries) {
        store.restore(tag.getLong("gt.energy"));
        displayedEnergy = tag.getByte("displayed");
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

    private void syncDisplayed() {
        byte next = profile.displayedEnergy(store.stored());
        if (next == displayedEnergy || level == null || level.isClientSide) {
            return;
        }
        displayedEnergy = next;
        setChanged();
        BlockState state = getBlockState();
        level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        level.getLightEngine().checkBlock(worldPosition);
    }

    private void markMutation() {
        setChanged();
        syncDisplayed();
    }

    private long gameTime() {
        if (level == null) {
            outputBudget.reset();
            return 0L;
        }
        return level.getGameTime();
    }

    private static EnergyBatteryProfile profileOf(BlockState state) {
        if (state.getBlock() instanceof BatteryBlock battery) {
            return battery.profile();
        }
        return EnergyBatteryCatalog.require(
                BuiltInRegistries.BLOCK.getKey(state.getBlock()));
    }
}
