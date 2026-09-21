package com.masson.cruciblecraft.recipe.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Map;
import java.util.stream.Collectors;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixTestFixture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * GT6 {@code OP.rockGt} pulverize / furnace unit yields, including mineral
 * metal content from {@code mTargetSmelting}.
 */
class RockGtProcessingTest {
    @BeforeEach
    void bootstrapPrefixes() {
        MaterialPrefixTestFixture.bootstrapBuiltins();
    }

    @Test
    void mineralRocksMatchGt6PulverAndSmeltingYields(@TempDir Path config) {
        Map<String, MaterialDefinition> byId = MaterialLoader.load(config).values()
                .stream()
                .collect(Collectors.toMap(MaterialDefinition::id, material -> material));

        RockGtProcessing.PulverPlan chalcopyriteDust = RockGtProcessing.pulverPlan(
                byId.get("chalcopyrite")).orElseThrow();
        assertEquals("chalcopyrite", chalcopyriteDust.materialId());
        assertEquals(MaterialPrefixes.SMALL_DUST, chalcopyriteDust.prefix());
        assertEquals(9, chalcopyriteDust.count());
        assertEquals(324L, chalcopyriteDust.units());

        RockGtProcessing.FurnacePlan chalcopyriteMetal = RockGtProcessing.furnacePlan(
                byId.get("chalcopyrite"), byId).orElseThrow();
        assertEquals("copper", chalcopyriteMetal.materialId());
        assertEquals(MaterialPrefixes.CHUNK, chalcopyriteMetal.prefix());
        assertEquals(2, chalcopyriteMetal.count());
        assertEquals(72L, chalcopyriteMetal.units());

        RockGtProcessing.FurnacePlan malachiteMetal = RockGtProcessing.furnacePlan(
                byId.get("malachite"), byId).orElseThrow();
        assertEquals("copper", malachiteMetal.materialId());
        assertEquals(MaterialPrefixes.NUGGET, malachiteMetal.prefix());
        assertEquals(3, malachiteMetal.count());
        assertEquals(54L, malachiteMetal.units());

        RockGtProcessing.FurnacePlan azuriteMetal = RockGtProcessing.furnacePlan(
                byId.get("azurite"), byId).orElseThrow();
        assertEquals("copper", azuriteMetal.materialId());
        assertEquals(MaterialPrefixes.CHUNK, azuriteMetal.prefix());
        assertEquals(1, azuriteMetal.count());

        RockGtProcessing.FurnacePlan galenaMetal = RockGtProcessing.furnacePlan(
                byId.get("galena"), byId).orElseThrow();
        assertEquals("lead", galenaMetal.materialId());
        assertEquals(MaterialPrefixes.CHUNK, galenaMetal.prefix());
        assertEquals(3, galenaMetal.count());

        RockGtProcessing.FurnacePlan cassiteriteMetal = RockGtProcessing.furnacePlan(
                byId.get("cassiterite"), byId).orElseThrow();
        assertEquals("tin", cassiteriteMetal.materialId());
        assertEquals(MaterialPrefixes.NUGGET, cassiteriteMetal.prefix());
        assertEquals(15, cassiteriteMetal.count());

        RockGtProcessing.FurnacePlan sphaleriteMetal = RockGtProcessing.furnacePlan(
                byId.get("sphalerite"), byId).orElseThrow();
        assertEquals("zinc", sphaleriteMetal.materialId());
        assertEquals(MaterialPrefixes.CHUNK, sphaleriteMetal.prefix());
        assertEquals(3, sphaleriteMetal.count());

        RockGtProcessing.PulverPlan netherBrickDust = RockGtProcessing.pulverPlan(
                byId.get("nether_brick")).orElseThrow();
        assertEquals("netherrack", netherBrickDust.materialId());
        assertEquals(9, netherBrickDust.count());

        RockGtProcessing.FurnacePlan netherrack = RockGtProcessing.furnacePlan(
                byId.get("netherrack"), byId).orElseThrow();
        assertTrue(netherrack.netherrackSpecial());
        assertEquals("nether_brick", netherrack.materialId());
        assertEquals("rock", netherrack.prefix().serializedName());
        assertEquals(1, netherrack.count());

        assertTrue(RockGtProcessing.furnacePlan(byId.get("hematite"), byId).isEmpty(),
                "hematite has no PROCESSING.FURNACE tag");
        assertEquals(36, RockGtProcessing.machineDuration(byId.get("chalcopyrite")));
        assertEquals(
                1.0F,
                RockGtProcessing.furnaceExperience(
                        byId.get("chalcopyrite"), chalcopyriteMetal));
        assertEquals(
                0.0F,
                RockGtProcessing.furnaceExperience(byId.get("netherrack"), netherrack));
    }
}
