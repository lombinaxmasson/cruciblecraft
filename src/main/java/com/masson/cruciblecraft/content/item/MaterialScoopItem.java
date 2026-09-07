package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BeehiveBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

/** GT6 scoop: silk-touch beehives via {@link ToolHarvestEvents}. */
public final class MaterialScoopItem extends MaterialDiggerItem {
    public MaterialScoopItem(Properties properties) {
        super(
                properties,
                ToolKind.SCOOP,
                "item.cruciblecraft.material_scoop",
                BlockTags.MINEABLE_WITH_HOE,
                1.0F,
                -2.4F,
                ItemAbilities.DEFAULT_SHEARS_ACTIONS);
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
        return state.getBlock() instanceof BeehiveBlock
                || state.is(BlockTags.BEEHIVES)
                || state.is(BlockTags.WOOL);
    }
}
