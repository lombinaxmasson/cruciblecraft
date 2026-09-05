package com.masson.cruciblecraft.energy.battery;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/** Kind-level battery chemistry, energy type and display names. */
public final class EnergyBatteryKindCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/energy_battery_kinds.json";
    public static final int EXPECTED_SIZE = 7;
    private static final EnergyBatteryKindCatalog BUNDLED = loadBundled();

    private final List<Kind> kinds;
    private final Map<ResourceLocation, Kind> byId;

    public static List<Kind> kinds() {
        return BUNDLED.kinds;
    }

    public static Kind require(ResourceLocation id) {
        Kind kind = BUNDLED.byId.get(id);
        if (kind == null) {
            throw new IllegalArgumentException(
                    "Unknown energy battery kind " + id);
        }
        return kind;
    }

    private EnergyBatteryKindCatalog(List<Kind> kinds) {
        this.kinds = List.copyOf(kinds);
        LinkedHashMap<ResourceLocation, Kind> indexed = new LinkedHashMap<>();
        for (Kind kind : this.kinds) {
            if (indexed.putIfAbsent(kind.id(), kind) != null) {
                throw new IllegalStateException(
                        "Duplicate energy battery kind " + kind.id());
            }
        }
        this.byId = Map.copyOf(indexed);
    }

    private static EnergyBatteryKindCatalog loadBundled() {
        Document document = CatalogJson.readBundled(
                EnergyBatteryKindCatalog.class, RESOURCE, Document.class);
        if (document.schemaVersion != 1
                || document.kinds == null
                || document.kinds.size() != EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Invalid energy battery kind catalog");
        }
        CatalogJson.requireRevision(document.sourceRevision, RESOURCE);
        List<Kind> kinds = document.kinds.stream()
                .map(KindRow::toKind)
                .toList();
        return new EnergyBatteryKindCatalog(kinds);
    }

    public record Kind(
            ResourceLocation id,
            String runtime,
            EnergyType energyType,
            int capacityMultiplier,
            int color,
            String textureProfile,
            boolean hasBar,
            String langZh,
            String langEn,
            String gt6Class) {
        public Kind {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(runtime, "runtime");
            Objects.requireNonNull(energyType, "energyType");
            Objects.requireNonNull(textureProfile, "textureProfile");
            Objects.requireNonNull(langZh, "langZh");
            Objects.requireNonNull(langEn, "langEn");
            Objects.requireNonNull(gt6Class, "gt6Class");
            if (!"battery".equals(runtime) || capacityMultiplier <= 0) {
                throw new IllegalArgumentException(
                        "Battery kind runtime and multiplier are invalid");
            }
        }
    }

    private static final class Document {
        @SerializedName("schema_version")
        private int schemaVersion;
        @SerializedName("source_revision")
        private String sourceRevision;
        private List<KindRow> kinds;
    }

    private static final class KindRow {
        private String id;
        private String runtime;
        private String energy;
        @SerializedName("capacity_multiplier")
        private int capacityMultiplier;
        private int color;
        @SerializedName("texture_profile")
        private String textureProfile;
        @SerializedName("has_bar")
        private boolean hasBar;
        @SerializedName("lang_key_zh")
        private String langKeyZh;
        @SerializedName("lang_key_en")
        private String langKeyEn;
        @SerializedName("gt6_class")
        private String gt6Class;

        private Kind toKind() {
            if (!CatalogJson.nonBlank(id)
                    || !CatalogJson.nonBlank(runtime)
                    || !CatalogJson.nonBlank(energy)
                    || !CatalogJson.nonBlank(textureProfile)
                    || !CatalogJson.nonBlank(langKeyZh)
                    || !CatalogJson.nonBlank(langKeyEn)
                    || !CatalogJson.nonBlank(gt6Class)) {
                throw new IllegalStateException(
                        "Incomplete energy battery kind row " + id);
            }
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                throw new IllegalStateException(
                        "Invalid energy battery kind id " + id);
            }
            return new Kind(
                    parsed,
                    runtime,
                    EnergyBatteryProfile.energyType(energy),
                    capacityMultiplier,
                    color,
                    textureProfile,
                    hasBar,
                    langKeyZh,
                    langKeyEn,
                    gt6Class);
        }
    }
}
