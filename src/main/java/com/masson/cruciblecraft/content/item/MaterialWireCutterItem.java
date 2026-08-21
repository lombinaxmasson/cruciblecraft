package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Component-driven material wire cutter used to set cable connections. */
public final class MaterialWireCutterItem extends MaterialToolItem {
    public MaterialWireCutterItem(Properties properties) {
        super(
                properties,
                ToolKind.WIRE_CUTTER,
                "item.cruciblecraft.material_wire_cutter");
    }
}
