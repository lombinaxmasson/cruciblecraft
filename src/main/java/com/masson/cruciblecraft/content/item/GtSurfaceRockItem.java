package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.content.block.GtSurfaceRockBlock;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Block item for the GT6 32757 overworld surface-rock placer. */
public final class GtSurfaceRockItem extends PebbleBlockItem {
    public GtSurfaceRockItem(
            GtSurfaceRockBlock block,
            Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        tooltip.add(Component.translatable(
                "tooltip.cruciblecraft.surface_rock.material",
                GtSurfaceRockBlock.materialName(getBlock().defaultBlockState())));
    }
}
