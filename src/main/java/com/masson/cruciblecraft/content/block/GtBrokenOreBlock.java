package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.BedrockOreBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/**
 * GT6 {@code BlocksGT.oreBroken} / {@code oreBrokenNetherrack}: cobble-host
 * PrefixBlock dropped by the bedrock drill and by hosted ores.
 */
public final class GtBrokenOreBlock extends Block implements EntityBlock {
    public static final EnumProperty<OreStoneHost> HOST =
            EnumProperty.create("host", OreStoneHost.class);

    public GtBrokenOreBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(HOST, OreStoneHost.STONE));
    }

    @Override
    protected void createBlockStateDefinition(
            StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(HOST);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new BedrockOreBlockEntity(pos, state);
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        BlockEntity blockEntity =
                params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        String material = "";
        if (blockEntity instanceof BedrockOreBlockEntity ore) {
            material = ore.materialId();
        }
        return List.of(item(material, state.getValue(HOST)));
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state) {
        String material = "";
        if (level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore) {
            material = ore.materialId();
        }
        return item(material, state.getValue(HOST));
    }

    public static ItemStack item(String materialId, OreStoneHost host) {
        ItemStack stack = new ItemStack(ModBlocks.GT_BROKEN_ORE.get());
        stack.set(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(HOST, host));
        if (materialId != null && !materialId.isEmpty()) {
            stack.set(ModComponents.ORE_MATERIAL.get(), materialId);
        }
        return stack;
    }
}
