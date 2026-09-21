package com.masson.cruciblecraft.content.block;

import java.util.List;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.blockentity.BedrockOreBlockEntity;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.worldgen.DropsSmallOre;
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
 * GT6 {@code WD.setSmallOre} muffin/tendril cubes. Drops follow
 * {@code Drops_SmallOre}.
 */
public final class GtSmallOreBlock extends Block implements EntityBlock {
    public static final EnumProperty<OreStoneHost> HOST =
            EnumProperty.create("host", OreStoneHost.class);

    public GtSmallOreBlock(Properties properties) {
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
        BlockPos pos = BlockPos.containing(
                params.getParameter(LootContextParams.ORIGIN));
        OreHarvest harvest = OreHarvest.from(params);
        return DropsSmallOre.drops(
                material, state.getValue(HOST), pos, harvest.fortune(), harvest.silkTouch());
    }

    public static ItemStack drop(String materialId) {
        if (materialId == null || materialId.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return MaterialLookup.tryStack(materialId, MaterialPrefixes.CRUSHED_ORE, 1)
                .or(() -> MaterialLookup.tryStack(materialId, MaterialPrefixes.RAW_ORE, 1))
                .or(() -> MaterialLookup.tryStack(materialId, MaterialPrefixes.DUST, 1))
                .orElse(ItemStack.EMPTY);
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
}
