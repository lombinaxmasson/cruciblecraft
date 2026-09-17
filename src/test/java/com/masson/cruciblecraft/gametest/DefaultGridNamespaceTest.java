package com.masson.cruciblecraft.gametest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

class DefaultGridNamespaceTest {
    @Test
    void kitchenSinkHoldersAreNotOnTheModIdNamespace() throws Exception {
        String mega = Files.readString(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest/"
                        + "CrucibleCraftGameTests.java"));
        String circuit = Files.readString(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest/"
                        + "CircuitTierGameTests.java"));
        String stub = Files.readString(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest/"
                        + "ModIdNamespaceGameTests.java"));
        assertFalse(mega.contains("@GameTestHolder(CrucibleCraft.MODID)"));
        assertFalse(circuit.contains("@GameTestHolder(CrucibleCraft.MODID)"));
        assertTrue(mega.contains("cruciblecraft_default_grid"));
        assertTrue(circuit.contains("CrucibleCraftGameTests.NAMESPACE"));
        assertTrue(stub.contains("@GameTestHolder(CrucibleCraft.MODID)"));
        assertTrue(stub.contains("defaultKitchenSinkGridIsOptIn"));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/data/cruciblecraft_default_grid/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/main/resources/data/cruciblecraft_default_grid/"
                        + "gametest/structure/empty.nbt")));
        assertFalse(Files.exists(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest")));
    }

    @Test
    void gameTestHoldersLiveInTheTestSourceSet() {
        assertTrue(Files.isRegularFile(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest/"
                        + "ModIdNamespaceGameTests.java")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/test/java/com/masson/cruciblecraft/scale/ScaleGameTests.java")));
        assertTrue(Files.isRegularFile(Path.of(
                "src/test/java/com/masson/cruciblecraft/census/"
                        + "RecipeCensusGameTests.java")));
        assertFalse(Files.exists(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest")));
        assertFalse(Files.exists(Path.of(
                "src/test/java/com/masson/cruciblecraft/scale/ScaleGameTests.java")));
        assertFalse(Files.exists(Path.of(
                "src/main/java/com/masson/cruciblecraft/census/"
                        + "RecipeCensusGameTests.java")));
    }
}
