package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * LU consumer on the upper face. Packet window is 16–64 LU with a 2048 buffer.
 */
public final class LaserEngraverBlockEntity
        extends ConfiguredProcessingMachineBlockEntity {
    public static final long INPUT_PACKET = 32L;
    public static final long INPUT_MINIMUM = 16L;
    public static final long INPUT_MAXIMUM = 64L;
    public static final long ENERGY_CAPACITY = 2_048L;

    public LaserEngraverBlockEntity(BlockPos pos, BlockState state) {
        super(
                ModBlockEntities.LASER_ENGRAVER.get(),
                pos,
                state,
                MachineVariant.legacy(ModProcessingMachines.LASER_ENGRAVER));
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            LaserEngraverBlockEntity engraver) {
        ConfiguredProcessingMachineBlockEntity.serverTick(level, pos, state, engraver);
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        return type == EnergyType.LU && side == Direction.UP;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (type != EnergyType.LU
                || side != Direction.UP
                || size < INPUT_MINIMUM
                || size > INPUT_MAXIMUM) {
            return 0L;
        }
        return super.insert(type, size, amount, side, simulate);
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
