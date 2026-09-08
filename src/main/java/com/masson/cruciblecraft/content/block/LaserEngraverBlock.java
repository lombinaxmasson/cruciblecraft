package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.LaserEngraverBlockEntity;
import com.masson.cruciblecraft.machine.processing.MachineVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Source-backed LU laser engraver. Same block identity, processing host. */
public final class LaserEngraverBlock extends ProcessingMachineBlock {
    public LaserEngraverBlock(Properties properties) {
        super(MachineVariant.legacy(ModProcessingMachines.LASER_ENGRAVER), properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaserEngraverBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            net.minecraft.world.level.Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide
                && type == ModBlockEntities.LASER_ENGRAVER.get()
                ? (world, pos, blockState, blockEntity) ->
                        LaserEngraverBlockEntity.serverTick(
                                world,
                                pos,
                                blockState,
                                (LaserEngraverBlockEntity) blockEntity)
                : null;
    }
}
