package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
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
}
