package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class T5ChemicalResourceTest {
    private static final Path ROOT = Path.of("src/t5_chemical_generated/resources");
    private static final Path RECIPE_ROOT =
            ROOT.resolve("data/cruciblecraft/recipe/t5");
    private static final Path FLUID_GATE =
            ROOT.resolve("data/cruciblecraft/t5_chemical_fluid_gate.json");
    private static final Path MANIFEST =
            Path.of("tools/t5_chemical_recipe_manifest.json");
    private static final Path ENGLISH =
            Path.of("src/generated/resources/assets/cruciblecraft/lang/en_us.json");
    private static final Path CHINESE =
            Path.of("src/generated/resources/assets/cruciblecraft/lang/zh_cn.json");
    private static final Path MATERIALS =
            Path.of("src/main/resources/data/cruciblecraft/materials");

    @Test
    void pinnedDecompositionProjectionIsBoundedAndTraceable() throws Exception {
        Map<String, Integer> mapCounts = new HashMap<>();
        int maxEut = 0;
        int maxItemOutputs = 0;
        int maxFluidOutputs = 0;
        int maxFluidAmount = 0;
        int fluidClosureRecipes = 0;
        Set<String> referencedFluids = new HashSet<>();
        try (var paths = Files.walk(RECIPE_ROOT)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                JsonObject recipe =
                        JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                assertEquals(
                        "cruciblecraft:gt_recipe",
                        recipe.get("type").getAsString());
                String material = path.getFileName().toString()
                        .replaceFirst("\\.json$", "");
                boolean fluidClosure = material.startsWith("fluid_closure_");
                boolean distillery = path.getParent().getFileName()
                        .toString().equals("distillery");
                if (fluidClosure) {
                    fluidClosureRecipes++;
                } else if (!distillery) {
                    assertTrue(
                            recipe.getAsJsonArray("item_inputs").asList().stream()
                                            .map(value -> value.getAsJsonObject())
                                            .filter(ingredient -> ingredient.has("tag"))
                                            .map(ingredient -> ingredient.get("tag").getAsString())
                                            .anyMatch(tag -> tag.equals("c:dusts/" + material)
                                                    || tag.equals(
                                                            "cruciblecraft:small_dusts/"
                                                                    + material)
                                                    || tag.equals(
                                                            "cruciblecraft:tiny_dusts/"
                                                                    + material)),
                            "T5 route must consume the declared terminal material "
                                    + material);
                }
                String map = recipe.get("map").getAsString();
                mapCounts.merge(map, 1, Integer::sum);
                JsonObject provenance = recipe.getAsJsonObject("provenance");
                assertEquals(
                        "gt6_pinned_dump_projection",
                        provenance.get("source_kind").getAsString());
                String source = provenance.get("selected_source_recipe").getAsString();
                assertTrue(
                        Files.isRegularFile(Path.of(source.substring(0, source.indexOf('#')))),
                        "Missing pinned source " + source);
                maxEut = Math.max(maxEut, recipe.get("eut").getAsInt());
                maxItemOutputs = Math.max(
                        maxItemOutputs,
                        recipe.has("item_outputs")
                                ? recipe.getAsJsonArray("item_outputs").size()
                                : 0);
                for (String fluidKey : Set.of("fluid_inputs", "fluid_outputs")) {
                    if (!recipe.has(fluidKey)) {
                        continue;
                    }
                    maxFluidOutputs = Math.max(
                            maxFluidOutputs,
                            recipe.getAsJsonArray(fluidKey).size());
                    recipe.getAsJsonArray(fluidKey).forEach(value -> {
                        JsonObject stack = value.getAsJsonObject();
                        referencedFluids.add(stack.get("id").getAsString());
                    });
                    for (var value : recipe.getAsJsonArray(fluidKey)) {
                        maxFluidAmount = Math.max(
                                maxFluidAmount,
                                value.getAsJsonObject().get("amount").getAsInt());
                    }
                }
            }
        }
        assertEquals(Map.of(
                "cruciblecraft:assembler", 1,
                "cruciblecraft:autoclave", 17,
                "cruciblecraft:bath", 6,
                "cruciblecraft:centrifuge", 14,
                "cruciblecraft:compressor", 5,
                "cruciblecraft:distillery", 1,
                "cruciblecraft:drying", 5,
                "cruciblecraft:electrolyzer", 62,
                "cruciblecraft:mixer", 34,
                "cruciblecraft:smelter", 7), mapCounts);
        assertEquals(152, mapCounts.values().stream().mapToInt(Integer::intValue).sum());
        assertEquals(6, fluidClosureRecipes);
        assertEquals(854, maxEut);
        assertEquals(6, maxItemOutputs);
        assertEquals(3, maxFluidOutputs);
        assertEquals(409_600, maxFluidAmount);

        Set<String> registeredFluids = new HashSet<>(fluidGateById().keySet());
        try (var paths = Files.list(MATERIALS)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".json"))
                    .filter(candidate -> !candidate.getFileName().toString().equals("index.json"))
                    .toList()) {
                JsonObject material =
                        JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if (material.get("molten_fluid").getAsBoolean()) {
                    registeredFluids.add(
                            "cruciblecraft:molten_" + material.get("id").getAsString());
                }
            }
        }
        for (String fluid : referencedFluids) {
            assertTrue(
                    fluid.equals("minecraft:water")
                            || fluid.equals("minecraft:lava")
                            || fluid.equals("cruciblecraft:steam")
                            || fluid.equals("cruciblecraft:creosote")
                            || registeredFluids.contains(fluid),
                    "Unregistered T5 fluid output " + fluid);
        }
    }

    @Test
    void chemicalFluidGateIsNonPlaceableAndMatchesMaterialColors() throws Exception {
        Map<String, JsonObject> fluids = fluidGateById();
        assertEquals(15, fluids.size());
        for (Map.Entry<String, JsonObject> entry : fluids.entrySet()) {
            JsonObject fluid = entry.getValue();
            assertFalse(fluid.get("world_placeable").getAsBoolean());
            assertTrue(fluid.get("density").getAsInt() != 0);
            assertTrue(fluid.get("viscosity").getAsInt() > 0);
            assertTrue(Set.of("gas", "liquid").contains(
                    fluid.get("state").getAsString()));
            JsonObject material = JsonParser.parseString(Files.readString(
                    MATERIALS.resolve(
                            fluid.get("material").getAsString() + ".json")))
                    .getAsJsonObject();
            assertEquals(
                    material.get("color").getAsString(),
                    fluid.get("color").getAsString());
        }
    }

    @Test
    void fluidClosureManifestPinsEveryFormerlyInputOnlyProducer() throws Exception {
        JsonObject manifest =
                JsonParser.parseString(Files.readString(MANIFEST)).getAsJsonObject();
        JsonObject closure = manifest.getAsJsonObject("acceptance")
                .getAsJsonObject("fluid_closure");
        assertTrue(closure.get("all_previously_input_only_have_producers").getAsBoolean());
        assertEquals(0, closure.getAsJsonArray("missing_producers").size());
        assertEquals(6, closure.getAsJsonArray("previously_input_only_fluids").size());
        JsonObject producers = closure.getAsJsonObject("producer_recipe_ids");
        assertEquals(6, producers.size());
        producers.entrySet().forEach(entry -> {
            assertFalse(entry.getValue().getAsJsonArray().isEmpty());
            entry.getValue().getAsJsonArray().forEach(recipeId -> {
                String path = recipeId.getAsString().substring("cruciblecraft:t5/".length());
                assertTrue(
                        Files.isRegularFile(RECIPE_ROOT.resolve(path + ".json")),
                        "Missing declared fluid closure producer " + recipeId);
            });
        });
        JsonObject distilledWater = closure.getAsJsonObject("water_distilled");
        assertEquals(
                "recoverable_surplus_byproduct",
                distilledWater.get("classification").getAsString());
        assertFalse(distilledWater.get("fake_voided").getAsBoolean());
        assertTrue(distilledWater.get("recoverable_surplus_amount").getAsInt() > 0);
    }

    @Test
    void everyLiveMaterialFluidHasAnEnglishNameAndChemicalsHaveChineseNames()
            throws Exception {
        JsonObject english =
                JsonParser.parseString(Files.readString(ENGLISH)).getAsJsonObject();
        JsonObject chinese =
                JsonParser.parseString(Files.readString(CHINESE)).getAsJsonObject();
        for (String id : fluidGateById().keySet()) {
            String key = "fluid_type." + id.replace(':', '.');
            assertTrue(english.has(key), "Missing English chemical fluid name " + key);
            assertTrue(chinese.has(key), "Missing Chinese chemical fluid name " + key);
        }
        try (var paths = Files.list(MATERIALS)) {
            for (Path path : paths.filter(candidate -> candidate.toString().endsWith(".json"))
                    .filter(candidate -> !candidate.getFileName().toString().equals("index.json"))
                    .toList()) {
                JsonObject material =
                        JsonParser.parseString(Files.readString(path)).getAsJsonObject();
                if (material.get("molten_fluid").getAsBoolean()) {
                    String key = "fluid_type.cruciblecraft.molten_"
                            + material.get("id").getAsString();
                    assertTrue(english.has(key), "Missing molten fluid name " + key);
                }
            }
        }
    }

    private static Map<String, JsonObject> fluidGateById() throws Exception {
        JsonObject gate =
                JsonParser.parseString(Files.readString(FLUID_GATE)).getAsJsonObject();
        assertEquals(1, gate.get("schema_version").getAsInt());
        Map<String, JsonObject> result = new HashMap<>();
        gate.getAsJsonArray("fluids").forEach(value -> {
            JsonObject fluid = value.getAsJsonObject();
            String id = "cruciblecraft:" + fluid.get("id").getAsString();
            assertFalse(result.containsKey(id), "Duplicate T5 fluid " + id);
            result.put(id, fluid);
        });
        return Map.copyOf(result);
    }
}
