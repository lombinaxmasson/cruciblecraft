package com.masson.cruciblecraft.energy.converter;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.JsonIOException;
import com.google.gson.JsonSyntaxException;

import net.minecraft.resources.ResourceLocation;

/** Validated bundled catalog of fixed-source energy converters. */
public final class EnergyConverterCatalog {
    private static final String RESOURCE =
            "/data/cruciblecraft/energy_converters.json";
    private static final String SOURCE_REVISION =
            "3703e40308c8c030763fd6297dea8b210d2a77b1";
    private static final Gson GSON = new Gson();
    private static final Set<String> EXPECTED_IDS = Set.of(
            "cruciblecraft:bronze_firebox",
            "cruciblecraft:bronze_boiler",
            "cruciblecraft:bronze_steam_engine",
            "cruciblecraft:bronze_dynamo",
            "cruciblecraft:bronze_fuel_engine",
            "cruciblecraft:bronze_gas_generator");
    private static final Set<String> COMPLETE_PROFILES = Set.of(
            "cruciblecraft:bronze_firebox",
            "cruciblecraft:bronze_boiler",
            "cruciblecraft:bronze_steam_engine",
            "cruciblecraft:bronze_dynamo",
            "cruciblecraft:bronze_fuel_engine",
            "cruciblecraft:bronze_gas_generator");
    private static final Map<ResourceLocation, EnergyConverterProfile>
            PROFILES = loadBundled();

    public static List<EnergyConverterProfile> profiles() {
        return List.copyOf(PROFILES.values());
    }

    public static EnergyConverterProfile require(String id) {
        ResourceLocation parsed = ResourceLocation.tryParse(id);
        EnergyConverterProfile profile =
                parsed == null ? null : PROFILES.get(parsed);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown energy converter profile " + id);
        }
        return profile;
    }

    private static Map<ResourceLocation, EnergyConverterProfile>
            loadBundled() {
        try (var stream = EnergyConverterCatalog.class.getResourceAsStream(
                RESOURCE)) {
            if (stream == null) {
                throw new IllegalStateException(
                        "Missing bundled energy converter catalog "
                                + RESOURCE);
            }
            Document document = GSON.fromJson(
                    new InputStreamReader(stream, StandardCharsets.UTF_8),
                    Document.class);
            if (document == null
                    || document.schemaVersion != 1
                    || document.source == null
                    || !SOURCE_REVISION.equals(document.source.revision)
                    || document.source.steamPerEu != 2
                    || document.profiles == null) {
                throw new IllegalStateException(
                        "Invalid energy converter catalog header");
            }
            LinkedHashMap<ResourceLocation, EnergyConverterProfile> result =
                    new LinkedHashMap<>();
            Set<Integer> sourceIds = new HashSet<>();
            for (ProfileRow row : document.profiles) {
                EnergyConverterProfile profile = row.toProfile();
                if (result.putIfAbsent(profile.id(), profile) != null) {
                    throw new IllegalStateException(
                            "Duplicate converter profile " + profile.id());
                }
                if (!sourceIds.add(profile.source().sourceId())) {
                    throw new IllegalStateException(
                            "Duplicate converter source id "
                                    + profile.source().sourceId());
                }
            }
            Set<String> actualIds = result.keySet().stream()
                    .map(ResourceLocation::toString)
                    .collect(java.util.stream.Collectors.toSet());
            if (!EXPECTED_IDS.equals(actualIds)) {
                throw new IllegalStateException(
                        "Energy converter profile set drifted: " + actualIds);
            }
            Set<String> complete = result.values().stream()
                    .filter(profile ->
                            profile.status()
                                    == EnergyConverterProfile.Status.COMPLETE)
                    .map(profile -> profile.id().toString())
                    .collect(java.util.stream.Collectors.toSet());
            if (!COMPLETE_PROFILES.equals(complete)) {
                throw new IllegalStateException(
                        "Completed converter profile set drifted");
            }
            validateSteamConverters(result);
            validateKineticConverters(result);
            validateGasGenerator(result);
            return Map.copyOf(result);
        } catch (IOException
                | JsonIOException
                | JsonSyntaxException
                | IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "Could not load energy converter catalog", exception);
        }
    }

    private static void validateSteamConverters(
            Map<ResourceLocation, EnergyConverterProfile> profiles) {
        EnergyConverterProfile firebox =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_firebox"));
        EnergyConverterProfile boiler =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_boiler"));
        EnergyConverterProfile engine =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_steam_engine"));
        EnergyConverterProfile.OutputSemantics engineSemantics =
                engine.outputSemantics();
        if (firebox.source().sourceId() != 1102
                || firebox.outputPacket().maxAmountPerTick() != 24L
                || !Integer.valueOf(7_500).equals(
                        firebox.efficiencyBps())
                || boiler.source().sourceId() != 1202
                || boiler.conservation().primaryInputUnits() != 80
                || boiler.conservation().secondaryInputUnits() != 1
                || boiler.conservation().outputUnits() != 160
                || engine.source().sourceId() != 1302
                || !"24/STEAM_PER_EU".equals(
                        engine.source().outputExpression())
                || engine.outputPacket().size() != 12L
                || !Integer.valueOf(5_000).equals(
                        engine.efficiencyBps())
                || engine.conservation().primaryInputUnits() != 200
                || engine.conservation().outputUnits() != 50
                || engineSemantics == null
                || !"SOURCE_BACKED".equals(
                        engineSemantics.conservation().classification())
                || engineSemantics.conservation().steamInputMb() != 200
                || engineSemantics.conservation().kuOutput() != 50
                || engineSemantics.conservation().steamMbPerKu() != 4
                || !"SOURCE_DERIVED_NOMINAL".equals(
                        engineSemantics.sourceNominal().classification())
                || engineSemantics.sourceNominal().registeredNumerator() != 24
                || engineSemantics.sourceNominal().steamPerEu() != 2
                || engineSemantics.sourceNominal().mOutputKu() != 12
                || !"DESIGN_POLICY_FIXED_OUTPUT".equals(
                        engineSemantics.fixedOutput().classification())
                || engineSemantics.fixedOutput().kuPerTick() != 12
                || !"DEFERRED_REPLACEMENT".equals(
                        engineSemantics.gt6Runtime().classification())
                || engineSemantics.gt6Runtime().minimumKuPerTick() != 6
                || engineSemantics.gt6Runtime().maximumKuPerTick() != 24
                || engineSemantics.sourceEvidencePaths().size() != 3) {
            throw new IllegalStateException(
                    "Steam-chain converter rows drifted");
        }
    }

    private static void validateKineticConverters(
            Map<ResourceLocation, EnergyConverterProfile> profiles) {
        EnergyConverterProfile dynamo =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_dynamo"));
        EnergyConverterProfile fuelEngine =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_fuel_engine"));
        if (dynamo.source().sourceId() != 10111
                || dynamo.inputWindow().minimum() != 16L
                || dynamo.inputWindow().nominal() != 32L
                || dynamo.inputWindow().maximum() != 64L
                || dynamo.outputPacket().size() != 22L
                || !Integer.valueOf(6_875).equals(
                        dynamo.efficiencyBps())
                || dynamo.conservation().primaryInputUnits() != 32
                || dynamo.conservation().outputUnits() != 22
                || dynamo.conservation().exhaustUnits() != 10
                || !dynamo.policy().sourceResolution().contains(
                        "LEGACY_LOCAL_24_RU_TO_24_EU_REPLACED")
                || fuelEngine.source().sourceId() != 9147
                || fuelEngine.outputPacket().size() != 16L
                || !Integer.valueOf(10_000).equals(
                        fuelEngine.efficiencyBps())
                || !"RU".equals(
                        fuelEngine.outputPacket().identity())) {
            throw new IllegalStateException(
                    "Kinetic converter rows drifted");
        }
    }

    private static void validateGasGenerator(
            Map<ResourceLocation, EnergyConverterProfile> profiles) {
        EnergyConverterProfile gasGenerator =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_gas_generator"));
        if (gasGenerator.source().sourceId() != 1602
                || !"MultiTileEntityGeneratorGas".equals(
                        gasGenerator.source().machineKind())
                || !"FM.Burn".equals(gasGenerator.fuelMap())
                || gasGenerator.outputPacket().size() != 1L
                || gasGenerator.outputPacket().maxAmountPerTick() != 24L
                || !Integer.valueOf(7_500).equals(
                        gasGenerator.efficiencyBps())
                || gasGenerator.conservation().primaryInputUnits() != 1536
                || gasGenerator.conservation().outputUnits() != 1152
                || gasGenerator.conservation().exhaustUnits() != 9
                || !gasGenerator.faces().energyOutputs().equals(
                        List.of("UP"))
                || !gasGenerator.policy().sourceResolution().contains(
                        "CURRENT_HEAT_IDENTITY_REQUIRED")
                || !gasGenerator.policy().sourceResolution().contains(
                        "MISSING_PARTIAL_OR_WRONG_IDENTITY_QUARANTINED")) {
            throw new IllegalStateException(
                    "Gas-generator converter row drifted");
        }
    }

    private static final class Document {
        private int schemaVersion;
        private SourceHeader source;
        private List<ProfileRow> profiles;
    }

    private static final class SourceHeader {
        private String revision;
        private int steamPerEu;
    }

    private static final class ProfileRow {
        private String id;
        private String stage;
        private String status;
        private String runtimeBinding;
        private SourceRow source;
        private List<String> accepts;
        private List<String> emits;
        private PacketRow inputPacket;
        private PacketRow outputPacket;
        private WindowRow inputWindow;
        private Integer efficiencyBps;
        private String fuelMap;
        private ConservationRow conservation;
        private ExhaustRow exhaust;
        private FacesRow faces;
        private PolicyRow policy;
        private OutputSemanticsRow outputSemantics;
        private int inputCapacity;
        private int outputCapacity;

        private EnergyConverterProfile toProfile() {
            ResourceLocation parsed = ResourceLocation.tryParse(id);
            if (parsed == null) {
                throw new IllegalStateException(
                        "Invalid converter profile id " + id);
            }
            EnergyConverterProfile.Status parsedStatus;
            try {
                parsedStatus = EnergyConverterProfile.Status.valueOf(status);
            } catch (IllegalArgumentException | NullPointerException error) {
                throw new IllegalStateException(
                        "Invalid converter profile status " + status, error);
            }
            if (source == null
                    || inputPacket == null
                    || outputPacket == null
                    || inputWindow == null
                    || conservation == null
                    || exhaust == null
                    || faces == null
                    || policy == null) {
                throw new IllegalStateException(
                        id + ": incomplete converter profile");
            }
            return new EnergyConverterProfile(
                    parsed,
                    stage,
                    parsedStatus,
                    runtimeBinding,
                    source.toSource(),
                    accepts == null ? List.of() : accepts,
                    emits == null ? List.of() : emits,
                    inputPacket.toPacket(),
                    outputPacket.toPacket(),
                    inputWindow.toWindow(),
                    efficiencyBps,
                    fuelMap,
                    conservation.toConservation(),
                    exhaust.toExhaust(),
                    faces.toFaces(),
                    policy.toPolicy(),
                    outputSemantics == null
                            ? null
                            : outputSemantics.toOutputSemantics(),
                    inputCapacity,
                    outputCapacity);
        }
    }

    private static final class SourceRow {
        private String machineKind;
        private int sourceId;
        private int sourceLine;
        private String materialExpression;
        private String normalizedRowKey;
        private String outputExpression;
        private String efficiencyExpression;
        private String fuelMap;

        private EnergyConverterProfile.Source toSource() {
            return new EnergyConverterProfile.Source(
                    machineKind,
                    sourceId,
                    sourceLine,
                    materialExpression,
                    normalizedRowKey,
                    outputExpression,
                    efficiencyExpression,
                    fuelMap);
        }
    }

    private static final class PacketRow {
        private String medium;
        private String identity;
        private long size;
        private long maxAmountPerTick;

        private EnergyConverterProfile.Packet toPacket() {
            return new EnergyConverterProfile.Packet(
                    medium, identity, size, maxAmountPerTick);
        }
    }

    private static final class WindowRow {
        private Long minimum;
        private Long nominal;
        private Long maximum;

        private EnergyConverterProfile.Window toWindow() {
            return new EnergyConverterProfile.Window(
                    minimum, nominal, maximum);
        }
    }

    private static final class ConservationRow {
        private String primaryInput;
        private int primaryInputUnits;
        private String secondaryInput;
        private int secondaryInputUnits;
        private String output;
        private int outputUnits;
        private String exhaust;
        private int exhaustUnits;

        private EnergyConverterProfile.Conservation toConservation() {
            return new EnergyConverterProfile.Conservation(
                    primaryInput,
                    primaryInputUnits,
                    secondaryInput,
                    secondaryInputUnits,
                    output,
                    outputUnits,
                    exhaust,
                    exhaustUnits);
        }
    }

    private static final class ExhaustRow {
        private String identity;
        private String mode;
        private int capacity;

        private EnergyConverterProfile.Exhaust toExhaust() {
            return new EnergyConverterProfile.Exhaust(
                    identity, mode, capacity);
        }
    }

    private static final class FacesRow {
        private List<String> energyInputs;
        private List<String> energyOutputs;
        private List<String> fluidInputs;
        private List<String> fluidOutputs;

        private EnergyConverterProfile.Faces toFaces() {
            return new EnergyConverterProfile.Faces(
                    list(energyInputs),
                    list(energyOutputs),
                    list(fluidInputs),
                    list(fluidOutputs));
        }
    }

    private static final class PolicyRow {
        private boolean simulateBeforeExecute;
        private boolean exactCommit;
        private String blockage;
        private String overflow;
        private String sourceResolution;

        private EnergyConverterProfile.Policy toPolicy() {
            return new EnergyConverterProfile.Policy(
                    simulateBeforeExecute,
                    exactCommit,
                    blockage,
                    overflow,
                    sourceResolution);
        }
    }

    private static final class OutputSemanticsRow {
        private ConservationClassificationRow conservation;
        private SourceNominalRow sourceNominal;
        private FixedOutputRow fixedOutput;
        private Gt6RuntimeOutputRow gt6Runtime;
        private List<String> sourceEvidencePaths;

        private EnergyConverterProfile.OutputSemantics toOutputSemantics() {
            if (conservation == null
                    || sourceNominal == null
                    || fixedOutput == null
                    || gt6Runtime == null) {
                throw new IllegalStateException(
                        "Incomplete converter output semantics");
            }
            return new EnergyConverterProfile.OutputSemantics(
                    conservation.toConservationClassification(),
                    sourceNominal.toSourceNominal(),
                    fixedOutput.toFixedOutput(),
                    gt6Runtime.toGt6RuntimeOutput(),
                    list(sourceEvidencePaths));
        }
    }

    private static final class ConservationClassificationRow {
        private String classification;
        private int steamInputMb;
        private int kuOutput;
        private int steamMbPerKu;

        private EnergyConverterProfile.ConservationClassification
                toConservationClassification() {
            return new EnergyConverterProfile.ConservationClassification(
                    classification, steamInputMb, kuOutput, steamMbPerKu);
        }
    }

    private static final class SourceNominalRow {
        private String classification;
        private int registeredNumerator;
        private int steamPerEu;
        private int mOutputKu;

        private EnergyConverterProfile.SourceNominal toSourceNominal() {
            return new EnergyConverterProfile.SourceNominal(
                    classification,
                    registeredNumerator,
                    steamPerEu,
                    mOutputKu);
        }
    }

    private static final class FixedOutputRow {
        private String classification;
        private int kuPerTick;

        private EnergyConverterProfile.FixedOutput toFixedOutput() {
            return new EnergyConverterProfile.FixedOutput(
                    classification, kuPerTick);
        }
    }

    private static final class Gt6RuntimeOutputRow {
        private String classification;
        private int minimumKuPerTick;
        private int maximumKuPerTick;
        private String behavior;
        private String replacementCondition;
        private String recheckPoint;

        private EnergyConverterProfile.Gt6RuntimeOutput
                toGt6RuntimeOutput() {
            return new EnergyConverterProfile.Gt6RuntimeOutput(
                    classification,
                    minimumKuPerTick,
                    maximumKuPerTick,
                    behavior,
                    replacementCondition,
                    recheckPoint);
        }
    }

    private static List<String> list(List<String> values) {
        return values == null ? List.of() : values;
    }

    private EnergyConverterCatalog() {}
}
