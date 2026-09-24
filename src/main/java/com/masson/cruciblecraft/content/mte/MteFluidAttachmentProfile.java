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
 * Source-backed material and phase properties for GT6 fluid attachments.
 *
 * <p>The general in-place catalog also contains non-fluid MTEs, so these
 * properties live in a narrow companion catalog instead of adding optional
 * fields to every MTE identity.
 */
public record MteFluidAttachmentProfile(
        ResourceLocation id,
        MteInPlaceKind kind,
        String materialId,
        Phase phase,
        boolean acidProof,
        boolean magicProof,
        float hardness,
        float resistance) {
    private static final String RESOURCE =
            "/data/cruciblecraft/mte_fluid_attachment_profiles.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Map<ResourceLocation, MteFluidAttachmentProfile> BY_ID =
            load();

    public MteFluidAttachmentProfile {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(materialId, "materialId");
        Objects.requireNonNull(phase, "phase");
        if (!kind.attachment() || kind == MteInPlaceKind.FAUCET) {
            throw new IllegalArgumentException(
                    "Fluid profile requires Tap/Funnel/Nozzle/Cap Nozzle: " + id);
        }
        if (materialId.isBlank() || hardness < 0.0F || resistance < 0.0F) {
            throw new IllegalArgumentException("Invalid fluid profile: " + id);
        }
    }

    public static MteFluidAttachmentProfile require(MteInPlaceSpec spec) {
        MteFluidAttachmentProfile profile = BY_ID.get(spec.id());
        if (profile == null) {
            throw new IllegalStateException(
                    "Missing GT6 fluid attachment profile for " + spec.id());
        }
        if (profile.kind() != spec.kind()) {
            throw new IllegalStateException(
                    "Fluid attachment profile kind drifted for " + spec.id());
        }
        return profile;
    }

    public static boolean contains(MteInPlaceSpec spec) {
        return BY_ID.containsKey(spec.id());
    }

    public static List<MteFluidAttachmentProfile> all() {
        return List.copyOf(BY_ID.values());
    }

    private static Map<ResourceLocation, MteFluidAttachmentProfile> load() {
        try (var stream =
                MteFluidAttachmentProfile.class.getResourceAsStream(RESOURCE)) {
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
                        "Invalid GT6 fluid attachment profile catalog");
            }
            LinkedHashMap<ResourceLocation, MteFluidAttachmentProfile> profiles =
                    new LinkedHashMap<>();
            for (Row row : document.profiles) {
                ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, row.path);
                MteFluidAttachmentProfile profile =
                        new MteFluidAttachmentProfile(
                                id,
                                MteInPlaceKind.valueOf(row.kind),
                                row.materialId,
                                Phase.valueOf(row.phase),
                                row.acidProof,
                                row.magicProof,
                                row.hardness,
                                row.resistance);
                if (profiles.put(id, profile) != null) {
                    throw new IllegalStateException(
                            "Duplicate GT6 fluid attachment profile " + id);
                }
            }
            return Map.copyOf(profiles);
        } catch (IOException | RuntimeException failure) {
            if (failure instanceof IllegalStateException state) {
                throw state;
            }
            throw new IllegalStateException(
                    "Failed to load GT6 fluid attachment profiles", failure);
        }
    }

    public enum Phase {
        LIQUID,
        GAS
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
        String kind;
        @SerializedName("material_id")
        String materialId;
        String phase;
        @SerializedName("acid_proof")
        boolean acidProof;
        @SerializedName("magic_proof")
        boolean magicProof;
        float hardness;
        float resistance;
    }
}
