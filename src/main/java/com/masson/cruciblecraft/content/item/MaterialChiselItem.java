package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Component-driven material chisel used by processing recipes. */
public final class MaterialChiselItem extends MaterialToolItem {
    public MaterialChiselItem(Properties properties) {
        super(
                properties,
                ToolKind.CHISEL,
                "item.cruciblecraft.material_chisel");
    }
}
