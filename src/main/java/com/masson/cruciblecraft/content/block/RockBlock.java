package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.content.item.MaterialFormItem;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.worldgen.PebbleBlocks;
import com.masson.cruciblecraft.worldgen.PebbleShape;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6-style small surface rock for one material's {@code rock} form
 * ({@code OP.rockGt}). Positional AABB matching {@code MultiTileEntityRock}.
 */
public final class RockBlock extends Block {
    private final String materialId;

    public RockBlock(String materialId, Properties properties) {
        super(properties);
        this.materialId = materialId;
    }

    public String materialId() {
        return materialId;
    }

    @Override
    public MutableComponent getName() {
        return MaterialFormItem.formName(
                materialId,
                MaterialPrefixCatalog.require("rock")).copy();
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
        PebbleBlocks.dropIfUnsupported(state, level, pos, new ItemStack(this));
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        return PebbleBlocks.collect(level, pos, player, new ItemStack(this));
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(this));
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level,
            BlockPos pos,
            BlockState state) {
        return new ItemStack(this);
    }
}
