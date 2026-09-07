package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;

public final class MaterialDoubleAxeItem extends MaterialDiggerItem {
    public MaterialDoubleAxeItem(Properties properties) {
        super(
                properties,
                ToolKind.DOUBLE_AXE,
                "item.cruciblecraft.material_double_axe",
                BlockTags.MINEABLE_WITH_AXE,
                6.0F,
                -3.2F,
                ItemAbilities.DEFAULT_AXE_ACTIONS);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return canApplyDurabilityDamage(context.getItemInHand())
                ? VanillaToolUseOn.axe(context)
                : InteractionResult.PASS;
    }
}
