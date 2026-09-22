package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.TankBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Legacy save-compatible tombstone for {@code tank_3x3x3}.
 *
 * <p>New placement and structure matching use Tank Main Valve MTEs.  This
 * registration remains only so old block states do not become an unknown
 * block during migration.</p>
 */
public final class TankBlock extends Block implements EntityBlock {
    public TankBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(
                ProcessingMachineBlock.FACING, Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                ProcessingMachineBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (state.getBlock() != next.getBlock()
                && level.getBlockEntity(pos)
                        instanceof TankBlockEntity tank) {
            tank.clearBindings();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ProcessingMachineBlock.FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return null;
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
