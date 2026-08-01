package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class MissingMaterialStackRewriterTest {
    @Test
    void redirectsLegacyGeneratedIdToOverride() {
        var plan = MissingMaterialStackRewriter.plan(
                "cruciblecraft:copper_ingot",
                Map.of("copper/ingot", "minecraft:copper_ingot"),
                ignored -> false);

        assertEquals(MissingMaterialStackRewriter.Kind.CANONICAL, plan.kind());
        assertEquals("minecraft:copper_ingot", plan.targetItemId());
    }

    @Test
    void preservesIdentityForRemovedMaterialAndLongestFormSuffix() {
        var plan = MissingMaterialStackRewriter.plan(
                "cruciblecraft:blue_steel_small_dust",
                Map.of(),
                ignored -> false);

        assertEquals(MissingMaterialStackRewriter.Kind.UNKNOWN, plan.kind());
        assertEquals("blue_steel", plan.materialId());
        assertEquals("small_dust", plan.form());
        assertEquals(MissingMaterialStackRewriter.UNKNOWN_ITEM_ID, plan.targetItemId());
    }

    @Test
    void canonicalizesLegacyGeneratedTinyDustDuringRestoration() {
        var plan = MissingMaterialStackRewriter.plan(
                "cruciblecraft:blue_steel_tiny_dust",
                Map.of("blue_steel/tiny_dust", "cruciblecraft:blue_steel/tiny_dust"),
                ignored -> false);

        assertEquals(MissingMaterialStackRewriter.Kind.CANONICAL, plan.kind());
        assertEquals("tiny_dust", plan.form());
        assertEquals("cruciblecraft:blue_steel/tiny_dust", plan.targetItemId());
    }

    @Test
    void leavesCanonicalAndUnrelatedItemsAlone() {
        assertEquals(
                MissingMaterialStackRewriter.Kind.UNCHANGED,
                MissingMaterialStackRewriter.plan(
                        "cruciblecraft:tin/ingot",
                        Map.of("tin/ingot", "cruciblecraft:tin/ingot"),
                        "cruciblecraft:tin/ingot"::equals).kind());
        assertEquals(
                MissingMaterialStackRewriter.Kind.UNCHANGED,
                MissingMaterialStackRewriter.plan(
                        "minecraft:iron_ingot",
                        Map.of(),
                        ignored -> false).kind());
        assertEquals(
                MissingMaterialStackRewriter.Kind.UNCHANGED,
                MissingMaterialStackRewriter.plan(
                        MissingMaterialStackRewriter.UNKNOWN_ITEM_ID,
                        Map.of(),
                        ignored -> false).kind());
    }

    @Test
    void leavesRegisteredItemsAloneWhenTheirSuffixLooksLikeAMaterialPrefix() {
        var plan = MissingMaterialStackRewriter.plan(
                "cruciblecraft:deepslate_copper_raw_ore",
                Map.of(),
                "cruciblecraft:deepslate_copper_raw_ore"::equals);

        assertEquals(MissingMaterialStackRewriter.Kind.UNCHANGED, plan.kind());
    }
}
