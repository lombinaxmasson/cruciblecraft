package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CasingMachineAcquisitionResourceTest {
    private static final Path RECIPES = Path.of(
            "src/generated/resources/data/cruciblecraft/recipe");

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void sixCasingRecipesHaveExactStructuresResultsAndRegistrations()
            throws Exception {
        List<RecipeSpec> expected = List.of(
                casing(
                        "components/aluminium/machine_casing",
                        "cruciblecraft:aluminium/machine_casing",
                        "cruciblecraft:aluminium/plate",
                        "cruciblecraft:aluminium/long_rod"),
                casing(
                        "components/bronze/machine_casing_double",
                        "cruciblecraft:bronze/machine_casing_double",
                        "cruciblecraft:bronze/double_plate",
                        "cruciblecraft:bronze/long_rod"),
                casing(
                        "components/stainless_steel/machine_casing",
                        "cruciblecraft:stainless_steel/machine_casing",
                        "cruciblecraft:stainless_steel/plate",
                        "cruciblecraft:stainless_steel/long_rod"),
                casing(
                        "components/steel/machine_casing_double",
                        "cruciblecraft:steel/machine_casing_double",
                        "cruciblecraft:steel/double_plate",
                        "cruciblecraft:steel/long_rod"),
                casing(
                        "components/steel_galvanized/machine_casing",
                        "cruciblecraft:steel_galvanized/machine_casing",
                        "cruciblecraft:steel_galvanized/plate",
                        "cruciblecraft:steel_galvanized/long_rod"),
                casing(
                        "components/titanium/machine_casing_double",
                        "cruciblecraft:titanium/machine_casing_double",
                        "cruciblecraft:titanium/double_plate",
                        "cruciblecraft:titanium/long_rod"));

        assertEquals(6, expected.size());
        expected.forEach(spec -> assertRecipe(spec));
    }

    @Test
    void nineVariantRecipesHaveExactStructuresResultsAndRegistrations()
            throws Exception {
        List<RecipeSpec> expected = List.of(
                centrifuge(
                        "centrifuge",
                        "bronze",
                        "bronze/machine_casing_double",
                        ModItems.CENTRIFUGE.getId().toString()),
                centrifuge(
                        "steel_centrifuge",
                        "steel",
                        "steel/machine_casing_double",
                        ModItems.STEEL_CENTRIFUGE.getId().toString()),
                centrifuge(
                        "titanium_centrifuge",
                        "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_CENTRIFUGE.getId().toString()),
                sifter(
                        "sifter",
                        "bronze",
                        "bronze/machine_casing_double",
                        ModItems.SIFTER.getId().toString()),
                sifter(
                        "steel_sifter",
                        "steel",
                        "steel/machine_casing_double",
                        ModItems.STEEL_SIFTER.getId().toString()),
                sifter(
                        "titanium_sifter",
                        "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_SIFTER.getId().toString()),
                electrolyzer(
                        "electrolyzer",
                        "steel_galvanized/machine_casing",
                        "tin",
                        ModItems.ELECTROLYZER.getId().toString()),
                electrolyzer(
                        "aluminium_electrolyzer",
                        "aluminium/machine_casing",
                        "copper",
                        ModItems.ALUMINIUM_ELECTROLYZER.getId().toString()),
                electrolyzer(
                        "stainless_steel_electrolyzer",
                        "stainless_steel/machine_casing",
                        "gold",
                        ModItems.STAINLESS_STEEL_ELECTROLYZER.getId().toString()));

        assertEquals(9, expected.size());
        expected.forEach(spec -> assertRecipe(spec));
        assertEquals(
                expected.stream().map(RecipeSpec::result).collect(Collectors.toSet()),
                ModMachineVariants.ALL.stream()
                        .filter(variant -> Set.of(
                                "centrifuge",
                                "sifter",
                                "electrolyzer")
                                .contains(variant.kind().id().getPath()))
                        .filter(variant -> ModMachineVariants.isOpening(variant.id()))
                        .map(variant -> variant.id().toString())
                        .collect(Collectors.toSet()));
    }

    private static RecipeSpec casing(
            String path,
            String result,
            String plate,
            String longRod) {
        return new RecipeSpec(
                path,
                result,
                result,
                List.of("YXX", "XwX", "XXY"),
                Map.of("X", plate, "Y", longRod),
                Map.of("w", "cruciblecraft:material_wrench"),
                "cruciblecraft:shaped_catalyst");
    }

    private static RecipeSpec centrifuge(
            String id,
            String material,
            String casing,
            String registration) {
        return new RecipeSpec(
                "machines/" + id,
                "cruciblecraft:" + id,
                registration,
                List.of("G ", "SC", "G "),
                Map.of(
                        "C", "cruciblecraft:" + casing,
                        "G", "cruciblecraft:" + material + "/gear",
                        "S", "cruciblecraft:" + material + "/long_rod"));
    }

    private static RecipeSpec sifter(
            String id,
            String material,
            String casing,
            String registration) {
        return new RecipeSpec(
                "machines/" + id,
                "cruciblecraft:" + id,
                registration,
                List.of("W W", "RCR", "S S"),
                Map.of(
                        "C", "cruciblecraft:" + casing,
                        "R", "cruciblecraft:" + material + "/rod",
                        "S", "cruciblecraft:" + material + "/spring",
                        "W", "cruciblecraft:" + material + "/fine_wire"));
    }

    private static RecipeSpec electrolyzer(
            String id,
            String casing,
            String cableMaterial,
            String registration) {
        return new RecipeSpec(
                "machines/" + id,
                "cruciblecraft:" + id,
                registration,
                List.of("SMS", "W W"),
                Map.of(
                        "M", "cruciblecraft:" + casing,
                        "S", "cruciblecraft:platinum/wire",
                        "W", "cruciblecraft:" + cableMaterial + "/cable"));
    }

    private static void assertRecipe(RecipeSpec expected) {
        try {
            var recipe = JsonParser.parseString(Files.readString(
                    RECIPES.resolve(expected.path() + ".json"))).getAsJsonObject();
            assertEquals(expected.type(),
                    recipe.get("type").getAsString(), expected.path());
            assertEquals(expected.pattern(), recipe.getAsJsonArray("pattern")
                    .asList().stream().map(value -> value.getAsString()).toList());
            Map<String, String> key = new LinkedHashMap<>();
            var keyObject = expected.catalysts().isEmpty()
                    ? recipe.getAsJsonObject("key")
                    : recipe.getAsJsonObject("ingredients");
            keyObject.entrySet().forEach(entry ->
                    key.put(
                            entry.getKey(),
                            entry.getValue().getAsJsonObject()
                                    .get("item").getAsString()));
            assertEquals(expected.key(), key, expected.path());
            if (!expected.catalysts().isEmpty()) {
                Map<String, String> catalysts = new LinkedHashMap<>();
                recipe.getAsJsonObject("catalysts").entrySet().forEach(entry ->
                        catalysts.put(
                                entry.getKey(),
                                entry.getValue().getAsJsonObject()
                                        .get("item").getAsString()));
                assertEquals(expected.catalysts(), catalysts, expected.path());
            } else {
                assertEquals("misc", recipe.get("category").getAsString(),
                        expected.path());
            }
            var result = recipe.getAsJsonObject("result");
            assertEquals(Set.of("count", "id"), result.keySet(), expected.path());
            assertEquals(1, result.get("count").getAsInt(), expected.path());
            assertEquals(expected.result(), result.get("id").getAsString(),
                    expected.path());
            assertEquals(expected.result(), expected.registration(), expected.path());
        } catch (Exception error) {
            throw new AssertionError(expected.path(), error);
        }
    }

    private record RecipeSpec(
            String path,
            String result,
            String registration,
            List<String> pattern,
            Map<String, String> key,
            Map<String, String> catalysts,
            String type) {
        RecipeSpec(
                String path,
                String result,
                String registration,
                List<String> pattern,
                Map<String, String> key) {
            this(
                    path,
                    result,
                    registration,
                    pattern,
                    key,
                    Map.of(),
                    "minecraft:crafting_shaped");
        }
    }
}
