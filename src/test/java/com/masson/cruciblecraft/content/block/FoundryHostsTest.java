package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

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
}
