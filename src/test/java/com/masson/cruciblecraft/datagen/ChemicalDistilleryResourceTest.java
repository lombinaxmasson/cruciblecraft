package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ChemicalDistilleryResourceTest {
    private static final Path RECIPE = Path.of(
            "src/chemical_recipe_generated/resources/data/cruciblecraft/recipe/"
                    + "chemical/distillery/water_to_water_distilled.json");
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void committedResourceMatchesTheOnlyProjectablePinnedRow() throws Exception {
        JsonObject recipe =
                JsonParser.parseString(Files.readString(RECIPE)).getAsJsonObject();
        assertEquals("cruciblecraft:gt_recipe", recipe.get("type").getAsString());
        assertEquals("cruciblecraft:distillery", recipe.get("map").getAsString());
        assertEquals(10, recipe.getAsJsonArray("fluid_inputs")
                .get(0).getAsJsonObject().get("amount").getAsInt());
        assertEquals("minecraft:water", recipe.getAsJsonArray("fluid_inputs")
                .get(0).getAsJsonObject().get("id").getAsString());
        assertEquals(8, recipe.getAsJsonArray("fluid_outputs")
                .get(0).getAsJsonObject().get("amount").getAsInt());
        assertEquals(
                "cruciblecraft:water_distilled",
                recipe.getAsJsonArray("fluid_outputs")
                        .get(0).getAsJsonObject().get("id").getAsString());
        assertEquals(
                "gt6_dump/gt6_recipe_dump/maps/gt.recipe.distillery.json"
                        + "#recipes[1138]",
                recipe.getAsJsonObject("provenance")
                        .get("selected_source_recipe").getAsString());
    }

    @Test
    void generatedRecipeDecodesAndPassesTheDistilleryValidator()
            throws Exception {
        String source = Files.readString(RECIPE)
                .replace("cruciblecraft:water_distilled", "minecraft:water");
        GTRecipe decoded = GTRecipe.CODEC.parse(
                RegistryOps.create(JsonOps.INSTANCE, registries),
                JsonParser.parseString(source)).getOrThrow();

        assertTrue(decoded.itemInputs().isEmpty());
        assertTrue(decoded.itemOutputs().isEmpty());
        assertEquals(1, decoded.fluidInputs().size());
        assertEquals(1, decoded.fluidOutputs().size());
        assertEquals(10, decoded.fluidInputs().getFirst().getAmount());
        assertEquals(8, decoded.fluidOutputs().getFirst().getAmount());
        assertTrue(
                ModProcessingMachines.DISTILLERY.validator()
                        .validate(decoded)
                        .isEmpty(),
                () -> ModProcessingMachines.DISTILLERY.validator()
                        .validate(decoded)
                        .orElse(""));
    }
}
