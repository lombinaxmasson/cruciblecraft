package com.masson.cruciblecraft.content.mte;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.resources.ResourceLocation;

/**
 * Source-backed material properties for GT6 crucible faucets.
 *
 * <p>Faucets have a different contract from liquid/gas attachments: their
 * fluid path is the crucible/mold path, so they intentionally do not share
 * {@link MteFluidAttachmentProfile}'s phase filter.
 */
public record MteFaucetProfile(
        ResourceLocation id,
        String materialId,
        boolean acidProof,
        float hardness,
        float resistance) {
    private static final String RESOURCE =
            "/data/cruciblecraft/mte_faucet_profiles.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Map<ResourceLocation, MteFaucetProfile> BY_ID =
            load();

    public MteFaucetProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(materialId, "materialId");
        if (materialId.isBlank() || hardness < 0.0F || resistance < 0.0F) {
            throw new IllegalArgumentException("Invalid GT6 faucet profile: " + id);
        }
    }

    public static MteFaucetProfile require(MteInPlaceSpec spec) {
        if (spec.kind() != MteInPlaceKind.FAUCET) {
            throw new IllegalArgumentException("Not a faucet: " + spec.id());
        }
        MteFaucetProfile profile = BY_ID.get(spec.id());
        if (profile == null) {
            throw new IllegalStateException(
                    "Missing GT6 faucet profile for " + spec.id());
        }
        return profile;
    }

    public static boolean contains(MteInPlaceSpec spec) {
        return spec != null && BY_ID.containsKey(spec.id());
    }

    public static List<MteFaucetProfile> all() {
        return List.copyOf(BY_ID.values());
    }

    private static Map<ResourceLocation, MteFaucetProfile> load() {
        try (var stream = MteFaucetProfile.class.getResourceAsStream(RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException("Missing bundled resource " + RESOURCE);
            }
            Document document = new Gson().fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || !SOURCE_REVISION.equals(document.sourceRevision)
                    || document.profiles == null) {
                throw new IllegalStateException(
                        "Invalid GT6 faucet profile catalog");
            }
            LinkedHashMap<ResourceLocation, MteFaucetProfile> profiles =
                    new LinkedHashMap<>();
            for (Row row : document.profiles) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, row.path);
                MteFaucetProfile profile = new MteFaucetProfile(
                        id,
                        row.materialId,
                        row.acidProof,
                        row.hardness,
                        row.resistance);
                if (profiles.put(id, profile) != null) {
                    throw new IllegalStateException(
                            "Duplicate GT6 faucet profile " + id);
                }
            }
            return Map.copyOf(profiles);
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof IllegalStateException state) {
                throw state;
            }
            throw new IllegalStateException(
                    "Failed to load GT6 faucet profiles", failure);
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        int schemaVersion;
        @SerializedName("source_revision")
        String sourceRevision;
        List<Row> profiles;
    }

    private static final class Row {
        String path;
        @SerializedName("material_id")
        String materialId;
        @SerializedName("acid_proof")
        boolean acidProof;
        float hardness;
        float resistance;
    }
}
