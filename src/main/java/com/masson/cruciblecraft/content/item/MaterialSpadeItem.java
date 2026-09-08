package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;

public final class MaterialSpadeItem extends MaterialDiggerItem {
    public MaterialSpadeItem(Properties properties) {
        super(
                properties,
                ToolKind.SPADE,
                "item.cruciblecraft.material_spade",
                BlockTags.MINEABLE_WITH_SHOVEL,
                1.5F,
                -3.0F,
                ItemAbilities.DEFAULT_SHOVEL_ACTIONS);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult tool = super.useOn(context);
        if (tool.consumesAction()) {
            return tool;
        }
        return canApplyDurabilityDamage(context.getItemInHand())
                ? VanillaToolUseOn.shovel(context)
                : InteractionResult.PASS;
    }
}
