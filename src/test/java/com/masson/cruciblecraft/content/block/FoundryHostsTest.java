package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.content.mte.MteInPlaceSpec;

import net.minecraft.resources.ResourceLocation;

class FoundryHostsTest {
    @Test
    void stainlessSteelKeepsCompoundToken() {
        assertEquals(
                "stainless_steel",
                FoundryHosts.token("foundry/mold_stainless_steel"));
        assertEquals(
                "stainless_steel",
                FoundryHosts.token("foundry/smelting_crucible_stainless_steel"));
        assertEquals(
                "stainless_steel",
                FoundryHosts.token("stainless_steel/smelting_crucible"));
        assertEquals(
                "stainless_steel",
                FoundryHosts.token("foundry/basin_stainless_steel"));
        assertEquals(
                "stainless_steel",
                FoundryHosts.token("foundry/crucible_crossing_stainless_steel"));
    }

    @Test
    void kindTemplatesMatchGt6Families() {
        assertEquals(
                "模具（%s）",
                FoundryHosts.kindTemplate("foundry/mold_steel").orElseThrow());
        assertEquals(
                "坩埚（%s）",
                FoundryHosts.kindTemplate("foundry/smelting_crucible_steel")
                        .orElseThrow());
        assertEquals(
                "坩埚（%s）",
                FoundryHosts.kindTemplate("stainless_steel/smelting_crucible")
                        .orElseThrow());
        assertEquals(
                "盆（%s）",
                FoundryHosts.kindTemplate("foundry/basin_bronze").orElseThrow());
        assertEquals(
                "坩埚交叉（%s）",
                FoundryHosts.kindTemplate("foundry/crucible_crossing_invar")
                        .orElseThrow());
        assertTrue(FoundryHosts.kindTemplate("steel/wall").isEmpty());
    }

    @Test
    void moldBasinAndCrossingStayDistinct() {
        MteInPlaceSpec mold = foundry(
                "foundry/mold_stone", "MultiTileEntityMold / Molds");
        MteInPlaceSpec basin = foundry(
                "foundry/basin_stone", "MultiTileEntityBasin / Molds");
        MteInPlaceSpec crossing = foundry(
                "foundry/crucible_crossing_stone",
                "MultiTileEntityCrossing / Molds");
        MteInPlaceSpec smeltery = foundry(
                "foundry/smelting_crucible_invar",
                "MultiTileEntitySmeltery / Smelting Crucibles");
        assertTrue(FoundryHosts.isMold(mold));
        assertTrue(FoundryHosts.isCasting(mold));
        assertFalse(FoundryHosts.isBasin(mold));
        assertFalse(FoundryHosts.isCrossing(mold));
        assertTrue(FoundryHosts.isBasin(basin));
        assertTrue(FoundryHosts.isCasting(basin));
        assertFalse(FoundryHosts.isMold(basin));
        assertTrue(FoundryHosts.isCrossing(crossing));
        assertFalse(FoundryHosts.isCasting(crossing));
        assertFalse(FoundryHosts.isMold(smeltery));
        assertFalse(FoundryHosts.isCasting(smeltery));
    }

    private static MteInPlaceSpec foundry(String path, String gt6Class) {
        return new MteInPlaceSpec(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path),
                path,
                0,
                MteInPlaceKind.CRUCIBLE_FOUNDRY,
                "crucible_foundry",
                "x",
                "x",
                gt6Class);
    }
}
