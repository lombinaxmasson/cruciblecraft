package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialLoader;

class MaterialRegistrationGateTest {
    @Test
    void shortJsonFormIdsMatchNamespacedPrefixIds(@TempDir Path config) {
        var materials = MaterialLoader.load(config).values();
        var registered = assertDoesNotThrow(
                () -> MaterialRegistrationGate.load(materials));
        var abyssalOat = registered.get("abyssal_oat");
        assertTrue(
                abyssalOat != null && abyssalOat.contains(MaterialPrefixes.DUST),
                "abyssal_oat must load when JSON forms use short ids");
        var arsenopyrite = registered.get("arsenopyrite");
        assertTrue(
                arsenopyrite != null && arsenopyrite.contains(MaterialPrefixes.ORE),
                "T38 source-backed acquisition may register ore beyond GT6 factual forms");
    }
}
