package com.masson.cruciblecraft.content.item;

import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MissingMaterialComponent;
import com.masson.cruciblecraft.material.MissingMaterialStackNbtAdapter;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
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
        if (missing.form().equals(MissingMaterialStackNbtAdapter.TOOL_COMPONENT_FORM)
                || missing.form().equals(
                        MissingMaterialStackNbtAdapter.MACHINE_COMPONENT_FORM)) {
            restoreComponentMaterialStack(player, slotId, stack, missing);
            return;
        }
        MaterialPrefix form;
        try {
            form = MaterialPrefixCatalog.require(missing.form());
        } catch (IllegalArgumentException exception) {
            return;
        }
        MaterialLookup.tryStack(missing.materialId(), form, stack.getCount())
                .ifPresent(restored -> restore(player, slotId, stack, restored));
    }

    private static void restoreComponentMaterialStack(
            Player player,
            int slotId,
            ItemStack stack,
            MissingMaterialComponent missing) {
        if (!MaterialCatalog.contains(missing.materialId())) {
            return;
        }
        String componentId = missing.form().equals(
                MissingMaterialStackNbtAdapter.TOOL_COMPONENT_FORM)
                ? MissingMaterialStackNbtAdapter.TOOL_MATERIAL_COMPONENT_ID
                : MissingMaterialStackNbtAdapter.MACHINE_MATERIAL_COMPONENT_ID;
        if (!MissingMaterialStackNbtAdapter.isComponentMaterialValid(
                missing.originalItemId(), componentId, missing.materialId())) {
            return;
        }
        ResourceLocation itemId = ResourceLocation.tryParse(missing.originalItemId());
        if (itemId == null || !BuiltInRegistries.ITEM.containsKey(itemId)) {
            return;
        }
        Item item = BuiltInRegistries.ITEM.get(itemId);
        ItemStack restored = new ItemStack(item, stack.getCount());
        if (missing.form().equals(MissingMaterialStackNbtAdapter.TOOL_COMPONENT_FORM)) {
            restored.set(ModComponents.TOOL_MATERIAL.get(), missing.materialId());
        } else {
            restored.set(ModComponents.MACHINE_MATERIAL.get(), missing.materialId());
        }
        restore(player, slotId, stack, restored);
    }

    private static void restore(
            Player player,
            int slotId,
            ItemStack stack,
            ItemStack restored) {
        restored.setCount(stack.getCount());
        restored.applyComponents(stack.getComponents());
        restored.remove(ModComponents.MISSING_MATERIAL.get());
        player.getInventory().setItem(slotId, restored);
    }

    private static void restore(
            Player player,
            int slotId,
            ItemStack stack,
            Item item) {
        restore(player, slotId, stack, new ItemStack(item, stack.getCount()));
    }
}
