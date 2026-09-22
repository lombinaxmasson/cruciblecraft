package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.worldgen.PebbleBlocks;
import com.masson.cruciblecraft.worldgen.StickShape;

import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code MultiTileEntityStick} (MTE 32756), used by
 * {@code WorldgenSticks}. It is a worldgen-only surface object and yields the
 * vanilla wooden stick on collection or break.
 */
public final class GtStickBlock extends Block {
    public GtStickBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.WOOD)
                .strength(0.25F)
                .sound(SoundType.WOOD)
                .noCollission()
                .noOcclusion()
                .isViewBlocking((state, level, pos) -> false)
                .isSuffocating((state, level, pos) -> false)
                .pushReaction(PushReaction.DESTROY)
                .ignitedByLava()
                .noLootTable());
    }

    @Override
    protected long getSeed(BlockState state, BlockPos pos) {
        return StickShape.seed(pos);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return StickShape.shape(pos);
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
        return PebbleBlocks.canSurvive(level, pos);
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            BlockPos neighborPos,
            boolean movedByPiston) {
        PebbleBlocks.dropIfUnsupported(state, level, pos, stick());
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        return PebbleBlocks.collect(level, pos, player, stick());
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(stick());
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state) {
        return stick();
    }

    private static ItemStack stick() {
        return new ItemStack(Items.STICK);
    }
}
