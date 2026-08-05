package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Component-driven material wrench used by processing recipes. */
public final class MaterialWrenchItem extends MaterialToolItem {
    public MaterialWrenchItem(Properties properties) {
        super(
                properties,
                ToolKind.WRENCH,
                "item.cruciblecraft.material_wrench");
    }
}
