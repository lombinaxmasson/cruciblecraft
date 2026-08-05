package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Durability-bearing workshop catalyst used by assembler recipes. */
public final class MaterialFileItem extends MaterialToolItem {
    public MaterialFileItem(Properties properties) {
        super(properties, ToolKind.FILE, "item.cruciblecraft.material_file");
    }
}
