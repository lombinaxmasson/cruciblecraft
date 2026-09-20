package com.masson.cruciblecraft.recipe.gt;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Compact-family item codecs that keep tag/component JSON on the vanilla
 * Ingredient / ItemStack path, then fall back to {@link BuiltInRegistries}
 * for plain {@code {"item"}} / {@code {"id"}} rows.
 *
 * <p>Recipe reload {@code RegistryOps} can reject a holder that already exists
 * in {@link BuiltInRegistries#ITEM}. {@code optionalFieldOf} then materializes
 * an empty list while counts stay present, and {@link GTRecipe} fails closed
 * with {@code inputs=0, counts=1}, dropping the whole family.
 */
final class CompactRelationItemCodecs {
    static final Codec<Ingredient> INGREDIENT = Codec.of(
            Ingredient.CODEC_NONEMPTY,
            CompactRelationItemCodecs::decodeIngredient);
    static final Codec<ItemStack> ITEM_STACK = Codec.of(
            ItemStack.STRICT_CODEC,
            CompactRelationItemCodecs::decodeItemStack);
    static final Codec<java.util.List<Ingredient>> ITEM_INPUTS = INGREDIENT.listOf();
    static final Codec<java.util.List<ItemStack>> ITEM_OUTPUTS = ITEM_STACK.listOf();
    static final Codec<java.util.List<java.util.List<Ingredient>>> ITEM_INPUT_DICTS =
            ITEM_INPUTS.listOf();
    static final Codec<java.util.List<java.util.List<ItemStack>>> ITEM_OUTPUT_DICTS =
            ITEM_OUTPUTS.listOf();

    private CompactRelationItemCodecs() {}

    private static <T> DataResult<Pair<Ingredient, T>> decodeIngredient(
            DynamicOps<T> ops,
            T input) {
        JsonElement json = new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue();
        java.util.Optional<Ingredient> publicExchange =
                PrefixMaterialItemCodecs.rewritePublicExchangeComponent(json);
        if (publicExchange.isPresent()) {
            return DataResult.success(Pair.of(publicExchange.orElseThrow(), input));
        }
        java.util.Optional<Ingredient> tightened =
                PrefixMaterialItemCodecs.tightenComponentIngredient(json);
        if (tightened.isPresent()) {
            return DataResult.success(Pair.of(tightened.orElseThrow(), input));
        }
        if (json != null && json.isJsonObject() && !json.getAsJsonObject().has("components")) {
            java.util.Optional<Ingredient> rewritten =
                    PrefixMaterialItemCodecs.rewriteIngredient(itemId(json, "item"));
            if (rewritten.isPresent()) {
                return DataResult.success(Pair.of(rewritten.orElseThrow(), input));
            }
        }
        DataResult<Pair<Ingredient, T>> primary =
                Ingredient.CODEC_NONEMPTY.decode(ops, input);
        if (primary.result().isPresent()
                && !primary.result().get().getFirst().isEmpty()) {
            return primary;
        }
        ResourceLocation itemId = itemId(json, "item");
        if (itemId == null) {
            return primary;
        }
        return builtInItem(itemId).map(item -> Pair.of(Ingredient.of(item), input));
    }

    private static <T> DataResult<Pair<ItemStack, T>> decodeItemStack(
            DynamicOps<T> ops,
            T input) {
        JsonElement json = new Dynamic<>(ops, input).convert(JsonOps.INSTANCE).getValue();
        java.util.Optional<ItemStack> publicExchange =
                PrefixMaterialItemCodecs.rewritePublicExchangeStack(json);
        if (publicExchange.isPresent()) {
            return DataResult.success(Pair.of(publicExchange.orElseThrow(), input));
        }
        if (json != null && json.isJsonObject() && !json.getAsJsonObject().has("components")) {
            ResourceLocation rewrittenId = itemId(json, "id");
            int rewrittenCount = 1;
            if (json.isJsonObject() && json.getAsJsonObject().has("count")) {
                JsonElement countElement = json.getAsJsonObject().get("count");
                if (countElement.isJsonPrimitive()
                        && countElement.getAsJsonPrimitive().isNumber()) {
                    rewrittenCount = countElement.getAsInt();
                }
            }
            java.util.Optional<ItemStack> rewritten =
                    PrefixMaterialItemCodecs.rewriteStack(rewrittenId, rewrittenCount);
            if (rewritten.isPresent()) {
                return DataResult.success(Pair.of(rewritten.orElseThrow(), input));
            }
        }
        DataResult<Pair<ItemStack, T>> primary = ItemStack.STRICT_CODEC.decode(ops, input);
        if (primary.result().isPresent() && !primary.result().get().getFirst().isEmpty()) {
            return primary;
        }
        ResourceLocation itemId = itemId(json, "id");
        if (itemId == null) {
            return primary;
        }
        int count = 1;
        if (json.isJsonObject() && json.getAsJsonObject().has("count")) {
            JsonElement countElement = json.getAsJsonObject().get("count");
            if (!countElement.isJsonPrimitive() || !countElement.getAsJsonPrimitive().isNumber()) {
                return DataResult.error(() -> "Compact item count is not a number: " + itemId);
            }
            count = countElement.getAsInt();
        }
        final int stackCount = count;
        return builtInItem(itemId).flatMap(item -> {
            if (stackCount <= 0 || stackCount > item.getDefaultMaxStackSize()) {
                return DataResult.error(
                        () -> "Compact item count " + stackCount + " is invalid for " + itemId);
            }
            return DataResult.success(Pair.of(new ItemStack(item, stackCount), input));
        });
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

    private static DataResult<Item> builtInItem(ResourceLocation itemId) {
        ResourceLocation resolved = PrefixMaterialItemCodecs.flattenLegacyVanillaItem(itemId);
        if (resolved == null || !BuiltInRegistries.ITEM.containsKey(resolved)) {
            return DataResult.error(
                    () -> "Compact item is not in BuiltInRegistries.ITEM: " + itemId);
        }
        return DataResult.success(BuiltInRegistries.ITEM.get(resolved));
    }
}
