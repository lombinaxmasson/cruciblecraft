package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import org.jetbrains.annotations.Nullable;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

public final class CokeOvenBlock extends Block implements EntityBlock, ToolInteractable {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CokeOvenBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult) {
        if (!level.isClientSide
                && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CokeOvenBlockEntity cokeOven) {
            serverPlayer.openMenu(cokeOven);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected ItemInteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        ItemInteractionResult tool = ToolClick.useItemOn(
                stack, level, player, hand, hitResult);
        if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            return tool;
        }
        if (stack.is(Items.FLINT_AND_STEEL)) {
            if (!level.isClientSide
                    && level.getBlockEntity(pos) instanceof CokeOvenBlockEntity cokeOven
                    && cokeOven.ignite()) {
                if (!player.getAbilities().instabuild) {
                    stack.hurtAndBreak(
                            1,
                            player,
                            hand == InteractionHand.MAIN_HAND
                                    ? EquipmentSlot.MAINHAND
                                    : EquipmentSlot.OFFHAND);
                }
                level.playSound(
                        null,
                        pos,
                        SoundEvents.FLINTANDSTEEL_USE,
                        SoundSource.BLOCKS,
                        1.0F,
                        level.random.nextFloat() * 0.4F + 0.8F);
                player.displayClientMessage(
                        Component.translatable("message.cruciblecraft.coke_oven_ignited"),
                        true);
            }
            return ItemInteractionResult.SUCCESS;
        }
        if (FluidUtil.getFluidHandler(stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof CokeOvenBlockEntity cokeOven) {
            var handler = cokeOven.automationFluids(hitResult.getDirection());
            if (handler != null
                    && FluidUtil.interactWithFluidHandler(player, hand, handler)) {
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (!(context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof CokeOvenBlockEntity cokeOven)) {
            return ToolResult.PASS;
        }
        if (action == ToolAction.IGNITER) {
            if (context.getLevel().isClientSide) {
                return ToolResult.SUCCESS;
            }
            if (cokeOven.ignite()) {
                ToolClick.hurt(context);
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            Component.translatable("message.cruciblecraft.coke_oven_ignited"),
                            true);
                }
                return ToolResult.SUCCESS;
            }
            return ToolResult.PASS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!context.getLevel().isClientSide) {
                cokeOven.resetBySoftHammer();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.PLUNGER) {
            return ToolClick.plunger(context, cokeOven.trashWithPlunger());
        }
        return ToolResult.PASS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable("tooltip.cruciblecraft.coke_oven.structure")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.cruciblecraft.coke_oven.controller")
                .withStyle(ChatFormatting.WHITE));
        tooltip.add(Component.translatable("tooltip.cruciblecraft.coke_oven.ignite")
                .withStyle(ChatFormatting.GOLD));
        tooltip.add(Component.translatable("tooltip.cruciblecraft.coke_oven.fluid_drain")
                .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.cruciblecraft.coke_oven.io")
                .withStyle(ChatFormatting.GRAY));
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (state.getBlock() != newState.getBlock()
                && level.getBlockEntity(pos) instanceof CokeOvenBlockEntity cokeOven) {
            cokeOven.dropContents();
            cokeOven.clearBindings();
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CokeOvenBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return level.isClientSide
                ? null
                : createTicker(
                        type,
                        ModBlockEntities.COKE_OVEN.get(),
                        CokeOvenBlockEntity::serverTick);
    }

    @SuppressWarnings("unchecked")
    private static <T extends BlockEntity, E extends BlockEntity> BlockEntityTicker<T> createTicker(
            BlockEntityType<T> actual,
            BlockEntityType<E> expected,
            BlockEntityTicker<? super E> ticker) {
        return actual == expected ? (BlockEntityTicker<T>) ticker : null;
    }
}
