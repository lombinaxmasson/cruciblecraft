package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.content.blockentity.SubsurfaceFluidDepositBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * Hidden reserve marker rendered as its stone or deepslate host.
 *
 * <p>No item is registered for this block. Extraction equipment reads
 * the persistent block entity instead of mining the marker.</p>
 */
public final class SubsurfaceFluidDepositBlock extends Block
        implements EntityBlock {
    public static final BooleanProperty DEEPSLATE =
            BooleanProperty.create("deepslate");

    public SubsurfaceFluidDepositBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(DEEPSLATE, false));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(DEEPSLATE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new SubsurfaceFluidDepositBlockEntity(pos, state);
    }
}
