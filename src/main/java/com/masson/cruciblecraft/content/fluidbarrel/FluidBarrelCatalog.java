package com.masson.cruciblecraft.content.fluidbarrel;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

/** Bundled 36-identity barrel and drum catalog. */
public final class FluidBarrelCatalog {
    public static final int EXPECTED = 36;
    public static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final String RESOURCE =
            "/data/cruciblecraft/fluid_barrel_catalog.json";
    private static final Gson GSON = new Gson();
    private static final List<FluidBarrelProfile> PROFILES = load();
    private static final Map<Integer, FluidBarrelProfile> BY_META = indexMeta();

    private FluidBarrelCatalog() {}

    public static List<FluidBarrelProfile> profiles() {
        return PROFILES;
    }

    public static FluidBarrelProfile byMeta(int meta) {
        FluidBarrelProfile profile = BY_META.get(meta);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown fluid barrel meta " + meta);
        }
        return profile;
    }

    private static Map<Integer, FluidBarrelProfile> indexMeta() {
        Map<Integer, FluidBarrelProfile> metas = new LinkedHashMap<>();
        for (FluidBarrelProfile profile : PROFILES) {
            if (metas.put(profile.meta(), profile) != null) {
                throw new IllegalStateException(
                        "Duplicate fluid barrel meta " + profile.meta());
            }
        }
        return Map.copyOf(metas);
    }

    private static List<FluidBarrelProfile> load() {
        try (InputStream stream =
                FluidBarrelCatalog.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing " + RESOURCE);
            }
            try (InputStreamReader reader =
                    new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                Document document = GSON.fromJson(reader, Document.class);
                if (document == null
                        || document.schemaVersion != 1
                        || !"FLUID_BARREL_CATALOG".equals(document.status)
                        || !SOURCE_REVISION.equals(document.sourceRevision)
                        || document.identities == null) {
                    throw new IllegalStateException(
                            "Fluid barrel catalog failed source checks");
                }
                List<FluidBarrelProfile> profiles = new ArrayList<>();
                Map<String, FluidBarrelProfile> paths = new LinkedHashMap<>();
                for (Row row : document.identities) {
                    FluidBarrelProfile profile = row.toProfile();
                    if (paths.put(profile.path(), profile) != null) {
                        throw new IllegalStateException(
                                "Duplicate fluid barrel path " + profile.path());
                    }
                    profiles.add(profile);
                }
                if (profiles.size() != EXPECTED) {
                    throw new IllegalStateException(
                            "Fluid barrel catalog drifted from 36 identities: "
                                    + profiles.size());
                }
                return List.copyOf(profiles);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Could not load fluid barrel catalog", exception);
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        String status;
        List<Row> identities;
    }

    private static final class Row {
        @SerializedName("registry_path")
        String registryPath;
        int meta;
        String kind;
        String art;
        @SerializedName("material_id")
        String materialId;
        @SerializedName("english_name")
        String englishName;
        @SerializedName("chinese_name")
        String chineseName;
        @SerializedName("gt6_class")
        String gt6Class;
        long capacity;
        @SerializedName("melting_kelvin")
        Long meltingKelvin;
        @SerializedName("gas_proof")
        boolean gasProof;
        @SerializedName("acid_proof")
        boolean acidProof;
        @SerializedName("plasma_proof")
        boolean plasmaProof;
        @SerializedName("magic_proof")
        boolean magicProof;
        boolean glowing;
        int flammability;
        float hardness;
        float resistance;
        @SerializedName("recipe_status")
        String recipeStatus;

        FluidBarrelProfile toProfile() {
            return new FluidBarrelProfile(
                    FluidBarrelProfile.id(registryPath),
                    meta,
                    FluidBarrelKind.parse(kind),
                    art,
                    materialId == null ? "" : materialId,
                    englishName,
                    chineseName,
                    gt6Class,
                    capacity,
                    meltingKelvin,
                    gasProof,
                    acidProof,
                    plasmaProof,
                    magicProof,
                    glowing,
                    flammability,
                    hardness,
                    resistance,
                    recipeStatus == null ? "" : recipeStatus);
        }
    }
}
