package com.masson.cruciblecraft.recipe.gt;

import java.util.Optional;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialFormHosts;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.recipe.crafting.CraftingTools;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/**
 * Long-tail slash ids rewrite onto the shared prefix Item plus
 * {@code prefix_material}. Public-exchange slash ids and leftover prefix Item
 * + component JSON rewrite onto the unique Item, never a {@code c:} tag.
 */
public final class PrefixMaterialItemCodecs {
    public static final Codec<Ingredient> INGREDIENT = Codec.of(
            Ingredient.CODEC_NONEMPTY,
            PrefixMaterialItemCodecs::decodeIngredient);
    public static final Codec<ItemStack> ITEM_STACK = Codec.of(
            ItemStack.STRICT_CODEC,
            PrefixMaterialItemCodecs::decodeItemStack);

    private PrefixMaterialItemCodecs() {}

    public static Optional<Ingredient> rewriteIngredient(ResourceLocation itemId) {
        Parsed parsed = parseSharedInventoryId(itemId).orElse(null);
        if (parsed == null) {
            return Optional.empty();
        }
        return MaterialLookup.ingredient(parsed.material(), parsed.form());
    }

    /**
     * Public-exchange and unique-hosted slash ids rewrite onto the live unique
     * Item. Compact shard routing cannot index {@code c:} tags.
     */
    public static Optional<Ingredient> rewriteUniqueItem(ResourceLocation itemId) {
        Parsed parsed = parseUniqueLiveId(itemId).orElse(null);
        if (parsed == null) {
            return Optional.empty();
        }
        return MaterialLookup.item(parsed.material(), parsed.form()).map(Ingredient::of);
    }

    public static Optional<ItemStack> rewriteStack(ResourceLocation itemId, int count) {
        Parsed parsed = parseSharedInventoryId(itemId).orElse(null);
        if (parsed == null || count <= 0) {
            return Optional.empty();
        }
        return MaterialLookup.tryStack(parsed.material().id(), parsed.form(), count);
    }

    public static Optional<ItemStack> rewriteUniqueStack(
            ResourceLocation itemId, int count) {
        Parsed parsed = parseUniqueLiveId(itemId).orElse(null);
        if (parsed == null || count <= 0) {
            return Optional.empty();
        }
        return MaterialLookup.item(parsed.material(), parsed.form())
                .filter(item -> count <= item.getDefaultMaxStackSize())
                .map(item -> new ItemStack(item, count));
    }

    public static Optional<Ingredient> rewriteTag(ResourceLocation tagId) {
        if (tagId == null) {
            return Optional.empty();
        }
        Optional<Ingredient> craftingTool = CraftingTools.tryTag(tagId);
        if (craftingTool.isPresent()) {
            return craftingTool;
        }
        if (!MaterialCatalog.isBootstrapped()) {
            return Optional.empty();
        }
        if (CrucibleCraft.MODID.equals(tagId.getNamespace())
                && "any_rubber_plates".equals(tagId.getPath())) {
            return MaterialLookup.ingredient(
                    "rubber",
                    com.masson.cruciblecraft.api.material.MaterialPrefixes.PLATE);
        }
        String path = tagId.getPath();
        int slash = path.lastIndexOf('/');
        if (slash <= 0 || slash >= path.length() - 1) {
            return Optional.empty();
        }
        String formTag = path.substring(0, slash);
        String materialTag = path.substring(slash + 1);
        MaterialPrefix form = null;
        for (MaterialPrefix candidate : MaterialPrefixCatalog.values()) {
            if (formTag.equals(candidate.tagDirectory())
                    && tagId.getNamespace().equals(candidate.tagNamespace())) {
                form = candidate;
                break;
            }
        }
        if (form == null) {
            return Optional.empty();
        }
        MaterialDefinition material = null;
        for (MaterialDefinition candidate : MaterialCatalog.values()) {
            if (materialTag.equals(candidate.tagName())) {
                material = candidate;
                break;
            }
        }
        if (material == null || !MaterialFormHosts.isSharedInventoryForm(material, form)) {
            return Optional.empty();
        }
        return MaterialLookup.ingredient(material, form);
    }

    static Optional<Parsed> parseSharedInventoryId(ResourceLocation itemId) {
        return parseSlashId(itemId)
                .filter(parsed -> MaterialFormHosts.isSharedInventoryForm(
                        parsed.material(), parsed.form()));
    }

    static Optional<Parsed> parseUniqueLiveId(ResourceLocation itemId) {
        return parseSlashId(itemId)
                .filter(parsed -> !MaterialFormHosts.isSharedInventoryForm(
                        parsed.material(), parsed.form()));
    }

    private static Optional<Parsed> parseSlashId(ResourceLocation itemId) {
        if (itemId == null
                || !CrucibleCraft.MODID.equals(itemId.getNamespace())
                || !MaterialCatalog.isBootstrapped()) {
            return Optional.empty();
        }
        String path = itemId.getPath();
        int slash = path.indexOf('/');
        if (slash <= 0 || slash >= path.length() - 1) {
            return Optional.empty();
        }
        MaterialDefinition material = MaterialCatalog.find(path.substring(0, slash))
                .orElse(null);
        MaterialPrefix form = MaterialPrefixCatalog.find(path.substring(slash + 1))
                .orElse(null);
        if (material == null
                || form == null
                || !MaterialCatalog.isFormRegistered(material, form)) {
            return Optional.empty();
        }
        return Optional.of(new Parsed(material, form));
    }

    public static Optional<Ingredient> rewritePublicExchangeComponent(JsonElement json) {
        Parsed parsed = parsePublicExchangeComponent(json).orElse(null);
        if (parsed == null) {
            return Optional.empty();
        }
        return MaterialLookup.item(parsed.material(), parsed.form()).map(Ingredient::of);
    }

    public static Optional<ItemStack> rewritePublicExchangeStack(JsonElement json) {
        Parsed parsed = parsePublicExchangeComponent(json).orElse(null);
        if (parsed == null) {
            return Optional.empty();
        }
        int count = 1;
        if (json != null && json.isJsonObject() && json.getAsJsonObject().has("count")) {
            JsonElement countElement = json.getAsJsonObject().get("count");
            if (countElement.isJsonPrimitive() && countElement.getAsJsonPrimitive().isNumber()) {
                count = countElement.getAsInt();
            }
        }
        return MaterialLookup.tryStack(parsed.material().id(), parsed.form(), count);
    }

    static Optional<Parsed> parsePublicExchangeComponent(JsonElement json) {
        if (json == null || !json.isJsonObject() || !MaterialCatalog.isBootstrapped()) {
            return Optional.empty();
        }
        JsonObject object = json.getAsJsonObject();
        if (!object.has("components") || !object.get("components").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject components = object.getAsJsonObject("components");
        if (!components.has("cruciblecraft:prefix_material")
                || !components.get("cruciblecraft:prefix_material").isJsonPrimitive()) {
            return Optional.empty();
        }
        String materialId = components.get("cruciblecraft:prefix_material").getAsString();
        ResourceLocation itemId = itemId(object, "items");
        if (itemId == null) {
            itemId = itemId(object, "item");
        }
        if (itemId == null) {
            itemId = itemId(object, "id");
        }
        if (itemId == null
                || !CrucibleCraft.MODID.equals(itemId.getNamespace())
                || itemId.getPath().indexOf('/') >= 0
                || !(MaterialFormHosts.isPublicExchangePrefixPath(itemId.getPath())
                        || MaterialFormHosts.isUniqueHostedPrefixPath(
                                itemId.getPath()))) {
            return Optional.empty();
        }
        MaterialDefinition material = MaterialCatalog.find(materialId).orElse(null);
        MaterialPrefix form = MaterialPrefixCatalog.find(itemId.getPath()).orElse(null);
        if (material == null
                || form == null
                || !MaterialCatalog.isFormRegistered(material, form)
                || MaterialFormHosts.isSharedInventoryForm(material, form)) {
            return Optional.empty();
        }
        return Optional.of(new Parsed(material, form));
    }

    /**
     * Collapses a fat {@code neoforge:components} snapshot (prefix_material plus
     * unrelated default-stack fields) onto the same tight predicate mill emit
     * uses. Extra indexable CC components stay on the vanilla codec path.
     */
    public static Optional<Ingredient> tightenComponentIngredient(JsonElement json) {
        if (json == null || !json.isJsonObject()) {
            return Optional.empty();
        }
        JsonObject object = json.getAsJsonObject();
        if (!object.has("components") || !object.get("components").isJsonObject()) {
            return Optional.empty();
        }
        JsonObject components = object.getAsJsonObject("components");
        if (!components.has("cruciblecraft:prefix_material")
                || !components.get("cruciblecraft:prefix_material").isJsonPrimitive()) {
            return Optional.empty();
        }
        if (components.has("cruciblecraft:tool_material")
                || components.has("cruciblecraft:machine_material")
                || components.has("cruciblecraft:circuit_config")
                || components.has("cruciblecraft:fireproof")) {
            return Optional.empty();
        }
        String materialId = components.get("cruciblecraft:prefix_material").getAsString();
        if (materialId == null || materialId.isBlank()) {
            return Optional.empty();
        }
        ResourceLocation itemId = itemId(object, "items");
        if (itemId == null) {
            itemId = itemId(object, "item");
        }
        if (itemId == null) {
            return Optional.empty();
        }
        Item item = BuiltInRegistries.ITEM.getOptional(itemId).orElse(null);
        if (item == null) {
            return Optional.empty();
        }
        return Optional.of(MaterialLookup.prefixMaterialIngredient(item, materialId));
    }

    public static Ingredient tightenLiveIngredient(Ingredient ingredient) {
        if (ingredient == null || ingredient.isEmpty()) {
            return ingredient;
        }
        if (ComponentIngredientIndex.extract(ingredient).supported()) {
            return ingredient;
        }
        if (!(ingredient.getCustomIngredient()
                instanceof DataComponentIngredient componentIngredient)) {
            JsonElement encoded = Ingredient.CODEC_NONEMPTY
                    .encodeStart(JsonOps.INSTANCE, ingredient)
                    .result()
                    .orElse(null);
            return tightenComponentIngredient(encoded).orElse(ingredient);
        }
        Item item = null;
        int itemCount = 0;
        for (var holder : componentIngredient.items()) {
            itemCount++;
            item = holder.value();
        }
        if (itemCount != 1 || item == null) {
            return ingredient;
        }
        String materialId = null;
        boolean otherIndexable = false;
        for (var entry : componentIngredient.components().asPatch().entrySet()) {
            if (entry.getValue().isEmpty()) {
                continue;
            }
            ResourceLocation componentId =
                    BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            if (componentId == null) {
                continue;
            }
            Object value = entry.getValue().orElseThrow();
            if (componentId.equals(ModComponents.PREFIX_MATERIAL.getId())
                    && value instanceof String string) {
                materialId = string;
            } else if (ComponentIngredientIndex.isIndexableStringComponent(componentId)) {
                otherIndexable = true;
            }
        }
        if (materialId == null || otherIndexable) {
            return ingredient;
        }
        return MaterialLookup.prefixMaterialIngredient(item, materialId);
    }

    private static <T> DataResult<Pair<Ingredient, T>> decodeIngredient(
            DynamicOps<T> ops,
            T input) {
        JsonElement json = new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue();
        Optional<Ingredient> publicExchange = rewritePublicExchangeComponent(json);
        if (publicExchange.isPresent()) {
            return DataResult.success(Pair.of(publicExchange.orElseThrow(), input));
        }
        Optional<Ingredient> tightened = tightenComponentIngredient(json);
        if (tightened.isPresent()) {
            return DataResult.success(Pair.of(tightened.orElseThrow(), input));
        }
        if (hasComponents(json)) {
            return Ingredient.CODEC_NONEMPTY.decode(ops, input);
        }
        Optional<Ingredient> rewritten = rewriteIngredient(itemId(json, "item"));
        if (rewritten.isPresent()) {
            return DataResult.success(Pair.of(rewritten.orElseThrow(), input));
        }
        Optional<Ingredient> uniqueItem = rewriteUniqueItem(itemId(json, "item"));
        if (uniqueItem.isPresent()) {
            return DataResult.success(Pair.of(uniqueItem.orElseThrow(), input));
        }
        Optional<Ingredient> rewrittenTag = rewriteTag(tagId(json));
        if (rewrittenTag.isPresent()) {
            return DataResult.success(Pair.of(rewrittenTag.orElseThrow(), input));
        }
        ResourceLocation rawItem = itemId(json, "item");
        if (rawItem != null && !BuiltInRegistries.ITEM.containsKey(rawItem)) {
            ResourceLocation flattened = flattenLegacyVanillaItem(rawItem);
            if (flattened != null && !flattened.equals(rawItem)) {
                Item item = BuiltInRegistries.ITEM.getOptional(flattened).orElse(null);
                if (item != null) {
                    return DataResult.success(Pair.of(Ingredient.of(item), input));
                }
            }
        }
        return Ingredient.CODEC_NONEMPTY.decode(ops, input);
    }

    private static <T> DataResult<Pair<ItemStack, T>> decodeItemStack(
            DynamicOps<T> ops,
            T input) {
        JsonElement json = new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue();
        Optional<ItemStack> publicExchange = rewritePublicExchangeStack(json);
        if (publicExchange.isPresent()) {
            return DataResult.success(Pair.of(publicExchange.orElseThrow(), input));
        }
        if (hasComponents(json)) {
            return ItemStack.STRICT_CODEC.decode(ops, input);
        }
        // GT6-derived item output declarations historically use `item`, while
        // vanilla ItemStack JSON uses `id`. Accept both so the material
        // identity rewrite runs before STRICT_CODEC validates the stack.
        ResourceLocation id = itemId(json, "id");
        if (id == null) {
            id = itemId(json, "item");
        }
        int count = 1;
        if (json != null && json.isJsonObject() && json.getAsJsonObject().has("count")) {
            JsonElement countElement = json.getAsJsonObject().get("count");
            if (countElement.isJsonPrimitive() && countElement.getAsJsonPrimitive().isNumber()) {
                count = countElement.getAsInt();
            }
        }
        Optional<ItemStack> rewritten = rewriteStack(id, count);
        if (rewritten.isEmpty()) {
            rewritten = rewriteUniqueStack(id, count);
        }
        if (rewritten.isPresent()) {
            return DataResult.success(Pair.of(rewritten.orElseThrow(), input));
        }
        ResourceLocation flattened = flattenLegacyVanillaItem(id);
        if (flattened != null && !flattened.equals(id)) {
            Item item = BuiltInRegistries.ITEM.getOptional(flattened).orElse(null);
            if (item != null && count > 0 && count <= item.getDefaultMaxStackSize()) {
                return DataResult.success(Pair.of(new ItemStack(item, count), input));
            }
        }
        return ItemStack.STRICT_CODEC.decode(ops, input);
    }

    static ResourceLocation flattenLegacyVanillaItem(ResourceLocation itemId) {
        if (itemId == null) {
            return null;
        }
        if (BuiltInRegistries.ITEM.containsKey(itemId)) {
            return itemId;
        }
        return switch (itemId.toString()) {
            case "minecraft:fish" -> ResourceLocation.parse("minecraft:cod");
            case "minecraft:cooked_fished" -> ResourceLocation.parse("minecraft:cooked_cod");
            case "minecraft:double_plant" -> ResourceLocation.parse("minecraft:sunflower");
            default -> itemId;
        };
    }

    private static boolean hasComponents(JsonElement json) {
        return json != null
                && json.isJsonObject()
                && json.getAsJsonObject().has("components");
    }

    private static ResourceLocation itemId(JsonElement json, String key) {
        if (json == null || !json.isJsonObject()) {
            return null;
        }
        JsonObject object = json.getAsJsonObject();
        if (!object.has(key) || !object.get(key).isJsonPrimitive()) {
            return null;
        }
        return ResourceLocation.tryParse(object.get(key).getAsString());
    }

    private static ResourceLocation tagId(JsonElement json) {
        return itemId(json, "tag");
    }

    record Parsed(MaterialDefinition material, MaterialPrefix form) {}
}
