package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class DustFunnelResourceTest {
    private static final Path GENERATED = Path.of("src/generated/resources");

    @Test
    void steelDustFunnelUsesSharedArtAndVanillaHopperRecipe() throws Exception {
        JsonObject recipe = JsonParser.parseString(Files.readString(
                GENERATED.resolve(
                        "data/cruciblecraft/recipe/hoppers/steel_dust_funnel.json")))
                .getAsJsonObject();
        assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
        String body = recipe.toString();
        assertTrue(body.contains("minecraft:hopper"));
        assertTrue(body.contains("cruciblecraft:iron/plate")
                || body.contains("iron/plate"));
        JsonObject model = JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/assets/cruciblecraft/models/block/dust_funnel.json")))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:block/dust_funnel/colored_sides",
                model.getAsJsonObject("textures").get("sides").getAsString());
        assertEquals(
                "cruciblecraft:block/dust_funnel/colored_hole",
                model.getAsJsonObject("textures").get("hole").getAsString());
        assertTrue(Files.isRegularFile(GENERATED.resolve(
                "assets/cruciblecraft/blockstates/steel_dust_funnel.json")));
        assertTrue(Files.isRegularFile(GENERATED.resolve(
                "assets/cruciblecraft/models/item/steel_dust_funnel.json")));
        assertTrue(Files.isRegularFile(GENERATED.resolve(
                "data/cruciblecraft/loot_table/blocks/steel_dust_funnel.json")));
    }
}
