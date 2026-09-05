package com.masson.cruciblecraft.energy.battery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.energy.EnergyType;

class EnergyBatteryCatalogTest {
    @Test
    void catalogLocksThirtySevenRowsAndLuIsNotElectric() {
        Map<String, EnergyBatteryProfile> profiles =
                EnergyBatteryCatalog.profiles().stream()
                        .collect(Collectors.toMap(
                                profile -> profile.id().toString(),
                                profile -> profile));
        assertEquals(37, profiles.size());
        EnergyBatteryProfile lead = profiles.get(
                "cruciblecraft:lead_acid_battery_ulv");
        EnergyBatteryProfile red = profiles.get(
                "cruciblecraft:red_energium_crystal_iv");
        EnergyBatteryProfile cyan = profiles.get(
                "cruciblecraft:cyan_energium_crystal_iv");
        assertNotNull(lead);
        assertNotNull(red);
        assertNotNull(cyan);
        assertEquals(14000, lead.sourceId());
        assertEquals(16_000L, lead.capacity());
        assertEquals(EnergyType.ELECTRIC, lead.energyType());
        assertEquals(14505, red.sourceId());
        assertEquals(3_276_800_000L, red.capacity());
        assertEquals(EnergyType.LU, red.energyType());
        assertEquals(14515, cyan.sourceId());
        assertEquals(6_553_600_000L, cyan.capacity());
        assertEquals(EnergyType.LU, cyan.energyType());
        assertNotEquals(EnergyType.ELECTRIC, EnergyType.LU);
        assertEquals(
                12,
                profiles.values().stream()
                        .filter(profile -> profile.energyType() == EnergyType.LU)
                        .count());
        assertEquals(
                25,
                profiles.values().stream()
                        .filter(profile ->
                                profile.energyType() == EnergyType.ELECTRIC)
                        .count());
    }

    @Test
    void catalogAndGameTestTemplateAreBundled() {
        assertNotNull(EnergyBatteryCatalogTest.class.getResource(
                "/data/cruciblecraft/energy_battery_kinds.json"));
        assertNotNull(EnergyBatteryCatalogTest.class.getResource(
                "/data/cruciblecraft/energy_battery_tiers.json"));
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_batteries";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }
}
