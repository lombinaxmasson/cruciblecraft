package com.masson.cruciblecraft.content.item;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialComponentPolicy;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code MultiTileEntityCoin}: a tiny-plate of metal stamped in the
 * coinage mold. Recycles as {@code tiny_plate} units; not a catalog prefix.
 */
public final class CoinItem extends Item implements MaterialComponentPolicy {
    public CoinItem(Properties properties) {
        super(properties);
    }

    public static boolean allowed(String materialId) {
        return MaterialCatalog.find(materialId)
                .filter(material -> MaterialCatalog.isFormRegistered(
                        material, MaterialPrefixes.TINY_PLATE))
                .isPresent();
    }

    public static ItemStack stack(String materialId, int count) {
        ItemStack stack = new ItemStack(ModItems.COIN.get(), Math.max(1, count));
        stack.set(ModComponents.PREFIX_MATERIAL, materialId);
        return stack;
    }

    @Override
    public String materialComponentId() {
        return PREFIX_MATERIAL_COMPONENT_ID;
    }

    @Override
    public String missingMaterialForm() {
        return "coin";
    }

    @Override
    public boolean isPersistedMaterialAllowed(String materialId) {
        return allowed(materialId);
    }

    @Override
    public Component getName(ItemStack stack) {
        String materialId = stack.get(ModComponents.PREFIX_MATERIAL);
        if (materialId == null || !isPersistedMaterialAllowed(materialId)) {
            return super.getName(stack);
        }
        return Component.translatableWithFallback(
                "item.cruciblecraft.coin",
                "%s Coin",
                MaterialFormItem.materialDisplayName(materialId));
    }
}
