package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.ItemAbilities;

/** Component-driven pickaxe with GT6 material stats projected to vanilla tiers. */
public final class MaterialPickaxeItem extends MaterialDiggerItem {
    public MaterialPickaxeItem(Properties properties) {
        super(
                properties,
                ToolKind.PICKAXE,
                "item.cruciblecraft.material_pickaxe",
                BlockTags.MINEABLE_WITH_PICKAXE,
                1.0F,
                -2.8F,
                ItemAbilities.DEFAULT_PICKAXE_ACTIONS);
    }
}
