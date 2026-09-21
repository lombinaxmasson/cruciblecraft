package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/**
 * GT6 {@code AdvancedCraftingTool(MAGNIFYING_GLASS, lens)}: stick plus a live
 * lens, with the lens material persisted on {@code tool_material}.
 */
public final class MagnifyingGlassItem extends MaterialToolItem {
    public MagnifyingGlassItem(Properties properties) {
        super(
                properties,
                ToolKind.MAGNIFYING_GLASS,
                "item.cruciblecraft.material_magnifying_glass");
    }
}
