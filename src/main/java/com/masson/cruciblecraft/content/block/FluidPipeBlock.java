package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Material-specific fluid pipe with per-segment storage. */
public final class FluidPipeBlock extends AbstractPipeBlock {
    public FluidPipeBlock(
            PipeCatalog.Entry pipe, Properties properties) {
        super(pipe, properties);
        if (pipe.kind() != PipeCatalog.Kind.FLUID) {
            throw new IllegalArgumentException(
                    "FluidPipeBlock requires fluid properties");
        }
    }

    @Override
    protected boolean connectsToEndpoint(
            Level level, BlockPos neighborPos, Direction neighborSide) {
        return level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                neighborPos,
                neighborSide) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FluidPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.FLUID_PIPE.get()
                ? (currentLevel, pos, currentState, blockEntity) ->
                        FluidPipeBlockEntity.serverTick(
                                currentLevel,
                                pos,
                                currentState,
                                (FluidPipeBlockEntity) blockEntity)
                : null;
    }
}
