package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

class PrefixPackRecipeResourceTest {
    private static final Path ROOT = Path.of(
            "src/generated/resources/data/cruciblecraft/recipe/prefix_pack");

    @Test
    void dustFamilyPackRecipesMatchGt6Counts() throws Exception {
        JsonObject tiny = object("dust_to_tiny_dust.json");
        assertEquals("cruciblecraft:prefix_pack", tiny.get("type").getAsString());
        assertEquals("dust", tiny.get("input_prefix").getAsString());
        assertEquals(1, tiny.get("input_count").getAsInt());
        assertEquals("tiny_dust", tiny.get("output_prefix").getAsString());
        assertEquals(9, tiny.get("output_count").getAsInt());
        assertEquals(0, tiny.get("unpack_index").getAsInt());
        assertEquals(2, tiny.get("unpack_modulus").getAsInt());

        JsonObject pack = object("dust_to_storage_dust.json");
        assertEquals("dust", pack.get("input_prefix").getAsString());
        assertEquals(9, pack.get("input_count").getAsInt());
        assertEquals("storage_dust", pack.get("output_prefix").getAsString());
        assertEquals(1, pack.get("output_count").getAsInt());

        JsonObject sugarPack = object("tiny_dust_to_dust.json");
        assertEquals("tiny_dust", sugarPack.get("input_prefix").getAsString());
        assertEquals(9, sugarPack.get("input_count").getAsInt());
        assertEquals("dust", sugarPack.get("output_prefix").getAsString());
        assertTrue(Files.isRegularFile(ROOT.resolve("small_dust_to_dust.json")));
        assertTrue(Files.isRegularFile(ROOT.resolve("storage_dust_to_dust.json")));
        JsonObject platePack = object("plate_to_storage_plate.json");
        assertEquals("plate", platePack.get("input_prefix").getAsString());
        assertEquals(9, platePack.get("input_count").getAsInt());
        assertEquals("storage_plate", platePack.get("output_prefix").getAsString());
        assertEquals(1, platePack.get("output_count").getAsInt());
        assertTrue(Files.isRegularFile(ROOT.resolve("storage_plate_to_plate.json")));

        JsonObject nuggetPack = object("nugget_to_ingot.json");
        assertEquals("nugget", nuggetPack.get("input_prefix").getAsString());
        assertEquals(9, nuggetPack.get("input_count").getAsInt());
        assertEquals("ingot", nuggetPack.get("output_prefix").getAsString());
        assertEquals(1, nuggetPack.get("output_count").getAsInt());
        JsonObject nuggetUnpack = object("ingot_to_nugget.json");
        assertEquals("ingot", nuggetUnpack.get("input_prefix").getAsString());
        assertEquals(1, nuggetUnpack.get("input_count").getAsInt());
        assertEquals("nugget", nuggetUnpack.get("output_prefix").getAsString());
        assertEquals(9, nuggetUnpack.get("output_count").getAsInt());
    }

    private static JsonObject object(String name) throws Exception {
        return JsonParser.parseString(Files.readString(ROOT.resolve(name)))
                .getAsJsonObject();
    }
}
