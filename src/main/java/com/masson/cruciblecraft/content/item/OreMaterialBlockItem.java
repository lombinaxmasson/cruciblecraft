package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.content.block.BedrockOreBlock;
import com.masson.cruciblecraft.content.block.GtBrokenOreBlock;
import com.masson.cruciblecraft.content.block.GtSmallOreBlock;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/** PrefixBlock-style ore item: material lives on {@code ORE_MATERIAL}. */
public final class OreMaterialBlockItem extends BlockItem {
    public OreMaterialBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        String materialId = stack.getOrDefault(ModComponents.ORE_MATERIAL.get(), "");
        Component materialName = MaterialFormItem.materialDisplayName(materialId);
        String key = getBlock() instanceof GtBrokenOreBlock
                ? "block.cruciblecraft.gt_broken_ore.named"
                : getBlock() instanceof GtSmallOreBlock
                        ? "block.cruciblecraft.gt_small_ore.named"
                        : "block.cruciblecraft.gt_hosted_ore.named";
        if (materialId.isEmpty()) {
            return super.getName(stack);
        }
        return Component.translatable(key, materialName);
    }

    @Override
    protected boolean updateCustomBlockEntityTag(
            BlockPos pos,
            Level level,
            Player player,
            ItemStack stack,
            BlockState state) {
        String materialId = stack.getOrDefault(ModComponents.ORE_MATERIAL.get(), "");
        if (!materialId.isEmpty()) {
            BedrockOreBlock.placeMaterial(level, pos, materialId);
        }
        return super.updateCustomBlockEntityTag(pos, level, player, stack, state);
    }
}
