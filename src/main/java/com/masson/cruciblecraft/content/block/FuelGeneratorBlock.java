package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.FuelGeneratorBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.machine.generation.FuelGeneratorSpec;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidUtil;

/** Typed-energy fuel generator configured by one fixed source-map family. */
public final class FuelGeneratorBlock extends Block
        implements EntityBlock, ToolInteractable,
                com.masson.cruciblecraft.energy.converter.EnergyConverterHost {
    public static final DirectionProperty FACING =
            BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private final FuelGeneratorSpec spec;

    public FuelGeneratorBlock(
            FuelGeneratorSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(
                FACING, Direction.NORTH).setValue(LIT, false));
    }

    public FuelGeneratorSpec spec() {
        return spec;
    }

    @Override
    public net.minecraft.resources.ResourceLocation converterId() {
        return spec.id();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getHorizontalDirection().getOpposite());
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
        if (tool != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION) {
            return tool;
        }
        if (level.getBlockEntity(pos)
                instanceof FuelGeneratorBlockEntity generator) {
            if (spec.requiresIgnition()
                    && stack.is(Items.FLINT_AND_STEEL)) {
                if (level.isClientSide) {
                    return ItemInteractionResult.SUCCESS;
                }
                if (generator.tryIgnite(
                        player, hand, hit.getDirection(), stack)) {
                    return ItemInteractionResult.SUCCESS;
                }
            }
        }
        if (FluidUtil.getFluidHandler(stack).isEmpty()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide) {
            return ItemInteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos)
                instanceof FuelGeneratorBlockEntity generator) {
            var handler = generator.fluids(hit.getDirection());
            if (handler != null
                    && FluidUtil.interactWithFluidHandler(
                            player, hand, handler)) {
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
                        instanceof FuelGeneratorBlockEntity generator) {
            return ToolClick.plunger(context, generator.trashWithPlunger());
        }
        return ToolResult.PASS;
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

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (spec.inputPhase() == FuelGeneratorSpec.InputPhase.GAS) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.burning_box.gas_only")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.burning_box.ignite")
                    .withStyle(ChatFormatting.GRAY));
        } else if (spec.inputPhase() == FuelGeneratorSpec.InputPhase.LIQUID) {
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.burning_box.liquid_only")
                    .withStyle(ChatFormatting.GRAY));
            tooltip.add(Component.translatable(
                            "tooltip.cruciblecraft.burning_box.ignite")
                    .withStyle(ChatFormatting.GRAY));
        }
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FuelGeneratorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide
                        && type == ModBlockEntities.FUEL_GENERATOR.get()
                ? (l, p, s, be) -> FuelGeneratorBlockEntity.serverTick(
                        l, p, s, (FuelGeneratorBlockEntity) be)
                : null;
    }
}
