package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.item.tool.InventoryBlockPlacer;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;

/** Component-driven material saw used by processing recipes and harvest. */
public final class MaterialSawItem extends MaterialToolItem {
    public MaterialSawItem(Properties properties) {
        super(
                properties,
                ToolKind.SAW,
                "item.cruciblecraft.material_saw");
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult tool = super.useOn(context);
        if (tool.consumesAction()) {
            return tool;
        }
        if (!canApplyDurabilityDamage(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        return InventoryBlockPlacer.placeSaplingOrWorkbench(context);
    }
}
