package com.masson.cruciblecraft.content.storage;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Pushes inventory NBT to the client so storage BERs can see contents. */
public final class StorageClientSync {
    private StorageClientSync() {}

    public static void send(BlockEntity entity) {
        Level level = entity.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        BlockState state = entity.getBlockState();
        level.sendBlockUpdated(entity.getBlockPos(), state, state, Block.UPDATE_CLIENTS);
    }
}
