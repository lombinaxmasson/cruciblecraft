package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;

/** Component-driven axe with vanilla NeoForge interactions. */
public final class MaterialAxeItem extends MaterialDiggerItem {
    public MaterialAxeItem(Properties properties) {
        super(
                properties,
                ToolKind.AXE,
                "item.cruciblecraft.material_axe",
                BlockTags.MINEABLE_WITH_AXE,
                5.0F,
                -3.0F,
                ItemAbilities.DEFAULT_AXE_ACTIONS);
    }

    @Override
    protected float baseAttackDamage(Tier tier) {
        if (tier == Tiers.WOOD) {
            return 6.0F;
        }
        if (tier == Tiers.STONE) {
            return 7.0F;
        }
        if (tier == Tiers.IRON) {
            return 6.0F;
        }
        return 5.0F;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return canApplyDurabilityDamage(context.getItemInHand())
                ? VanillaToolUseOn.axe(context)
                : InteractionResult.PASS;
    }
}
