package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;

import net.minecraft.resources.ResourceLocation;

/** The MAIN creative tab enumerates ModMachineVariants.ALL; this test pins
 *  that enumeration to the machine_tiers.json variant rows so the tab cannot
 *  drift from the tier catalog. (Block-item resolution for each variant is
 *  exercised in-game by the GameTest EMI enumeration.) */
class MachineVariantEnumerationTest {
    @Test
    void allVariantsMatchTheTierCatalogRowsExactly() {
        Set<String> actual = ModMachineVariants.ALL.stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(33, actual.size());
        try (var stream = MachineVariantEnumerationTest.class
                .getClassLoader()
                .getResourceAsStream(
                        "data/cruciblecraft/machine_tiers.json")) {
            assertTrue(stream != null, "machine_tiers.json is missing");
            JsonObject doc = JsonParser.parseReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))
                    .getAsJsonObject();
            Set<String> expected = doc.getAsJsonArray("variants").asList()
                    .stream()
                    .map(row -> row.getAsJsonObject().get("id").getAsString())
                    .collect(Collectors.toUnmodifiableSet());
            assertEquals(expected, actual);
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    void publicWrappersResolveToTheSameCatalogHolders() {
        assertEquals(33, MachineTierCatalog.entries().size());
        assertEquals(33, ModBlocks.tieredProcessingBlocksById().size());
        assertEquals(33, ModItems.tieredProcessingItemsById().size());
        assertSame(
                ModBlocks.CENTRIFUGE,
                ModBlocks.tieredProcessingBlocksById().get(
                        ResourceLocation.parse("cruciblecraft:centrifuge")));
        assertSame(
                ModBlocks.STEEL_CENTRIFUGE,
                ModBlocks.tieredProcessingBlocksById().get(
                        ResourceLocation.parse("cruciblecraft:steel_centrifuge")));
        assertSame(
                ModBlocks.TITANIUM_SMELTER,
                ModBlocks.tieredProcessingBlocksById().get(
                        ResourceLocation.parse("cruciblecraft:titanium_smelter")));
        assertSame(
                ModItems.CENTRIFUGE,
                ModItems.tieredProcessingItemsById().get(
                        ResourceLocation.parse("cruciblecraft:centrifuge")));
        assertSame(
                ModItems.STAINLESS_STEEL_ELECTROLYZER,
                ModItems.tieredProcessingItemsById().get(
                        ResourceLocation.parse(
                                "cruciblecraft:stainless_steel_electrolyzer")));
        ModMachineVariants.ALL.forEach(variant -> {
            assertSame(
                    ModBlocks.tieredProcessingBlocksById().get(variant.id()),
                    ModBlocks.tieredProcessingBlocksById().get(variant.id()));
            assertTrue(
                    ModBlocks.tieredProcessingBlocksById().containsKey(
                            variant.id()),
                    variant.id().toString());
            assertTrue(
                    ModItems.tieredProcessingItemsById().containsKey(
                            variant.id()),
                    variant.id().toString());
        });
    }

    @Test
    void namingPolicyFreezesLegacyBareIdsAndForbidsMatrixCompletion() {
        MachineTierCatalog.NamingPolicy policy =
                MachineTierCatalog.namingPolicy();
        assertEquals("frozen_legacy_baseline", policy.tier1BareId());
        assertEquals("<material>_<kind>", policy.newSubsystemId());
        assertFalse(policy.automaticKindTierCompletion());
        assertEquals(
                3,
                MachineTierCatalog.variantsOf(
                        ResourceLocation.parse("cruciblecraft:centrifuge"))
                        .size());
        assertTrue(
                MachineTierCatalog.variantsOf(
                        ResourceLocation.parse("cruciblecraft:hopper"))
                        .isEmpty());
    }
}
