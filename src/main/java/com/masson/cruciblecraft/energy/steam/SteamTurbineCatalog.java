package com.masson.cruciblecraft.energy.steam;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** GT6 steam turbines: 15 singles + 4 large housings. */
public final class SteamTurbineCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/steam_turbines.json";
    private static final Catalog CATALOG = loadBundled();

    public static List<Profile> profiles() {
        return CATALOG.byId.values().stream().toList();
    }

    public static Optional<Profile> find(ResourceLocation id) {
        return Optional.ofNullable(CATALOG.byId.get(id));
    }

    public static Profile require(ResourceLocation id) {
        Profile profile = CATALOG.byId.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown steam turbine " + id);
        }
        return profile;
    }

    public record Profile(
            ResourceLocation id,
            int sourceId,
            String kind,
            int steamInputMax,
            long ruOutput,
            int steamPerEu,
            int steamPerWater,
            ResourceLocation wallId,
            Recipe recipe,
            String langEn,
            String langZh) {
        public Profile {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(recipe, "recipe");
            Objects.requireNonNull(langEn, "langEn");
            Objects.requireNonNull(langZh, "langZh");
            if (!"single".equals(kind) && !"large".equals(kind)) {
                throw new IllegalArgumentException("Unknown turbine kind " + kind);
            }
            if (steamInputMax <= 0 || ruOutput <= 0L || steamPerEu <= 0
                    || steamPerWater <= 0) {
                throw new IllegalArgumentException(
                        "Steam turbine rates must be positive: " + id);
            }
        }

        public boolean large() {
            return "large".equals(kind);
        }

        public long energyCapacity() {
            return Math.max(16_384L, Math.multiplyExact(ruOutput, 4L));
        }

        public int tankCapacityMb() {
            return Math.toIntExact(Math.min(
                    Integer.MAX_VALUE,
                    Math.max(1_000L, (long) steamInputMax * 4L)));
        }
    }

    public record Recipe(
            List<String> pattern,
            Map<String, Ingredient> keys,
            List<String> catalysts) {
        public Recipe {
            pattern = List.copyOf(pattern);
            keys = Map.copyOf(keys);
            catalysts = catalysts == null ? List.of() : List.copyOf(catalysts);
        }
    }

    public record Ingredient(String item, String prefix, String material, String family) {}

    private record Catalog(Map<ResourceLocation, Profile> byId) {}

    private static Catalog loadBundled() {
        Document document = CatalogJson.readBundled(
                SteamTurbineCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.machines == null
                || document.expectedSingles != 15
                || document.expectedLarges != 4
                || document.steamPerEu != 2) {
            throw new IllegalStateException("Invalid steam turbine catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        LinkedHashMap<ResourceLocation, Profile> byId = new LinkedHashMap<>();
        int singles = 0;
        int larges = 0;
        for (MachineRow row : document.machines) {
            Profile profile = row.toProfile();
            if (byId.put(profile.id(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate steam turbine " + profile.id());
            }
            if (profile.large()) {
                larges++;
            } else {
                singles++;
            }
        }
        if (singles != 15 || larges != 4) {
            throw new IllegalStateException(
                    "Steam turbine counts drifted: singles="
                            + singles + " larges=" + larges);
        }
        return new Catalog(Map.copyOf(byId));
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("steam_per_eu")
        private int steamPerEu;
        @SerializedName("expected_singles")
        private int expectedSingles;
        @SerializedName("expected_larges")
        private int expectedLarges;
        private List<MachineRow> machines;
    }

    private static final class MachineRow {
        private String id;
        @SerializedName("source_id")
        private int sourceId;
        private String kind;
        @SerializedName("steam_input_max")
        private int steamInputMax;
        @SerializedName("ru_output")
        private long ruOutput;
        @SerializedName("steam_per_eu")
        private int steamPerEu;
        @SerializedName("steam_per_water")
        private int steamPerWater;
        @SerializedName("wall_id")
        private String wallId;
        @SerializedName("lang_en")
        private String langEn;
        @SerializedName("lang_zh")
        private String langZh;
        private RecipeRow recipe;

        private Profile toProfile() {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null || recipe == null
                    || !CatalogJson.nonBlank(langEn)
                    || !CatalogJson.nonBlank(langZh)) {
                throw new IllegalStateException("Incomplete steam turbine " + id);
            }
            ResourceLocation wall = CatalogJson.nonBlank(wallId)
                    ? ResourceLocation.parse(wallId)
                    : null;
            return new Profile(
                    parsed,
                    sourceId,
                    kind,
                    steamInputMax,
                    ruOutput,
                    steamPerEu == 0 ? 2 : steamPerEu,
                    steamPerWater,
                    wall,
                    recipe.toRecipe(),
                    langEn,
                    langZh);
        }
    }

    private static final class RecipeRow {
        private List<String> pattern;
        private Map<String, IngredientRow> keys;
        private String catalyst;

        private Recipe toRecipe() {
            LinkedHashMap<String, Ingredient> parsed = new LinkedHashMap<>();
            if (keys != null) {
                keys.forEach((letter, row) -> parsed.put(letter, row.toIngredient()));
            }
            List<String> tools = CatalogJson.nonBlank(catalyst)
                    ? List.of(catalyst)
                    : List.of();
            return new Recipe(
                    pattern == null ? List.of() : pattern, parsed, tools);
        }
    }

    private static final class IngredientRow {
        private String item;
        private String prefix;
        private String material;
        private String family;

        private Ingredient toIngredient() {
            return new Ingredient(item, prefix, material, family);
        }
    }

    private SteamTurbineCatalog() {}
}
