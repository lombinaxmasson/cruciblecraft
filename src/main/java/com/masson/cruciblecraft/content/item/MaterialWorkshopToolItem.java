package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Durability workshop tool without a dedicated harvest class. */
public final class MaterialWorkshopToolItem extends MaterialToolItem {
    public MaterialWorkshopToolItem(
            Properties properties, ToolKind kind, String nameKey) {
        super(properties, kind, nameKey);
    }
}
