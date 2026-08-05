package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Component-driven hammer shared by anvil interaction and T4 recipes. */
public final class SmithingHammerItem extends MaterialToolItem {
    public SmithingHammerItem(Properties properties) {
        super(
                properties,
                ToolKind.SMITHING_HAMMER,
                "item.cruciblecraft.smithing_hammer");
    }
}
