package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.LaserEngraverBlock;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Dedicated T1 Laser Engraver host. Packet window and buffer come from
 * {@link LaserEngraverBlock#t1Variant()} ({@code lu_tier_1}).
 */
public final class LaserEngraverBlockEntity
        extends ConfiguredProcessingMachineBlockEntity {
    public LaserEngraverBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LASER_ENGRAVER.get(),
                pos,
                state,
                ((LaserEngraverBlock) state.getBlock()).variant());
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LaserEngraverBlockEntity engraver) {
        ConfiguredProcessingMachineBlockEntity.serverTick(level, pos, state, engraver);
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        if (tag.contains("gt.lu") && !tag.contains("energy")) {
            tag.putLong("energy", tag.getLong("gt.lu"));
        }
        super.loadAdditional(tag, registries);
    }
}
