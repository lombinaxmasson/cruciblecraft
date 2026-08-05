package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

class PortableFluidTankResourceTest {
    private static final Path GENERATED = Path.of("src/generated/resources");

    @Test
    void portableTankHasGeneratedModelAndCraftingRecipe() throws Exception {
        Path modelPath = GENERATED.resolve(
                "assets/cruciblecraft/models/item/portable_fluid_tank.json");
        Path recipePath = GENERATED.resolve(
                "data/cruciblecraft/recipe/portable_fluid_tank.json");
        assertTrue(Files.isRegularFile(modelPath));
        assertTrue(Files.isRegularFile(recipePath));

        var model = JsonParser.parseString(
                Files.readString(modelPath)).getAsJsonObject();
        assertEquals(
                "minecraft:item/generated",
                model.get("parent").getAsString());

        var recipe = JsonParser.parseString(
                Files.readString(recipePath)).getAsJsonObject();
        assertEquals(
                "minecraft:crafting_shaped",
                recipe.get("type").getAsString());
        assertEquals(
                List.of("CGC", "G G", "CGC"),
                recipe.getAsJsonArray("pattern").asList().stream()
                        .map(value -> value.getAsString()).toList());
        assertEquals(
                "cruciblecraft:portable_fluid_tank",
                recipe.getAsJsonObject("result").get("id").getAsString());
    }
}
