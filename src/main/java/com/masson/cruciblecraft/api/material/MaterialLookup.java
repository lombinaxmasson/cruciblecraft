package com.masson.cruciblecraft.api.material;

import java.util.Optional;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;

public final class MaterialLookup {
    private MaterialLookup() {}

    public static Optional<MaterialDefinition> byId(String id) {
        return MaterialCatalog.find(id);
    }

    public static Optional<MaterialUnits.Entry> byItem(ItemStack stack) {
        return MaterialUnits.resolve(stack);
    }

    public static Optional<Item> item(String materialId, MaterialPrefix form) {
        return byId(materialId)
                .filter(material -> MaterialCatalog.isFormRegistered(material, form))
                .flatMap(material -> item(material, form, MaterialCatalog.runtimePreferences()));
    }

    /**
     * Resolves against an explicit candidate preference snapshot. Recipe reload
     * staging uses this overload; normal runtime callers use the published state.
     */
    public static Optional<Item> item(
            MaterialDefinition material,
            MaterialPrefix form,
            Map<String, String> preferences) {
        if (!MaterialCatalog.isFormRegistered(material, form)) {
            return Optional.empty();
        }
        return BuiltInRegistries.ITEM.getOptional(
                resolveItemId(material, form, preferences));
    }

    public static Optional<ResourceLocation> itemId(String materialId, MaterialPrefix form) {
        return byId(materialId)
                .filter(material -> MaterialCatalog.isFormRegistered(material, form))
                .map(material -> resolveItemId(
                        material,
                        form,
                        MaterialCatalog.runtimePreferences()));
    }

    /**
     * Resolves the canonical registry id without touching a deferred holder.
     * Published preferences are already tag-validated before reaching this path.
     */
    public static ResourceLocation resolveItemId(
            MaterialDefinition material,
            MaterialPrefix form,
            Map<String, String> preferences) {
        if (form.equals(MaterialPrefixes.ORE)) {
            return ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID,
                    material.id() + "_ore");
        }
        String itemId = preferences.get(
                material.id() + "/" + form.serializedId());
        if (itemId == null) {
            itemId = material.formItems().get(form);
        }
        if (itemId == null) {
            return ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID,
                    material.registryName(form));
        }
        ResourceLocation resolved = ResourceLocation.tryParse(itemId);
        if (resolved == null) {
            throw new IllegalArgumentException(
                    "Invalid material item id for " + material.id() + "/"
                            + form.serializedId() + ": " + itemId);
        }
        return resolved;
    }

    /** Tag-based input projection allows every valid unified item. */
    public static Optional<Ingredient> ingredient(String materialId, MaterialPrefix prefix) {
        return byId(materialId)
                .filter(material -> MaterialCatalog.isFormRegistered(material, prefix))
                .flatMap(material -> ingredient(material, prefix));
    }

    public static Optional<Ingredient> ingredient(
            MaterialDefinition material, MaterialPrefix prefix) {
        return MaterialCatalog.isFormRegistered(material, prefix)
                ? Optional.of(Ingredient.of(materialTag(material, prefix)))
                : Optional.empty();
    }

    public static boolean isValidPreference(
            String materialId,
            MaterialPrefix prefix,
            ResourceLocation itemId) {
        return byId(materialId)
                .filter(material -> MaterialCatalog.isFormRegistered(material, prefix))
                .flatMap(material -> BuiltInRegistries.ITEM.getOptional(itemId)
                        .filter(item -> item.builtInRegistryHolder().is(
                                materialTag(material, prefix))))
                .isPresent();
    }

    private static TagKey<Item> materialTag(
            MaterialDefinition material,
            MaterialPrefix prefix) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(
                        prefix.tagNamespace(),
                        prefix.tagDirectory() + "/" + material.tagName()));
    }
}
