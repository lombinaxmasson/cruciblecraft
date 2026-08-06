package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.registry.ModMachineIdentityMigrations;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class MachineIdentityPolicyTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void kindTierMaterialAndEnergyMismatchesAllQuarantine() {
        MachineIdentityPolicy.Identity current =
                new MachineIdentityPolicy.Identity(
                        "test:centrifuge",
                        "test:tier",
                        "test:material",
                        "KINETIC_ROTATION");
        List<MachineIdentityPolicy.Identity> mismatches = List.of(
                new MachineIdentityPolicy.Identity(
                        "test:sifter",
                        current.tierProfile(),
                        current.materialId(),
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        "test:other_tier",
                        current.materialId(),
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        current.tierProfile(),
                        "test:other_material",
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        current.tierProfile(),
                        current.materialId(),
                        "KINETIC_PUSH"));

        for (MachineIdentityPolicy.Identity mismatch : mismatches) {
            MachineIdentityPolicy.Decision decision =
                    MachineIdentityPolicy.resolve(
                            "test:variant",
                            mismatch,
                            current,
                            List.of());
            assertEquals(
                    MachineIdentityPolicy.Resolution.QUARANTINED,
                    decision.resolution());
            assertEquals(mismatch, decision.persistedIdentity());
            assertTrue(decision.quarantineReason().isPresent());
        }
    }

    @Test
    void onlyExactLegacyLargeCentrifugeTupleMigrates() {
        MachineIdentityPolicy.Identity legacy =
                ModMachineIdentityMigrations
                        .LEGACY_LARGE_CENTRIFUGE_IDENTITY;
        MachineIdentityPolicy.Identity current =
                ModMachineIdentityMigrations.LARGE_CENTRIFUGE_IDENTITY;
        MachineIdentityPolicy.Decision exact =
                ModMachineIdentityMigrations.resolve(
                        ModMachineIdentityMigrations
                                .LARGE_CENTRIFUGE_VARIANT_ID,
                        legacy,
                        current);
        assertEquals(
                MachineIdentityPolicy.Resolution.MIGRATED,
                exact.resolution());
        assertEquals(current, exact.persistedIdentity());
        assertTrue(exact.quarantineReason().isEmpty());

        List<MachineIdentityPolicy.Identity> nearMisses = List.of(
                new MachineIdentityPolicy.Identity(
                        "cruciblecraft:large_centrifuge_near",
                        legacy.tierProfile(),
                        legacy.materialId(),
                        legacy.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        legacy.machineKind(),
                        legacy.tierProfile() + "_near",
                        legacy.materialId(),
                        legacy.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        legacy.machineKind(),
                        legacy.tierProfile(),
                        legacy.materialId() + "_near",
                        legacy.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        legacy.machineKind(),
                        legacy.tierProfile(),
                        legacy.materialId(),
                        "KINETIC_PUSH"));
        for (MachineIdentityPolicy.Identity nearMiss : nearMisses) {
            MachineIdentityPolicy.Decision decision =
                    ModMachineIdentityMigrations.resolve(
                            ModMachineIdentityMigrations
                                    .LARGE_CENTRIFUGE_VARIANT_ID,
                            nearMiss,
                            current);
            assertEquals(
                    MachineIdentityPolicy.Resolution.QUARANTINED,
                    decision.resolution());
            assertEquals(nearMiss, decision.persistedIdentity());
            assertTrue(decision.quarantineReason().isPresent());
        }
    }

    @Test
    void fiveLegacyTierOneTuplesAreDerivedExactlyAndNearMissesQuarantine() {
        assertEquals(
                5,
                ModMachineIdentityMigrations
                        .T16_LEGACY_TIER1_MIGRATIONS.size());
        for (MachineIdentityPolicy.Migration migration
                : ModMachineIdentityMigrations
                        .T16_LEGACY_TIER1_MIGRATIONS) {
            MachineVariant current = ModMachineVariants.require(
                    ResourceLocation.parse(migration.variantId()));
            MachineIdentityPolicy.Identity expectedLegacy =
                    ModMachineIdentityMigrations.legacyIdentityOf(current);
            MachineIdentityPolicy.Identity expectedCurrent =
                    ModMachineIdentityMigrations.identityOf(current);
            assertEquals(expectedLegacy, migration.from());
            assertEquals(expectedCurrent, migration.to());
            assertEquals(
                    "cruciblecraft:legacy",
                    expectedLegacy.materialId());
            assertEquals(
                    current.kind().behavior().energy().type().name(),
                    expectedLegacy.energyIdentity());

            MachineIdentityPolicy.Decision exact =
                    ModMachineIdentityMigrations.resolve(
                            migration.variantId(),
                            expectedLegacy,
                            expectedCurrent);
            assertEquals(
                    MachineIdentityPolicy.Resolution.MIGRATED,
                    exact.resolution());
            assertEquals(expectedCurrent, exact.persistedIdentity());

            List<MachineIdentityPolicy.Identity> nearMisses = List.of(
                    new MachineIdentityPolicy.Identity(
                            expectedLegacy.machineKind() + "_near",
                            expectedLegacy.tierProfile(),
                            expectedLegacy.materialId(),
                            expectedLegacy.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            expectedLegacy.machineKind(),
                            expectedLegacy.tierProfile() + "_near",
                            expectedLegacy.materialId(),
                            expectedLegacy.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            expectedLegacy.machineKind(),
                            expectedLegacy.tierProfile(),
                            expectedLegacy.materialId() + "_near",
                            expectedLegacy.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            expectedLegacy.machineKind(),
                            expectedLegacy.tierProfile(),
                            expectedLegacy.materialId(),
                            expectedLegacy.energyIdentity() + "_near"));
            for (MachineIdentityPolicy.Identity nearMiss : nearMisses) {
                MachineIdentityPolicy.Decision rejected =
                        ModMachineIdentityMigrations.resolve(
                                migration.variantId(),
                                nearMiss,
                                expectedCurrent);
                assertEquals(
                        MachineIdentityPolicy.Resolution.QUARANTINED,
                        rejected.resolution());
                assertEquals(nearMiss, rejected.persistedIdentity());
            }

            MachineIdentityPolicy.Decision blank =
                    ModMachineIdentityMigrations.resolve(
                            migration.variantId(),
                            new MachineIdentityPolicy.Identity(
                                    "", "", "", ""),
                            expectedCurrent);
            assertEquals(
                    MachineIdentityPolicy.Resolution.ACCEPTED,
                    blank.resolution());
            assertEquals(expectedCurrent, blank.persistedIdentity());
        }
    }

    @Test
    void threeT17LegacyTierOneTuplesUseHistoricalEnergyAndFailClosed() {
        Map<String, EnergyType> historicalEnergy = Map.of(
                "cruciblecraft:distillery", EnergyType.ELECTRIC,
                "cruciblecraft:drying", EnergyType.ELECTRIC,
                "cruciblecraft:smelter", EnergyType.HEAT);
        assertEquals(
                historicalEnergy.keySet(),
                ModMachineIdentityMigrations
                        .T17_LEGACY_TIER1_MIGRATIONS.stream()
                        .map(MachineIdentityPolicy.Migration::variantId)
                        .collect(java.util.stream.Collectors.toSet()));

        for (MachineIdentityPolicy.Migration migration
                : ModMachineIdentityMigrations
                        .T17_LEGACY_TIER1_MIGRATIONS) {
            MachineVariant current = ModMachineVariants.require(
                    ResourceLocation.parse(migration.variantId()));
            MachineIdentityPolicy.Identity expectedLegacy =
                    ModMachineIdentityMigrations.legacyIdentityOf(
                            current,
                            historicalEnergy.get(migration.variantId()));
            MachineIdentityPolicy.Identity expectedCurrent =
                    ModMachineIdentityMigrations.identityOf(current);
            assertEquals(expectedLegacy, migration.from());
            assertEquals(expectedCurrent, migration.to());
            assertEquals(
                    historicalEnergy.get(migration.variantId()).name(),
                    expectedLegacy.energyIdentity());
            assertEquals(
                    "cruciblecraft:legacy",
                    expectedLegacy.materialId());
            assertEquals(
                    MachineIdentityPolicy.Resolution.MIGRATED,
                    ModMachineIdentityMigrations.resolve(
                            migration.variantId(),
                            expectedLegacy,
                            expectedCurrent).resolution());

            List<MachineIdentityPolicy.Identity> nearMisses = List.of(
                    new MachineIdentityPolicy.Identity(
                            expectedLegacy.machineKind() + "_near",
                            expectedLegacy.tierProfile(),
                            expectedLegacy.materialId(),
                            expectedLegacy.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            expectedLegacy.machineKind(),
                            expectedLegacy.tierProfile() + "_near",
                            expectedLegacy.materialId(),
                            expectedLegacy.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            expectedLegacy.machineKind(),
                            expectedLegacy.tierProfile(),
                            expectedLegacy.materialId() + "_near",
                            expectedLegacy.energyIdentity()),
                    new MachineIdentityPolicy.Identity(
                            expectedLegacy.machineKind(),
                            expectedLegacy.tierProfile(),
                            expectedLegacy.materialId(),
                            expectedLegacy.energyIdentity() + "_near"));
            for (MachineIdentityPolicy.Identity nearMiss : nearMisses) {
                MachineIdentityPolicy.Decision rejected =
                        ModMachineIdentityMigrations.resolve(
                                migration.variantId(),
                                nearMiss,
                                expectedCurrent);
                assertEquals(
                        MachineIdentityPolicy.Resolution.QUARANTINED,
                        rejected.resolution());
                assertEquals(nearMiss, rejected.persistedIdentity());
            }

            MachineIdentityPolicy.Decision blank =
                    ModMachineIdentityMigrations.resolve(
                            migration.variantId(),
                            new MachineIdentityPolicy.Identity(
                                    "", "", "", ""),
                            expectedCurrent);
            assertEquals(
                    MachineIdentityPolicy.Resolution.ACCEPTED,
                    blank.resolution());
            assertEquals(expectedCurrent, blank.persistedIdentity());
        }
    }
}
