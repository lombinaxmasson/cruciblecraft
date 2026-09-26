package com.masson.cruciblecraft.energy.zpm;

import com.masson.cruciblecraft.energy.battery.BatteryCharge;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Placed zero-point module. It stores QU and does not emit it. */
public final class ZpmModuleBlockEntity extends BlockEntity {
    private long energy;

    public ZpmModuleBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZERO_POINT_MODULE.get(), pos, state);
    }

    public long energy() {
        return energy;
    }

    public int lightLevel() {
        return ZpmModule.displayedEnergy(energy);
    }

    public void readFromItem(ItemStack stack) {
        energy = Math.max(0L, Math.min(ZpmModule.CAPACITY, ZpmModule.charge(stack)));
        setChanged();
    }

    public void writeToItem(ItemStack stack) {
        BatteryCharge.set(stack, energy, ZpmModule.CAPACITY);
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("gt.energy", energy);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy = Math.max(0L, Math.min(ZpmModule.CAPACITY, tag.getLong("gt.energy")));
    }
}
