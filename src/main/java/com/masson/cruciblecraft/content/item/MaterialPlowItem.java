package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

/** GT6 plow: snow-family blocks, 3×3 extra breaks via {@link ToolHarvestEvents}. */
public final class MaterialPlowItem extends MaterialDiggerItem {
    public MaterialPlowItem(Properties properties) {
        super(
                properties,
                ToolKind.PLOW,
                "item.cruciblecraft.material_plow",
                BlockTags.MINEABLE_WITH_SHOVEL,
                1.0F,
                -3.0F,
                ItemAbilities.DEFAULT_SHOVEL_ACTIONS);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return material(stack)
                .filter(ignored -> harvestable(state))
                .map(materialId -> ToolMaterialRules.miningSpeed(
                        kind(), materialId))
                .orElse(1.0F);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return material(stack).isPresent() && harvestable(state);
    }

    static boolean harvestable(BlockState state) {
        return state.is(Blocks.SNOW)
                || state.is(Blocks.SNOW_BLOCK)
                || state.is(Blocks.POWDER_SNOW)
                || state.is(BlockTags.SNOW);
    }
}
