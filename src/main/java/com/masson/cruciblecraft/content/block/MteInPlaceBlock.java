package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolInteractable;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.logistics.core.LogisticsCorePart;
import com.masson.cruciblecraft.logistics.core.LogisticsCoreTagged;
import com.masson.cruciblecraft.content.blockentity.DrawerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MatterFabricatorBlockEntity;
import com.masson.cruciblecraft.energy.largedynamo.LargeDynamoBlockEntity;
import com.masson.cruciblecraft.energy.lightningrod.LightningRodBlockEntity;
import com.masson.cruciblecraft.energy.vondagraagg.VonDaGraaggBlockEntity;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineBlockEntity;
import com.masson.cruciblecraft.energy.largegasturbine.LargeGasTurbineTooltips;
import com.masson.cruciblecraft.energy.largegasturbine.LargeTurbineWalls;
import com.masson.cruciblecraft.content.blockentity.FoundryCastingBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FoundryCrossingBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MteInPlaceBlockEntity;
import com.masson.cruciblecraft.content.blockentity.AnvilBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CoinageMoldBlockEntity;
import com.masson.cruciblecraft.content.blockentity.CrucibleEntityMelts;
import com.masson.cruciblecraft.content.blockentity.CruciblePlayerInteraction;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerBlockEntity;
import com.masson.cruciblecraft.content.blockentity.LargeBoilerTier;
import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;
import com.masson.cruciblecraft.content.blockentity.MultiblockPortBlockEntity;
import com.masson.cruciblecraft.content.blockentity.TankBlockEntity;
import com.masson.cruciblecraft.content.block.SluiceParts;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.mte.BathingPotRuntime;
import com.masson.cruciblecraft.content.mte.FluidAttachmentPlayerInteraction;
import com.masson.cruciblecraft.content.mte.MteFaucetProfile;
import com.masson.cruciblecraft.content.mte.MteFluidAttachmentProfile;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;
import com.masson.cruciblecraft.content.multiblock.TankControllerProfiles;
import com.masson.cruciblecraft.content.storage.MassStorageClicks;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBlockInteraction;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.energy.steam.SteamTurbinePresentation;
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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Explosion;
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
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
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
        implements EntityBlock, ToolInteractable, LogisticsCoreTagged {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    public static final BooleanProperty FAST = BooleanProperty.create("fast");
    public static final BooleanProperty COUNTERCLOCKWISE =
            BooleanProperty.create("counterclockwise");
    public static final IntegerProperty WHEEL_DESIGN =
            IntegerProperty.create("wheel_design", 0, 3);
    public static final IntegerProperty SLUICE_DESIGN =
            IntegerProperty.create("sluice_design", 0, 7);
    public static final IntegerProperty ELECTROLYZER_DESIGN =
            IntegerProperty.create("electrolyzer_design", 0, 7);
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

    /**
     * {@link Block} calls {@link #createBlockStateDefinition} inside
     * {@code super()}, before {@link #spec} is assigned. Controllers and walls
     * stash the spec here so {@code formed} is actually added to the definition.
     */
    private static final ThreadLocal<MteInPlaceSpec> CONSTRUCTING = new ThreadLocal<>();

    private final MteInPlaceSpec spec;

    public MteInPlaceBlock(MteInPlaceSpec spec, Properties properties) {
        super(stashConstructingSpec(spec, properties));
        this.spec = spec;
        BlockState initial = stateDefinition.any().setValue(FACING, Direction.NORTH);
        if (SteamTurbinePresentation.single(spec)) {
            initial = initial
                    .setValue(LIT, false)
                    .setValue(FAST, false)
                    .setValue(COUNTERCLOCKWISE, false);
        } else if (SteamTurbinePresentation.large(spec)) {
            initial = initial.setValue(LIT, false);
        }
        if (LargeCrucibleHosts.usesFormedState(spec)) {
            initial = initial.setValue(LargeCrucibleHosts.FORMED, false);
        }
        if (LargeTurbineWalls.usesOutletState(spec)) {
            initial = initial.setValue(LargeTurbineWalls.OUTLET, false);
        }
        if (CrusherWheels.isPart(spec) || ShredderBlades.isPart(spec)) {
            initial = initial.setValue(WHEEL_DESIGN, 0);
        }
        if (SluiceParts.isPart(spec)) {
            initial = initial.setValue(SLUICE_DESIGN, 0);
        }
        if (ElectrolyzerParts.isPart(spec)) {
            initial = initial.setValue(ELECTROLYZER_DESIGN, 0);
        }
        if (DistillationTowerParts.usesTowerSkin(spec)) {
            initial = initial
                    .setValue(MultiblockPortBlock.TOWER_SKIN, false)
                    .setValue(MultiblockPortBlock.BACK_HOLE, false);
        }
        if (StainlessSteelMixerWalls.isWall(spec)) {
            initial = initial.setValue(StainlessSteelMixerWalls.DESIGN_HOLE, false);
        }
        registerDefaultState(initial);
    }

    private static Properties stashConstructingSpec(
            MteInPlaceSpec spec, Properties properties) {
        CONSTRUCTING.set(spec);
        return properties;
    }

    private MteInPlaceSpec constructingSpec() {
        return spec != null ? spec : CONSTRUCTING.get();
    }

    public MteInPlaceSpec spec() {
        return spec;
    }

    @Override
    public LogisticsCorePart corePart() {
        if (spec != null
                && "multiblock/galvanized_steel_wall".equals(spec.registryPath())) {
            return LogisticsCorePart.WALL;
        }
        return null;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        try {
            builder.add(FACING);
            if (SteamTurbinePresentation.single(constructingSpec())) {
                builder.add(LIT, FAST, COUNTERCLOCKWISE);
            } else if (SteamTurbinePresentation.large(constructingSpec())) {
                builder.add(LIT);
            }
            if (LargeCrucibleHosts.usesFormedState(constructingSpec())) {
                builder.add(LargeCrucibleHosts.FORMED);
            }
            if (LargeTurbineWalls.usesOutletState(constructingSpec())) {
                builder.add(LargeTurbineWalls.OUTLET);
            }
            if (CrusherWheels.isPart(constructingSpec())
                    || ShredderBlades.isPart(constructingSpec())) {
                builder.add(WHEEL_DESIGN);
            }
            if (SluiceParts.isPart(constructingSpec())) {
                builder.add(SLUICE_DESIGN);
            }
            if (ElectrolyzerParts.isPart(constructingSpec())) {
                builder.add(ELECTROLYZER_DESIGN);
            }
            if (DistillationTowerParts.usesTowerSkin(constructingSpec())) {
                builder.add(
                        MultiblockPortBlock.TOWER_SKIN,
                        MultiblockPortBlock.BACK_HOLE);
            }
            if (StainlessSteelMixerWalls.isWall(constructingSpec())) {
                builder.add(StainlessSteelMixerWalls.DESIGN_HOLE);
            }
        } finally {
            CONSTRUCTING.remove();
        }
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction clicked = context.getClickedFace();
        Direction facing;
        if (TankControllerProfiles.isController(spec)) {
            facing = clicked.getOpposite();
        } else if (AnvilHosts.isAnvil(spec)) {
            facing = context.getHorizontalDirection();
        } else if (spec.kind().attachment()) {
            facing = clicked.getOpposite();
        } else {
            facing = context.getHorizontalDirection().getOpposite();
        }
        if (!TankControllerProfiles.isController(spec)
                && !AnvilHosts.isAnvil(spec)
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
        if (LargeCrucibleHosts.formed(state) && LargeCrucibleHosts.isWall(spec)) {
            return formedWallShape(level, pos);
        }
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
        if (MteFluidAttachmentProfile.contains(spec)
                || kind == MteInPlaceKind.FAUCET) {
            return FluidAttachmentShapes.shape(
                    kind, state.getValue(FACING));
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

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        if (LargeCrucibleHosts.formed(state) && LargeCrucibleHosts.isWall(spec)) {
            return Shapes.block();
        }
        return super.getCollisionShape(state, level, pos, context);
    }

    private VoxelShape formedWallShape(BlockGetter level, BlockPos pos) {
        LargeCrucibleBlockEntity controller = LargeCrucibleWalls.controllerAt(level, pos);
        if (controller == null) {
            return Shapes.block();
        }
        BlockPos delta = pos.subtract(controller.getBlockPos());
        return LargeCrucibleHullShape.shape(delta.getX(), delta.getY(), delta.getZ());
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
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (spec.kind() == MteInPlaceKind.GAS_TURBINE) {
            LargeGasTurbineTooltips.append(spec, tooltip);
        }
        if (MteFaucetProfile.contains(spec)) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.fluid_attachment.faucet"));
            if (MteFaucetProfile.require(spec).acidProof()) {
                tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.fluid_attachment.acid_proof"));
            }
        }
        if (MteFluidAttachmentProfile.contains(spec)) {
            MteFluidAttachmentProfile profile =
                    MteFluidAttachmentProfile.require(spec);
            tooltip.add(Component.translatable(
                    profile.phase() == MteFluidAttachmentProfile.Phase.GAS
                            ? "tooltip.cruciblecraft.fluid_attachment.gas"
                            : "tooltip.cruciblecraft.fluid_attachment.liquid"));
            if (profile.acidProof()) {
                tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.fluid_attachment.acid_proof"));
            }
            if (profile.magicProof()) {
                tooltip.add(Component.translatable(
                        "tooltip.cruciblecraft.fluid_attachment.magic_proof"));
            }
        }
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        if (spec.kind() == MteInPlaceKind.CHEST) {
            return RenderShape.ENTITYBLOCK_ANIMATED;
        }
        if (LargeCrucibleHosts.formed(state)) {
            return LargeCrucibleHosts.formedRenderShape(spec);
        }
        return RenderShape.MODEL;
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
                level, pos, direction);
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
    protected boolean triggerEvent(
            BlockState state, Level level, BlockPos pos, int id, int param) {
        super.triggerEvent(state, level, pos, id, param);
        BlockEntity entity = level.getBlockEntity(pos);
        return entity != null && entity.triggerEvent(id, param);
    }

    @Override
    public ToolResult useTool(ToolAction action, UseOnContext context) {
        if (DistillationTowerParts.isLivePort(spec)) {
            return DistillationTowerParts.useTool(action, context);
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof MachineCoverHost machine) {
            ToolResult coverResult = MachineCoverBlockInteraction.useTool(
                    machine, action, context);
            if (coverResult != ToolResult.PASS) {
                return coverResult;
            }
        }
        if (FoundryHosts.isCasting(spec)) {
            ToolResult casting = FoundryCastingInteractions.useTool(action, context);
            if (casting != ToolResult.PASS) {
                return casting;
            }
        }
        if (CoinageMoldHosts.isCoinage(spec)) {
            ToolResult coinage = CoinageMoldInteractions.useTool(action, context);
            if (coinage != ToolResult.PASS) {
                return coinage;
            }
        }
        if (AnvilHosts.isAnvil(spec)) {
            ToolResult anvil = AnvilInteractions.useTool(action, context);
            if (anvil != ToolResult.PASS) {
                return anvil;
            }
        }
        if (spec.kind().massStorage()) {
            ToolResult mass = useMassStorageTool(action, context);
            if (mass != ToolResult.PASS) {
                return mass;
            }
        }
        if (spec.kind() == MteInPlaceKind.FAUCET
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof MteInPlaceBlockEntity faucet) {
            if (action == ToolAction.SOFT_HAMMER) {
                if (!context.getLevel().isClientSide) {
                    faucet.clearFaucetAutoPull();
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
            if (action == ToolAction.MONKEY_WRENCH) {
                if (!context.getLevel().isClientSide) {
                    boolean automatic = faucet.toggleFaucetAutoPull();
                    Player player = context.getPlayer();
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable(
                                        automatic
                                                ? "message.cruciblecraft.mold_auto_input_on"
                                                : "message.cruciblecraft.mold_auto_input_redstone"),
                                true);
                    }
                    ToolClick.hurt(context);
                }
                return ToolResult.SUCCESS;
            }
        }
        if (action == ToolAction.PLUNGER
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof LargeGasTurbineBlockEntity gas) {
            return ToolClick.plunger(context, gas.trashWithPlunger());
        }
        if (action == ToolAction.PLUNGER
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof MteInPlaceBlockEntity host) {
            LargeGasTurbineBlockEntity gas = host.boundGasTurbine();
            if (gas != null) {
                return ToolClick.plunger(context, gas.trashWithPlunger());
            }
            MteInPlaceBlockEntity steam = host.boundSteamTurbine();
            if (steam != null) {
                return ToolClick.plunger(context, steam.trashWithPlunger());
            }
            return ToolClick.plunger(context, host.trashWithPlunger());
        }
        if (context.getLevel().getBlockEntity(context.getClickedPos())
                instanceof LargeBoilerBlockEntity boiler) {
            if (action == ToolAction.PLUNGER) {
                return ToolClick.plunger(context, boiler.trashWithPlunger());
            }
            if (action == ToolAction.CHISEL) {
                if (context.getLevel().isClientSide) {
                    return ToolResult.SUCCESS;
                }
                if (boiler.decalcify(context.getPlayer())) {
                    ToolClick.hurt(context);
                    return ToolResult.SUCCESS;
                }
                return ToolResult.PASS;
            }
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
                && spec.kind() == MteInPlaceKind.GAS_TURBINE
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof LargeGasTurbineBlockEntity gas) {
            if (!context.getLevel().isClientSide) {
                boolean running = gas.toggleStopped();
                Player player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    running
                                            ? "message.cruciblecraft.gas_turbine.running"
                                            : "message.cruciblecraft.gas_turbine.stopped"),
                            true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SOFT_HAMMER
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof MteInPlaceBlockEntity host) {
            LargeGasTurbineBlockEntity bound = host.boundGasTurbine();
            if (bound != null) {
                if (!context.getLevel().isClientSide) {
                    boolean running = bound.toggleStopped();
                    Player player = context.getPlayer();
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable(
                                        running
                                                ? "message.cruciblecraft.gas_turbine.running"
                                                : "message.cruciblecraft.gas_turbine.stopped"),
                                true);
                    }
                    ToolClick.hurt(context);
                }
                return ToolResult.SUCCESS;
            }
            MteInPlaceBlockEntity steam = host.boundSteamTurbine();
            if (steam != null) {
                if (!context.getLevel().isClientSide) {
                    boolean running = steam.toggleSteamTurbineStopped();
                    Player player = context.getPlayer();
                    if (player != null) {
                        player.displayClientMessage(
                                Component.translatable(
                                        running
                                                ? "message.cruciblecraft.steam_turbine.running"
                                                : "message.cruciblecraft.steam_turbine.stopped"),
                                true);
                    }
                    ToolClick.hurt(context);
                }
                return ToolResult.SUCCESS;
            }
        }
        if (action == ToolAction.SOFT_HAMMER
                && spec.kind() == MteInPlaceKind.STEAM_TURBINE
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof MteInPlaceBlockEntity host) {
            if (!context.getLevel().isClientSide) {
                boolean running = host.toggleSteamTurbineStopped();
                Player player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    running
                                            ? "message.cruciblecraft.steam_turbine.running"
                                            : "message.cruciblecraft.steam_turbine.stopped"),
                            true);
                }
                ToolClick.hurt(context);
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
        if (action == ToolAction.MONKEY_WRENCH
                && spec.kind() == MteInPlaceKind.STEAM_TURBINE
                && context.getLevel().getBlockEntity(context.getClickedPos())
                        instanceof MteInPlaceBlockEntity host) {
            if (!context.getLevel().isClientSide) {
                boolean counterclockwise = host.toggleSteamCounterClockwise();
                Player player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    counterclockwise
                                            ? "message.cruciblecraft.steam_turbine.counterclockwise"
                                            : "message.cruciblecraft.steam_turbine.clockwise"),
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
        if (level.getBlockEntity(pos) instanceof MachineCoverHost machine
                && MachineCoverBlockInteraction.rightClick(
                        machine, level, pos, player, hit)) {
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (spec.kind().attachment()) {
            ItemInteractionResult fluidAttachment =
                    FluidAttachmentPlayerInteraction.useItemOn(
                            spec, stack, level, pos, player, hand, hit);
            if (fluidAttachment.consumesAction()) {
                return fluidAttachment;
            }
        }
        if (AnvilHosts.isAnvil(spec)) {
            ItemInteractionResult placed =
                    AnvilInteractions.useItemOn(stack, level, pos, player, hit);
            if (placed.consumesAction()) {
                return placed;
            }
        }
        ItemInteractionResult tool = ToolClick.useItemOn(
                stack, level, player, hand, hit);
        if (tool.consumesAction()) {
            return tool;
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
        if (CoinageMoldHosts.isCoinage(spec)) {
            ItemInteractionResult coinage = CoinageMoldInteractions.useItemOn(
                    stack, level, pos, player, hit);
            if (coinage.consumesAction()) {
                return coinage;
            }
        }
        if (spec.kind().massStorage()
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
        if (level.getBlockEntity(pos) instanceof MachineCoverHost machine
                && MachineCoverBlockInteraction.rightClick(
                        machine, level, pos, player, hit)) {
            return InteractionResult.SUCCESS;
        }
        if (DistillationTowerParts.isLivePort(spec)) {
            return DistillationTowerParts.useWithoutItem(level, pos, player, hit);
        }
        if (FoundryHosts.isCasting(spec)) {
            return FoundryCastingInteractions.useWithoutItem(level, pos, player, hit);
        }
        if (CoinageMoldHosts.isCoinage(spec)) {
            return CoinageMoldInteractions.useWithoutItem(level, pos, player, hit);
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
        if (kind == MteInPlaceKind.MATTER_FABRICATOR) {
            if (!level.isClientSide
                    && player instanceof ServerPlayer server
                    && level.getBlockEntity(pos)
                            instanceof MatterFabricatorBlockEntity fabricator) {
                server.openMenu(fabricator, data -> data.writeBlockPos(pos));
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
        if (kind.massStorage()) {
            if (hit.getDirection() != state.getValue(FACING)) {
                return InteractionResult.PASS;
            }
            if (!level.isClientSide
                    && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
                host.massStorageActivated(player, ItemStack.EMPTY, hit);
            }
            return InteractionResult.sidedSuccess(level.isClientSide);
        }
        return FluidAttachmentPlayerInteraction.useWithoutItem(
                spec, level, pos, state, player, hit);
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
        CrusherWheels.hurtIfRunning(level, pos, entity);
        ShredderBlades.hurtIfRunning(level, pos, entity);
        if (LargeCrucibleHosts.isController(spec)) {
            LargeCrucibleBlock.applyHotContact(level, pos, entity);
        }
        if (SteamTurbinePresentation.single(spec)
                && state.getValue(FACING) == Direction.UP
                && state.getValue(LIT)
                && entity instanceof LivingEntity living) {
            SteamTurbinePresentation.spinWalker(
                    living,
                    state.getValue(COUNTERCLOCKWISE),
                    state.getValue(FAST));
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
        if (CrusherWheels.isPart(spec)) {
            CrusherWheels.hurtIfRunning(level, pos, entity);
            return;
        }
        if (ShredderBlades.isPart(spec)) {
            ShredderBlades.hurtIfRunning(level, pos, entity);
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
        if (!level.isClientSide
                && spec.kind() == MteInPlaceKind.FAUCET
                && level.hasNeighborSignal(pos)
                && level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity faucet
                && !faucet.faucetAutoPull()) {
            faucet.transferOnce();
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
            MachineCoverBlockInteraction.dropCovers(level, pos);
            if (DistillationTowerParts.isLivePort(spec)) {
                DistillationTowerParts.dropCovers(level, pos);
            } else if (level.getBlockEntity(pos) instanceof AnvilBlockEntity anvil) {
                anvil.dropContents();
            } else if (level.getBlockEntity(pos) instanceof CrucibleBlockEntity crucible) {
                crucible.dropBuffer(level, pos);
            } else if (level.getBlockEntity(pos) instanceof LargeCrucibleBlockEntity crucible) {
                crucible.clearBindings();
            } else if (level.getBlockEntity(pos)
                    instanceof LargeBoilerBlockEntity boiler) {
                boiler.clearBindings();
            } else if (level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity mold) {
                mold.dropContents();
            } else if (level.getBlockEntity(pos) instanceof CoinageMoldBlockEntity coinage) {
                coinage.dropContents();
            } else if (level.getBlockEntity(pos) instanceof MteInPlaceBlockEntity host) {
                host.unbindFusion();
                host.dropContents();
            }
        }
        super.onRemove(state, level, pos, next, moved);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level,
            BlockPos pos,
            BlockState state,
            Player player) {
        if (spec.kind() == MteInPlaceKind.LARGE_BOILER
                && level.getBlockEntity(pos)
                        instanceof LargeBoilerBlockEntity boiler) {
            boiler.removedByPlayer(player);
        }
        return super.playerWillDestroy(level, pos, state, player);
    }

    @Override
    public void wasExploded(Level level, BlockPos pos, Explosion explosion) {
        if (spec.kind() == MteInPlaceKind.LARGE_BOILER
                && !level.isClientSide
                && level.getBlockEntity(pos)
                        instanceof LargeBoilerBlockEntity boiler) {
            boiler.onExploded();
        }
        super.wasExploded(level, pos, explosion);
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
        if (FoundryHosts.isMold(spec)
                && level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity mold) {
            Integer stored = stack.get(ModComponents.MOLD_PATTERN);
            if (stored != null) {
                mold.setPattern(stored);
            }
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
        if (FoundryHosts.isMold(spec)
                && params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)
                        instanceof FoundryCastingBlockEntity mold) {
            for (ItemStack drop : drops) {
                if (drop.getItem() == asItem() && mold.pattern() != 0) {
                    drop.set(ModComponents.MOLD_PATTERN, mold.pattern());
                }
            }
        }
        return drops;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        if (TankControllerProfiles.isController(spec)) {
            return new TankBlockEntity(pos, state);
        }
        if (DistillationTowerParts.isLivePort(spec)) {
            return new MultiblockPortBlockEntity(pos, state);
        }
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
        if (CoinageMoldHosts.isCoinage(spec)) {
            return new CoinageMoldBlockEntity(pos, state);
        }
        if (spec.kind() == MteInPlaceKind.LARGE_BOILER
                || LargeBoilerTier.bySpec(spec).isPresent()) {
            return new LargeBoilerBlockEntity(pos, state);
        }
        if (spec.kind() == MteInPlaceKind.GAS_TURBINE) {
            return new LargeGasTurbineBlockEntity(pos, state);
        }
        if (spec.kind() == MteInPlaceKind.LARGE_DYNAMO) {
            return new LargeDynamoBlockEntity(pos, state);
        }
        if (spec.kind() == MteInPlaceKind.LIGHTNING_ROD) {
            return new LightningRodBlockEntity(pos, state);
        }
        if (spec.kind() == MteInPlaceKind.MATTER_FABRICATOR) {
            return new MatterFabricatorBlockEntity(pos, state);
        }
        if (spec.kind() == MteInPlaceKind.VON_DA_GRAAGG) {
            return new VonDaGraaggBlockEntity(pos, state);
        }
        return new MteInPlaceBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level,
            BlockState state,
            BlockEntityType<T> type) {
        if (TankControllerProfiles.isController(spec)) {
            return type == ModBlockEntities.TANK_3X3X3.get()
                    ? (lvl, pos, st, be) -> {
                        if (!lvl.isClientSide
                                && be instanceof TankBlockEntity tank) {
                            TankBlockEntity.serverTick(lvl, pos, st, tank);
                        }
                    }
                    : null;
        }
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
        if (spec.kind() == MteInPlaceKind.LARGE_BOILER
                || LargeBoilerTier.bySpec(spec).isPresent()) {
            return type == ModBlockEntities.LARGE_BOILER.get()
                    ? (lvl, pos, st, be) -> {
                        if (!lvl.isClientSide
                                && be instanceof LargeBoilerBlockEntity boiler) {
                            LargeBoilerBlockEntity.serverTick(
                                    lvl, pos, st, boiler);
                        }
                    }
                    : null;
        }
        if (spec.kind() == MteInPlaceKind.GAS_TURBINE) {
            return type == ModBlockEntities.LARGE_GAS_TURBINE.get()
                    ? (lvl, pos, st, be) -> {
                        if (be instanceof LargeGasTurbineBlockEntity turbine) {
                            if (lvl.isClientSide) {
                                LargeGasTurbineBlockEntity.clientTick(
                                        lvl, pos, st, turbine);
                            } else {
                                LargeGasTurbineBlockEntity.serverTick(
                                        lvl, pos, st, turbine);
                            }
                        }
                    }
                    : null;
        }
        if (spec.kind() == MteInPlaceKind.LARGE_DYNAMO) {
            return type == ModBlockEntities.LARGE_DYNAMO.get()
                    ? (lvl, pos, st, be) -> {
                        if (!lvl.isClientSide
                                && be instanceof LargeDynamoBlockEntity dynamo) {
                            LargeDynamoBlockEntity.serverTick(lvl, pos, st, dynamo);
                        }
                    }
                    : null;
        }
        if (spec.kind() == MteInPlaceKind.LIGHTNING_ROD) {
            return type == ModBlockEntities.LIGHTNING_ROD.get()
                    ? (lvl, pos, st, be) -> {
                        if (!lvl.isClientSide
                                && be instanceof LightningRodBlockEntity rod) {
                            LightningRodBlockEntity.serverTick(lvl, pos, st, rod);
                        }
                    }
                    : null;
        }
        if (spec.kind() == MteInPlaceKind.MATTER_FABRICATOR) {
            return type == ModBlockEntities.MATTER_FABRICATOR.get()
                    ? (lvl, pos, st, be) -> {
                        if (!lvl.isClientSide
                                && be instanceof MatterFabricatorBlockEntity fabricator) {
                            MatterFabricatorBlockEntity.serverTick(
                                    lvl, pos, st, fabricator);
                        }
                    }
                    : null;
        }
        if (spec.kind() == MteInPlaceKind.VON_DA_GRAAGG) {
            return type == ModBlockEntities.VON_DA_GRAAGG.get()
                    ? (lvl, pos, st, be) -> {
                        if (!lvl.isClientSide
                                && be instanceof VonDaGraaggBlockEntity graagg) {
                            VonDaGraaggBlockEntity.serverTick(lvl, pos, st, graagg);
                        }
                    }
                    : null;
        }
        if (FoundryHosts.isCrossing(spec)
                || AnvilHosts.isAnvil(spec)
                || CoinageMoldHosts.isCoinage(spec)
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
