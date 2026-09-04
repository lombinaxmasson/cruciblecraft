package com.masson.cruciblecraft.census;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import net.minecraft.SharedConstants;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Bidirectional set checks for the census runtime inventory classifier. Drift
 * must fix the fixture or the classifier, never a namespace whitelist.
 */
class CensusRuntimeInventoryClassifierTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void inventoryFixtureItemSetMatchesTheCensusGateFixture() throws Exception {
        RecipeCensusRuntimeRegistryGateFixture fixture =
                RecipeCensusRuntimeRegistryGateFixture.load();
        Path inventoryPath = Path.of(
                "tools/owner_runtime_expression_inventory.json");
        assertTrue(
                Files.isRegularFile(inventoryPath),
                "Census runtime inventory must exist for classifier checks");
        JsonObject inventory = JsonParser.parseString(
                Files.readString(inventoryPath, StandardCharsets.UTF_8))
                .getAsJsonObject();
        JsonObject bidirectional = inventory.getAsJsonObject(
                "registry_fixture_bidirectional");
        assertEquals(
                fixture.categories().get("items").size(),
                bidirectional.get("fixture_item_count").getAsInt());
        assertTrue(inventory.get("minecraft_prefix_is_not_proof").getAsBoolean());
        Set<String> extra = jsonStringSet(bidirectional, "extra_in_live_not_in_fixture_sample");
        Set<String> missing = jsonStringSet(bidirectional, "missing_from_live_sample");
        var extraDiff = RecipeCensusRuntimeRegistryEnumerator.compareCategory(
                "items",
                fixture.categories().get("items"),
                List.copyOf(union(fixture.categories().get("items"), extra)));
        assertTrue(extraDiff.frozenSubsetOk() || extra.isEmpty());
        assertTrue(missing.isEmpty(), "classifier must not drop fixture ids");
    }

    @Test
    void vanillaAllowlistDoesNotTreatMinecraftPrefixAsProof() throws Exception {
        Path allowlistPath = Path.of("tools/owner_vanilla_item_allowlist.json");
        assertTrue(Files.isRegularFile(allowlistPath));
        JsonObject allowlist = JsonParser.parseString(
                Files.readString(allowlistPath, StandardCharsets.UTF_8))
                .getAsJsonObject();
        assertTrue(allowlist.get("minecraft_prefix_is_not_proof").getAsBoolean());
        assertTrue(allowlist.get("not_a_121_registry_scrape").getAsBoolean());
        assertEquals(
                "assembler_aliases_plus_explicit",
                allowlist.get("provenance").getAsString());
        assertEquals("minecraft-1.21.1", allowlist.get("version").getAsString());
        Set<String> itemIds = jsonArraySet(allowlist, "item_ids");
        assertFalse(itemIds.isEmpty());
        for (String itemId : itemIds) {
            ResourceLocation id = ResourceLocation.parse(itemId);
            assertTrue(
                    BuiltInRegistries.ITEM.containsKey(id),
                    "allowlist item must exist in live 1.21.1 ITEM registry: "
                            + itemId);
        }
        Set<String> notProven = Set.of(
                "minecraft:anvil",
                "minecraft:iron_ingot",
                "minecraft:bucket");
        for (String itemId : notProven) {
            ResourceLocation id = ResourceLocation.parse(itemId);
            assertTrue(
                    BuiltInRegistries.ITEM.containsKey(id),
                    itemId + " must exist in live ITEM registry");
            assertFalse(
                    itemIds.contains(itemId),
                    itemId + " must stay off the allowlist");
        }
        assertTrue(BuiltInRegistries.ITEM.containsKey(
                BuiltInRegistries.ITEM.getKey(Items.ANVIL)));
        for (String fluidId : jsonArraySet(allowlist, "fluid_ids")) {
            assertTrue(
                    BuiltInRegistries.FLUID.containsKey(
                            ResourceLocation.parse(fluidId)),
                    fluidId);
        }
        assertTrue(BuiltInRegistries.FLUID.getKey(Fluids.WATER)
                .toString()
                .equals("minecraft:water"));
    }

    private static Set<String> jsonArraySet(JsonObject parent, String key) {
        Set<String> values = new HashSet<>();
        parent.getAsJsonArray(key).forEach(element -> values.add(element.getAsString()));
        return values;
    }

    private static Set<String> jsonStringSet(JsonObject parent, String key) {
        Set<String> values = new HashSet<>();
        parent.getAsJsonArray(key).forEach(element -> values.add(element.getAsString()));
        return values;
    }

    private static Set<String> union(List<String> left, Set<String> right) {
        Set<String> values = new HashSet<>(left);
        values.addAll(right);
        return values;
    }
}
