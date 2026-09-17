package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.blockentity.GtSurfaceRockBlockEntity;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.PebbleShape;
import com.masson.cruciblecraft.worldgen.SurfaceRockAppearance;
import com.masson.cruciblecraft.worldgen.SurfaceRockContents;
import com.masson.cruciblecraft.worldgen.SurfaceRockFeature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code MultiTileEntityRock} 32757 overworld placer: copied terrain look,
 * positional AABB, no collision. WorldgenRocks uses contents; stone-layer
 * pebbles store {@code tLastRock} on the block entity.
 */
public final class GtSurfaceRockBlock extends Block implements EntityBlock {
    public GtSurfaceRockBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.STONE)
                .strength(0.25F)
                .sound(SoundType.STONE)
                .noCollission()
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .pushReaction(PushReaction.DESTROY));
        registerDefaultState(
                stateDefinition.any()
                        .setValue(SurfaceRockAppearance.PROPERTY, SurfaceRockAppearance.STONE)
                        .setValue(SurfaceRockContents.PROPERTY, SurfaceRockContents.EMPTY));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(SurfaceRockAppearance.PROPERTY, SurfaceRockContents.PROPERTY);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState()
                .setValue(
                        SurfaceRockAppearance.PROPERTY,
                        SurfaceRockFeature.appearance(
                                context.getLevel().getBlockState(
                                        context.getClickedPos().below())))
                .setValue(SurfaceRockContents.PROPERTY, SurfaceRockContents.EMPTY);
    }

    @Override
    protected long getSeed(BlockState state, BlockPos pos) {
        return PebbleShape.seed(pos);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return PebbleShape.shape(pos);
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return Shapes.empty();
    }

    @Override
    protected VoxelShape getOcclusionShape(
            BlockState state, BlockGetter level, BlockPos pos) {
        return Shapes.empty();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        return level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        if (level.isClientSide) {
            return;
        }
        if (!state.canSurvive(level, pos) || adjacentLiquid(level, pos)) {
            Block.popResource(level, pos, loot(state, level.getBlockEntity(pos)));
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!level.isClientSide) {
            ItemStack drop = loot(state, level.getBlockEntity(pos));
            if (!player.addItem(drop)) {
                Block.popResource(level, pos, drop);
            }
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(loot(
                state,
                params.getOptionalParameter(LootContextParams.BLOCK_ENTITY)));
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state) {
        return loot(state, level.getBlockEntity(pos));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new GtSurfaceRockBlockEntity(pos, state);
    }

    public static ItemStack loot(BlockState state) {
        return loot(state, null);
    }

    public static ItemStack loot(BlockState state, @Nullable BlockEntity blockEntity) {
        return switch (state.getValue(SurfaceRockContents.PROPERTY)) {
            case FLINT -> new ItemStack(Items.FLINT);
            case METEORIC_ROCK -> new ItemStack(
                    ModItems.materialItem(
                            "meteoric_iron",
                            MaterialPrefixCatalog.require("rock")).get());
            case METEORIC_RAW -> MaterialLookup.stack(
                    "meteoric_iron", MaterialPrefixes.RAW_ORE);
            case EMPTY -> layerOrStone(blockEntity);
        };
    }

    public static Component materialName(BlockState state) {
        return materialName(state, null);
    }

    public static Component materialName(
            BlockState state, @Nullable BlockEntity blockEntity) {
        return loot(state, blockEntity).getHoverName();
    }

    private static ItemStack layerOrStone(@Nullable BlockEntity blockEntity) {
        if (blockEntity instanceof GtSurfaceRockBlockEntity rock
                && rock.hasMaterial()) {
            if (rock.rawOre()) {
                return MaterialLookup.tryStack(
                                rock.materialId(), MaterialPrefixes.RAW_ORE, 1)
                        .orElseGet(() -> rockDrop(rock.materialId()));
            }
            return rockDrop(rock.materialId());
        }
        return rockDrop("stone");
    }

    private static ItemStack rockDrop(String materialId) {
        return new ItemStack(ModItems.materialItem(
                materialId,
                MaterialPrefixCatalog.require("rock")).get());
    }

    private static boolean adjacentLiquid(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (direction == Direction.DOWN) {
                continue;
            }
            if (!level.getFluidState(pos.relative(direction)).isEmpty()) {
                return true;
            }
        }
        return false;
    }
}
