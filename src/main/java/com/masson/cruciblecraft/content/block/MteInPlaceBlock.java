package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.DrawerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FoundryCastingBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FoundryCrossingBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleEntityMelts;
import com.masson.cruciblecraft.content.blockentity.CruciblePlayerInteraction;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.mte.BathingPotRuntime;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.storage.MassStorageClicks;
import com.masson.cruciblecraft.heat.TemperatureDamage;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
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
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
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
        Direction facing;
        if (AnvilHosts.isAnvil(spec)) {
            facing = context.getHorizontalDirection();
        } else if (spec.kind().attachment()) {
            facing = clicked.getOpposite();
        } else {
            facing = context.getHorizontalDirection().getOpposite();
        }
        if (!AnvilHosts.isAnvil(spec)
                && !spec.kind().attachment()
                && facing.getAxis().isVertical()) {
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
        if (AnvilHosts.isAnvil(spec)) {
            return AnvilHosts.shape(AnvilHosts.horizontalFacing(state));
        }
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
        if (FoundryHosts.isCasting(spec)) {
            ToolResult casting = FoundryCastingInteractions.useTool(action, context);
            if (casting != ToolResult.PASS) {
                return casting;
            }
        }
        if (AnvilHosts.isAnvil(spec)) {
            ToolResult anvil = AnvilInteractions.useTool(action, context);
            if (anvil != ToolResult.PASS) {
                return anvil;
            }
        }
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
        if (action == ToolAction.SOFT_HAMMER
                && spec.kind().rotationEngine()
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof MteInPlaceBlockEntity host) {
            if (!context.getLevel().isClientSide) {
                boolean running = host.toggleRotationEngineStopped();
                Player player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    running
                                            ? "message.cruciblecraft.rotation_engine.running"
                                            : "message.cruciblecraft.rotation_engine.stopped"),
                            true);
                }
                ToolClick.hurt(context);
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
        if (AnvilHosts.isAnvil(spec)
                && AnvilHosts.isHammer(stack)
                && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil
                && AnvilInteractions.canPlaceHeld(anvil, stack)) {
            return AnvilInteractions.useItemOn(stack, level, pos, player, hit);
        }
        ItemInteractionResult tool = ToolClick.useItemOn(
                stack, level, player, hand, hit);
        if (tool.consumesAction()) {
            return tool;
        }
        if (AnvilHosts.isAnvil(spec)) {
            ItemInteractionResult placed = AnvilInteractions.useItemOn(
                    stack, level, pos, player, hit);
            if (placed.consumesAction()) {
                return placed;
            }
        }
        if (SmelteryHosts.isSmeltery(spec)
                && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            return CruciblePlayerInteraction.useItemOn(
                    level,
                    player,
                    hand,
                    stack,
                    hit.getDirection(),
                    crucible.process(),
                    crucible.inputBuffer(),
                    crucible.externalFluids(),
                    true);
        }
        if (LargeCrucibleHosts.isController(spec)
                && level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible) {
            return CruciblePlayerInteraction.useItemOn(
                    level,
                    player,
                    hand,
                    stack,
                    hit.getDirection(),
                    crucible.process(),
                    crucible.inventory(),
                    crucible.process().fluids(),
                    crucible.structureValid() && !crucible.pluginQuarantined());
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
        if (FoundryHosts.isCasting(spec)) {
            return FoundryCastingInteractions.useWithoutItem(level, pos, player, hit);
        }
        if (AnvilHosts.isAnvil(spec)) {
            return AnvilInteractions.useWithoutItem(level, pos, player, hit);
        }
        if (SmelteryHosts.isSmeltery(spec)
                && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            return CruciblePlayerInteraction.useEmpty(
                    level,
                    player,
                    hit.getDirection(),
                    crucible.process(),
                    crucible.inputBuffer(),
                    true);
        }
        if (LargeCrucibleHosts.isController(spec)
                && level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible) {
            return CruciblePlayerInteraction.useEmpty(
                    level,
                    player,
                    hit.getDirection(),
                    crucible.process(),
                    crucible.inventory(),
                    crucible.structureValid() && !crucible.pluginQuarantined());
        }
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
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (FoundryHosts.isCasting(spec)) {
            FoundryCastingInteractions.applyContactDamage(level, pos, entity);
        }
        if (LargeCrucibleHosts.isController(spec)) {
            LargeCrucibleBlock.applyHotContact(level, pos, entity);
        }
        super.stepOn(level, pos, state, entity);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        if (SmelteryHosts.isSmeltery(spec)) {
            if (level.isClientSide
                    || !(level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible)) {
                return;
            }
            float temperature = crucible.temperature();
            boolean damaged = TemperatureDamage.apply(entity, temperature, 1.0F, 10.0F);
            if (damaged && TemperatureDamage.kelvin(temperature) > 320L) {
                CrucibleEntityMelts.tryMelt(
                        crucible.process(),
                        entity,
                        CrucibleBlockEntity.AMBIENT_TEMPERATURE);
            }
            return;
        }
        if (FoundryHosts.isCasting(spec)) {
            FoundryCastingInteractions.applyContactDamage(level, pos, entity);
            return;
        }
        if (LargeCrucibleHosts.isController(spec)) {
            LargeCrucibleBlock.applyHotContact(level, pos, entity);
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
                && FoundryHosts.isMold(spec)
                && level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity mold) {
            mold.onNeighborChanged();
        }
    }

    @Override
    protected void onRemove(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState next,
            boolean moved) {
        if (!state.is(next.getBlock())) {
            if (level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
                anvil.dropContents();
            } else if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
                crucible.dropBuffer(level, pos);
            } else if (level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible) {
                crucible.clearBindings();
            } else if (level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity mold) {
                mold.dropContents();
            } else if (level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
                host.dropContents();
            }
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public void setPlacedBy(
            Level level,
            BlockPos pos,
            BlockState state,
            LivingEntity placer,
            ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (AnvilHosts.isAnvil(spec)
                && level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
            anvil.setMaterial(
                    AnvilHosts.materialId(spec),
                    stack.get(ModComponents.MACHINE_DURABILITY));
        }
        if (SmelteryHosts.isSmeltery(spec)
                && level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
            crucible.setCasingMaterialId(SmelteryHosts.materialId(spec));
        }
        if (LargeCrucibleHosts.isController(spec)
                && level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible) {
            crucible.setCasingMaterialId(LargeCrucibleHosts.materialId(spec));
        }
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (AnvilHosts.isAnvil(spec)
                && params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                        instanceof AnvilBlockEntity anvil) {
            for (ItemStack drop : drops) {
                if (drop.getItem() == asItem()) {
                    drop.set(ModComponents.MACHINE_DURABILITY, anvil.durabilityComponent());
                }
            }
        }
        return drops;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (AnvilHosts.isAnvil(spec)) {
            return new AnvilBlockEntity(pos, state);
        }
        if (SmelteryHosts.isSmeltery(spec)) {
            return new CrucibleBlockEntity(pos, state);
        }
        if (FoundryHosts.isCasting(spec)) {
            return new FoundryCastingBlockEntity(pos, state);
        }
        if (FoundryHosts.isCrossing(spec)) {
            return new FoundryCrossingBlockEntity(pos, state);
        }
        if (LargeCrucibleHosts.isController(spec)) {
            return new LargeCrucibleBlockEntity(pos, state);
        }
        return new MteInPlaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        if (SmelteryHosts.isSmeltery(spec)) {
            return type == ModBlockEntities.CRUCIBLE.get()
                    ? (lvl, pos, st, be) -> {
                        if (be instanceof CrucibleBlockEntity crucible) {
                            if (lvl.isClientSide) {
                                CrucibleBlockEntity.clientTick(lvl, pos, st, crucible);
                            } else {
                                CrucibleBlockEntity.serverTick(lvl, pos, st, crucible);
                            }
                        }
                    }
                    : null;
        }
        if (LargeCrucibleHosts.isController(spec)) {
            return type == ModBlockEntities.LARGE_CRUCIBLE.get()
                    ? (lvl, pos, st, be) -> {
                        if (be instanceof LargeCrucibleBlockEntity crucible) {
                            if (lvl.isClientSide) {
                                LargeCrucibleBlockEntity.clientTick(lvl, pos, st, crucible);
                            } else {
                                LargeCrucibleBlockEntity.serverTick(lvl, pos, st, crucible);
                            }
                        }
                    }
                    : null;
        }
        if (FoundryHosts.isCasting(spec)) {
            return type == ModBlockEntities.FOUNDRY_CASTING.get()
                    ? (lvl, pos, st, be) -> {
                        if (!lvl.isClientSide
                                && be instanceof FoundryCastingBlockEntity mold) {
                            FoundryCastingBlockEntity.serverTick(lvl, pos, st, mold);
                        }
                    }
                    : null;
        }
        if (FoundryHosts.isCrossing(spec)
                || AnvilHosts.isAnvil(spec)
                || type != ModBlockEntities.MTE_INPLACE.get()) {
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
