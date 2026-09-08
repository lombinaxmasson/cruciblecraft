package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;

/** Component-driven hoe with vanilla NeoForge interactions. */
public final class MaterialHoeItem extends MaterialDiggerItem {
    public MaterialHoeItem(Properties properties) {
        super(
                properties,
                ToolKind.HOE,
                "item.cruciblecraft.material_hoe",
                BlockTags.MINEABLE_WITH_HOE,
                0.0F,
                -3.0F,
                ItemAbilities.DEFAULT_HOE_ACTIONS);
    }

    @Override
    protected float baseAttackDamage(Tier tier) {
        return -tier.getAttackDamageBonus();
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult tool = super.useOn(context);
        if (tool.consumesAction()) {
            return tool;
        }
        return canApplyDurabilityDamage(context.getItemInHand())
                ? VanillaToolUseOn.hoe(context)
                : InteractionResult.PASS;
    }
}
