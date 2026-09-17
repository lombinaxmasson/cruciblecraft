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
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Rewrites legacy {@code cruciblecraft:{material}/{form}} recipe JSON onto the
 * shared prefix Item plus {@code prefix_material}. Unique hosted forms keep
 * their per-material ids.
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
        return rewriteStack(itemId, 1).map(stack ->
                net.neoforged.neoforge.common.crafting.DataComponentIngredient
                        .of(false, stack));
    }

    public static Optional<ItemStack> rewriteStack(ResourceLocation itemId, int count) {
        Parsed parsed = parseSharedInventoryId(itemId).orElse(null);
        if (parsed == null || count <= 0) {
            return Optional.empty();
        }
        return MaterialLookup.tryStack(parsed.material().id(), parsed.form(), count);
    }

    public static Optional<Ingredient> rewriteTag(ResourceLocation tagId) {
        if (tagId == null || !MaterialCatalog.isBootstrapped()) {
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
                || !MaterialFormHosts.isSharedInventoryForm(material, form)) {
            return Optional.empty();
        }
        return Optional.of(new Parsed(material, form));
    }

    private static <T> DataResult<Pair<Ingredient, T>> decodeIngredient(
            DynamicOps<T> ops,
            T input) {
        JsonElement json = new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue();
        if (hasComponents(json)) {
            return Ingredient.CODEC_NONEMPTY.decode(ops, input);
        }
        Optional<Ingredient> rewritten = rewriteIngredient(itemId(json, "item"));
        if (rewritten.isPresent()) {
            return DataResult.success(Pair.of(rewritten.orElseThrow(), input));
        }
        Optional<Ingredient> rewrittenTag = rewriteTag(tagId(json));
        if (rewrittenTag.isPresent()) {
            return DataResult.success(Pair.of(rewrittenTag.orElseThrow(), input));
        }
        return Ingredient.CODEC_NONEMPTY.decode(ops, input);
    }

    private static <T> DataResult<Pair<ItemStack, T>> decodeItemStack(
            DynamicOps<T> ops,
            T input) {
        JsonElement json = new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue();
        if (hasComponents(json)) {
            return ItemStack.STRICT_CODEC.decode(ops, input);
        }
        ResourceLocation id = itemId(json, "id");
        int count = 1;
        if (json != null && json.isJsonObject() && json.getAsJsonObject().has("count")) {
            JsonElement countElement = json.getAsJsonObject().get("count");
            if (countElement.isJsonPrimitive() && countElement.getAsJsonPrimitive().isNumber()) {
                count = countElement.getAsInt();
            }
        }
        Optional<ItemStack> rewritten = rewriteStack(id, count);
        if (rewritten.isPresent()) {
            return DataResult.success(Pair.of(rewritten.orElseThrow(), input));
        }
        return ItemStack.STRICT_CODEC.decode(ops, input);
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
