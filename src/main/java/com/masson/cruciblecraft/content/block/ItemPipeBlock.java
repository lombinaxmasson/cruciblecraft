package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
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

/** Material-specific item pipe with GT6 in-pipe inventory and monkey-wrench I/O. */
public final class ItemPipeBlock extends AbstractPipeBlock {
    public ItemPipeBlock(
            PipeCatalog.Entry pipe, Properties properties) {
        super(pipe, properties);
        if (pipe.kind() != PipeCatalog.Kind.ITEM) {
            throw new IllegalArgumentException(
                    "ItemPipeBlock requires item properties");
        }
    }

    @Override
    protected boolean connectsToEndpoint(
            Level level, BlockPos neighborPos, Direction neighborSide) {
        return level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                neighborPos,
                neighborSide) != null;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ItemPipeBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.ITEM_PIPE.get()
                ? (currentLevel, pos, currentState, blockEntity) ->
                        ItemPipeBlockEntity.serverTick(
                                currentLevel,
                                pos,
                                currentState,
                                (ItemPipeBlockEntity) blockEntity)
                : null;
    }
}
