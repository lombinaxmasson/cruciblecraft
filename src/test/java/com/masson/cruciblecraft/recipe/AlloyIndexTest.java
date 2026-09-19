package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialCatalog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

class AlloyIndexTest {
    @BeforeAll
    static void bootstrapCatalog(@TempDir Path configDirectory) {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!MaterialCatalog.isBootstrapped()) {
            MaterialCatalog.bootstrap(configDirectory);
        }
    }

    @Test
    void firstLevelBlackBronzeUsesElectrumNotFlattenedGoldSilver() {
        int ingot = MaterialPrefixes.INGOT.units();
        var blackBronze = MaterialCatalog.alloys().match(
                Map.of("copper", 3 * ingot, "electrum", 2 * ingot));
        assertTrue(blackBronze.isPresent());
        assertEquals("black_bronze", blackBronze.orElseThrow().resultId());
        assertTrue(MaterialCatalog.alloys().match(
                Map.of("copper", 3 * ingot, "gold", ingot, "silver", ingot))
                .isEmpty());
    }

    @Test
    void noDecomposeRedAlloyStillAlloysFromFirstLevelParts() {
        int ingot = MaterialPrefixes.INGOT.units();
        var redAlloy = MaterialCatalog.alloys().match(
                Map.of("copper", ingot, "redstone", 4 * ingot));
        assertTrue(redAlloy.isPresent());
        assertEquals("red_alloy", redAlloy.orElseThrow().resultId());
    }

    @Test
    void extrasIncludeAnnealedCopperBronze() {
        int ingot = MaterialPrefixes.INGOT.units();
        var bronze = MaterialCatalog.alloys().match(
                Map.of("annealed_copper", 3 * ingot, "tin", ingot));
        assertTrue(bronze.isPresent());
        assertEquals("bronze", bronze.orElseThrow().resultId());
        assertEquals(4, bronze.orElseThrow().outputDivider());
    }

    @Test
    void compositionUsesGt6CommonDividerNotPartSum() {
        int ingot = MaterialPrefixes.INGOT.units();
        var redAlloy = MaterialCatalog.alloys().match(
                Map.of("copper", ingot, "redstone", 4 * ingot));
        assertEquals(1, redAlloy.orElseThrow().outputDivider());
        var purpleAlloy = MaterialCatalog.alloys().match(
                Map.of("red_alloy", ingot, "blue_alloy", ingot));
        assertEquals(1, purpleAlloy.orElseThrow().outputDivider());
        var lumium = MaterialCatalog.alloys().match(
                Map.of("tin", 3 * ingot, "silver", ingot, "glowstone", 4 * ingot));
        assertEquals(4, lumium.orElseThrow().outputDivider());
    }

    @Test
    void leftoverBronzeKeepsExtraCopper() {
        int ingot = MaterialPrefixes.INGOT.units();
        float temperature =
                (float) MaterialCatalog.require("bronze").thermal().meltingPoint() + 50.0F;
        var conversion = MaterialCatalog.alloys().preferredCrucibleConversion(
                Map.of("copper", 5 * ingot, "tin", ingot),
                temperature);
        assertTrue(conversion.isPresent());
        assertEquals(4 * ingot, conversion.get().outputUnits());
        assertEquals(3 * ingot, conversion.get().consumption().get("copper"));
        assertEquals(ingot, conversion.get().consumption().get("tin"));
    }
}
