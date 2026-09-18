package com.masson.cruciblecraft.energy.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.machine.generation.FuelGeneratorSpec;

import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

class EnergyConverterCatalogTest {
    @Test
    void selectedSourceRowsAreFixedAndStagesRemainExplicit() {
        Map<String, EnergyConverterProfile> profiles =
                EnergyConverterCatalog.profiles().stream()
                        .collect(Collectors.toMap(
                                profile -> profile.id().toString(),
                                profile -> profile));
        assertEquals(179, profiles.size());
        assertEquals(1202, profiles.get("cruciblecraft:bronze_boiler").source().sourceId());
        assertEquals(1302, profiles.get("cruciblecraft:bronze_steam_engine").source().sourceId());
        assertEquals(10111, profiles.get("cruciblecraft:bronze_dynamo").source().sourceId());
        assertEquals(9147, profiles.get("cruciblecraft:bronze_fuel_engine").source().sourceId());
        assertEquals(1602, profiles.get("cruciblecraft:bronze_burning_box_gas").source().sourceId());
        assertEquals(
                179,
                profiles.values().stream()
                        .filter(profile ->
                                profile.status()
                                        == EnergyConverterProfile.Status.COMPLETE)
                        .count());
        assertTrue(profiles.values().stream()
                .filter(profile -> profile.stage().equals("steam_ku_chain"))
                .allMatch(profile ->
                        profile.status()
                                == EnergyConverterProfile.Status.COMPLETE));
        assertTrue(profiles.values().stream()
                .filter(profile -> profile.stage().equals("liquid_fuel_ru_chain"))
                .allMatch(profile ->
                        profile.status()
                                == EnergyConverterProfile.Status.COMPLETE));
        assertTrue(profiles.values().stream()
                .filter(profile -> profile.stage().equals("gas_hu_chain"))
                .allMatch(profile ->
                        profile.status()
                                == EnergyConverterProfile.Status.COMPLETE));
        assertTrue(profiles.values().stream()
                .filter(profile -> profile.stage().equals("electric_hu_chain"))
                .allMatch(profile ->
                        profile.status()
                                == EnergyConverterProfile.Status.COMPLETE));
        assertTrue(profiles.values().stream()
                .filter(profile -> profile.stage().equals("electric_ku_chain"))
                .allMatch(profile ->
                        profile.status()
                                == EnergyConverterProfile.Status.COMPLETE));
    }

    @Test
    void electricHeaterAndEngineLockSourceRowsAndDirections() {
        EnergyConverterProfile heater = EnergyConverterCatalog.require(
                "cruciblecraft:steel_galvanized_electric_heater");
        EnergyConverterProfile engine = EnergyConverterCatalog.require(
                "cruciblecraft:steel_galvanized_electric_engine");
        assertEquals(10001, heater.source().sourceId());
        assertEquals(10011, engine.source().sourceId());
        assertEquals("MultiTileEntityHeaterElectric",
                heater.source().machineKind());
        assertEquals("MultiTileEntityEngineElectric",
                engine.source().machineKind());
        assertEquals("HU", heater.outputPacket().identity());
        assertEquals("KU", engine.outputPacket().identity());
        assertEquals(16L, heater.inputWindow().minimum());
        assertEquals(32L, heater.inputWindow().nominal());
        assertEquals(64L, heater.inputWindow().maximum());
        assertEquals(java.util.List.of("ALL_BUT_FRONT"),
                heater.faces().energyInputs());
        assertEquals(java.util.List.of("BACK"),
                engine.faces().energyInputs());
        assertTrue(engine.policy().sourceResolution().contains(
                "PISTON_PHASE_SIGNED_OUTPUT"));
    }

    @Test
    void bronzeSteamEngineChooses1302AndResolvesThe24UnitExpression() {
        EnergyConverterProfile engine = EnergyConverterCatalog.require(
                "cruciblecraft:bronze_steam_engine");
        EnergyConverterProfile.OutputSemantics semantics =
                engine.outputSemantics();
        assertEquals(1302, engine.source().sourceId());
        assertEquals(586, engine.source().sourceLine());
        assertEquals("MT.Bronze", engine.source().materialExpression());
        assertEquals(
                "24/STEAM_PER_EU", engine.source().outputExpression());
        assertEquals(12L, engine.outputPacket().size());
        assertEquals(5_000, engine.efficiencyBps());
        assertNotNull(semantics);
        assertEquals(
                "SOURCE_BACKED",
                semantics.conservation().classification());
        assertEquals(200, semantics.conservation().steamInputMb());
        assertEquals(50, semantics.conservation().kuOutput());
        assertEquals(4, semantics.conservation().steamMbPerKu());
        assertEquals(
                semantics.conservation().steamInputMb(),
                semantics.conservation().kuOutput()
                        * semantics.conservation().steamMbPerKu());
        assertEquals(
                "SOURCE_DERIVED_NOMINAL",
                semantics.sourceNominal().classification());
        assertEquals(24, semantics.sourceNominal().registeredNumerator());
        assertEquals(2, semantics.sourceNominal().steamPerEu());
        assertEquals(
                semantics.sourceNominal().mOutputKu(),
                semantics.sourceNominal().registeredNumerator()
                        / semantics.sourceNominal().steamPerEu());
        assertEquals(
                "DESIGN_POLICY_FIXED_OUTPUT",
                semantics.fixedOutput().classification());
        assertEquals(12, semantics.fixedOutput().kuPerTick());
        assertEquals(
                "SOURCE_BACKED",
                semantics.gt6Runtime().classification());
        assertEquals(6, semantics.gt6Runtime().minimumKuPerTick());
        assertEquals(24, semantics.gt6Runtime().maximumKuPerTick());
        assertTrue(!semantics.gt6Runtime().replacementCondition().isBlank());
        assertTrue(!semantics.gt6Runtime().recheckPoint().isBlank());
        assertEquals(
                java.util.List.of(
                        "gt6_code/gregtech6/src/main/java/gregtech/loaders/b/"
                                + "Loader_MultiTileEntities.java:586",
                        "gt6_code/gregtech6/src/main/java/gregapi/data/"
                                + "CS.java:240",
                        "gt6_code/gregtech6/src/main/java/gregtech/tileentity/"
                                + "energy/converters/"
                                + "MultiTileEntityEngineSteam.java:"
                                + "58,62-63,77-80,98-103,119-165,175,224-226,239"),
                semantics.sourceEvidencePaths());
        assertTrue(engine.policy().sourceResolution().contains(
                "DESIGN_POLICY_FIXED_OUTPUT_12_KU_PER_TICK"));
        assertTrue(engine.policy().sourceResolution().contains(
                "LIVE_STATE_DEPENDENT_6_TO_24"));
        assertTrue(engine.policy().sourceResolution().contains(
                "DISTW_SIDE_BUFFER_THEN_DRAIN"));
        assertTrue(engine.policy().sourceResolution().contains(
                "SOFT_HAMMER_AND_STEAM_VENT_STOP"));
    }

    @Test
    void completeSteamChainProfilesLockConservationAndFaces() {
        EnergyConverterProfile boiler = EnergyConverterCatalog.require(
                "cruciblecraft:bronze_boiler");
        EnergyConverterProfile engine = EnergyConverterCatalog.require(
                "cruciblecraft:bronze_steam_engine");
        assertEquals(80, boiler.conservation().primaryInputUnits());
        assertEquals(1, boiler.conservation().secondaryInputUnits());
        assertEquals(160, boiler.conservation().outputUnits());
        assertEquals(200, engine.conservation().primaryInputUnits());
        assertEquals(50, engine.conservation().outputUnits());
        assertEquals(1, engine.conservation().exhaustUnits());
        assertEquals(
                "cruciblecraft:water_distilled",
                engine.conservation().exhaust());
        assertEquals(
                "cruciblecraft:water_distilled",
                engine.exhaust().identity());
        assertEquals("BUFFER_THEN_DRAIN", engine.exhaust().mode());
        assertEquals(
                java.util.List.of("BACK"),
                engine.faces().fluidInputs());
        assertEquals(
                java.util.List.of("SIDES"),
                engine.faces().fluidOutputs());
        assertEquals(
                java.util.List.of("DOWN"),
                boiler.faces().energyInputs());
        assertEquals(
                java.util.List.of("FRONT"),
                engine.faces().energyOutputs());
    }

    @Test
    void dynamoCorrectsLegacy24To24WithAuditableSourceLoss() {
        EnergyConverterProfile dynamo = EnergyConverterCatalog.require(
                "cruciblecraft:bronze_dynamo");

        assertEquals(10111, dynamo.source().sourceId());
        assertEquals("MT.DATA.Electric_T[1]",
                dynamo.source().materialExpression());
        assertEquals(16L, dynamo.inputWindow().minimum());
        assertEquals(32L, dynamo.inputWindow().nominal());
        assertEquals(64L, dynamo.inputWindow().maximum());
        assertEquals(22L, dynamo.outputPacket().size());
        assertEquals(6_875, dynamo.efficiencyBps());
        assertEquals(32, dynamo.conservation().primaryInputUnits());
        assertEquals(22, dynamo.conservation().outputUnits());
        assertEquals(10, dynamo.conservation().exhaustUnits());
        assertEquals(
                32,
                dynamo.conservation().outputUnits()
                        + dynamo.conservation().exhaustUnits());
        assertTrue(dynamo.policy().blockage().contains(
                "NBT_WASTE_ENERGY"));
        assertTrue(dynamo.policy().sourceResolution().contains(
                "LEGACY_LOCAL_24_RU_TO_24_EU_REPLACED"));
    }

    @Test
    void fuelEngineLocksMotorLiquidSourceAndRuPacket() {
        EnergyConverterProfile engine = EnergyConverterCatalog.require(
                "cruciblecraft:bronze_fuel_engine");

        assertEquals(9147, engine.source().sourceId());
        assertEquals("MultiTileEntityMotorLiquid",
                engine.source().machineKind());
        assertEquals("FM.Engine", engine.fuelMap());
        assertEquals(10_000, engine.efficiencyBps());
        assertEquals("RU", engine.outputPacket().identity());
        assertEquals(16L, engine.outputPacket().size());
        assertEquals(512, engine.conservation().outputUnits());
        assertEquals(1, engine.conservation().exhaustUnits());
        assertTrue(engine.policy().sourceResolution().contains(
                "CURRENT_KINETIC_ROTATION_IDENTITY_REQUIRED"));
        assertTrue(engine.policy().sourceResolution().contains(
                "MISSING_PARTIAL_OR_WRONG_IDENTITY_QUARANTINED"));
        assertTrue(engine.policy().sourceResolution().contains(
                "MOTOR_LIQUID_BACK_PUSH_BUFFER"));
        assertEquals(
                java.util.List.of("SIDES"),
                engine.faces().fluidInputs());
        assertEquals(
                java.util.List.of("BACK"),
                engine.faces().fluidOutputs());
        FuelGeneratorSpec live = EnergyConverterFuelSpecs.fromProfile(engine);
        assertTrue(live.pushesExhaust());
        assertEquals(
                java.util.List.of(Direction.WEST),
                live.resolvedExhaustSides(Direction.EAST));
    }

    @Test
    void gasGeneratorLocksBurnFuelHuRateAndEfficiency() {
        EnergyConverterProfile generator = EnergyConverterCatalog.require(
                "cruciblecraft:bronze_burning_box_gas");

        assertEquals(1602, generator.source().sourceId());
        assertEquals("MultiTileEntityGeneratorGas",
                generator.source().machineKind());
        assertEquals("FM.Burn", generator.fuelMap());
        assertEquals("HU", generator.outputPacket().identity());
        assertEquals(1L, generator.outputPacket().size());
        assertEquals(24L,
                generator.outputPacket().maxAmountPerTick());
        assertEquals(7_500, generator.efficiencyBps());
        assertEquals(1_536,
                generator.conservation().primaryInputUnits());
        assertEquals(1_152, generator.conservation().outputUnits());
        assertEquals(9, generator.conservation().exhaustUnits());
        assertEquals(java.util.List.of("UP"),
                generator.faces().energyOutputs());
        assertTrue(generator.policy().sourceResolution().contains(
                "CURRENT_HEAT_IDENTITY_REQUIRED"));
        assertTrue(generator.policy().sourceResolution().contains(
                "MISSING_PARTIAL_OR_WRONG_IDENTITY_QUARANTINED"));
        assertEquals(
                FuelGeneratorSpec.InputPhase.GAS,
                EnergyConverterFuelSpecs.fromProfile(generator).inputPhase());
        assertEquals(
                FuelGeneratorSpec.InputPhase.LIQUID,
                EnergyConverterFuelSpecs.fromProfile(
                                EnergyConverterCatalog.require(
                                        "cruciblecraft:bronze_burning_box_liquid"))
                        .inputPhase());
        assertEquals(
                FuelGeneratorSpec.InputPhase.ANY,
                EnergyConverterFuelSpecs.fromProfile(
                                EnergyConverterCatalog.require(
                                        "cruciblecraft:bronze_fuel_engine"))
                        .inputPhase());
    }

    @Test
    void catalogAndSchemaAreBundledRuntimeResources() {
        assertNotNull(EnergyConverterCatalogTest.class.getResource(
                "/data/cruciblecraft/energy_converter_kinds.json"));
        assertNotNull(EnergyConverterCatalogTest.class.getResource(
                "/data/cruciblecraft/energy_converter_tiers.json"));
        assertNotNull(EnergyConverterCatalogTest.class.getResource(
                "/data/cruciblecraft/energy_converters.json"));
        Path schema = Path.of(
                "src/main/resources/data/cruciblecraft/schema/"
                        + "energy_converters.schema.json");
        assertTrue(Files.isRegularFile(schema));
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_converter_catalog";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }
}
