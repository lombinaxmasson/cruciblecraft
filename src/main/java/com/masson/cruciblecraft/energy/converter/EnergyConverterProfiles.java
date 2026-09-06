package com.masson.cruciblecraft.energy.converter;

import java.util.List;

import net.minecraft.resources.ResourceLocation;

/** Builds live EnergyConverterProfile rows from kinds × tiers. */
final class EnergyConverterProfiles {
    private static final int STEAM_PER_EU = 2;
    private static final String BRONZE_BOILER = "cruciblecraft:bronze_boiler";
    private static final String BRONZE_STEAM_ENGINE =
            "cruciblecraft:bronze_steam_engine";
    private static final String BRONZE_DYNAMO = "cruciblecraft:bronze_dynamo";
    private static final String BRONZE_FUEL_ENGINE =
            "cruciblecraft:bronze_fuel_engine";
    private static final String BRONZE_GAS = "cruciblecraft:bronze_burning_box_gas";

    static EnergyConverterProfile synthesize(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        return switch (kind.runtime()) {
            case "boiler" -> boiler(kind, tier);
            case "steam_engine" -> steamEngine(kind, tier);
            case "fuel_engine" -> fuelEngine(kind, tier);
            case "dynamo" -> dynamo(kind, tier);
            case "electric_motor" -> motor(kind, tier);
            case "electric_heater" -> electricHeater(kind, tier);
            case "electric_engine" -> electricEngine(kind, tier);
            case "solid_burning_box" -> solidBox(kind, tier);
            case "fluid_burning_box" -> fluidBox(kind, tier);
            case "fluid_bed_burning_box" -> fluidBed(kind, tier);
            default -> throw new IllegalStateException(
                    "Unknown converter runtime " + kind.runtime());
        };
    }

    private static EnergyConverterProfile boiler(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        int huMax = Math.max(1, tier.nbtOutput() / STEAM_PER_EU);
        int steamMax = scale(1_280, 24, huMax);
        return new EnergyConverterProfile(
                tier.id(),
                "steam_ku_chain",
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, "NONE", "NONE"),
                kind.accepts(),
                kind.emits(),
                packet("ENERGY", "HU", 1L, huMax),
                packet("FLUID", "STEAM", 160L, steamMax),
                window(1L, (long) huMax, (long) huMax),
                10_000,
                "NONE",
                conservation(
                        "HU", 80, "minecraft:water", 1,
                        "STEAM", 160, "NONE", 0),
                exhaust("NONE", "NONE", 0),
                kind.faces(),
                policy(
                        "NO_WATER_NO_HEAT_OR_STEAM_FULL",
                        "KEEP_HU_AND_WATER_BUFFERED",
                        "SOURCE_" + tier.sourceId()
                                + "_WITH_CC_80_HU_PLUS_1_MB_WATER_TO_160_MB_STEAM"),
                null,
                scale(16_000, 24, huMax),
                scale(64_000, 24, huMax));
    }

    private static EnergyConverterProfile steamEngine(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        boolean bronze = BRONZE_STEAM_ENGINE.equals(tier.id().toString());
        long packet = bronze ? 12L : Math.max(1L, tier.nbtOutput());
        int numerator = bronze ? 24 : Math.toIntExact(packet * STEAM_PER_EU);
        EnergyConverterProfile.OutputSemantics semantics =
                new EnergyConverterProfile.OutputSemantics(
                        new EnergyConverterProfile.ConservationClassification(
                                "SOURCE_BACKED", 200, 50, 4),
                        new EnergyConverterProfile.SourceNominal(
                                "SOURCE_DERIVED_NOMINAL",
                                numerator,
                                STEAM_PER_EU,
                                Math.toIntExact(packet)),
                        new EnergyConverterProfile.FixedOutput(
                                "DESIGN_POLICY_FIXED_OUTPUT",
                                Math.toIntExact(packet)),
                        new EnergyConverterProfile.Gt6RuntimeOutput(
                                "DEFERRED_REPLACEMENT",
                                6,
                                24,
                                "STATE_DEPENDENT_MOUTPUT_HALF_TO_DOUBLE",
                                "Replace the fixed KU/t CC output only with a measured state accumulator, piston-phase emission, saturation/venting, persistence and transaction model matching the pinned GT6 behavior.",
                                "Recheck when a dedicated steam-engine dynamics batch is selected or before adding another steam-engine tier."),
                        List.of(
                                "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/"
                                        + "Loader_MultiTileEntities.java:"
                                        + tier.sourceLine(),
                                "gt6_code/gregtech6/src/main/java/gregapi/data/"
                                        + "CS.java:240",
                                "gt6_code/gregtech6/src/main/java/gregtech/tileentity/"
                                        + "energy/converters/"
                                        + "MultiTileEntityEngineSteam.java:"
                                        + "58,62-63,77-80,98-103,121-146"));
        Integer efficiency = tier.efficiencyBps() <= 0
                ? null
                : tier.efficiencyBps();
        return new EnergyConverterProfile(
                tier.id(),
                "steam_ku_chain",
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, "NONE", String.valueOf(tier.efficiencyBps())),
                kind.accepts(),
                kind.emits(),
                packet("FLUID", "STEAM", 200L, 200L),
                packet("ENERGY", "KU", packet, 1L),
                window(200L, 200L, 200L),
                efficiency,
                "NONE",
                conservation(
                        "STEAM", 200, "NONE", 0,
                        "KU", 50, "minecraft:water", 1),
                exhaust("minecraft:water", "BUFFER_THEN_DRAIN", 16_000),
                kind.faces(),
                policy(
                        "NO_STEAM_KU_FULL_OR_EXHAUST_FULL",
                        "KEEP_STEAM_BUFFERED",
                        bronze
                                ? "EXACT_STANDARD_BRONZE_SOURCE_1302; SOURCE_BACKED_200_MB_TO_50_KU_AT_4_MB_PER_KU; SOURCE_DERIVED_NOMINAL_24_DIV_STEAM_PER_EU_2_EQUALS_12_KU_MOUTPUT; DESIGN_POLICY_FIXED_OUTPUT_12_KU_PER_TICK; GT6_STATE_DEPENDENT_6_TO_24_KU_DEFERRED_REPLACEMENT"
                                : "SOURCE_DERIVED_NOMINAL_KU_PACKET_" + packet
                                        + "; DEFERRED_REPLACEMENT_6_TO_24"),
                semantics,
                16_000,
                1_024);
    }

    private static EnergyConverterProfile fuelEngine(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        long packet = Math.max(1L, tier.nbtOutput());
        int ru = scale(512, 16, (int) packet);
        int tanks = scale(8_000, 16, (int) packet);
        return new EnergyConverterProfile(
                tier.id(),
                "liquid_fuel_ru_chain",
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, "FM.Engine", "10000"),
                kind.accepts(),
                kind.emits(),
                packet("FLUID_FUEL", "FM.Engine", 1L, 1L),
                packet("ENERGY", "RU", packet, 1L),
                absentWindow(),
                10_000,
                "FM.Engine",
                conservation(
                        "FM.Engine", 1, "NONE", 0,
                        "RU", ru, "cruciblecraft:carbon_dioxide", 1),
                exhaust("RECIPE_DEFINED", "BUFFER_ALL_OUTPUTS", tanks),
                kind.faces(),
                policy(
                        "EXHAUST_AND_RU_OUTPUT_GATE_START",
                        "KEEP_RECIPE_ENERGY_IN_RU_BUFFER_AND_EMIT_ONE_PACKET_PER_TICK",
                        "EXACT_SOURCE_" + tier.sourceId()
                                + "_MOTOR_LIQUID_" + packet
                                + "_RU_10000_BPS; CURRENT_KINETIC_ROTATION_IDENTITY_REQUIRED; MISSING_PARTIAL_OR_WRONG_IDENTITY_QUARANTINED"),
                null,
                tanks,
                scale(65_536, 16, (int) packet));
    }

    private static EnergyConverterProfile dynamo(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        int nominalIn = Math.max(1, tier.nbtInput());
        int nominalOut = Math.max(1, tier.nbtOutput());
        long min = Math.max(1L, 16L * nominalIn / 32L);
        long nom = nominalIn;
        long max = Math.max(nom, 64L * nominalIn / 32L);
        long out = nominalOut;
        int loss = Math.max(0, nominalIn - nominalOut);
        int efficiency = Math.min(10_000, Math.max(1, nominalOut * 10_000 / nominalIn));
        boolean bronze = BRONZE_DYNAMO.equals(tier.id().toString());
        return new EnergyConverterProfile(
                tier.id(),
                "liquid_fuel_ru_chain",
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, "NONE", nominalOut + "/" + nominalIn),
                kind.accepts(),
                kind.emits(),
                packet("ENERGY", "RU", nom, 1L),
                packet("ENERGY", "EU", out, 1L),
                window(min, nom, max),
                efficiency,
                "NONE",
                conservation(
                        "RU", nominalIn, "NONE", 0,
                        "EU", nominalOut, "LOSS", loss),
                exhaust("LOSS", "DISSIPATE", 0),
                kind.faces(),
                policy(
                        "NBT_WASTE_ENERGY_DISCARDS_RU_WHEN_EU_BLOCKED",
                        "INPUT_ABOVE_MAX_RU_OVERLOADS_AND_DISSIPATES",
                        bronze
                                ? "EXACT_SOURCE_10111_ELECTRIC_T1; LEGACY_LOCAL_24_RU_TO_24_EU_REPLACED_BY_16_32_64_RU_WINDOW_TO_11_22_44_EU; NOMINAL_32_RU=22_EU+10_LOSS"
                                : "SOURCE_" + tier.sourceId()
                                        + "_ELECTRIC_WINDOW_"
                                        + min + "_" + nom + "_" + max),
                null,
                (int) max,
                0);
    }

    private static EnergyConverterProfile motor(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        int eu = Math.max(1, tier.nbtInput() > 0 ? tier.nbtInput() : 32);
        int ru = Math.max(1, tier.nbtOutput());
        return new EnergyConverterProfile(
                tier.id(),
                "liquid_fuel_ru_chain",
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, "NONE", "NONE"),
                kind.accepts(),
                kind.emits(),
                packet("ENERGY", "EU", eu, 1L),
                packet("ENERGY", "RU", ru, 1L),
                window((long) eu / 2, (long) eu, (long) eu * 2),
                null,
                "NONE",
                conservation(
                        "EU", eu, "NONE", 0,
                        "RU", ru, "NONE", 0),
                exhaust("NONE", "NONE", 0),
                kind.faces(),
                policy(
                        "EU_INPUT_BLOCKED_OR_RU_FULL",
                        "KEEP_EU_BUFFERED",
                        "SOURCE_" + tier.sourceId()
                                + "_MOTOR_ELECTRIC_" + eu + "_EU_TO_" + ru + "_RU"),
                null,
                1_024,
                1_024);
    }

    private static EnergyConverterProfile electricHeater(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        int input = Math.max(1, tier.nbtInput());
        int output = Math.max(1, tier.nbtOutput());
        long minimum = Math.max(1L, input / 2L);
        long maximum = Math.multiplyExact(input, 2L);
        return new EnergyConverterProfile(
                tier.id(),
                "electric_hu_chain",
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, "NONE", "NONE"),
                kind.accepts(),
                kind.emits(),
                packet("ENERGY", "EU", input, 1L),
                packet("ENERGY", "HU", 1L, output),
                window(minimum, (long) input, maximum),
                null,
                "NONE",
                conservation(
                        "EU", input, "NONE", 0,
                        "HU", output, "NONE", 0),
                exhaust("NONE", "NONE", 0),
                kind.faces(),
                policy(
                        "NBT_WASTE_ENERGY_CONSUMES_EU_WHEN_HU_BLOCKED",
                        "INPUT_ABOVE_MAX_EU_OVERLOADS",
                        "SOURCE_" + tier.sourceId()
                                + "_ELECTRIC_HEATER_EU_TO_HU_"
                                + input + "_TO_" + output
                                + "; SOURCE_BACKED_DYNAMIC_BATCH; "
                                + "NO_STAND_IN_COMPONENTS"),
                null,
                Math.toIntExact(maximum),
                0);
    }

    private static EnergyConverterProfile electricEngine(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        int input = Math.max(1, tier.nbtInput());
        int output = Math.max(1, tier.nbtOutput());
        long minimum = Math.max(1L, input / 2L);
        long maximum = Math.multiplyExact(input, 2L);
        return new EnergyConverterProfile(
                tier.id(),
                "electric_ku_chain",
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, "NONE", "NONE"),
                kind.accepts(),
                kind.emits(),
                packet("ENERGY", "EU", input, 1L),
                packet("ENERGY", "KU", output, 1L),
                window(minimum, (long) input, maximum),
                null,
                "NONE",
                conservation(
                        "EU", input, "NONE", 0,
                        "KU", output, "NONE", 0),
                exhaust("NONE", "NONE", 0),
                kind.faces(),
                policy(
                        "NBT_WASTE_ENERGY_CONSUMES_EU_WHEN_KU_BLOCKED",
                        "INPUT_ABOVE_MAX_EU_OVERLOADS",
                        "SOURCE_" + tier.sourceId()
                                + "_ELECTRIC_ENGINE_EU_TO_KU_"
                                + input + "_TO_" + output
                                + "; STATE_DEPENDENT_OUTPUT_"
                                + (output / 2) + "_TO_" + (output * 2)
                                + "; PISTON_PHASE_SIGNED_OUTPUT; "
                                + "SCREWDRIVER_STATE_CONTROL"),
                null,
                Math.toIntExact(maximum),
                0);
    }

    private static EnergyConverterProfile solidBox(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        int hu = Math.max(1, tier.nbtOutput());
        Integer efficiency = clampedEfficiency(tier.efficiencyBps(), 5_000);
        return burning(
                kind,
                tier,
                "solid_hu_chain",
                "ITEM_FURNACE_FUEL",
                "FM.Furnace",
                hu,
                efficiency,
                64,
                scale(288_000, 24, hu),
                0);
    }

    private static EnergyConverterProfile fluidBox(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        int hu = Math.max(1, tier.nbtOutput());
        boolean bronzeGas = BRONZE_GAS.equals(tier.id().toString());
        Integer efficiency = clampedEfficiency(tier.efficiencyBps(), 7_500);
        int fuel = bronzeGas ? 1_536 : scale(1_536, 24, hu);
        int output = bronzeGas ? 1_152 : scale(1_152, 24, hu);
        int exhaust = bronzeGas ? 9 : Math.max(1, scale(9, 24, hu));
        return new EnergyConverterProfile(
                tier.id(),
                "gas_hu_chain",
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, "FM.Burn", String.valueOf(efficiency)),
                kind.accepts(),
                kind.emits(),
                packet("FLUID_FUEL", "FM.Burn", 1L, 1L),
                packet("ENERGY", "HU", 1L, hu),
                absentWindow(),
                efficiency,
                "FM.Burn",
                conservation(
                        "FM.Burn", fuel, "NONE", 0,
                        "HU", output, "RECIPE_DEFINED", exhaust),
                exhaust("RECIPE_DEFINED", "BUFFER_ALL_OUTPUTS", scale(16_000, 24, hu)),
                kind.faces(),
                policy(
                        "EXHAUST_AND_HU_OUTPUT_GATE_START",
                        "KEEP_RECIPE_HU_BUFFERED_AND_EMIT_UP_TO_NBT_HU_PER_TICK",
                        bronzeGas
                                ? "EXACT_SOURCE_1602_BRONZE_GENERATOR_GAS_24_HU_7500_BPS_FM_BURN; METHANE_ROW_20_5_MB_AT_-64_X_24_EQUALS_1536_SOURCE_UNITS_AND_1152_HU; CURRENT_HEAT_IDENTITY_REQUIRED; MISSING_PARTIAL_OR_WRONG_IDENTITY_QUARANTINED"
                                : "SOURCE_" + tier.sourceId()
                                        + "_FM_BURN_" + hu
                                        + "_HU; CURRENT_HEAT_IDENTITY_REQUIRED; MISSING_PARTIAL_OR_WRONG_IDENTITY_QUARANTINED"),
                null,
                scale(16_000, 24, hu),
                scale(288_000, 24, hu));
    }

    private static EnergyConverterProfile fluidBed(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier) {
        int hu = Math.max(1, tier.nbtOutput());
        Integer efficiency = clampedEfficiency(tier.efficiencyBps(), 7_500);
        return burning(
                kind,
                tier,
                "fluidbed_hu_chain",
                "FM.FluidBed",
                "FM.FluidBed",
                hu,
                efficiency,
                scale(16_000, 24, hu),
                scale(288_000, 24, hu),
                scale(16_000, 24, hu));
    }

    private static EnergyConverterProfile burning(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier,
            String stage,
            String fuelIdentity,
            String fuelMap,
            int hu,
            Integer efficiency,
            int inputCapacity,
            int energyCapacity,
            int exhaustCapacity) {
        return new EnergyConverterProfile(
                tier.id(),
                stage,
                EnergyConverterProfile.Status.COMPLETE,
                kind.runtime(),
                source(kind, tier, fuelMap, String.valueOf(
                        efficiency == null ? 0 : efficiency)),
                kind.accepts(),
                kind.emits(),
                packet("FUEL", fuelIdentity, 1L, 1L),
                packet("ENERGY", "HU", 1L, hu),
                absentWindow(),
                efficiency,
                fuelMap,
                conservation(
                        fuelIdentity, 1, "NONE", 0,
                        "HU", hu, "ASH", 1),
                exhaust("ASH", "BUFFER_THEN_DRAIN", exhaustCapacity),
                kind.faces(),
                policy(
                        "NO_FUEL_OR_ASH_FULL_OR_NO_OXYGEN",
                        "KEEP_FUEL_BUFFERED",
                        "SOURCE_" + tier.sourceId()
                                + "_" + fuelMap + "_" + hu
                                + "_HU; CURRENT_HEAT_IDENTITY_REQUIRED"),
                null,
                Math.max(1, inputCapacity),
                Math.max(hu, energyCapacity));
    }

    private static EnergyConverterProfile.Source source(
            EnergyConverterKindCatalog.Kind kind,
            EnergyConverterTierCatalog.Entry tier,
            String fuelMap,
            String efficiencyExpression) {
        return new EnergyConverterProfile.Source(
                kind.gt6Class(),
                tier.sourceId(),
                tier.sourceLine(),
                tier.materialExpression(),
                "loader:" + kind.id().getPath() + "|" + tier.sourceId()
                        + "|" + kind.gt6Class(),
                tier.outputExpression(),
                efficiencyExpression,
                fuelMap);
    }

    private static EnergyConverterProfile.Packet packet(
            String medium, String identity, long size, long maxAmountPerTick) {
        return new EnergyConverterProfile.Packet(
                medium, identity, size, maxAmountPerTick);
    }

    private static EnergyConverterProfile.Window window(
            Long minimum, Long nominal, Long maximum) {
        return new EnergyConverterProfile.Window(minimum, nominal, maximum);
    }

    private static EnergyConverterProfile.Window absentWindow() {
        return new EnergyConverterProfile.Window(null, null, null);
    }

    private static EnergyConverterProfile.Conservation conservation(
            String primaryInput,
            int primaryInputUnits,
            String secondaryInput,
            int secondaryInputUnits,
            String output,
            int outputUnits,
            String exhaust,
            int exhaustUnits) {
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

    private static EnergyConverterProfile.Exhaust exhaust(
            String identity, String mode, int capacity) {
        return new EnergyConverterProfile.Exhaust(identity, mode, capacity);
    }

    private static EnergyConverterProfile.Policy policy(
            String blockage, String overflow, String sourceResolution) {
        return new EnergyConverterProfile.Policy(
                true, true, blockage, overflow, sourceResolution);
    }

    private static Integer clampedEfficiency(int bps, int fallback) {
        if (bps <= 0) {
            return fallback;
        }
        return Math.min(10_000, bps);
    }

    private static int scale(int bronze, int bronzeRate, int actualRate) {
        if (bronzeRate <= 0) {
            return bronze;
        }
        long scaled = (long) bronze * actualRate / bronzeRate;
        return (int) Math.max(1L, Math.min(Integer.MAX_VALUE, scaled));
    }

    private EnergyConverterProfiles() {}
}
