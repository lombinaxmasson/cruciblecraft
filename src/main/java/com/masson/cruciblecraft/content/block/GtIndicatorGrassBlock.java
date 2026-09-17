package com.masson.cruciblecraft.content.block;

import java.util.List;

import com.masson.cruciblecraft.worldgen.IndicatorGrass;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.storage.loot.LootParams;

/**
 * GT6 {@code BlockGrass}: dyed indicator grass that does not spread. Drops dirt.
 */
public final class GtIndicatorGrassBlock extends Block {
    public static final EnumProperty<IndicatorGrass> GRASS =
            EnumProperty.create("grass", IndicatorGrass.class);

    public GtIndicatorGrassBlock() {
        super(BlockBehaviour.Properties.of()
                .mapColor(MapColor.GRASS)
                .strength(0.6F)
                .sound(SoundType.GRASS)
                .noLootTable());
        registerDefaultState(
                stateDefinition.any().setValue(GRASS, IndicatorGrass.MEDIUM));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(GRASS);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        return List.of(new ItemStack(Items.DIRT));
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
                BlockItemStateProperties.EMPTY.with(GRASS, state.getValue(GRASS)));
        return stack;
    }

    public static Component displayName(BlockState state) {
        return Component.translatable(
                "block.cruciblecraft.gt_indicator_grass."
                        + state.getValue(GRASS).getSerializedName());
    }

    public static boolean isPlantableGrass(BlockState state) {
        return state.is(Blocks.GRASS_BLOCK)
                || state.getBlock() instanceof GtIndicatorGrassBlock;
    }
}
