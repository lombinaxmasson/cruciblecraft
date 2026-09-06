package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** Component-driven GT6 monkey wrench used for directional machine controls. */
public final class MaterialMonkeyWrenchItem extends MaterialToolItem {
    public MaterialMonkeyWrenchItem(Properties properties) {
        super(
                properties,
                ToolKind.MONKEY_WRENCH,
                "item.cruciblecraft.material_monkey_wrench");
    }
}
