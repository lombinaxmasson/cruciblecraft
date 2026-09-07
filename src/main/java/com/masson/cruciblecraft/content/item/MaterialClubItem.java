package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/** GT6 club: rock or ingot one-shot with a wooden stick in the grid. */
public final class MaterialClubItem extends MaterialToolItem {
    public MaterialClubItem(Properties properties) {
        super(properties, ToolKind.CLUB, "item.cruciblecraft.material_club");
    }
}
