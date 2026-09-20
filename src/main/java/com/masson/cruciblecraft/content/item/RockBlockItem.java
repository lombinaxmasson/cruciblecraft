package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.localization.RockFormNames;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Placeable GT6 {@code OP.rockGt} item; sneak places, standing click picks up. */
public final class RockBlockItem extends PebbleBlockItem implements MaterialFormItem {
    public RockBlockItem(RockBlock block, Item.Properties properties) {
        super(block, properties);
    }

    @Override
    public String materialId() {
        return ((RockBlock) getBlock()).materialId();
    }

    @Override
    public MaterialPrefix form() {
        return MaterialPrefixCatalog.require("rock");
    }

    @Override
    public Component getName(ItemStack stack) {
        return materialFormName();
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (RockFormNames.indicatesOccurrence(materialId())) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.rock.indicates",
                    MaterialFormItem.materialDisplayName(materialId())));
        }
    }
}
