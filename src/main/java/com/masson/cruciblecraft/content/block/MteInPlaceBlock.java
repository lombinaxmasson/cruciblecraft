package com.masson.cruciblecraft.content.block;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.DrawerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.mte.BathingPotRuntime;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.storage.MassStorageClicks;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Face-placed GT6 MTE host. Attachments sit on the clicked face; this is not
 * a {@code PipeCover}.
 */
public final class MteInPlaceBlock extends Block
        implements EntityBlock, ToolInteractable {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    private static final VoxelShape DOWN = Block.box(4.0, 0.0, 4.0, 12.0, 8.0, 12.0);
    private static final VoxelShape UP = Block.box(4.0, 8.0, 4.0, 12.0, 16.0, 12.0);
    private static final VoxelShape NORTH = Block.box(4.0, 4.0, 0.0, 12.0, 12.0, 8.0);
    private static final VoxelShape SOUTH = Block.box(4.0, 4.0, 8.0, 12.0, 12.0, 16.0);
    private static final VoxelShape WEST = Block.box(0.0, 4.0, 4.0, 8.0, 12.0, 12.0);
    private static final VoxelShape EAST = Block.box(8.0, 4.0, 4.0, 16.0, 12.0, 12.0);
    private static final VoxelShape PANEL = Block.box(0.0, 0.0, 7.0, 16.0, 16.0, 9.0);
    private static final VoxelShape ROPE = Block.box(6.0, 0.0, 6.0, 10.0, 16.0, 10.0);
    private static final VoxelShape FOUNDRY_SMELTERY = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 2.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 2.0, 16.0, 16.0),
            Block.box(14.0, 0.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 2.0),
            Block.box(0.0, 0.0, 14.0, 16.0, 16.0, 16.0));
    private static final VoxelShape FOUNDRY_BASIN = Shapes.or(
            Block.box(0.0, 0.0, 0.0, 16.0, 1.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 1.0, 16.0, 16.0),
            Block.box(15.0, 0.0, 0.0, 16.0, 16.0, 16.0),
            Block.box(0.0, 0.0, 0.0, 16.0, 16.0, 1.0),
            Block.box(0.0, 0.0, 15.0, 16.0, 16.0, 16.0));
    private static final VoxelShape FOUNDRY_MOLD = Block.box(0.0, 0.0, 0.0, 16.0, 7.0, 16.0);
    private static final VoxelShape FOUNDRY_CROSSING = Block.box(0.0, 0.0, 0.0, 16.0, 6.0, 16.0);
    private static final VoxelShape CHEST = Block.box(1.0, 0.0, 1.0, 15.0, 14.0, 15.0);
    private static final VoxelShape BATHING_POT = Block.box(0.0, 0.0, 0.0, 16.0, 8.0, 16.0);

    private final MteInPlaceSpec spec;

    public MteInPlaceBlock(MteInPlaceSpec spec, Properties properties) {
        super(properties);
        this.spec = spec;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    public MteInPlaceSpec spec() {
        return spec;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction facing = spec.kind().attachment()
                ? clicked.getOpposite()
                : context.getHorizontalDirection().getOpposite();
        if (!spec.kind().attachment() && facing.getAxis().isVertical()) {
            facing = context.getHorizontalDirection().getOpposite();
        }
        return defaultBlockState().setValue(FACING, facing);
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
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        MteInPlaceKind kind = spec.kind();
        if (BathingPotRuntime.hosts(spec)) {
            return BathingPotRuntime.table(spec) ? Shapes.block() : BATHING_POT;
        }
        if (kind == MteInPlaceKind.ROPE) {
            return ROPE;
        }
        if (kind == MteInPlaceKind.WOOD_PANEL) {
            return PANEL;
        }
        if (kind == MteInPlaceKind.CRUCIBLE_FOUNDRY) {
            return foundryShape();
        }
        if (kind == MteInPlaceKind.CHEST) {
            return CHEST;
        }
        if (!kind.attachment()) {
            return super.getShape(state, level, pos, context);
        }
        return switch (state.getValue(FACING)) {
            case DOWN -> DOWN;
            case UP -> UP;
            case NORTH -> NORTH;
            case SOUTH -> SOUTH;
            case WEST -> WEST;
            case EAST -> EAST;
        };
    }

    private VoxelShape foundryShape() {
        String gt6Class = spec.gt6Class();
        if (gt6Class.contains("Crossing")) {
            return FOUNDRY_CROSSING;
        }
        if (gt6Class.contains("Smeltery")) {
            return FOUNDRY_SMELTERY;
        }
        if (gt6Class.contains("Basin")) {
            return FOUNDRY_BASIN;
        }
        return FOUNDRY_MOLD;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return spec.kind() == MteInPlaceKind.CHEST
                ? RenderShape.ENTITYBLOCK_ANIMATED
                : RenderShape.MODEL;
    }

    @Override
    protected boolean triggerEvent(
            BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (spec.kind() == MteInPlaceKind.MASS_STORAGE) {
            ToolResult mass = useMassStorageTool(action, context);
            if (mass != ToolResult.PASS) {
                return mass;
            }
        }
        if (action == ToolAction.PLUNGER
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof MteInPlaceBlockEntity host) {
            return ToolClick.plunger(context, host.trashWithPlunger());
        }
        if (action == ToolAction.PINCERS
                && spec.kind() == MteInPlaceKind.BOOKSHELF
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof MteInPlaceBlockEntity host) {
            Player player = context.getPlayer();
            if (player == null) {
                return ToolResult.PASS;
            }
            if (!context.getLevel().isClientSide) {
                var items = host.items();
                for (int slot = 0; slot < items.getSlots(); slot++) {
                    ItemStack taken = items.extractItem(slot, 64, false);
                    if (!taken.isEmpty()) {
                        ToolClick.give(player, taken);
                        ToolClick.hurt(context);
                        break;
                    }
                }
            }
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    @Override
    public float getEnchantPowerBonus(
            BlockState state, LevelReader level, BlockPos pos) {
        if (spec.kind() == MteInPlaceKind.BOOKSHELF
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
            return host.enchantPower();
        }
        return 0.0F;
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
        if (spec.kind() == MteInPlaceKind.MASS_STORAGE
                && hit.getDirection() == state.getValue(FACING)
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
            if (!level.isClientSide) {
                host.massStorageActivated(player, stack, hit);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (BathingPotRuntime.hosts(spec)
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host
                && host.bathingPot() != null
                && host.bathingPot().useItem(level, player, hand)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
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
        MteInPlaceKind kind = spec.kind();
        if (BathingPotRuntime.hosts(spec)
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host
                && host.bathingPot() != null) {
            if (!level.isClientSide) {
                host.bathingPot().extract(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind.playerInventoryGui()) {
            if (!level.isClientSide
                    && player instanceof ServerPlayer serverPlayer
                    && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
                if (kind == MteInPlaceKind.DRAWER) {
                    if (hit.getDirection() != state.getValue(FACING)) {
                        return InteractionResult.PASS;
                    }
                    host.setDrawerCompartment(MassStorageClicks.drawerCompartment(
                            hit.getDirection(), pos, hit.getLocation()));
                }
                int visible = kind == MteInPlaceKind.DRAWER
                        ? DrawerBlockEntity.COMPARTMENT_SLOTS
                        : kind.slots();
                int offset = kind == MteInPlaceKind.DRAWER
                        ? host.drawerCompartment() * DrawerBlockEntity.COMPARTMENT_SLOTS
                        : 0;
                serverPlayer.openMenu(host, buf -> {
                    buf.writeBlockPos(pos);
                    buf.writeVarInt(visible);
                    buf.writeVarInt(offset);
                });
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind == MteInPlaceKind.LOCKER) {
            if (hit.getDirection() != state.getValue(FACING)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide
                    && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
                host.swapArmor(player);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (kind == MteInPlaceKind.MASS_STORAGE) {
            if (hit.getDirection() != state.getValue(FACING)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide
                    && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
                host.massStorageActivated(player, ItemStack.EMPTY, hit);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
            host.transferOnce();
        }
        return InteractionResult.CONSUME;
    }

    private ToolResult useMassStorageTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        if (!(level.getBlockEntity(context.getClickedPos())
                instanceof MteInPlaceBlockEntity host)
                || host.massStorage() == null) {
            return ToolResult.PASS;
        }
        if (action == ToolAction.CROWBAR) {
            return pickUpMassStorage(context, host);
        }
        if (action == ToolAction.WRENCH) {
            Direction target = Gt6StyleConnections.sideFromHit(ToolClick.hit(context));
            if (!target.getAxis().isHorizontal()) {
                return ToolResult.PASS;
            }
            BlockState state = level.getBlockState(context.getClickedPos());
            if (!level.isClientSide) {
                if (state.getValue(FACING) != target) {
                    level.setBlock(
                            context.getClickedPos(),
                            state.setValue(FACING, target),
                            Block.UPDATE_ALL);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.PINCERS) {
            if (!level.isClientSide && context.getPlayer() != null) {
                host.giveMassToPlayer(context.getPlayer());
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!level.isClientSide) {
                host.dumpMassInFront();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SCREWDRIVER) {
            if (!level.isClientSide) {
                host.toggleResetFilterWhenEmpty();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            host.filterMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.MONKEY_WRENCH) {
            if (!level.isClientSide) {
                host.toggleAutoOutput();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            host.autoOutputMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.WIRE_CUTTER) {
            if (!level.isClientSide) {
                host.toggleOverflow();
                if (context.getPlayer() != null) {
                    context.getPlayer().displayClientMessage(
                            host.overflowMessage(), true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    private static ToolResult pickUpMassStorage(
            UseOnContext context, MteInPlaceBlockEntity host) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        BlockState state = level.getBlockState(pos);
        ItemStack packed = new ItemStack(state.getBlock());
        host.saveToItem(packed, level.registryAccess());
        host.clearMassContents();
        level.removeBlock(pos, false);
        Player player = context.getPlayer();
        if (player == null || !player.addItem(packed)) {
            Block.popResource(level, pos, packed);
        }
        ToolClick.hurt(context);
        level.playSound(
                null,
                pos,
                SoundEvents.WOOD_BREAK,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);
        return ToolResult.SUCCESS;
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (!state.is(next.getBlock())
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
            host.dropContents();
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MteInPlaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        if (type != ModBlockEntities.MTE_INPLACE.get()) {
            return null;
        }
        if (level.isClientSide) {
            return spec.kind() == MteInPlaceKind.CHEST
                    ? (lvl, pos, st, be) -> MteInPlaceBlockEntity.clientTick(
                            lvl, pos, st, (MteInPlaceBlockEntity) be)
                    : null;
        }
        return (lvl, pos, st, be) -> MteInPlaceBlockEntity.serverTick(
                lvl, pos, st, (MteInPlaceBlockEntity) be);
    }
}
