package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/**
 * GT6 knife. Flint is the early {@code SX} harvest craft; metals/gems use
 * plate or plate-gem with a wooden stick in the grid.
 */
public final class MaterialKnifeItem extends MaterialToolItem {
    public MaterialKnifeItem(Properties properties) {
        super(properties, ToolKind.KNIFE, "item.cruciblecraft.material_knife");
    }
}
