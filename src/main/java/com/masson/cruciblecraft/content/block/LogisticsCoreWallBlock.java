package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.LogisticsCoreWallBlockEntity;
import com.masson.cruciblecraft.logistics.core.LogisticsCorePart;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreTagged;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/** Energy-accepting galvanized wall that forwards EU into a formed Core. */
public final class LogisticsCoreWallBlock extends Block
        implements EntityBlock, LogisticsCoreTagged {
    public LogisticsCoreWallBlock(Properties properties) {
        super(properties);
    }

    @Override
    public LogisticsCorePart corePart() {
        return LogisticsCorePart.WALL;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LogisticsCoreWallBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return null;
    }
}
