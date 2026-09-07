package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.neoforged.neoforge.common.ItemAbilities;

/** GT6 gem pick: silk-touch drops via {@link ToolHarvestEvents}. */
public final class MaterialGemPickItem extends MaterialDiggerItem {
    public MaterialGemPickItem(Properties properties) {
        super(
                properties,
                ToolKind.GEM_PICK,
                "item.cruciblecraft.material_gem_pick",
                BlockTags.MINEABLE_WITH_PICKAXE,
                1.0F,
                -2.8F,
                ItemAbilities.DEFAULT_PICKAXE_ACTIONS);
    }
}
