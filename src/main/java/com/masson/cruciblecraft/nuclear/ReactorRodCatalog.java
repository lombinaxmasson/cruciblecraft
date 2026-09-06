package com.masson.cruciblecraft.nuclear;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonArray;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/** Source-backed GT6 reactor-rod identities 9201-9441. */
public final class ReactorRodCatalog {
    public enum Kind {
        EMPTY,
        ABSORBER,
        REFLECTOR,
        MODERATOR,
        NUCLEAR,
        DEPLETED,
        BREEDER,
        PRODUCT
    }

    public record Entry(
            ResourceLocation id,
            Kind kind,
            int sourceId,
            String material,
            long durability,
            int neutronSelf,
            int neutronOther,
            int neutronDiv,
            int neutronMax,
            int neutronLoss,
            String depletedPath,
            String productPath) {
        public boolean hasDurability() {
            return kind == Kind.NUCLEAR || kind == Kind.BREEDER;
        }
    }

    private static final Map<ResourceLocation, Entry> BY_ID = load();

    private ReactorRodCatalog() {}

    public static List<Entry> entries() {
        return List.copyOf(BY_ID.values());
    }

    public static Entry require(ResourceLocation id) {
        Entry entry = BY_ID.get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown reactor rod " + id);
        }
        return entry;
    }

    public static Optional<Entry> find(ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    private static Map<ResourceLocation, Entry> load() {
        JsonObject root;
        try (var stream = ReactorRodCatalog.class.getClassLoader()
                .getResourceAsStream("data/cruciblecraft/nuclear_reactor_rods.json")) {
            if (stream == null) {
                throw new IllegalStateException("Missing nuclear_reactor_rods.json");
            }
            root = JsonParser.parseReader(
                            new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
        } catch (Exception failure) {
            throw new IllegalStateException("Failed to load reactor rod catalog", failure);
        }
        JsonArray rods = root.getAsJsonArray("rods");
        LinkedHashMap<ResourceLocation, Entry> entries = new LinkedHashMap<>();
        for (JsonElement element : rods) {
            JsonObject json = element.getAsJsonObject();
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, json.get("id").getAsString());
            Kind kind = Kind.valueOf(
                    json.get("kind").getAsString().toUpperCase());
            Entry entry = new Entry(
                    id,
                    kind,
                    json.get("source_id").getAsInt(),
                    json.get("material").getAsString(),
                    json.has("durability") ? json.get("durability").getAsLong() : 0L,
                    json.has("neutron_self") ? json.get("neutron_self").getAsInt() : 0,
                    json.has("neutron_other") ? json.get("neutron_other").getAsInt() : 0,
                    json.has("neutron_div") ? json.get("neutron_div").getAsInt() : 0,
                    json.has("neutron_max") ? json.get("neutron_max").getAsInt() : 0,
                    json.has("neutron_loss") ? json.get("neutron_loss").getAsInt() : 0,
                    json.has("depleted") ? json.get("depleted").getAsString() : "",
                    json.has("product") ? json.get("product").getAsString() : "");
            if (entries.put(id, entry) != null) {
                throw new IllegalStateException("Duplicate reactor rod " + id);
            }
        }
        if (entries.size() != 46) {
            throw new IllegalStateException(
                    "Reactor rod catalog drifted from 46 GT6 identities");
        }
        return Map.copyOf(entries);
    }
}
