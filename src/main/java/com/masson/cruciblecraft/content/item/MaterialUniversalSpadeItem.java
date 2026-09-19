package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.item.tool.InventoryBlockPlacer;
import com.masson.cruciblecraft.content.item.tool.ToolMining;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

/** GT6 universal spade: shovel plus crowbar clicks, extra harvest, torch place. */
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
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (material(stack).isEmpty()) {
            return 1.0F;
        }
        if (state.is(BlockTags.MINEABLE_WITH_SHOVEL)
                || ToolMining.mineable(kind(), state)) {
            return ToolMining.destroySpeed(
                    kind(), material(stack).orElseThrow(), state);
        }
        return 1.0F;
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
        if (!canApplyDurabilityDamage(context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        InteractionResult shovel = VanillaToolUseOn.shovel(context);
        if (shovel.consumesAction()) {
            return shovel;
        }
        InteractionResult hoe = VanillaToolUseOn.hoe(context);
        if (hoe.consumesAction()) {
            return hoe;
        }
        InteractionResult plug = InventoryBlockPlacer.plugLeak(context);
        if (plug.consumesAction()) {
            return plug;
        }
        return InventoryBlockPlacer.placeTorch(context);
    }
}
