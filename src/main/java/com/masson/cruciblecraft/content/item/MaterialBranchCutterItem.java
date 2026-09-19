package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

public final class MaterialBranchCutterItem extends MaterialDiggerItem {
    public MaterialBranchCutterItem(Properties properties) {
        super(
                properties,
                ToolKind.BRANCH_CUTTER,
                "item.cruciblecraft.material_branch_cutter",
                BlockTags.MINEABLE_WITH_AXE,
                2.0F,
                -2.4F,
                ItemAbilities.DEFAULT_SHEARS_ACTIONS);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return material(stack)
                .map(materialId -> ToolMining.destroySpeed(
                        kind(), materialId, state))
                .orElse(1.0F);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return material(stack).isPresent()
                && ToolMining.correctTool(kind(), state);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        InteractionResult tool = super.useOn(context);
        if (tool.consumesAction()) {
            return tool;
        }
        return canApplyDurabilityDamage(context.getItemInHand())
                ? VanillaToolUseOn.axe(context)
                : InteractionResult.PASS;
    }
}
