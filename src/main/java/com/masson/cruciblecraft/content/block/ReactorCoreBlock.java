package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.ReactorCoreBlockEntity;
import com.masson.cruciblecraft.content.item.ReactorRodItem;
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
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** GT6 1x1 / 2x2 nuclear reactor core. */
public final class ReactorCoreBlock extends Block
        implements EntityBlock, ToolInteractable {
    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    private final int slots;

    public ReactorCoreBlock(int slots, Properties properties) {
        super(properties);
        if (slots != 1 && slots != 4) {
            throw new IllegalArgumentException("Reactor cores are 1 or 4 slots");
        }
        this.slots = slots;
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.NORTH)
                        .setValue(LIT, false));
    }

    public int slots() {
        return slots;
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
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
        ItemInteractionResult tool = ToolClick.useItemOn(
                stack, level, player, hand, hit);
        if (tool.consumesAction()) {
            return tool;
        }
        if (!(level.getBlockEntity(pos) instanceof ReactorCoreBlockEntity core)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (hit.getDirection() == Direction.UP
                && stack.getItem() instanceof ReactorRodItem) {
            if (!level.isClientSide) {
                core.insertRod(slotAt(hit), stack, player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (hit.getDirection() == Direction.UP && stack.isEmpty()) {
            if (!level.isClientSide) {
                core.extractRod(slotAt(hit), player);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof ReactorCoreBlockEntity core)) {
            return ToolResult.PASS;
        }
        if (action == ToolAction.HAMMER) {
            if (!level.isClientSide) {
                core.toggleStopped();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.PINCERS) {
            Player player = context.getPlayer();
            if (player == null) {
                return ToolResult.PASS;
            }
            if (!level.isClientSide) {
                ItemStack extracted = core.extractRod(
                        slotAt(ToolClick.hit(context)), player);
                if (!extracted.isEmpty()) {
                    ToolClick.hurt(context);
                }
            }
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    private int slotAt(BlockHitResult hit) {
        if (slots == 1) {
            return 0;
        }
        boolean west = hit.getLocation().x - hit.getBlockPos().getX() < 0.5D;
        boolean north = hit.getLocation().z - hit.getBlockPos().getZ() < 0.5D;
        return west ? (north ? 0 : 1) : (north ? 2 : 3);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ReactorCoreBlockEntity(pos, state, slots);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.REACTOR_CORE.get()
                ? (world, blockPos, blockState, blockEntity) ->
                        ReactorCoreBlockEntity.serverTick(
                                world,
                                blockPos,
                                blockState,
                                (ReactorCoreBlockEntity) blockEntity)
                : null;
    }
}
