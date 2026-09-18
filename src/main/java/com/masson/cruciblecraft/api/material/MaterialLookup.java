package com.masson.cruciblecraft.api.material;

import java.util.Optional;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.content.item.PrefixMaterialItem;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialFormHosts;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

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

    public static Optional<Item> item(MaterialDefinition material, MaterialPrefix form) {
        return item(material, form, MaterialCatalog.runtimePreferences());
    }

    /**
     * Resolves against an explicit candidate preference snapshot. Recipe reload
     * staging uses this overload; normal runtime callers use the published state.
     *
     * <p>For shared inventory prefixes this is the prefix Item, not a complete
     * stack. Construct stacks with {@link #stack}.
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
     * Logical identity used by creative-tab plans and workbench recipe plans.
     * Long-tail shared inventory forms keep {@code cruciblecraft:{material}/{form}}
     * so stacks stay distinct even when they share one Item. Public exchange
     * prefixes use the same path as the live unique Item.
     */
    public static ResourceLocation logicalItemId(
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

    /**
     * Live registry id of the Item. Shared long-tail prefixes resolve to
     * {@code cruciblecraft:{prefix}} rather than a per-material path.
     * Public exchange prefixes keep {@code cruciblecraft:{material}/{form}}.
     */
    public static ResourceLocation resolveItemId(
            MaterialDefinition material,
            MaterialPrefix form,
            Map<String, String> preferences) {
        ResourceLocation logical = logicalItemId(material, form, preferences);
        if (material.formItems().containsKey(form)
                || form.equals(MaterialPrefixes.ORE)
                || !MaterialFormHosts.isSharedInventoryForm(material, form)) {
            return logical;
        }
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                MaterialFormHosts.prefixItemPath(form));
    }

    public static ItemStack stack(String materialId, MaterialPrefix form) {
        return stack(materialId, form, 1);
    }

    public static ItemStack stack(String materialId, MaterialPrefix form, int count) {
        return tryStack(materialId, form, count).orElseThrow(() ->
                new IllegalArgumentException(
                        "No live stack for " + materialId + "/"
                                + form.serializedName()));
    }

    public static Optional<ItemStack> tryStack(
            String materialId, MaterialPrefix form, int count) {
        return byId(materialId).flatMap(material ->
                tryStack(material, form, count, MaterialCatalog.runtimePreferences()));
    }

    public static Optional<ItemStack> tryStack(
            MaterialDefinition material, MaterialPrefix form, int count) {
        return tryStack(material, form, count, MaterialCatalog.runtimePreferences());
    }

    public static Optional<ItemStack> tryStack(
            MaterialDefinition material,
            MaterialPrefix form,
            int count,
            Map<String, String> preferences) {
        Optional<Item> item = item(material, form, preferences);
        if (item.isEmpty() || count <= 0) {
            return Optional.empty();
        }
        ItemStack stack = new ItemStack(item.orElseThrow(), count);
        if (MaterialFormHosts.isSharedInventoryForm(material, form)) {
            stack.set(ModComponents.PREFIX_MATERIAL, material.id());
        }
        return Optional.of(stack);
    }

    public static Optional<ItemStack> stackFromLogicalId(String logicalId) {
        ResourceLocation location = ResourceLocation.tryParse(logicalId);
        if (location == null) {
            return Optional.empty();
        }
        Optional<Item> registered = BuiltInRegistries.ITEM.getOptional(location);
        if (registered.isPresent()
                && !(registered.orElseThrow() instanceof PrefixMaterialItem)) {
            return Optional.of(new ItemStack(registered.orElseThrow()));
        }
        if (!CrucibleCraft.MODID.equals(location.getNamespace())) {
            return Optional.empty();
        }
        String path = location.getPath();
        int slash = path.indexOf('/');
        if (slash <= 0 || slash >= path.length() - 1) {
            return Optional.empty();
        }
        MaterialPrefix form = com.masson.cruciblecraft.material.prefix
                .MaterialPrefixCatalog.find(path.substring(slash + 1))
                .orElse(null);
        if (form == null) {
            return Optional.empty();
        }
        return tryStack(path.substring(0, slash), form, 1);
    }

    public static boolean matches(ItemStack stack, String materialId, MaterialPrefix form) {
        return MaterialUnits.resolve(stack)
                .filter(entry -> entry.materialId().equals(materialId)
                        && entry.form().equals(form))
                .isPresent();
    }

    /** Tag-based input projection allows every valid unified item. */
    public static Optional<Ingredient> ingredient(String materialId, MaterialPrefix prefix) {
        return byId(materialId)
                .filter(material -> MaterialCatalog.isFormRegistered(material, prefix))
                .flatMap(material -> ingredient(material, prefix));
    }

    public static Optional<Ingredient> ingredient(
            MaterialDefinition material, MaterialPrefix prefix) {
        if (!MaterialCatalog.isFormRegistered(material, prefix)) {
            return Optional.empty();
        }
        if (MaterialFormHosts.isSharedInventoryForm(material, prefix)) {
            return item(material, prefix, MaterialCatalog.runtimePreferences())
                    .map(resolved -> prefixMaterialIngredient(resolved, material.id()));
        }
        return Optional.of(Ingredient.of(materialTag(material, prefix)));
    }

    /**
     * Non-strict predicate for a shared prefix Item. Only
     * {@link ModComponents#PREFIX_MATERIAL} is required so compact routing can
     * extract a single component key; extra stack defaults stay unmatched.
     */
    public static Ingredient prefixMaterialIngredient(Item item, String materialId) {
        return DataComponentIngredient.of(
                false,
                DataComponentPredicate.builder()
                        .expect(ModComponents.PREFIX_MATERIAL.get(), materialId)
                        .build(),
                item);
    }

    public static Optional<Ingredient> ingredientFromLogicalId(String logicalId) {
        ResourceLocation location = ResourceLocation.tryParse(logicalId);
        if (location == null) {
            return Optional.empty();
        }
        Optional<Item> registered = BuiltInRegistries.ITEM.getOptional(location);
        if (registered.isPresent()
                && !(registered.orElseThrow() instanceof PrefixMaterialItem)) {
            return Optional.of(Ingredient.of(registered.orElseThrow()));
        }
        if (!CrucibleCraft.MODID.equals(location.getNamespace())) {
            return Optional.empty();
        }
        String path = location.getPath();
        int slash = path.indexOf('/');
        if (slash <= 0 || slash >= path.length() - 1) {
            return Optional.empty();
        }
        MaterialPrefix form = com.masson.cruciblecraft.material.prefix
                .MaterialPrefixCatalog.find(path.substring(slash + 1))
                .orElse(null);
        if (form == null) {
            return Optional.empty();
        }
        return ingredient(path.substring(0, slash), form);
    }

    public static boolean isValidPreference(
            String materialId,
            MaterialPrefix prefix,
            ResourceLocation itemId) {
        return byId(materialId)
                .filter(material -> MaterialCatalog.isFormRegistered(material, prefix))
                .flatMap(material -> {
                    if (MaterialFormHosts.isSharedInventoryForm(material, prefix)) {
                        return Optional.empty();
                    }
                    return BuiltInRegistries.ITEM.getOptional(itemId)
                            .filter(item -> !(item instanceof PrefixMaterialItem))
                            .filter(item -> item.builtInRegistryHolder().is(
                                    materialTag(material, prefix)));
                })
                .isPresent();
    }

    public static TagKey<Item> materialTag(
            MaterialDefinition material,
            MaterialPrefix prefix) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(
                        prefix.tagNamespace(),
                        prefix.tagDirectory() + "/" + material.tagName()));
    }
}
