package com.masson.cruciblecraft.energy.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.content.mte.MteInPlaceCatalog;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

class RotationEngineCatalogTest {
    @Test
    void liveEnginesMatchGt6VoltageTable() {
        assertEquals(9, RotationEngineCatalog.profiles().size());
        Set<String> live = MteInPlaceCatalog.specs().stream()
                .filter(spec -> spec.kind() == MteInPlaceKind.ROTATION_ENGINE)
                .map(spec -> spec.id().toString())
                .collect(Collectors.toSet());
        assertEquals(9, live.size());
        for (RotationEngineCatalog.Profile profile : RotationEngineCatalog.profiles()) {
            assertTrue(live.contains(profile.id().toString()), profile.id().toString());
            assertEquals(profile.inputRec() / 2L, profile.outputRec());
        }
        RotationEngineCatalog.Profile steel = RotationEngineCatalog.require(
                ResourceLocation.parse("cruciblecraft:steel/rotation_engine"));
        assertEquals(24827, steel.sourceMeta());
        assertEquals(128L, steel.inputRec());
        assertEquals(64L, steel.outputRec());
        RotationEngineCatalog.Profile adamantium = RotationEngineCatalog.require(
                ResourceLocation.parse(
                        "cruciblecraft:adamantium/rotation_engine"));
        assertEquals(2_097_152L, adamantium.inputRec());
        assertEquals(1_048_576L, adamantium.outputRec());
    }
}
