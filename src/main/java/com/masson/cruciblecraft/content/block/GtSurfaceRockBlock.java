package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
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
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code MultiTileEntityRock} 32757 overworld placer: copied terrain look,
 * positional AABB, no collision, empty / flint / meteoric loot.
 */
public final class GtSurfaceRockBlock extends Block {
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
            Block.popResource(level, pos, loot(state));
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
            ItemStack drop = loot(state);
            if (!player.addItem(drop)) {
                Block.popResource(level, pos, drop);
            }
            level.removeBlock(pos, false);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(loot(state));
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state) {
        return loot(state);
    }

    public static ItemStack loot(BlockState state) {
        return switch (state.getValue(SurfaceRockContents.PROPERTY)) {
            case FLINT -> new ItemStack(Items.FLINT);
            case METEORIC_ROCK -> new ItemStack(
                    ModItems.materialItem(
                            "meteoric_iron",
                            MaterialPrefixCatalog.require("rock")).get());
            case METEORIC_RAW -> new ItemStack(
                    ModItems.materialItem("meteoric_iron", MaterialPrefixes.RAW_ORE).get());
            case EMPTY -> new ItemStack(
                    ModItems.materialItem(
                            "stone",
                            MaterialPrefixCatalog.require("rock")).get());
        };
    }

    public static Component materialName(BlockState state) {
        return loot(state).getHoverName();
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
