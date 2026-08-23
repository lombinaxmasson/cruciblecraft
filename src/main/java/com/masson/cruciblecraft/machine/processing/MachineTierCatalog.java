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

    /** Actual existing rows for one kind. This is not a kind × tier matrix. */
    public static List<Entry> variantsOf(ResourceLocation kindId) {
        Objects.requireNonNull(kindId, "kindId");
        return CATALOG.byKind().getOrDefault(kindId, List.of());
    }

    public static Entry require(ResourceLocation id) {
        Entry entry = CATALOG.byId().get(id);
        if (entry == null) {
            throw new IllegalArgumentException("Unknown machine variant " + id);
        }
        return entry;
    }

    public static NamingPolicy namingPolicy() {
        return CATALOG.namingPolicy();
    }

    /** Catalog-declared texture folder; never inferred from filename coupling. */
    public static String textureProfile(String variantPath) {
        Objects.requireNonNull(variantPath, "variantPath");
        String profile = CATALOG.textureProfiles().get(variantPath);
        return profile == null || profile.isBlank() ? variantPath : profile;
    }

    public static List<TierProfile> controllerTierBands() {
        return List.copyOf(CATALOG.controllerTierBands().values());
    }

    public static TierProfile requireControllerTierBand(
            ResourceLocation tierBandId) {
        TierProfile profile = CATALOG.controllerTierBands().get(tierBandId);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown machine controller tier band " + tierBandId);
        }
        return profile;
    }

    /** @deprecated Use {@link #controllerTierBands()}. */
    @Deprecated(forRemoval = false)
    public static List<TierProfile> controllerProfiles() {
        return controllerTierBands();
    }

    /** @deprecated Use {@link #requireControllerTierBand(ResourceLocation)}. */
    @Deprecated(forRemoval = false)
    public static TierProfile requireControllerProfile(ResourceLocation id) {
        return requireControllerTierBand(id);
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
                    || document.schemaVersion != 3
                    || document.variants == null
                    || document.controllerProfiles == null
                    || document.source == null
                    || document.namingPolicy == null
                    || !"3703e40308c8c030763fd6297dea8b210d2a77b1"
                            .equals(document.source.revision)
                    || !document.source.complete()
                    || !document.namingPolicy.complete()) {
                throw new IllegalStateException(
                        "Invalid machine tier catalog schema");
            }
            // Opening 33 ids remain in the T36 target overlay; current size is
            // the frozen target, not a hard ceiling.
            List<Entry> entries = document.variants.stream()
                    .filter(row -> row.resourceProfile == null
                            || !row.resourceProfile.skipGenericRegistration)
                    .map(Row::toEntry)
                    .toList();
            Set<ResourceLocation> ids = new HashSet<>();
            for (Entry entry : entries) {
                if (!ids.add(entry.variantId())) {
                    throw new IllegalStateException(
                            "Duplicate machine variant " + entry.variantId());
                }
            }
            Map<ResourceLocation, Set<TierBandCapabilities>>
                    capabilitiesByTierBand =
                            entries.stream().collect(Collectors.groupingBy(
                                    entry -> entry.tierBand().tierBandId(),
                                    Collectors.mapping(
                                            entry -> TierBandCapabilities.of(
                                                    entry.tierBand()),
                                            Collectors.toSet())));
            List<ResourceLocation> inconsistentTierBands =
                    capabilitiesByTierBand.entrySet().stream()
                            .filter(entry -> entry.getValue().size() != 1)
                            .map(Map.Entry::getKey)
                            .toList();
            if (!inconsistentTierBands.isEmpty()) {
                throw new IllegalStateException(
                        "Machine tier-band capabilities differ within bands: "
                                + inconsistentTierBands);
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
            Set<String> skippedVariants = document.variants.stream()
                    .filter(row -> row.resourceProfile != null
                            && row.resourceProfile.skipGenericRegistration)
                    .map(row -> row.id)
                    .collect(java.util.stream.Collectors.toSet());
            Set<String> expectedSourced = new HashSet<>(runtimeVariants);
            expectedSourced.addAll(skippedVariants);
            if (!sourcedVariants.equals(expectedSourced)) {
                throw new IllegalStateException(
                        "Machine tier source rows do not cover variants: "
                                + sourcedVariants
                                + " != "
                                + expectedSourced);
            }
            LinkedHashMap<ResourceLocation, TierProfile> controllerTierBands =
                    new LinkedHashMap<>();
            for (ControllerProfileRow row : document.controllerProfiles) {
                TierProfile profile = row.toProfile();
                if (controllerTierBands.putIfAbsent(
                        profile.tierBandId(), profile) != null) {
                    throw new IllegalStateException(
                            "Duplicate machine controller tier band "
                                    + profile.tierBandId());
                }
            }
            Set<String> sourcedProfiles =
                    document.source.controllerProfileRows.keySet();
            Set<String> runtimeProfiles =
                    controllerTierBands.keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(java.util.stream.Collectors.toSet());
            if (!sourcedProfiles.equals(runtimeProfiles)) {
                throw new IllegalStateException(
                        "Machine tier source rows do not cover controller profiles: "
                                + sourcedProfiles
                                + " != "
                                + runtimeProfiles);
            }
            LinkedHashMap<ResourceLocation, Entry> byId = new LinkedHashMap<>();
            for (Entry entry : entries) {
                byId.put(entry.variantId(), entry);
            }
            Map<ResourceLocation, List<Entry>> byKind = entries.stream().collect(
                    Collectors.groupingBy(
                            Entry::kindId,
                            LinkedHashMap::new,
                            Collectors.toUnmodifiableList()));
            LinkedHashMap<String, String> textureProfiles = new LinkedHashMap<>();
            for (Row row : document.variants) {
                ResourceLocation variantId = parse(row.id, "variant id");
                String profile = row.resourceProfile == null
                        || row.resourceProfile.textureProfile == null
                        || row.resourceProfile.textureProfile.isBlank()
                        ? variantId.getPath()
                        : row.resourceProfile.textureProfile;
                textureProfiles.put(variantId.getPath(), profile);
            }
            return new Catalog(
                    List.copyOf(entries),
                    Map.copyOf(controllerTierBands),
                    Map.copyOf(byId),
                    byKind,
                    document.namingPolicy.toPolicy(),
                    Map.copyOf(textureProfiles));
        } catch (IOException
                | JsonIOException
                | JsonSyntaxException exception) {
            throw new IllegalStateException(
                    "Could not load machine tier catalog", exception);
        }
    }

    private record Catalog(
            List<Entry> entries,
            Map<ResourceLocation, TierProfile> controllerTierBands,
            Map<ResourceLocation, Entry> byId,
            Map<ResourceLocation, List<Entry>> byKind,
            NamingPolicy namingPolicy,
            Map<String, String> textureProfiles) {}

    public record NamingPolicy(
            String tier1BareId,
            String newSubsystemId,
            boolean automaticKindTierCompletion) {
        public NamingPolicy {
            if (!"frozen_legacy_baseline".equals(tier1BareId)
                    || !"<material>_<kind>".equals(newSubsystemId)
                    || automaticKindTierCompletion) {
                throw new IllegalStateException(
                        "Machine naming policy must freeze legacy bare ids "
                                + "and forbid kind × tier completion");
            }
        }
    }

    public record ResourceProfile(
            String sharedModel,
            String textureProfile,
            boolean skipGenericRegistration) {
        public ResourceProfile {
            Objects.requireNonNull(sharedModel, "sharedModel");
            Objects.requireNonNull(textureProfile, "textureProfile");
        }
    }

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
            TierProfile tierBand,
            ResourceProfile resourceProfile) {
        public Entry {
            Objects.requireNonNull(variantId, "variantId");
            Objects.requireNonNull(kindId, "kindId");
            if (sourceId <= 0 || sourceTier <= 0) {
                throw new IllegalArgumentException(
                        "Machine source id and tier must be positive");
            }
            Objects.requireNonNull(overclockPolicy, "overclockPolicy");
            Objects.requireNonNull(tierBand, "tierBand");
            Objects.requireNonNull(resourceProfile, "resourceProfile");
            if (tierBand.energyType() == EnergyType.HEAT
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

    private record TierBandCapabilities(
            String materialId,
            EnergyType energyType,
            long inputMinimum,
            long inputNominal,
            long inputMaximum,
            long energyCapacity,
            int efficiency) {
        private static TierBandCapabilities of(TierProfile profile) {
            return new TierBandCapabilities(
                    profile.materialId(),
                    profile.energyType(),
                    profile.inputMinimum(),
                    profile.inputNominal(),
                    profile.inputMaximum(),
                    profile.energyCapacity(),
                    profile.efficiency());
        }
    }

    private static final class Document {
        private int schemaVersion;
        private NamingPolicyRow namingPolicy;
        private Source source;
        private List<Row> variants;
        @SerializedName("controller_profiles")
        private List<ControllerProfileRow> controllerProfiles;
    }

    private static final class NamingPolicyRow {
        private String tier1BareId;
        private String newSubsystemId;
        private Boolean automaticKindTierCompletion;

        private boolean complete() {
            return "frozen_legacy_baseline".equals(tier1BareId)
                    && "<material>_<kind>".equals(newSubsystemId)
                    && Boolean.FALSE.equals(automaticKindTierCompletion);
        }

        private NamingPolicy toPolicy() {
            return new NamingPolicy(
                    tier1BareId,
                    newSubsystemId,
                    automaticKindTierCompletion);
        }
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

    private static final class ResourceProfileRow {
        private String sharedModel;
        private String textureProfile;
        private boolean skipGenericRegistration;

        private ResourceProfile toProfile(String variantPath) {
            String model = nonBlank(sharedModel)
                    ? sharedModel
                    : "processing_machine";
            String texture = nonBlank(textureProfile)
                    ? textureProfile
                    : variantPath;
            return new ResourceProfile(model, texture, skipGenericRegistration);
        }
    }

    private static final class Row {
        private String id;
        private String kind;
        private String tierBand;
        private String material;
        private String energy;
        private int sourceId;
        private int sourceTier;
        private String sourceMaterial;
        private boolean materialRegistered;
        private String acquisitionBlocker;
        private String overclock;
        private boolean parallelDuration;
        private ResourceProfileRow resourceProfile;
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
            ResourceProfile profile = resourceProfile == null
                    ? new ResourceProfile(
                            "processing_machine",
                            variantId.getPath(),
                            false)
                    : resourceProfile.toProfile(variantId.getPath());
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
                            tierBand,
                            material,
                            energy,
                            inputMinimum,
                            inputNominal,
                            inputMaximum,
                            energyCapacity,
                            parallel,
                            efficiency),
                    profile);
        }
    }

    private static final class ControllerProfileRow {
        private String tierBand;
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
                    tierBand,
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
            String tierBand,
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
                parse(tierBand, "tier band id"),
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
