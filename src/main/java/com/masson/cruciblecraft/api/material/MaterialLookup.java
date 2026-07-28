package com.masson.cruciblecraft.api.material;

import java.util.Optional;

import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;

public final class MaterialLookup {
    private MaterialLookup() {}

    public static Optional<MaterialDefinition> byId(String id) {
        return MaterialCatalog.values().stream()
                .filter(material -> material.id().equals(id))
                .findFirst();
    }

    public static Optional<MaterialUnits.Entry> byItem(ItemStack stack) {
        return MaterialUnits.resolve(stack);
    }

    public static Optional<Item> item(String materialId, MaterialForm form) {
        return byId(materialId).flatMap(material -> {
            String override = material.formItems().get(form);
            if (override != null) {
                return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(override));
            }
            try {
                return Optional.of(ModItems.materialItem(materialId, form).get());
            } catch (IllegalArgumentException exception) {
                return Optional.empty();
            }
        });
    }

    public static Optional<ResourceLocation> itemId(String materialId, MaterialForm form) {
        return byId(materialId)
                .filter(material -> material.forms().contains(form))
                .map(material -> {
                    String override = material.formItems().get(form);
                    return override == null
                            ? ResourceLocation.fromNamespaceAndPath(
                                    "cruciblecraft",
                                    material.registryName(form))
                            : ResourceLocation.parse(override);
                });
    }
}
