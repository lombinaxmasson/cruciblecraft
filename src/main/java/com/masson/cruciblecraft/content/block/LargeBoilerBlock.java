package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

/**
 * Legacy named-controller shell. New worlds use the five source-backed
 * {@code boiler_main_barometer} MTE controllers; this block only survives
 * long enough for {@link LargeBoilerBlockEntity} to migrate old saves.
 */
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
        if (state.getBlock() != next.getBlock()) {
            MachineCoverBlockInteraction.dropCovers(level, pos);
            if (level.getBlockEntity(pos)
                    instanceof LargeBoilerBlockEntity boiler) {
                boiler.clearBindings();
            }
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player) {
        if (level.getBlockEntity(pos)
                instanceof LargeBoilerBlockEntity boiler) {
            boiler.removedByPlayer(player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                        instanceof LargeBoilerBlockEntity boiler) {
            boiler.onExploded();
        }
        super.wasExploded(level, pos, explosion);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof MachineCoverHost machine) {
            ToolResult coverResult = MachineCoverBlockInteraction.useTool(
                    machine, action, context);
            if (coverResult != ToolResult.PASS) {
                return coverResult;
            }
        }
        if (action == ToolAction.PLUNGER
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof LargeBoilerBlockEntity boiler) {
            return ToolClick.plunger(context, boiler.trashWithPlunger());
        }
        if (action == ToolAction.CHISEL
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof LargeBoilerBlockEntity boiler) {
            if (context.getLevel().isClientSide) {
                return ToolResult.SUCCESS;
            }
            if (boiler.decalcify(context.getPlayer())) {
                ToolClick.hurt(context);
                return ToolResult.SUCCESS;
            }
            return ToolResult.PASS;
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
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            net.minecraft.world.phys.BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MachineCoverHost machine
                && MachineCoverBlockInteraction.rightClick(
                        machine, level, pos, player, hit)) {
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected int getSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return MachineCoverBlockInteraction.weakRedstone(level, pos, direction);
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return MachineCoverBlockInteraction.directRedstone(level, pos, direction);
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        return MachineCoverBlockInteraction.canConnectRedstone(
                level, pos, direction);
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
