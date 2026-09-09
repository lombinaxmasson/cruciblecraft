package com.masson.cruciblecraft.energy.heatexchanger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.energy.EnergyType;

class HeatExchangerCatalogTest {
    @Test
    void catalogLocksEightHotFluidIdentities() {
        Map<String, HeatExchangerProfile> profiles =
                HeatExchangerCatalog.profiles().stream()
                        .collect(Collectors.toMap(
                                profile -> profile.id().toString(),
                                profile -> profile));
        assertEquals(8, profiles.size());
        HeatExchangerProfile invar = profiles.get(
                "cruciblecraft:heat_exchanger_invar");
        HeatExchangerProfile tungstensteel = profiles.get(
                "cruciblecraft:heat_exchanger_tungstensteel");
        HeatExchangerProfile denseTantalum = profiles.get(
                "cruciblecraft:dense_heat_exchanger_tantalum_hafnium_carbide");
        assertNotNull(invar);
        assertNotNull(tungstensteel);
        assertNotNull(denseTantalum);
        assertEquals(9103, invar.sourceId());
        assertEquals(16, invar.huRate());
        assertEquals(10_000, invar.efficiencyBps());
        assertEquals(320L, invar.energyCapacity());
        assertEquals(EnergyType.HEAT, invar.energyType());
        assertEquals(9108, tungstensteel.sourceId());
        assertEquals(128, tungstensteel.huRate());
        assertEquals(9_000, tungstensteel.efficiencyBps());
        assertEquals(9159, denseTantalum.sourceId());
        assertEquals(1024, denseTantalum.huRate());
        assertEquals(
                Set.of(9103, 9107, 9108, 9109, 9153, 9157, 9158, 9159),
                profiles.values().stream()
                        .map(HeatExchangerProfile::sourceId)
                        .collect(Collectors.toSet()));
        assertTrue(profiles.values().stream().noneMatch(
                profile -> profile.sourceId() == 17197));
        assertTrue(profiles.values().stream().allMatch(
                profile -> profile.energyType() == EnergyType.HEAT
                        && profile.packetSize() == 1L));
        assertEquals(
                2,
                profiles.values().stream()
                        .filter(profile -> profile.efficiencyBps() == 9_000)
                        .count());
    }

    @Test
    void catalogAndGameTestTemplateAreBundled() {
        assertNotNull(HeatExchangerCatalogTest.class.getResource(
                "/data/cruciblecraft/heat_exchangers.json"));
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_heat_exchangers";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }
}
