package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/**
 * GT6 metal/gem knife. Flint stays {@link FlintKnifeItem}; this is the
 * plate/plate-gem one-shot with a wooden stick handle in the grid.
 */
public final class MaterialKnifeItem extends MaterialToolItem {
    public MaterialKnifeItem(Properties properties) {
        super(properties, ToolKind.KNIFE, "item.cruciblecraft.material_knife");
    }
}
