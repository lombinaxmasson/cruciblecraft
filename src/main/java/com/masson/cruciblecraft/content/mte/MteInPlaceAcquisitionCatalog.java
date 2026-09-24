package com.masson.cruciblecraft.content.mte;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;

import net.minecraft.resources.ResourceLocation;

/** Source-exact in-place MTE workbench grids from Loader_MultiTileEntities. */
public final class MteInPlaceAcquisitionCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/mte_inplace_acquisition.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Catalog CATALOG = loadBundled();

    public static List<Recipe> recipes() {
        return CATALOG.recipes;
    }

    public record Recipe(
            String path,
            String domain,
            List<String> pattern,
            Map<String, Slot> ingredients,
            Map<String, Slot> catalysts,
            ResourceLocation resultId,
            int count,
            String type,
            Slot ingredient,
            float experience,
            int cookingTime) {
        public Recipe {
            Objects.requireNonNull(path, "path");
            Objects.requireNonNull(domain, "domain");
            Objects.requireNonNull(type, "type");
            pattern = List.copyOf(pattern);
            ingredients = Map.copyOf(new LinkedHashMap<>(ingredients));
            catalysts = Map.copyOf(new LinkedHashMap<>(catalysts));
            Objects.requireNonNull(resultId, "resultId");
            if (count < 1) {
                throw new IllegalArgumentException("count must be positive: " + path);
            }
            if ("minecraft:smelting".equals(type)
                    && (ingredient == null
                            || experience < 0.0F
                            || cookingTime < 1)) {
                throw new IllegalArgumentException(
                        "Incomplete smelting recipe: " + path);
            }
        }

        public boolean smelting() {
            return type.equals("minecraft:smelting");
        }
    }

    public record Slot(String item, String tag) {}

    private record Catalog(List<Recipe> recipes) {}

    private static Catalog loadBundled() {
        Document document;
        try (InputStream stream =
                MteInPlaceAcquisitionCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled catalog " + RESOURCE);
            }
            document = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
        } catch (IOException | JsonIOException | JsonSyntaxException exception) {
            throw new IllegalStateException(
                    "Could not load catalog " + RESOURCE, exception);
        }
        if (document == null
                || document.schemaVersion != 1
                || !SOURCE_REVISION.equals(document.sourceRevision)
                || document.recipes == null) {
            throw new IllegalStateException("Invalid in-place MTE acquisition catalog");
        }
        if (Boolean.TRUE.equals(document.autoPromotePlayerComplete)) {
            throw new IllegalStateException(
                    "In-place MTE acquisition catalog must not auto-promote player_complete");
        }
        LinkedHashMap<String, Recipe> byPath = new LinkedHashMap<>();
        for (RecipeRow row : document.recipes) {
            Recipe recipe = row.toRecipe();
            if (byPath.put(recipe.path(), recipe) != null) {
                throw new IllegalStateException(
                        "Duplicate in-place MTE recipe " + recipe.path());
            }
        }
        return new Catalog(List.copyOf(byPath.values()));
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        @SerializedName("auto_promote_player_complete")
        private Boolean autoPromotePlayerComplete;
        private List<RecipeRow> recipes;
    }

    private static final class RecipeRow {
        private String path;
        private String domain;
        private String type;
        private List<String> pattern;
        private Map<String, Slot> ingredients;
        private Map<String, Slot> catalysts;
        private Slot ingredient;
        private Float experience;
        @SerializedName("cookingtime")
        private Integer cookingTime;
        private ResultRow result;

        private Recipe toRecipe() {
            if (path == null || path.isBlank() || domain == null || domain.isBlank()
                    || result == null || result.id == null) {
                throw new IllegalStateException("Incomplete in-place MTE recipe " + path);
            }
            ResourceLocation resultId = ResourceLocation.tryParse(result.id);
            if (resultId == null) {
                throw new IllegalStateException("Bad in-place MTE result " + result.id);
            }
            return new Recipe(
                    path,
                    domain,
                    pattern == null ? List.of() : pattern,
                    ingredients == null ? Map.of() : ingredients,
                    catalysts == null ? Map.of() : catalysts,
                    resultId,
                    result.count < 1 ? 1 : result.count,
                    type == null ? "cruciblecraft:shaped_catalyst" : type,
                    ingredient,
                    experience == null ? 0.1F : experience,
                    cookingTime == null ? 200 : cookingTime);
        }
    }

    private static final class ResultRow {
        private String id;
        private int count;
    }

    private MteInPlaceAcquisitionCatalog() {}
}
