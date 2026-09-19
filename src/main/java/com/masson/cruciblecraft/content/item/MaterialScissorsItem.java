package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

public final class MaterialScissorsItem extends MaterialDiggerItem {
    public MaterialScissorsItem(Properties properties) {
        super(
                properties,
                ToolKind.SCISSORS,
                "item.cruciblecraft.material_scissors",
                BlockTags.MINEABLE_WITH_HOE,
                0.0F,
                -2.0F,
                ItemAbilities.DEFAULT_SHEARS_ACTIONS);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return material(stack)
                .map(materialId -> ToolMining.destroySpeed(
                        kind(), materialId, state))
                .orElse(1.0F);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return material(stack).isPresent()
                && ToolMining.correctTool(kind(), state);
    }
}
