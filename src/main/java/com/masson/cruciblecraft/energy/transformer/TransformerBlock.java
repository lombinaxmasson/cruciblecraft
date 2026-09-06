package com.masson.cruciblecraft.energy.transformer;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.item.MaterialMonkeyWrenchItem;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * GT6 electric transformer: 6-way facing, monkey wrench reverses, no wrench
 * rotation. Overlay state 0/1/2 is {@code TE_Behavior_Active_Trinary}.
 */
public final class TransformerBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final IntegerProperty ACTIVITY =
            IntegerProperty.create("activity", 0, 2);

    private final EnergyTransformerProfile profile;

    public TransformerBlock(
            EnergyTransformerProfile profile, Properties properties) {
        super(properties);
        this.profile = profile;
        registerDefaultState(
                stateDefinition.any()
                        .setValue(FACING, Direction.UP)
                        .setValue(ACTIVITY, 0));
    }

    public EnergyTransformerProfile profile() {
        return profile;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ACTIVITY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(
                FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
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
        if (!(stack.getItem() instanceof MaterialMonkeyWrenchItem)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!level.isClientSide
                && level.getBlockEntity(pos)
                        instanceof TransformerBlockEntity transformer) {
            transformer.toggleReversed();
            player.displayClientMessage(
                    Component.translatable(
                            transformer.reversed()
                                    ? "message.cruciblecraft.transformer.reversed"
                                    : "message.cruciblecraft.transformer.normal"),
                    true);
            if (!player.getAbilities().instabuild) {
                stack.hurtAndBreak(
                        1,
                        player,
                        hand == InteractionHand.MAIN_HAND
                                ? net.minecraft.world.entity.EquipmentSlot.MAINHAND
                                : net.minecraft.world.entity.EquipmentSlot.OFFHAND);
            }
        }
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TransformerBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide && type == ModBlockEntities.TRANSFORMER.get()
                ? (current, pos, currentState, blockEntity) ->
                        TransformerBlockEntity.serverTick(
                                current,
                                pos,
                                currentState,
                                (TransformerBlockEntity) blockEntity)
                : null;
    }
}
