package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.DustFunnelBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Steel-only dust funnel. Not a HopperKind. */
public final class DustFunnelBlock extends Block
        implements EntityBlock, ToolInteractable {
    private static final VoxelShape SHAPE = Shapes.or(
            Block.box(0.0, 8.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0),
            Block.box(0.0, 0.0, 0.0, 2.0, 8.0, 2.0),
            Block.box(0.0, 0.0, 14.0, 2.0, 8.0, 16.0),
            Block.box(14.0, 0.0, 0.0, 16.0, 8.0, 2.0),
            Block.box(14.0, 0.0, 14.0, 16.0, 8.0, 16.0));

    public DustFunnelBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof DustFunnelBlockEntity funnel)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        ItemInteractionResult tool = ToolClick.useItemOn(
                stack, level, player, hand, hit);
        if (tool.consumesAction()) {
            return tool;
        }
        if (hit.getDirection() == Direction.UP && funnel.insertFromHand(stack)) {
            return ItemInteractionResult.SUCCESS;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (action != ToolAction.WRENCH) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof DustFunnelBlockEntity funnel)) {
            return ToolResult.PASS;
        }
        if (!level.isClientSide) {
            funnel.cycleMode(
                    context.getPlayer() != null
                            && context.getPlayer().isShiftKeyDown());
            if (context.getPlayer() != null) {
                context.getPlayer().displayClientMessage(
                        funnel.statusMessage(), true);
            }
            ToolClick.hurt(context);
        }
        return ToolResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        return InteractionResult.PASS;
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof DustFunnelBlockEntity funnel) {
            funnel.readFromItem(stack);
        }
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state) {
        ItemStack stack = new ItemStack(this);
        if (level.getBlockEntity(pos) instanceof DustFunnelBlockEntity funnel) {
            funnel.writeToItem(stack);
        }
        return stack;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = new java.util.ArrayList<>(super.getDrops(state, params));
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                instanceof DustFunnelBlockEntity funnel) {
            ItemStack blockItem = drops.isEmpty()
                    ? new ItemStack(this)
                    : drops.getFirst();
            if (drops.isEmpty()) {
                drops.add(blockItem);
            }
            funnel.exportToDrops(drops, blockItem);
        }
        return drops;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new DustFunnelBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.DUST_FUNNEL.get()
                ? (current, pos, currentState, blockEntity) ->
                        DustFunnelBlockEntity.serverTick(
                                current,
                                pos,
                                currentState,
                                (DustFunnelBlockEntity) blockEntity)
                : null;
    }
}
