package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.blockentity.BedrockOreBlockEntity;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.worldgen.OreHarvest;

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
 * GT6 {@code WD.setOre} PrefixBlock for netherrack and materials without a
 * registered {@code generates_ore} cube.
 */
public final class GtHostedOreBlock extends Block implements EntityBlock {
    public static final EnumProperty<OreStoneHost> HOST =
            EnumProperty.create("host", OreStoneHost.class);

    public GtHostedOreBlock(Properties properties) {
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
        String material = material(params);
        if (material.isEmpty()) {
            return List.of();
        }
        OreHarvest harvest = OreHarvest.from(params);
        OreStoneHost host = state.getValue(HOST);
        if (harvest.silkTouch() && harvest.fortune() <= 0) {
            return List.of(item(state, material));
        }
        if (harvest.fortune() > 0) {
            ItemStack raw = MaterialLookup.tryStack(
                            material, MaterialPrefixes.RAW_ORE,
                            1 + params.getLevel().random.nextInt(harvest.fortune() + 1))
                    .orElse(ItemStack.EMPTY);
            if (!raw.isEmpty()) {
                return List.of(raw);
            }
        }
        return List.of(GtBrokenOreBlock.item(material, host));
    }

    @Override
    public ItemStack getCloneItemStack(
            LevelReader level, BlockPos pos, BlockState state) {
        String material = "";
        if (level.getBlockEntity(pos) instanceof BedrockOreBlockEntity ore) {
            material = ore.materialId();
        }
        return item(state, material);
    }

    public static ItemStack item(BlockState state, String materialId) {
        ItemStack stack = new ItemStack(state.getBlock());
        stack.set(
                DataComponents.BLOCK_STATE,
                BlockItemStateProperties.EMPTY.with(HOST, state.getValue(HOST)));
        if (materialId != null && !materialId.isEmpty()) {
            stack.set(ModComponents.ORE_MATERIAL.get(), materialId);
        }
        return stack;
    }

    private static String material(LootParams.Builder params) {
        BlockEntity blockEntity =
                params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        if (blockEntity instanceof BedrockOreBlockEntity ore) {
            return ore.materialId();
        }
        return "";
    }
}
