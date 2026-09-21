package com.masson.cruciblecraft.crops;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public record CropCard(
        String id,
        String name,
        String discoveredBy,
        CropDrop drop,
        List<CropDrop> specialDrops,
        CropDrop baseSeed,
        boolean crossbreedOnly,
        int tier,
        int maxSize,
        int growthSpeed,
        int afterHarvestSize,
        int harvestSize,
        int statChemical,
        int statFood,
        int statDefensive,
        int statColor,
        int statWeed,
        List<String> attributes,
        boolean dependsOnFoods,
        String blockedWithout) {
    public static CropCard fromJson(JsonObject json) {
        List<CropDrop> specials = new ArrayList<>();
        JsonArray array = json.getAsJsonArray("special_drops");
        if (array != null) {
            array.forEach(element -> specials.add(CropDrop.fromJson(element.getAsJsonObject())));
        }
        String blocked = json.has("blocked_without") && !json.get("blocked_without").isJsonNull()
                ? json.get("blocked_without").getAsString()
                : null;
        return new CropCard(
                json.get("id").getAsString(),
                json.get("name").getAsString(),
                json.get("discovered_by").getAsString(),
                CropDrop.fromJson(json.getAsJsonObject("drop")),
                List.copyOf(specials),
                CropDrop.fromJson(json.getAsJsonObject("base_seed")),
                json.get("crossbreed_only").getAsBoolean(),
                json.get("tier").getAsInt(),
                json.get("max_size").getAsInt(),
                json.get("growth_speed").getAsInt(),
                json.get("after_harvest_size").getAsInt(),
                json.get("harvest_size").getAsInt(),
                json.get("stat_chemical").getAsInt(),
                json.get("stat_food").getAsInt(),
                json.get("stat_defensive").getAsInt(),
                json.get("stat_color").getAsInt(),
                json.get("stat_weed").getAsInt(),
                attributes(json),
                json.get("depends_on_foods_addon").getAsBoolean(),
                blocked);
    }

    public boolean canGrow(int size) {
        return size < maxSize;
    }

    public boolean canHarvest(int size) {
        return size >= harvestSize;
    }

    public boolean canCross(int size) {
        return size + 2 > maxSize;
    }

    public boolean matchesBaseSeed(ItemStack stack) {
        if (crossbreedOnly || stack.isEmpty()) {
            return false;
        }
        return baseSeed.matches(stack);
    }

    public Optional<ItemStack> harvestDrop() {
        return drop.stack();
    }

    public List<ItemStack> extraDrops(net.minecraft.util.RandomSource random) {
        List<ItemStack> extras = new ArrayList<>();
        for (CropDrop extra : specialDrops) {
            if (random.nextInt(4) == 0) {
                extra.stack().ifPresent(extras::add);
            }
        }
        return extras;
    }

    private static List<String> attributes(JsonObject json) {
        List<String> values = new ArrayList<>();
        json.getAsJsonArray("attributes").forEach(element ->
                values.add(element.getAsString()));
        return List.copyOf(values);
    }

    public record CropDrop(
            String kind,
            String material,
            String prefix,
            String item,
            String il,
            int count) {
        static CropDrop fromJson(JsonObject json) {
            String kind = json.get("kind").getAsString();
            return new CropDrop(
                    kind,
                    json.has("material") && !json.get("material").isJsonNull()
                            ? json.get("material").getAsString()
                            : null,
                    json.has("prefix") && !json.get("prefix").isJsonNull()
                            ? json.get("prefix").getAsString()
                            : null,
                    json.has("item") && !json.get("item").isJsonNull()
                            ? json.get("item").getAsString()
                            : null,
                    json.has("il") && !json.get("il").isJsonNull()
                            ? json.get("il").getAsString()
                            : null,
                    json.has("count") ? json.get("count").getAsInt() : 1);
        }

        boolean matches(ItemStack stack) {
            return stack(count).map(expected ->
                    expected.getItem() == stack.getItem())
                    .orElse(false);
        }

        Optional<ItemStack> stack() {
            return stack(Math.max(1, count));
        }

        Optional<ItemStack> stack(int amount) {
            if ("none".equals(kind) || "external".equals(kind)
                    || "external_arsmagica".equals(kind)
                    || "unparsed".equals(kind)) {
                return Optional.empty();
            }
            if ("plant_form".equals(kind) || "material_form".equals(kind)) {
                return MaterialLookup.tryStack(
                        material,
                        MaterialPrefixCatalog.require(prefix),
                        amount);
            }
            if ("vanilla".equals(kind) && item != null) {
                return BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(item))
                        .map(found -> new ItemStack(found, amount));
            }
            if ("foods_addon".equals(kind) && il != null) {
                if ("Food_Apple_Red".equals(il)) {
                    return BuiltInRegistries.ITEM.getOptional(
                                    ResourceLocation.parse("minecraft:apple"))
                            .map(found -> new ItemStack(found, amount));
                }
                if (!ModList.get().isLoaded("cruciblecraft_foods")) {
                    return Optional.empty();
                }
                return BuiltInRegistries.ITEM.getOptional(
                                ResourceLocation.fromNamespaceAndPath(
                                        "cruciblecraft_foods", foodPath(il)))
                        .map(found -> new ItemStack(found, amount));
            }
            return Optional.empty();
        }

        private static String foodPath(String ilName) {
            String trimmed = ilName.startsWith("Food_")
                    ? ilName.substring("Food_".length())
                    : ilName.startsWith("Crop_")
                            ? ilName.substring("Crop_".length())
                            : ilName;
            return trimmed.toLowerCase(java.util.Locale.ROOT);
        }
    }
}
