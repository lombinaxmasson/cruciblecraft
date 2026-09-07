package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.ItemAbilities;

public final class MaterialScissorsItem extends MaterialDiggerItem {
    public MaterialScissorsItem(Properties properties) {
        super(
                properties,
                ToolKind.SCISSORS,
                "item.cruciblecraft.material_scissors",
                BlockTags.MINEABLE_WITH_HOE,
                0.0F,
                -2.0F,
                ItemAbilities.DEFAULT_SHEARS_ACTIONS);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        if (material(stack).isEmpty()) {
            return 1.0F;
        }
        if (state.is(BlockTags.LEAVES)
                || state.is(BlockTags.WOOL)
                || state.is(BlockTags.WOOL_CARPETS)) {
            return 15.0F;
        }
        return super.getDestroySpeed(stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return material(stack).isPresent()
                && (state.is(BlockTags.LEAVES)
                        || state.is(BlockTags.WOOL)
                        || super.isCorrectToolForDrops(stack, state));
    }
}
