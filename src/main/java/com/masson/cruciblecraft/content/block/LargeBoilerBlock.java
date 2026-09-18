package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/** Facing/ticker shell for the JSON large-boiler conversion controller. */
public final class LargeBoilerBlock extends Block
        implements EntityBlock, ToolInteractable {
    public LargeBoilerBlock(Properties properties) {
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
                        instanceof LargeBoilerBlockEntity boiler) {
            boiler.clearBindings();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action == ToolAction.PLUNGER
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof LargeBoilerBlockEntity boiler) {
            return ToolClick.plunger(context, boiler.trashWithPlunger());
        }
        return ToolResult.PASS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            net.minecraft.world.phys.BlockHitResult hit) {
        return ToolClick.useItemOn(stack, level, player, hand, hit);
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(ProcessingMachineBlock.FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LargeBoilerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.LARGE_BOILER.get()
                ? (l, p, s, be) -> LargeBoilerBlockEntity.serverTick(
                        l, p, s, (LargeBoilerBlockEntity) be)
                : null;
    }
}
