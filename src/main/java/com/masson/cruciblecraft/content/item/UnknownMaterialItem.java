package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MissingMaterialComponent;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class UnknownMaterialItem extends Item {
    public UnknownMaterialItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            List<Component> tooltip,
            TooltipFlag flag) {
        MissingMaterialComponent missing = stack.get(ModComponents.MISSING_MATERIAL.get());
        if (missing != null) {
            tooltip.add(Component.translatable(
                    "tooltip.cruciblecraft.unknown_material",
                    missing.materialId(),
                    missing.form()).withStyle(ChatFormatting.RED));
            tooltip.add(Component.literal(missing.originalItemId()).withStyle(ChatFormatting.DARK_GRAY));
        }
    }

    @Override
    public void inventoryTick(
            ItemStack stack,
            Level level,
            Entity entity,
            int slotId,
            boolean isSelected) {
        if (level.isClientSide || !(entity instanceof Player player)) {
            return;
        }
        MissingMaterialComponent missing = stack.get(ModComponents.MISSING_MATERIAL.get());
        if (missing == null) {
            return;
        }
        MaterialPrefix form;
        try {
            form = MaterialPrefixCatalog.require(missing.form());
        } catch (IllegalArgumentException exception) {
            return;
        }
        MaterialLookup.item(missing.materialId(), form).ifPresent(item -> {
            ItemStack restored = new ItemStack(item, stack.getCount());
            restored.applyComponents(stack.getComponents());
            restored.remove(ModComponents.MISSING_MATERIAL.get());
            player.getInventory().setItem(slotId, restored);
        });
    }
}
