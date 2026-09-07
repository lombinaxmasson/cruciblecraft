package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

/** GT6 scythe: plants/leaves, 3×3 extra breaks via {@link ToolHarvestEvents}. */
public final class MaterialSenseItem extends MaterialDiggerItem {
    public MaterialSenseItem(Properties properties) {
        super(
                properties,
                ToolKind.SENSE,
                "item.cruciblecraft.material_sense",
                BlockTags.MINEABLE_WITH_HOE,
                3.0F,
                -2.4F,
                ItemAbilities.DEFAULT_HOE_ACTIONS);
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
        return state.is(BlockTags.MINEABLE_WITH_HOE)
                || state.is(BlockTags.LEAVES)
                || state.is(BlockTags.REPLACEABLE_BY_TREES);
    }
}
