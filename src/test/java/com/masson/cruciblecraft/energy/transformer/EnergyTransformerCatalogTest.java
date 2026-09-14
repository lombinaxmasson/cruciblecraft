package com.masson.cruciblecraft.energy.transformer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.energy.EnergyType;

class EnergyTransformerCatalogTest {
    @Test
    void catalogLocksNineEuVoltagePairs() {
        Map<String, EnergyTransformerProfile> profiles =
                EnergyTransformerCatalog.profiles().stream()
                        .collect(Collectors.toMap(
                                profile -> profile.id().toString(),
                                profile -> profile));
        assertEquals(14, profiles.size());
        EnergyTransformerProfile ulv = profiles.get(
                "cruciblecraft:electric_transformer_ulv_lv");
        EnergyTransformerProfile puv = profiles.get(
                "cruciblecraft:electric_transformer_uv_puv1");
        assertNotNull(ulv);
        assertNotNull(puv);
        assertEquals(10040, ulv.sourceId());
        assertEquals(32L, ulv.inputSize());
        assertEquals(8L, ulv.outputSize());
        assertEquals(64L, ulv.capacity());
        assertEquals("tin_alloy", ulv.material());
        assertEquals(EnergyType.ELECTRIC, ulv.energyType());
        assertEquals(16L, ulv.acceptMin(false));
        assertEquals(64L, ulv.acceptMax(false));
        assertEquals(1L, ulv.acceptMin(true));
        assertEquals(64L, ulv.acceptMax(true));
        assertEquals(8L, ulv.emitRec(false));
        assertEquals(32L, ulv.emitRec(true));
        assertEquals(4L, ulv.packetMultiplier(false));
        assertEquals(1L, ulv.packetMultiplier(true));
        assertEquals(10048, puv.sourceId());
        assertEquals(2_097_152L, puv.inputSize());
        assertEquals(524_288L, puv.outputSize());
        assertEquals("trinitanium", puv.material());
        assertEquals("puv1", puv.highVoltage());
        EnergyTransformerProfile lv = profiles.get(
                "cruciblecraft:electric_transformer_lv_mv");
        assertNotNull(lv);
        assertEquals(16L, lv.acceptMin(true));
        EnergyTransformerProfile omega = profiles.get(
                "cruciblecraft:electric_transformer_puv5_omega");
        assertNotNull(omega);
        assertEquals(81053, omega.sourceId());
        assertEquals(2_147_483_648L, omega.inputSize());
        assertEquals("omega", omega.highVoltage());
        assertTrue(profiles.values().stream().allMatch(
                profile -> profile.energyType() == EnergyType.ELECTRIC
                        && profile.multiplier() == 4L));
    }

    @Test
    void catalogAndGameTestTemplateAreBundled() {
        assertNotNull(EnergyTransformerCatalogTest.class.getResource(
                "/data/cruciblecraft/energy_transformer_kinds.json"));
        assertNotNull(EnergyTransformerCatalogTest.class.getResource(
                "/data/cruciblecraft/energy_transformer_tiers.json"));
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_transformers";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }
}
