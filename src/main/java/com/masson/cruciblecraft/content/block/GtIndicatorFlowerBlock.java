package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.worldgen.IndicatorFlower;
import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GT6 {@code BlockFlowersA}/{@code BlockFlowersB}: indicator plants for
 * {@code WorldgenOresBedrock}. Dye/squeezer recipes stay with those machines.
 */
public final class GtIndicatorFlowerBlock extends BushBlock {
    public static final MapCodec<GtIndicatorFlowerBlock> CODEC =
            simpleCodec(GtIndicatorFlowerBlock::new);
    public static final EnumProperty<IndicatorFlower> FLOWER =
            EnumProperty.create("flower", IndicatorFlower.class);
    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 10.0, 11.0);

    public GtIndicatorFlowerBlock(Properties properties) {
        super(properties);
        registerDefaultState(
                stateDefinition.any().setValue(FLOWER, IndicatorFlower.ORECHID));
    }

    public GtIndicatorFlowerBlock() {
        this(BlockBehaviour.Properties.of()
                .mapColor(MapColor.PLANT)
                .noCollission()
                .instabreak()
                .sound(SoundType.GRASS)
                .offsetType(BlockBehaviour.OffsetType.XZ)
                .pushReaction(PushReaction.DESTROY)
                .noLootTable());
    }

    @Override
    public MapCodec<GtIndicatorFlowerBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FLOWER);
    }

    @Override
    protected VoxelShape getShape(
            BlockState state,
            BlockGetter level,
            BlockPos pos,
            CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return canStayOn(state, level.getBlockState(pos.below()));
    }

    public static boolean canStayOn(BlockState flower, BlockState contact) {
        IndicatorFlower variant = flower.getValue(FLOWER);
        if (variant.desert()) {
            return contact.is(BlockTags.SAND) || contact.is(BlockTags.TERRACOTTA);
        }
        return contact.is(BlockTags.DIRT);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(item(state));
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state) {
        return item(state);
    }

    public static ItemStack item(BlockState state) {
        ItemStack stack = new ItemStack(state.getBlock());
        stack.set(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(FLOWER, state.getValue(FLOWER)));
        return stack;
    }

    public static Component displayName(BlockState state) {
        return Component.translatable(
                "block.cruciblecraft.gt_indicator_flower."
                        + state.getValue(FLOWER).getSerializedName());
    }
}
