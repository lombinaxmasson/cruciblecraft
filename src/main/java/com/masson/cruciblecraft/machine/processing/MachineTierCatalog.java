package com.masson.cruciblecraft.machine.processing;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;
import com.google.gson.annotations.SerializedName;
import com.masson.cruciblecraft.api.energy.EnergyType;

import net.minecraft.resources.ResourceLocation;

/** Bundled catalog for source-backed variants and fixed controller profiles. */
public final class MachineTierCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/machine_tiers.json";
    private static final Gson GSON = new Gson();
    private static final Catalog CATALOG = loadBundled();

    public static List<Entry> entries() {
        return CATALOG.entries();
    }

    public static List<TierProfile> controllerProfiles() {
        return List.copyOf(CATALOG.controllerProfiles().values());
    }

    public static TierProfile requireControllerProfile(ResourceLocation id) {
        TierProfile profile = CATALOG.controllerProfiles().get(id);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown machine controller profile " + id);
        }
        return profile;
    }

    private static Catalog loadBundled() {
        try (var stream = MachineTierCatalog.class.getResourceAsStream(
                RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled machine tier catalog " + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(
                            stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || document.variants == null
                    || document.controllerProfiles == null
                    || document.source == null
                    || !"3703e40308c8c030763fd6297dea8b210d2a77b1"
                            .equals(document.source.revision)
                    || !document.source.complete()) {
                throw new IllegalStateException(
                        "Invalid machine tier catalog schema");
            }
            List<Entry> entries = document.variants.stream()
                    .map(Row::toEntry)
                    .toList();
            Set<ResourceLocation> ids = new HashSet<>();
            for (Entry entry : entries) {
                if (!ids.add(entry.variantId())) {
                    throw new IllegalStateException(
                            "Duplicate machine variant " + entry.variantId());
                }
            }
            Map<ResourceLocation, Set<KindPolicy>> policiesByKind =
                    entries.stream().collect(Collectors.groupingBy(
                            Entry::kindId,
                            Collectors.mapping(
                                    entry -> new KindPolicy(
                                            entry.overclockPolicy(),
                                            entry.parallelDuration()),
                                    Collectors.toSet())));
            List<ResourceLocation> inconsistentKinds =
                    policiesByKind.entrySet().stream()
                            .filter(entry -> entry.getValue().size() != 1)
                            .map(Map.Entry::getKey)
                            .toList();
            if (!inconsistentKinds.isEmpty()) {
                throw new IllegalStateException(
                        "Machine tier source policies differ within kinds: "
                                + inconsistentKinds);
            }
            Set<String> sourcedVariants =
                    document.source.variantRows.keySet();
            Set<String> runtimeVariants = entries.stream()
                    .map(entry -> entry.variantId().toString())
                    .collect(java.util.stream.Collectors.toSet());
            if (!sourcedVariants.equals(runtimeVariants)) {
                throw new IllegalStateException(
                        "Machine tier source rows do not cover variants: "
                                + sourcedVariants
                                + " != "
                                + runtimeVariants);
            }
            LinkedHashMap<ResourceLocation, TierProfile> controllerProfiles =
                    new LinkedHashMap<>();
            for (ControllerProfileRow row : document.controllerProfiles) {
                TierProfile profile = row.toProfile();
                if (controllerProfiles.putIfAbsent(
                        profile.id(), profile) != null) {
                    throw new IllegalStateException(
                            "Duplicate machine controller profile "
                                    + profile.id());
                }
            }
            Set<String> sourcedProfiles =
                    document.source.controllerProfileRows.keySet();
            Set<String> runtimeProfiles = controllerProfiles.keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(java.util.stream.Collectors.toSet());
            if (!sourcedProfiles.equals(runtimeProfiles)) {
                throw new IllegalStateException(
                        "Machine tier source rows do not cover controller profiles: "
                                + sourcedProfiles
                                + " != "
                                + runtimeProfiles);
            }
            return new Catalog(
                    List.copyOf(entries), Map.copyOf(controllerProfiles));
        } catch (IOException
                | JsonIOException
                | JsonSyntaxException exception) {
            throw new IllegalStateException(
                    "Could not load machine tier catalog", exception);
        }
    }

    private record Catalog(
            List<Entry> entries,
            Map<ResourceLocation, TierProfile> controllerProfiles) {}

    public record Entry(
            ResourceLocation variantId,
            ResourceLocation kindId,
            int sourceId,
            int sourceTier,
            String sourceMaterial,
            boolean materialRegistered,
            String acquisitionBlocker,
            MachineKindSpec.OverclockPolicy overclockPolicy,
            boolean parallelDuration,
            TierProfile tier) {
        public Entry {
            Objects.requireNonNull(variantId, "variantId");
            Objects.requireNonNull(kindId, "kindId");
            if (sourceId <= 0 || sourceTier <= 0) {
                throw new IllegalArgumentException(
                        "Machine source id and tier must be positive");
            }
            Objects.requireNonNull(overclockPolicy, "overclockPolicy");
            Objects.requireNonNull(tier, "tier");
            if (tier.energyType() == EnergyType.HEAT
                    && (!nonBlank(sourceMaterial)
                            || materialRegistered
                                    == nonBlank(acquisitionBlocker))) {
                throw new IllegalArgumentException(
                        "Heat tier source material mapping is incomplete for "
                                + variantId);
            }
        }
    }

    private record KindPolicy(
            MachineKindSpec.OverclockPolicy overclockPolicy,
            boolean parallelDuration) {}

    private static final class Document {
        private int schemaVersion;
        private Source source;
        private List<Row> variants;
        @SerializedName("controller_profiles")
        private List<ControllerProfileRow> controllerProfiles;
    }

    private static final class Source {
        private String repository;
        private String revision;
        @SerializedName("machine_registration")
        private String machineRegistration;
        @SerializedName("runtime_semantics")
        private String runtimeSemantics;
        @SerializedName("material_arrays")
        private String materialArrays;
        @SerializedName("controller_registration")
        private String controllerRegistration;
        private Normalization normalization;
        @SerializedName("variant_rows")
        private Map<String, String> variantRows;
        @SerializedName("controller_profile_rows")
        private Map<String, String> controllerProfileRows;

        private boolean complete() {
            return nonBlank(repository)
                    && nonBlank(machineRegistration)
                    && nonBlank(runtimeSemantics)
                    && nonBlank(materialArrays)
                    && nonBlank(controllerRegistration)
                    && normalization != null
                    && normalization.complete()
                    && variantRows != null
                    && controllerProfileRows != null;
        }
    }

    private static final class Normalization {
        @SerializedName("input_window")
        private String inputWindow;
        private String efficiency;
        @SerializedName("parallel_duration")
        private String parallelDuration;
        private String overclock;
        @SerializedName("controller_profile")
        private String controllerProfile;

        private boolean complete() {
            return nonBlank(inputWindow)
                    && nonBlank(efficiency)
                    && nonBlank(parallelDuration)
                    && nonBlank(overclock)
                    && nonBlank(controllerProfile);
        }
    }

    private static final class Row {
        private String id;
        private String kind;
        private String tier;
        private String material;
        private String energy;
        private int sourceId;
        private int sourceTier;
        private String sourceMaterial;
        private boolean materialRegistered;
        private String acquisitionBlocker;
        private String overclock;
        private boolean parallelDuration;
        private long inputMinimum;
        private long inputNominal;
        private long inputMaximum;
        private long energyCapacity;
        private int parallel;
        private int efficiency;

        private Entry toEntry() {
            ResourceLocation variantId = parse(id, "variant id");
            ResourceLocation kindId = parse(kind, "kind id");
            MachineKindSpec.OverclockPolicy overclockPolicy;
            try {
                overclockPolicy =
                        MachineKindSpec.OverclockPolicy.valueOf(overclock);
            } catch (IllegalArgumentException | NullPointerException exception) {
                throw new IllegalStateException(
                        "Unknown machine overclock policy " + overclock,
                        exception);
            }
            return new Entry(
                    variantId,
                    kindId,
                    sourceId,
                    sourceTier,
                    sourceMaterial,
                    materialRegistered,
                    acquisitionBlocker,
                    overclockPolicy,
                    parallelDuration,
                    toProfile(
                            tier,
                            material,
                            energy,
                            inputMinimum,
                            inputNominal,
                            inputMaximum,
                            energyCapacity,
                            parallel,
                            efficiency));
        }
    }

    private static final class ControllerProfileRow {
        private String id;
        private String material;
        private String energy;
        private long inputMinimum;
        private long inputNominal;
        private long inputMaximum;
        private long energyCapacity;
        private int parallel;
        private int efficiency;

        private TierProfile toProfile() {
            return MachineTierCatalog.toProfile(
                    id,
                    material,
                    energy,
                    inputMinimum,
                    inputNominal,
                    inputMaximum,
                    energyCapacity,
                    parallel,
                    efficiency);
        }
    }

    private static TierProfile toProfile(
            String id,
            String material,
            String energy,
            long inputMinimum,
            long inputNominal,
            long inputMaximum,
            long energyCapacity,
            int parallel,
            int efficiency) {
        EnergyType energyType;
        try {
            energyType = EnergyType.valueOf(energy);
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Unknown machine tier energy " + energy, exception);
        }
        return new TierProfile(
                parse(id, "tier id"),
                Objects.requireNonNull(material, "tier material"),
                energyType,
                inputMinimum,
                inputNominal,
                inputMaximum,
                energyCapacity,
                parallel,
                efficiency);
    }

    private static ResourceLocation parse(String value, String name) {
        ResourceLocation parsed = ResourceLocation.tryParse(value);
        if (parsed == null) {
            throw new IllegalStateException(
                    "Invalid " + name + ": " + value);
        }
        return parsed;
    }

    private static boolean nonBlank(String value) {
        return value != null && !value.isBlank();
    }

    private MachineTierCatalog() {}
}
