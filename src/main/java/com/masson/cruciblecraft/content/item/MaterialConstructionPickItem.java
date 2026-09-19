package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

public final class MaterialConstructionPickItem extends MaterialDiggerItem {
    public MaterialConstructionPickItem(Properties properties) {
        super(
                properties,
                ToolKind.CONSTRUCTION_PICK,
                "item.cruciblecraft.material_construction_pick",
                BlockTags.MINEABLE_WITH_PICKAXE,
                1.0F,
                -2.8F,
                ItemAbilities.DEFAULT_PICKAXE_ACTIONS);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float base = super.getDestroySpeed(stack, state);
        if (base <= 1.0F) {
            return base;
        }
        float doubled = base * 2.0F;
        return ToolMining.isOre(state) ? doubled / 4.0F : doubled;
    }
}
