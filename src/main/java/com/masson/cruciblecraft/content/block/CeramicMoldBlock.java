package com.masson.cruciblecraft.content.block;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.CeramicMoldBlockEntity;
import com.masson.cruciblecraft.content.item.CeramicMoldBlockItem;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldRecipes;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import org.jetbrains.annotations.Nullable;

public final class CeramicMoldBlock extends Block implements EntityBlock, ToolInteractable {
    public static final BooleanProperty FILLED = BooleanProperty.create("filled");
    private static final VoxelShape SHAPE = Block.box(0.0, 0.0, 0.0, 16.0, 5.0, 16.0);

    public CeramicMoldBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILLED, false));
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
        return ToolClick.useItemOn(stack, level, player, hand, hit);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold)) {
            return ToolResult.PASS;
        }
        if (action == ToolAction.CHISEL) {
            if (mold.isFilled()) {
                return ToolResult.PASS;
            }
            Vec3 location = context.getClickLocation();
            Optional<Integer> bit = MoldRecipes.chiselBit(
                    location.x - pos.getX(),
                    location.z - pos.getZ());
            if (bit.isEmpty()) {
                return ToolResult.PASS;
            }
            if (!level.isClientSide && mold.chiselBit(bit.get())) {
                ToolClick.hurt(context);
            }
            return mold.pattern() != 0 && (mold.pattern() & bit.get()) != 0
                    ? ToolResult.SUCCESS
                    : ToolResult.PASS;
        }
        if (action == ToolAction.PINCERS) {
            Player player = context.getPlayer();
            if (player == null) {
                return ToolResult.PASS;
            }
            if (level.isClientSide) {
                return ToolResult.SUCCESS;
            }
            ItemStack output = mold.takeOutput(player, false);
            if (output.isEmpty()) {
                return ToolResult.PASS;
            }
            if (!player.addItem(output)) {
                player.drop(output, false);
            }
            ToolClick.hurt(context);
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.WRENCH || action == ToolAction.SCREWDRIVER) {
            if (!level.isClientSide) {
                mold.rotatePattern();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!level.isClientSide) {
                mold.clearAutoInput();
                Player player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.cruciblecraft.mold_auto_input_cleared"),
                            true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action != ToolAction.MONKEY_WRENCH) {
            return ToolResult.PASS;
        }
        if (context.getClickedFace() != Direction.UP) {
            return ToolResult.PASS;
        }
        if (!level.isClientSide) {
            Direction target = Gt6StyleConnections.sideFromHit(ToolClick.hit(context));
            Player player = context.getPlayer();
            if (target.getAxis().isHorizontal()) {
                boolean enabled = mold.toggleAutoPull(target);
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    enabled
                                            ? "message.cruciblecraft.mold_auto_input_on"
                                            : "message.cruciblecraft.mold_auto_input_off"),
                            true);
                }
            } else {
                boolean redstone = mold.toggleRedstoneMode();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    redstone
                                            ? "message.cruciblecraft.mold_auto_input_redstone"
                                            : "message.cruciblecraft.mold_auto_input_no_redstone"),
                            true);
                }
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
            BlockHitResult hitResult) {
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (!(level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold)) {
            return InteractionResult.PASS;
        }

        ItemStack output = mold.takeOutput(player, true);
        if (!output.isEmpty()) {
            if (!player.addItem(output)) {
                player.drop(output, false);
            }
            return InteractionResult.CONSUME;
        }
        if (mold.isFilled()) {
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.mold_cooling", Math.round(mold.temperature())),
                    true);
            return InteractionResult.CONSUME;
        }
        if (hitResult.getDirection() != Direction.UP) {
            return InteractionResult.CONSUME;
        }

        Direction target = Gt6StyleConnections.sideFromHit(hitResult);
        boolean poured = target.getAxis().isVertical()
                ? pourAllHorizontal(level, pos, mold)
                : pourFrom(level, pos, mold, target);
        if (poured) {
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.mold_filled"),
                    true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.mold_no_molten_material"),
                    true);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        applyContactDamage(level, pos, entity);
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        applyContactDamage(level, pos, entity);
    }

    private static void applyContactDamage(Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold) {
            TemperatureDamage.apply(entity, mold.temperature(), 1.0F, 5.0F);
        }
    }

    private static boolean pourAllHorizontal(
            Level level, BlockPos pos, CeramicMoldBlockEntity mold) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (pourFrom(level, pos, mold, direction)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pourFrom(
            Level level,
            BlockPos pos,
            CeramicMoldBlockEntity mold,
            Direction side) {
        if (!side.getAxis().isHorizontal()) {
            return false;
        }
        CruciblePour crucible = CruciblePour.at(level, pos.relative(side));
        return crucible != null
                && crucible.fillMoldAtSide(mold, side.getOpposite(), side);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            @Nullable LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (stack.getItem() instanceof CeramicMoldBlockItem item
                && level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold) {
            mold.setShape(item.shape());
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        super.neighborChanged(
                state, level, pos, neighborBlock, neighborPos, movedByPiston);
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold) {
            mold.onNeighborChanged();
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState newState,
            boolean movedByPiston) {
        if (isBlockReplacement(state, newState)
                && !level.isClientSide
                && level.getBlockEntity(pos) instanceof CeramicMoldBlockEntity mold) {
            for (ItemStack stack : removalDrops(
                    mold.moldStack(), mold.contentsStack())) {
                popResource(level, pos, stack);
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    static boolean isBlockReplacement(
            BlockState state, BlockState newState) {
        return state.getBlock() != newState.getBlock();
    }

    static List<ItemStack> removalDrops(
            ItemStack moldStack, ItemStack contentsStack) {
        return contentsStack.isEmpty()
                ? List.of(moldStack)
                : List.of(moldStack, contentsStack);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            net.minecraft.world.level.BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FILLED);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CeramicMoldBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        if (level.isClientSide || type != ModBlockEntities.CERAMIC_MOLD.get()) {
            return null;
        }
        @SuppressWarnings("unchecked")
        BlockEntityTicker<T> ticker = (BlockEntityTicker<T>) (BlockEntityTicker<CeramicMoldBlockEntity>)
                CeramicMoldBlockEntity::serverTick;
        return ticker;
    }
}
