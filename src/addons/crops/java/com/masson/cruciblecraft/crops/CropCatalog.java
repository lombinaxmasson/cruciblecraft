package com.masson.cruciblecraft.crops;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.world.item.ItemStack;
import net.neoforged.fml.ModList;

public final class CropCatalog {
    private static final Gson GSON = new Gson();
    private static final Map<String, CropCard> CARDS = new LinkedHashMap<>();
    private static boolean overlayInstalled;

    private CropCatalog() {}

    public static void installOverlay() {
        JsonObject root = ledger();
        for (JsonElement element : root.getAsJsonArray("named_plant_forms")) {
            JsonObject pair = element.getAsJsonObject();
            String material = pair.get("material").getAsString();
            String prefix = pair.get("prefix").getAsString();
            if (metadataOnly(material)) {
                continue;
            }
            if (!MaterialCatalog.addStartupForm(material, prefix)) {
                throw new IllegalStateException(
                        "Duplicate crop plant overlay " + material + "/" + prefix);
            }
            CropRegistries.recordOverlay(material, prefix);
        }
        overlayInstalled = true;
    }

    public static boolean overlayInstalled() {
        return overlayInstalled;
    }

    public static void freezeCards() {
        if (!CARDS.isEmpty()) {
            return;
        }
        JsonObject root = ledger();
        boolean foods = ModList.get().isLoaded("cruciblecraft_foods");
        for (JsonElement element : root.getAsJsonArray("crops")) {
            CropCard card = CropCard.fromJson(element.getAsJsonObject());
            if (card.dependsOnFoods() && !foods) {
                continue;
            }
            if (card.blockedWithout() != null && !dependencyLoaded(card.blockedWithout())) {
                continue;
            }
            CARDS.put(card.id(), card);
        }
    }

    public static CropCard require(String id) {
        freezeCards();
        CropCard card = CARDS.get(id);
        if (card == null) {
            throw new IllegalArgumentException("Unknown crop card " + id);
        }
        return card;
    }

    public static Optional<CropCard> find(String id) {
        freezeCards();
        return Optional.ofNullable(CARDS.get(id));
    }

    public static Collection<CropCard> cards() {
        freezeCards();
        return CARDS.values();
    }

    public static Optional<CropCard> byBaseSeed(ItemStack stack) {
        freezeCards();
        for (CropCard card : CARDS.values()) {
            if (card.matchesBaseSeed(stack)) {
                return Optional.of(card);
            }
        }
        return Optional.empty();
    }

    public static List<CropCard> sharingAttribute(CropCard card) {
        freezeCards();
        List<CropCard> matches = new ArrayList<>();
        for (CropCard other : CARDS.values()) {
            if (other.id().equals(card.id())) {
                continue;
            }
            for (String attribute : card.attributes()) {
                if (other.attributes().contains(attribute)) {
                    matches.add(other);
                    break;
                }
            }
        }
        return matches;
    }

    static int namedPlantFormCount() {
        JsonArray pairs = ledger().getAsJsonArray("named_plant_forms");
        return pairs.size();
    }

    private static boolean dependencyLoaded(String token) {
        return switch (token) {
            case "arsmagica" -> ModList.get().isLoaded("arsmagica2")
                    || ModList.get().isLoaded("ars_nouveau");
            case "thaumcraft" -> ModList.get().isLoaded("thaumcraft");
            case "twilightforest" -> ModList.get().isLoaded("twilightforest");
            default -> ModList.get().isLoaded(token);
        };
    }

    private static boolean metadataOnly(String materialId) {
        try (var stream = MaterialCatalog.class.getResourceAsStream(
                "/data/cruciblecraft/materials/" + materialId + ".json")) {
            if (stream == null) {
                return false;
            }
            JsonObject json = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    JsonObject.class);
            return json.has("metadata_only") && json.get("metadata_only").getAsBoolean();
        } catch (Exception exception) {
            throw new IllegalStateException(
                    "Failed to read material " + materialId, exception);
        }
    }

    private static JsonObject ledger() {
        try (var stream = CropCatalog.class.getResourceAsStream(
                "/data/cruciblecraft_crops/crop_ledger.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing crop ledger");
            }
            return GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    JsonObject.class);
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to load crop ledger", exception);
        }
    }
}
