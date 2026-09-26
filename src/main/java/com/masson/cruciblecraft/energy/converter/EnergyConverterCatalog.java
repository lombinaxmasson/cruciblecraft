package com.masson.cruciblecraft.energy.converter;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/** Validated bundled catalog of fixed-source energy converters. */
public final class EnergyConverterCatalog {
    private static final Set<String> EXPECTED_IDS = Set.of(
            "cruciblecraft:bronze_boiler",
            "cruciblecraft:bronze_steam_engine",
            "cruciblecraft:bronze_dynamo",
            "cruciblecraft:bronze_fuel_engine",
            "cruciblecraft:bronze_small_gas_turbine",
            "cruciblecraft:bronze_burning_box_gas",
            "cruciblecraft:steel_galvanized_electric_heater",
            "cruciblecraft:steel_galvanized_electric_engine");
    private static final Set<String> COMPLETE_PROFILES = Set.of(
            "cruciblecraft:bronze_boiler",
            "cruciblecraft:bronze_steam_engine",
            "cruciblecraft:bronze_dynamo",
            "cruciblecraft:bronze_fuel_engine",
            "cruciblecraft:bronze_small_gas_turbine",
            "cruciblecraft:bronze_burning_box_gas",
            "cruciblecraft:steel_galvanized_electric_heater",
            "cruciblecraft:aluminium_electric_heater",
            "cruciblecraft:stainless_steel_electric_heater",
            "cruciblecraft:chromium_electric_heater",
            "cruciblecraft:titanium_electric_heater",
            "cruciblecraft:steel_galvanized_electric_engine",
            "cruciblecraft:aluminium_electric_engine",
            "cruciblecraft:stainless_steel_electric_engine",
            "cruciblecraft:chromium_electric_engine",
            "cruciblecraft:titanium_electric_engine");
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

    public static EnergyConverterProfile require(ResourceLocation id) {
        EnergyConverterProfile profile = PROFILES.get(id);
        if (profile == null) {
            throw new IllegalArgumentException(
                    "Unknown energy converter profile " + id);
        }
        return profile;
    }

    private static Map<ResourceLocation, EnergyConverterProfile>
            loadBundled() {
        LinkedHashMap<ResourceLocation, EnergyConverterProfile> result =
                new LinkedHashMap<>();
        Set<Integer> sourceIds = new HashSet<>();
        for (EnergyConverterTierCatalog.Entry tier
                : EnergyConverterTierCatalog.entries()) {
            EnergyConverterKindCatalog.Kind kind =
                    EnergyConverterKindCatalog.require(tier.kindId());
            EnergyConverterProfile profile =
                    EnergyConverterProfiles.synthesize(kind, tier);
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
        if (result.size() != EnergyConverterTierCatalog.EXPECTED_SIZE) {
            throw new IllegalStateException(
                    "Energy converter profile count drifted: "
                            + result.size());
        }
        Set<String> actualIds = result.keySet().stream()
                .map(ResourceLocation::toString)
                .collect(java.util.stream.Collectors.toSet());
        if (!actualIds.containsAll(EXPECTED_IDS)) {
            throw new IllegalStateException(
                    "Energy converter profile set drifted: " + actualIds);
        }
        Set<String> complete = result.values().stream()
                .filter(profile ->
                        profile.status()
                                == EnergyConverterProfile.Status.COMPLETE)
                .map(profile -> profile.id().toString())
                .collect(java.util.stream.Collectors.toSet());
        if (!complete.containsAll(COMPLETE_PROFILES)) {
            throw new IllegalStateException(
                    "Completed converter profile set drifted");
        }
        validateSteamConverters(result);
        validateKineticConverters(result);
        validateSmallGasTurbine(result);
        validateGasGenerator(result);
        validateElectricConverters(result);
        validateLaserMagnetZpm(result);
        return Map.copyOf(result);
    }

    private static void validateSteamConverters(
            Map<ResourceLocation, EnergyConverterProfile> profiles) {
        EnergyConverterProfile boiler =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_boiler"));
        EnergyConverterProfile engine =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_steam_engine"));
        if (boiler.source().sourceId() != 1202
                || boiler.conservation().primaryInputUnits() != 80
                || boiler.conservation().secondaryInputUnits() != 1
                || boiler.conservation().outputUnits() != 160
                || boiler.inputPacket().maxAmountPerTick() != 24L
                || boiler.outputPacket().maxAmountPerTick() != 48L
                || boiler.outputCapacity() != 480_000
                || engine.source().sourceId() != 1302
                || !"24/STEAM_PER_EU".equals(
                        engine.source().outputExpression())) {
            throw new IllegalStateException(
                    "Steam-chain converter rows drifted");
        }
        for (EnergyConverterProfile candidate : profiles.values()) {
            if (!"boiler".equals(candidate.runtimeBinding())) {
                continue;
            }
            long huPerTick = candidate.inputPacket().maxAmountPerTick();
            long steamPerTick = candidate.outputPacket().maxAmountPerTick();
            if (steamPerTick != huPerTick * 2L
                    || candidate.inputCapacity() != 4_000
                    || candidate.outputCapacity()
                            != Math.multiplyExact(steamPerTick, 10_000L)) {
                throw new IllegalStateException(
                        "Boiler rate or tank capacity drifted: "
                                + candidate.id());
            }
        }
        int steamCount = 0;
        for (EnergyConverterProfile candidate : profiles.values()) {
            if (!"steam_engine".equals(candidate.runtimeBinding())) {
                continue;
            }
            steamCount++;
            long nominal = candidate.outputPacket().size();
            int efficiency = candidate.efficiencyBps();
            int kuPerBatch = (int) (100L * efficiency / 10_000L);
            EnergyConverterProfile.OutputSemantics semantics =
                    candidate.outputSemantics();
            if (nominal <= 0L
                    || efficiency <= 0
                    || candidate.inputCapacity() != nominal * 400L
                    || candidate.outputCapacity() <= 0
                    || candidate.inputWindow().minimum()
                            != gt6Units(nominal * 2L, efficiency * 2L)
                    || candidate.inputWindow().nominal()
                            != gt6Units(nominal * 2L, efficiency)
                    || candidate.inputWindow().maximum()
                            != gt6Units(nominal * 4L, efficiency)
                    || candidate.conservation().primaryInputUnits() != 200
                    || candidate.conservation().outputUnits() != kuPerBatch
                    || semantics == null
                    || semantics.conservation().kuOutput() != kuPerBatch
                    || semantics.sourceNominal().mOutputKu() != nominal
                    || semantics.fixedOutput().kuPerTick() != nominal
                    || semantics.gt6Runtime().minimumKuPerTick()
                            != nominal / 2L
                    || semantics.gt6Runtime().maximumKuPerTick()
                            != nominal * 2L
                    || candidate.exhaust().capacity() != nominal * 2L) {
                throw new IllegalStateException(
                        "Steam-engine profile drifted: " + candidate.id());
            }
        }
        if (steamCount != 28) {
            throw new IllegalStateException(
                    "Steam-engine profile count drifted: " + steamCount);
        }
    }

    private static long gt6Units(long amount, long efficiency) {
        return amount * 10_000L / efficiency;
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
                        fuelEngine.outputPacket().identity())
                || !fuelEngine.faces().fluidOutputs().equals(List.of("BACK"))
                || !fuelEngine.faces().fluidInputs().equals(List.of("SIDES"))
                || !fuelEngine.policy().sourceResolution().contains(
                        "MOTOR_LIQUID_BACK_PUSH_VENT")) {
            throw new IllegalStateException(
                    "Kinetic converter rows drifted");
        }
    }

    private static void validateSmallGasTurbine(
            Map<ResourceLocation, EnergyConverterProfile> profiles) {
        EnergyConverterProfile turbine =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_small_gas_turbine"));
        EnergyConverterProfile chromium =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:chromium_small_gas_turbine"));
        EnergyConverterProfile tungstensteel =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:tungstensteel_small_gas_turbine"));
        EnergyConverterProfile iridium =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:iridium_small_gas_turbine"));
        if (turbine == null
                || chromium == null
                || tungstensteel == null
                || iridium == null
                || turbine.source().sourceId() != 110000
                || !"MultiTileEntityGasMotor".equals(
                        turbine.source().machineKind())
                || !"FM.Gas".equals(turbine.fuelMap())
                || turbine.outputPacket().size() != 16L
                || !Integer.valueOf(3_500).equals(turbine.efficiencyBps())
                || chromium.outputPacket().size() != 256L
                || tungstensteel.outputPacket().size() != 372L
                || iridium.outputPacket().size() != 768L
                || iridium.source().sourceId() != 110005
                || chromium.source().sourceId() != 110006
                || !"RU".equals(turbine.outputPacket().identity())
                || !turbine.faces().fluidOutputs().equals(List.of("BACK"))
                || !turbine.policy().sourceResolution().contains(
                        "KTFRUADDON_GAS_MOTOR_FM_GAS")) {
            throw new IllegalStateException(
                    "Small gas turbine converter rows drifted");
        }
    }

    private static void validateGasGenerator(
            Map<ResourceLocation, EnergyConverterProfile> profiles) {
        EnergyConverterProfile gasGenerator =
                profiles.get(ResourceLocation.parse(
                        "cruciblecraft:bronze_burning_box_gas"));
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

    private static void validateElectricConverters(
            Map<ResourceLocation, EnergyConverterProfile> profiles) {
        EnergyConverterProfile heater = profiles.get(ResourceLocation.parse(
                "cruciblecraft:steel_galvanized_electric_heater"));
        EnergyConverterProfile engine = profiles.get(ResourceLocation.parse(
                "cruciblecraft:steel_galvanized_electric_engine"));
        if (heater == null
                || engine == null
                || heater.source().sourceId() != 10001
                || engine.source().sourceId() != 10011
                || !"MultiTileEntityHeaterElectric".equals(
                        heater.source().machineKind())
                || !"MultiTileEntityEngineElectric".equals(
                        engine.source().machineKind())
                || !"electric_hu_chain".equals(heater.stage())
                || !"electric_ku_chain".equals(engine.stage())
                || heater.inputWindow().minimum() != 16L
                || heater.inputWindow().nominal() != 32L
                || heater.inputWindow().maximum() != 64L
                || engine.inputWindow().minimum() != 16L
                || engine.inputWindow().nominal() != 32L
                || engine.inputWindow().maximum() != 64L
                || !"EU".equals(heater.inputPacket().identity())
                || !"HU".equals(heater.outputPacket().identity())
                || heater.outputPacket().maxAmountPerTick() != 16L
                || !"EU".equals(engine.inputPacket().identity())
                || !"KU".equals(engine.outputPacket().identity())
                || engine.outputPacket().size() != 16L
                || !heater.faces().energyInputs().equals(
                        List.of("ALL_BUT_FRONT"))
                || !engine.faces().energyOutputs().equals(
                        List.of("FRONT"))) {
            throw new IllegalStateException(
                    "Electric heater/engine converter rows drifted");
        }
    }

    private static void validateLaserMagnetZpm(
            Map<ResourceLocation, EnergyConverterProfile> profiles) {
        assertFamily(profiles, "laser_electric", 5, "LU", false);
        assertFamily(profiles, "laser_absorber", 5, "EU", false);
        assertFamily(profiles, "magnet_electric", 5, "MU", true);
        EnergyConverterProfile zpm = profiles.get(ResourceLocation.parse(
                "cruciblecraft:osmiridium_zpm_decharger"));
        if (zpm == null
                || !"zpm_decharger".equals(zpm.runtimeBinding())
                || zpm.source().sourceId() != 11171
                || !"MultiTileEntityZPMDechargerEU".equals(
                        zpm.source().machineKind())
                || !"QU".equals(zpm.inputPacket().identity())
                || !"EU".equals(zpm.outputPacket().identity())
                || zpm.inputPacket().size() != zpm.outputPacket().size()
                || !zpm.policy().sourceResolution().contains("ZPM_ITEM_SLOT")
                || !zpm.policy().sourceResolution().contains(
                        "ZPM_MODULE_14999")) {
            throw new IllegalStateException("ZPM decharger row drifted");
        }
        EnergyConverterProfile quantum = profiles.get(ResourceLocation.parse(
                "cruciblecraft:osmiridium_zpm_decharger_qu"));
        if (quantum == null
                || !"zpm_decharger_qu".equals(quantum.runtimeBinding())
                || quantum.source().sourceId() != 11170
                || !"MultiTileEntityZPMDechargerQU".equals(
                        quantum.source().machineKind())
                || !"QU".equals(quantum.inputPacket().identity())
                || !"QU".equals(quantum.outputPacket().identity())
                || quantum.inputPacket().size() != quantum.outputPacket().size()
                || !quantum.policy().sourceResolution().contains(
                        "ZPM_MODULE_14999")) {
            throw new IllegalStateException("Quantum ZPM decharger row drifted");
        }
    }

    private static void assertFamily(
            Map<ResourceLocation, EnergyConverterProfile> profiles,
            String runtime,
            int expected,
            String outputIdentity,
            boolean bipolar) {
        List<EnergyConverterProfile> rows = profiles.values().stream()
                .filter(profile -> runtime.equals(profile.runtimeBinding()))
                .toList();
        if (rows.size() != expected) {
            throw new IllegalStateException(
                    runtime + " count drifted: " + rows.size());
        }
        for (EnergyConverterProfile profile : rows) {
            if (profile.outputPacket().size() * 2L
                            != profile.inputPacket().size()
                    || !outputIdentity.equals(profile.outputPacket().identity())
                    || profile.outputPacket().maxAmountPerTick() != 1L
                    || !profile.policy().sourceResolution().contains(
                            "WASTE_ENERGY")
                    || bipolar != profile.policy().sourceResolution().contains(
                            "BIPOLAR")) {
                throw new IllegalStateException(
                        "Converter row drifted: " + profile.id());
            }
        }
    }

    private EnergyConverterCatalog() {}
}
