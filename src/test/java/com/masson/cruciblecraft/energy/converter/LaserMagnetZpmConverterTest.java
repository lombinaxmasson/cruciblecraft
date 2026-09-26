package com.masson.cruciblecraft.energy.converter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.resources.ResourceLocation;

class LaserMagnetZpmConverterTest {
    @Test
    void sixteenHostsKeepGt6RatiosAndPolicies() {
        List<EnergyConverterProfile> lasers = family("laser_electric");
        List<EnergyConverterProfile> absorbers = family("laser_absorber");
        List<EnergyConverterProfile> magnets = family("magnet_electric");
        assertEquals(5, lasers.size());
        assertEquals(5, absorbers.size());
        assertEquals(5, magnets.size());
        assertWasteRatio(lasers, "LU", false);
        assertWasteRatio(absorbers, "EU", false);
        assertWasteRatio(magnets, "MU", true);
        EnergyConverterProfile zpm = EnergyConverterCatalog.require(
                ResourceLocation.parse(
                        "cruciblecraft:osmiridium_zpm_decharger"));
        assertEquals(11171, zpm.source().sourceId());
        assertEquals("zpm_qu_chain", zpm.stage());
        assertEquals("QU", zpm.inputPacket().identity());
        assertEquals("EU", zpm.outputPacket().identity());
        assertEquals(zpm.inputPacket().size(), zpm.outputPacket().size());
        assertTrue(zpm.policy().sourceResolution().contains("ZPM_ITEM_SLOT"));
        assertTrue(zpm.policy().sourceResolution().contains(
                "ZPM_MODULE_14999"));
        EnergyConverterProfile quantum = EnergyConverterCatalog.require(
                ResourceLocation.parse(
                        "cruciblecraft:osmiridium_zpm_decharger_qu"));
        assertEquals(11170, quantum.source().sourceId());
        assertEquals("QU", quantum.outputPacket().identity());
        assertEquals(
                quantum.inputPacket().size(), quantum.outputPacket().size());
        assertTrue(lasers.getFirst().faces().energyInputs().contains(
                "ALL_BUT_FRONT"));
        assertTrue(absorbers.getFirst().faces().energyInputs().contains("BACK"));
        assertTrue(magnets.getFirst().faces().energyOutputs().contains(
                "FRONT_AND_BACK"));
    }

    private static List<EnergyConverterProfile> family(String runtime) {
        return EnergyConverterCatalog.profiles().stream()
                .filter(profile -> runtime.equals(profile.runtimeBinding()))
                .toList();
    }

    private static void assertWasteRatio(
            List<EnergyConverterProfile> profiles,
            String outputIdentity,
            boolean bipolar) {
        for (EnergyConverterProfile profile : profiles) {
            assertEquals(
                    profile.inputPacket().size(),
                    profile.outputPacket().size() * 2L);
            assertEquals(outputIdentity, profile.outputPacket().identity());
            assertEquals(1L, profile.outputPacket().maxAmountPerTick());
            assertTrue(profile.policy().sourceResolution().contains(
                    "WASTE_ENERGY"));
            assertEquals(
                    bipolar,
                    profile.policy().sourceResolution().contains("BIPOLAR"));
        }
    }
}
