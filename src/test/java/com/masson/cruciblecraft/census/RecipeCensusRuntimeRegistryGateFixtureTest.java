package com.masson.cruciblecraft.census;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class RecipeCensusRuntimeRegistryGateFixtureTest {
    @Test
    void fixtureResourceMatchesCommittedGateJson() throws Exception {
        RecipeCensusRuntimeRegistryGateFixture fixture =
                RecipeCensusRuntimeRegistryGateFixture.load();
        Path gatePath = Path.of(
                "src/main/resources/census/t35_runtime_registry_gate.json");
        assertTrue(
                Files.isRegularFile(gatePath),
                "committed gate fixture must exist");
        String onDisk = Files.readString(gatePath, StandardCharsets.UTF_8);
        RecipeCensusRuntimeRegistryGateFixture fromDisk =
                RecipeCensusRuntimeRegistryGateFixture.decode(
                        com.google.gson.JsonParser.parseString(onDisk)
                                .getAsJsonObject());
        assertEquals(
                fixture.compatibleSchemaVersion(),
                fromDisk.compatibleSchemaVersion());
        assertEquals(fixture.namespace(), fromDisk.namespace());
        assertEquals(
                fixture.fullArtifactSha256(),
                fromDisk.fullArtifactSha256());
        assertEquals(fixture.totalExpectedIds(), fromDisk.totalExpectedIds());
        assertEquals(fixture.categories(), fromDisk.categories());
        assertEquals(
                fixture.countsByCategory(),
                fromDisk.countsByCategory());
    }

    @Test
    void censusGameTestsAreIsolatedFromDailyGrid() throws Exception {
        String gametest = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/gametest/"
                        + "CrucibleCraftGameTests.java"));
        assertFalse(gametest.contains("runtimeRegistryGate"));
        String holder = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/census/"
                        + "RecipeCensusGameTests.java"));
        assertTrue(holder.contains("@GameTestHolder"));
        assertTrue(holder.contains("cruciblecraft_census"));
        assertTrue(holder.contains("runtimeRegistryGate"));
    }

    @Test
    void fixtureDeclaresSixteenCategories() {
        RecipeCensusRuntimeRegistryGateFixture fixture =
                RecipeCensusRuntimeRegistryGateFixture.load();
        assertEquals(16, fixture.categories().size());
        assertEquals(16, fixture.countsByCategory().size());
        assertEquals(
                fixture.totalExpectedIds(),
                fixture.countsByCategory().values().stream()
                        .mapToInt(Integer::intValue)
                        .sum());
    }
}
