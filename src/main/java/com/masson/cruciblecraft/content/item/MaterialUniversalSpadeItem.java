package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.neoforged.neoforge.common.ItemAbilities;

/** GT6 universal spade: shovel plus crowbar machine clicks. */
public final class MaterialUniversalSpadeItem extends MaterialDiggerItem {
    public MaterialUniversalSpadeItem(Properties properties) {
        super(
                properties,
                ToolKind.UNIVERSAL_SPADE,
                "item.cruciblecraft.material_universal_spade",
                BlockTags.MINEABLE_WITH_SHOVEL,
                1.5F,
                -3.0F,
                ItemAbilities.DEFAULT_SHOVEL_ACTIONS);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (!canApplyDurabilityDamage(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        InteractionResult crowbar = MachineToolInteractions.crowbar(context);
        if (crowbar.consumesAction()) {
            return crowbar;
        }
        return VanillaToolUseOn.shovel(context);
    }
}
