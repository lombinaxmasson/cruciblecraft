package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.blockentity.StorageInserterBlockEntity;
import com.masson.cruciblecraft.content.storage.StorageVariant;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class StorageInserterBlock extends StorageHostBlock {
    public StorageInserterBlock(StorageVariant variant, Properties properties) {
        super(variant, properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new StorageInserterBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof StorageInserterBlockEntity inserter) {
            inserter.insertFromPlayer(player);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
