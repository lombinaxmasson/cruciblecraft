package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MissingMaterialStackRewriterTest {
    @Test
    void redirectsLegacyGeneratedIdToOverride() {
        var plan = MissingMaterialStackRewriter.plan(
                "cruciblecraft:copper_ingot",
                Map.of("copper/ingot", "minecraft:copper_ingot"));

        assertEquals(MissingMaterialStackRewriter.Kind.CANONICAL, plan.kind());
        assertEquals("minecraft:copper_ingot", plan.targetItemId());
    }

    @Test
    void preservesIdentityForRemovedMaterialAndLongestFormSuffix() {
        var plan = MissingMaterialStackRewriter.plan(
                "cruciblecraft:blue_steel_small_dust",
                Map.of());

        assertEquals(MissingMaterialStackRewriter.Kind.UNKNOWN, plan.kind());
        assertEquals("blue_steel", plan.materialId());
        assertEquals("small_dust", plan.form());
        assertEquals(MissingMaterialStackRewriter.UNKNOWN_ITEM_ID, plan.targetItemId());
    }

    @Test
    void leavesCanonicalAndUnrelatedItemsAlone() {
        assertEquals(
                MissingMaterialStackRewriter.Kind.UNCHANGED,
                MissingMaterialStackRewriter.plan(
                        "cruciblecraft:tin_ingot",
                        Map.of("tin/ingot", "cruciblecraft:tin_ingot")).kind());
        assertEquals(
                MissingMaterialStackRewriter.Kind.UNCHANGED,
                MissingMaterialStackRewriter.plan("minecraft:iron_ingot", Map.of()).kind());
        assertEquals(
                MissingMaterialStackRewriter.Kind.UNCHANGED,
                MissingMaterialStackRewriter.plan(
                        MissingMaterialStackRewriter.UNKNOWN_ITEM_ID,
                        Map.of()).kind());
    }
}
