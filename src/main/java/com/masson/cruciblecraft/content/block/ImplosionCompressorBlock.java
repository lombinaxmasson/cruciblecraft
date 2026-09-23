package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.ImplosionCompressorBlockEntity;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;

/** Facing/menu/ticker shell for GT6 Implosion Compressor 17110. */
public final class ImplosionCompressorBlock
        extends Block
        implements EntityBlock, com.masson.cruciblecraft.api.tool.ToolInteractable {
    public ImplosionCompressorBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(
                ProcessingMachineBlock.FACING,
                Direction.NORTH));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                ProcessingMachineBlock.FACING,
                context.getHorizontalDirection().getOpposite());
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.implosion_compressor.structure"));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.implosion_compressor.controller"));
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.implosion_compressor.io"));
        super.appendHoverText(stack, context, tooltip, flag);
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
                        machine,
                        level,
                        pos,
                        player,
                        hit)) {
            return InteractionResult.SUCCESS;
        }
        if (!level.isClientSide
                && player instanceof ServerPlayer server
                && level.getBlockEntity(pos)
                        instanceof ImplosionCompressorBlockEntity implosion) {
            server.openMenu(implosion, data -> data.writeBlockPos(pos));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public com.masson.cruciblecraft.api.tool.ToolResult useTool(
            com.masson.cruciblecraft.api.tool.ToolAction action,
            net.minecraft.world.item.context.UseOnContext context) {
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof MachineCoverHost machine) {
            return MachineCoverBlockInteraction.useTool(
                    machine,
                    action,
                    context);
        }
        return com.masson.cruciblecraft.api.tool.ToolResult.PASS;
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
        return MachineCoverBlockInteraction.weakRedstone(
                level,
                pos,
                direction);
    }

    @Override
    protected int getDirectSignal(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            Direction direction) {
        return MachineCoverBlockInteraction.directRedstone(
                level,
                pos,
                direction);
    }

    @Override
    public boolean canConnectRedstone(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        return MachineCoverBlockInteraction.canConnectRedstone(
                level,
                pos,
                direction);
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
                        instanceof ImplosionCompressorBlockEntity implosion) {
            implosion.clearBindings();
            implosion.dropContents();
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
        return new ImplosionCompressorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.IMPLOSION_COMPRESSOR.get()
                ? (l, p, s, be) -> ImplosionCompressorBlockEntity.serverTick(
                        l,
                        p,
                        s,
                        (ImplosionCompressorBlockEntity) be)
                : null;
    }
}
