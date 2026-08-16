package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class MachineIdentityPolicyTest {
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
                        current.tierBand(),
                        current.materialId(),
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        "test:other_tier",
                        current.materialId(),
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        current.tierBand(),
                        "test:other_material",
                        current.energyIdentity()),
                new MachineIdentityPolicy.Identity(
                        current.machineKind(),
                        current.tierBand(),
                        current.materialId(),
                        "KINETIC_PUSH"));

        for (MachineIdentityPolicy.Identity mismatch : mismatches) {
            MachineIdentityPolicy.Decision decision =
                    MachineIdentityPolicy.resolve(
                            mismatch,
                            current);
            assertEquals(
                    MachineIdentityPolicy.Resolution.QUARANTINED,
                    decision.resolution());
            assertEquals(mismatch, decision.persistedIdentity());
            assertTrue(decision.quarantineReason().isPresent());
        }
    }

    @Test
    void blankAndCurrentIdentitiesAreAcceptedAsCurrent() {
        MachineIdentityPolicy.Identity current =
                new MachineIdentityPolicy.Identity(
                        "test:centrifuge",
                        "test:tier",
                        "test:material",
                        "KINETIC_ROTATION");
        MachineIdentityPolicy.Decision blank =
                MachineIdentityPolicy.resolve(
                        new MachineIdentityPolicy.Identity(
                                "", "", "", ""),
                        current);
        assertEquals(
                MachineIdentityPolicy.Resolution.ACCEPTED,
                blank.resolution());
        assertEquals(current, blank.persistedIdentity());
        assertTrue(blank.quarantineReason().isEmpty());

        MachineIdentityPolicy.Decision exact =
                MachineIdentityPolicy.resolve(current, current);
        assertEquals(
                MachineIdentityPolicy.Resolution.ACCEPTED,
                exact.resolution());
        assertEquals(current, exact.persistedIdentity());
        assertTrue(exact.quarantineReason().isEmpty());
    }
}
