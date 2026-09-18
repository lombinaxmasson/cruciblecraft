package com.masson.cruciblecraft.content.block;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.SteamEngineBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import org.jetbrains.annotations.Nullable;

public final class SteamEngineBlock extends Block
        implements EntityBlock, ToolInteractable,
                com.masson.cruciblecraft.energy.converter.EnergyConverterHost {
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    private final net.minecraft.resources.ResourceLocation converterId;
    public SteamEngineBlock(
            net.minecraft.resources.ResourceLocation converterId,
            Properties properties) {
        super(properties);
        this.converterId = converterId;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    @Override
    public net.minecraft.resources.ResourceLocation converterId() {
        return converterId;
    }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

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
        if (action != ToolAction.SOFT_HAMMER) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        if (!level.isClientSide
                && level.getBlockEntity(context.getClickedPos())
                        instanceof SteamEngineBlockEntity engine) {
            boolean running = engine.toggleStopped();
            Player player = context.getPlayer();
            if (player != null) {
                player.displayClientMessage(
                        Component.translatable(
                                running
                                        ? "message.cruciblecraft.steam_engine.running"
                                        : "message.cruciblecraft.steam_engine.stopped"),
                        true);
            }
            ToolClick.hurt(context);
        }
        return ToolResult.SUCCESS;
    }

    @Override protected ItemInteractionResult useItemOn(
            ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        ItemInteractionResult tool = ToolClick.useItemOn(stack, level, player, hand, hit);
        if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            return tool;
        }
        if (level.isClientSide) {
            return FluidUtil.getFluidHandler(stack).isPresent()
                    ? ItemInteractionResult.SUCCESS
                    : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.getBlockEntity(pos) instanceof SteamEngineBlockEntity engine) {
            var handler = engine.fluids(hit.getDirection());
            if (handler != null && FluidUtil.interactWithFluidHandler(player, hand, handler)) {
                return ItemInteractionResult.SUCCESS;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
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
        return MachineCoverBlockInteraction.directRedstone(
                level, pos, direction);
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
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (state.getBlock() != next.getBlock()) {
            MachineCoverBlockInteraction.dropCovers(level, pos);
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new SteamEngineBlockEntity(pos, state); }
    @Nullable @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.STEAM_ENGINE.get()
                ? (l, p, s, be) -> SteamEngineBlockEntity.serverTick(l, p, s, (SteamEngineBlockEntity) be) : null;
    }
}
