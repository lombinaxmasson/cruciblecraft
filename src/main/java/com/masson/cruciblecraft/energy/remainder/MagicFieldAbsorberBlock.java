package com.masson.cruciblecraft.energy.remainder;

import java.util.Set;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
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

/** GT6 magic field absorber. Reads the block above and emits from the front. */
public final class MagicFieldAbsorberBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final Set<Block> SKULLS = Set.of(
            Blocks.SKELETON_SKULL,
            Blocks.SKELETON_WALL_SKULL,
            Blocks.WITHER_SKELETON_SKULL,
            Blocks.WITHER_SKELETON_WALL_SKULL,
            Blocks.ZOMBIE_HEAD,
            Blocks.ZOMBIE_WALL_HEAD,
            Blocks.PLAYER_HEAD,
            Blocks.PLAYER_WALL_HEAD,
            Blocks.CREEPER_HEAD,
            Blocks.CREEPER_WALL_HEAD);

    private final RemainderDevice device;

    public MagicFieldAbsorberBlock(RemainderDevice device, Properties properties) {
        super(properties);
        this.device = device;
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(LIT, false));
    }

    public RemainderDevice device() {
        return device;
    }

    public static MagicFieldOffer.Source sourceOf(Block block) {
        if (block == Blocks.DRAGON_EGG) {
            return MagicFieldOffer.Source.DRAGON_EGG;
        }
        if (SKULLS.contains(block)) {
            return MagicFieldOffer.Source.SKULL;
        }
        return MagicFieldOffer.Source.NONE;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, LIT);
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
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MagicFieldAbsorberBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(
            Level level, BlockState state, BlockEntityType<T> type) {
        return !level.isClientSide
                && type == ModBlockEntities.MAGIC_FIELD_ABSORBER.get()
                ? (current, pos, currentState, blockEntity) ->
                        MagicFieldAbsorberBlockEntity.serverTick(
                                current,
                                pos,
                                currentState,
                                (MagicFieldAbsorberBlockEntity) blockEntity)
                : null;
    }

    public static final class MagicFieldAbsorberBlockEntity extends BlockEntity
            implements IEnergyHandler {
        private final RemainderDevice device;
        private MagicFieldOffer offer;

        public MagicFieldAbsorberBlockEntity(BlockPos pos, BlockState state) {
            super(ModBlockEntities.MAGIC_FIELD_ABSORBER.get(), pos, state);
            if (!(state.getBlock() instanceof MagicFieldAbsorberBlock block)) {
                throw new IllegalStateException(
                        "Magic absorber bound to " + state.getBlock());
            }
            this.device = block.device();
        }

        public RemainderDevice device() {
            return device;
        }

        public MagicFieldOffer offer() {
            return offer;
        }

        public static void serverTick(
                Level level,
                BlockPos pos,
                BlockState state,
                MagicFieldAbsorberBlockEntity absorber) {
            absorber.scan();
        }

        public void scan() {
            Level level = getLevel();
            if (level == null || level.isClientSide) {
                return;
            }
            Block above = level.getBlockState(worldPosition.above()).getBlock();
            offer = MagicFieldOffer.of(sourceOf(above));
            boolean active = offer != null;
            if (active) {
                Direction facing = getBlockState().getValue(FACING);
                EnergyEmitter.pushToSide(
                        level,
                        worldPosition,
                        offer.type(),
                        offer.packetSize(),
                        offer.packetAmount(),
                        facing);
            }
            BlockState state = getBlockState();
            if (state.getValue(LIT) != active) {
                level.setBlock(
                        worldPosition,
                        state.setValue(LIT, active),
                        Block.UPDATE_ALL);
            }
        }

        @Override
        public long outputSize(EnergyType type, Direction side) {
            if (offer == null || type != offer.type()) {
                return 0L;
            }
            Direction facing = getBlockState().getValue(FACING);
            if (side != null && side != facing) {
                return 0L;
            }
            return offer.packetSize();
        }
    }
}
