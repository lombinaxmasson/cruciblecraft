package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Component-driven material screwdriver used by processing recipes. */
public final class MaterialScrewdriverItem extends MaterialToolItem {
    public MaterialScrewdriverItem(Properties properties) {
        super(
                properties,
                ToolKind.SCREWDRIVER,
                "item.cruciblecraft.material_screwdriver");
    }
}
