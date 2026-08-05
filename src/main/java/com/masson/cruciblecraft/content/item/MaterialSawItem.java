package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Component-driven material saw used by processing recipes. */
public final class MaterialSawItem extends MaterialToolItem {
    public MaterialSawItem(Properties properties) {
        super(
                properties,
                ToolKind.SAW,
                "item.cruciblecraft.material_saw");
    }
}
