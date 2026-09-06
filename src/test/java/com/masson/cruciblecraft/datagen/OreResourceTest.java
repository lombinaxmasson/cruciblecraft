package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;

class OreResourceTest {
    private static final String COMPAT_SHORTCUT_GROUP = "cruciblecraft:compat_shortcut";
    private static final Path RESOURCES = Path.of("src/main/resources");
    private static final Path GENERATED_RESOURCES = Path.of("src/generated/resources");
    private static final Path COMPONENT_RULE_RESOURCES =
            Path.of("src/component_rule_generated/resources");
    private static final Path WORLDGEN_RESOURCES =
            Path.of("src/worldgen_generated/resources");
    private static final Path ORE_CHAIN_INDEX = Path.of("tools/gt6_ore_chain.json");
    @Test
    void everyOreHasRegistrationFacingGeneratedResources(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var oreMaterials = materials.stream()
                .filter(material ->
                        registered.get(material.id()).contains(MaterialPrefixes.ORE))
                .toList();
        assertEquals(147, oreMaterials.size());
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        for (var material : oreMaterials) {
            String materialId = material.id();
            assertTrue(
                    registered.get(materialId).contains(MaterialPrefixes.RAW_ORE),
                    materialId);
            for (String prefix : List.of("", "deepslate_")) {
                String ore = prefix + materialId + "_ore";
                assertTrue(clientFiles.containsKey(
                        "assets/cruciblecraft/blockstates/" + ore + ".json"), ore);
                assertTrue(clientFiles.containsKey(
                        "assets/cruciblecraft/models/block/" + ore + ".json"), ore);
                assertTrue(clientFiles.containsKey(
                        "assets/cruciblecraft/models/item/" + ore + ".json"), ore);
                assertTrue(serverFiles.containsKey(
                        "data/cruciblecraft/loot_table/blocks/" + ore + ".json"), ore);
            }
            assertTrue(serverFiles.containsKey(
                    "data/c/tags/block/ores/" + material.tagName() + ".json"));
            assertTrue(serverFiles.containsKey(
                    "data/c/tags/item/ores/" + material.tagName() + ".json"));
            assertTrue(clientFiles.keySet().stream().noneMatch(
                    path -> path.endsWith("/models/item/"
                            + material.registryName(MaterialPrefixes.ORE) + ".json")));
        }
        assertTrue(serverFiles.containsKey("data/c/tags/block/ores.json"));
        assertTrue(serverFiles.containsKey("data/c/tags/item/ores.json"));
        assertTrue(clientFiles.get("assets/cruciblecraft/lang/en_us.json")
                .contains("\"block.cruciblecraft.tungsten_ore\""));

        var invalid = new LinkedHashMap<>(registered);
        var firstOre = oreMaterials.getFirst();
        invalid.put(
                firstOre.id(),
                registered.get(firstOre.id()).stream()
                        .filter(form -> !form.equals(MaterialPrefixes.RAW_ORE))
                        .toList());
        assertThrows(
                IllegalStateException.class,
                () -> GeneratedMaterialPack.planServerFiles(materials, invalid));
    }

    @Test
    void staticMiningTagsContainNoRegisteredOreBlocks(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        Set<String> oreBlocks = materials.stream()
                .filter(material ->
                        registered.get(material.id()).contains(MaterialPrefixes.ORE))
                .flatMap(material -> Stream.of(
                        "cruciblecraft:" + material.id() + "_ore",
                        "cruciblecraft:deepslate_" + material.id() + "_ore"))
                .collect(Collectors.toUnmodifiableSet());

        for (var tag : Map.of(
                "data/minecraft/tags/block/mineable/pickaxe.json",
                GENERATED_RESOURCES,
                "data/minecraft/tags/block/needs_stone_tool.json",
                GENERATED_RESOURCES,
                "data/minecraft/tags/block/needs_iron_tool.json",
                RESOURCES).entrySet()) {
            Set<String> leaked = tagValues(readString(
                    tag.getValue().resolve(tag.getKey()))).stream()
                    .filter(oreBlocks::contains)
                    .collect(Collectors.toUnmodifiableSet());
            assertEquals(Set.of(), leaked, tag.getKey());
        }
    }

    @Test
    void generatedMaterialPackExclusivelyPlansDynamicModelsAndItemTags(
            @TempDir Path configDirectory) throws Exception {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        Set<String> expectedModels = new HashSet<>();
        Map<String, List<String>> expectedTags = new LinkedHashMap<>();
        Map<String, List<String>> aggregateTags = new LinkedHashMap<>();
        for (var material : materials) {
            List<String> materialItems = new ArrayList<>();
            for (MaterialPrefix prefix : registered.get(material.id())) {
                List<String> itemIds;
                if (prefix.equals(MaterialPrefixes.ORE)) {
                    itemIds = List.of(
                            "cruciblecraft:" + material.id() + "_ore",
                            "cruciblecraft:deepslate_" + material.id() + "_ore");
                } else {
                    String override = material.formItems().get(prefix);
                    String itemId = override != null
                            ? override
                            : "cruciblecraft:" + material.registryName(prefix);
                    itemIds = List.of(itemId);
                    if (override == null) {
                        expectedModels.add(
                                "assets/cruciblecraft/models/item/"
                                        + material.registryName(prefix) + ".json");
                    }
                }
                String tagId = prefix.tagNamespace() + ":" + prefix.tagDirectory()
                        + "/" + material.tagName();
                expectedTags.put(
                        "data/" + prefix.tagNamespace() + "/tags/item/"
                                + prefix.tagDirectory() + "/" + material.tagName() + ".json",
                        itemIds);
                aggregateTags
                        .computeIfAbsent(
                                "data/" + prefix.tagNamespace() + "/tags/item/"
                                        + prefix.tagDirectory() + ".json",
                                ignored -> new ArrayList<>())
                        .add("#" + tagId);
                materialItems.addAll(itemIds);
            }
            if (!materialItems.isEmpty()) {
                expectedTags.put(
                        "data/cruciblecraft/tags/item/materials/"
                                + material.id() + ".json",
                        materialItems);
            }
        }
        expectedTags.putAll(aggregateTags);
        Set<String> catalogBlockObjectModels = new HashSet<>();
        for (GtBlockObjectCatalog.Variant variant : GtBlockObjectCatalog.variants()) {
            if (variant.registryPath().contains("/")) {
                catalogBlockObjectModels.add(
                        "assets/cruciblecraft/models/item/"
                                + variant.registryPath() + ".json");
            }
        }
        for (GtBlockObjectCatalog.Variant variant
                : BathRemainderBlockObjectCatalog.variants()) {
            if (variant.registryPath().contains("/")) {
                catalogBlockObjectModels.add(
                        "assets/cruciblecraft/models/item/"
                                + variant.registryPath() + ".json");
            }
        }
        expectedModels.addAll(catalogBlockObjectModels);

        Set<String> actualModels = clientFiles.keySet().stream()
                .filter(path -> path.startsWith("assets/cruciblecraft/models/item/"))
                .filter(path -> path.substring(
                                "assets/cruciblecraft/models/item/".length())
                        .contains("/"))
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(expectedModels, actualModels);
        assertTrue(actualModels.stream().noneMatch(path -> path.contains("/models/item/item/")));

        Path datagenModels =
                GENERATED_RESOURCES.resolve("assets/cruciblecraft/models/item");
        for (String path : expectedModels) {
            if (catalogBlockObjectModels.contains(path)) {
                continue;
            }
            String relative = path.substring(
                    "assets/cruciblecraft/models/item/".length());
            assertTrue(Files.notExists(datagenModels.resolve(relative)), relative);
        }

        Map<String, String> actualTagFiles = serverFiles.entrySet().stream()
                .filter(entry -> entry.getKey().contains("/tags/item/"))
                .collect(Collectors.toUnmodifiableMap(Map.Entry::getKey, Map.Entry::getValue));
        assertEquals(expectedTags.keySet(), actualTagFiles.keySet());
        expectedTags.forEach((path, values) ->
                assertEquals(
                        Set.copyOf(values),
                        tagValues(actualTagFiles.get(path)),
                        path));

        Path generatedData = GENERATED_RESOURCES.resolve("data");
        if (Files.exists(generatedData)) {
            try (var paths = Files.walk(generatedData)) {
                List<Path> snapshots = paths
                        .filter(Files::isRegularFile)
                        .filter(path -> {
                            String normalized = path.toString().replace('\\', '/');
                            return normalized.contains("/tags/item/")
                                    || normalized.contains("/tags/block/");
                        })
                        .toList();
                assertEquals(
                        Set.of(
                                generatedData.resolve(
                                        "cruciblecraft/tags/item/"
                                                + "extruder_shapes.json"),
                                generatedData.resolve(
                                        "cruciblecraft/tags/block/"
                                                + "gt_stones.json"),
                                generatedData.resolve(
                                        "cruciblecraft/tags/block/"
                                                + "gt_block_objects.json"),
                                generatedData.resolve(
                                        "minecraft/tags/block/mineable/"
                                                + "pickaxe.json"),
                                generatedData.resolve(
                                        "minecraft/tags/block/mineable/"
                                                + "axe.json"),
                                generatedData.resolve(
                                        "minecraft/tags/block/"
                                                + "needs_stone_tool.json"),
                                generatedData.resolve(
                                        "minecraft/tags/block/"
                                                + "rails.json")),
                        Set.copyOf(snapshots),
                        "only catalog-driven static item/block tags may ship "
                                + "in generated data");
            }
        }
    }

    @Test
    void initialLargeVeinFamiliesAreFullyWired() {
        var families = List.of("copper", "tin", "iron", "gold", "tungsten");
        for (String family : families) {
            assertWorldgenResource(
                    "data/cruciblecraft/worldgen/configured_feature/large_"
                            + family + "_vein.json");
            assertWorldgenResource(
                    "data/cruciblecraft/worldgen/placed_feature/large_"
                            + family + "_vein.json");
            assertResource("data/cruciblecraft/veins/large_" + family + "_vein.json");
        }
        Path modifier = WORLDGEN_RESOURCES.resolve(
                "data/cruciblecraft/neoforge/biome_modifier/add_large_veins.json");
        assertTrue(Files.isRegularFile(modifier), modifier.toString());
        var features = JsonParser.parseString(readString(modifier))
                .getAsJsonObject()
                .getAsJsonArray("features")
                .asList()
                .stream()
                .map(element -> element.getAsString())
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(
                families.stream()
                        .map(family -> "cruciblecraft:large_" + family + "_vein")
                        .collect(Collectors.toUnmodifiableSet()),
                features);
        assertResource("data/cruciblecraft/tags/block/large_vein_replaceables.json");
    }

    @Test
    void configuredFamiliesHaveDistinctRequiredSaltsAndSingleOriginPlacement() throws Exception {
        var salts = new HashSet<Integer>();
        for (String family : List.of("copper", "tin", "iron", "gold", "tungsten")) {
            Path configuredPath = WORLDGEN_RESOURCES.resolve(
                    "data/cruciblecraft/worldgen/configured_feature/large_"
                            + family + "_vein.json");
            var config = JsonParser.parseString(Files.readString(configuredPath))
                    .getAsJsonObject()
                    .getAsJsonObject("config");
            assertTrue(salts.add(config.get("salt").getAsInt()), family + " duplicate salt");
            for (String layer : List.of("top", "bottom", "between", "spread")) {
                assertTrue(!config.getAsJsonArray(layer).isEmpty(), family + " " + layer);
            }

            Path placedPath = WORLDGEN_RESOURCES.resolve(
                    "data/cruciblecraft/worldgen/placed_feature/large_"
                            + family + "_vein.json");
            var placed = JsonParser.parseString(Files.readString(placedPath))
                    .getAsJsonObject();
            assertTrue(placed.getAsJsonArray("placement").isEmpty(), family + " placement");
        }
        assertEquals(5, salts.size());
    }

    @Test
    void generatedWorldgenResourcesUseDeclaredRegistryIds() throws Exception {
        Path veinRoot = RESOURCES.resolve("data/cruciblecraft/veins");
        try (var sources = Files.list(veinRoot)) {
            for (Path source : sources.filter(path ->
                    path.getFileName().toString().endsWith(".json")).toList()) {
                var authored = JsonParser.parseString(Files.readString(source))
                        .getAsJsonObject();
                String id = authored.get("id").getAsString();
                assertEquals(
                        source.getFileName().toString().replace(".json", ""),
                        id,
                        source.toString());
                Path configured = WORLDGEN_RESOURCES.resolve(
                        "data/cruciblecraft/worldgen/configured_feature/" + id + ".json");
                Path placed = WORLDGEN_RESOURCES.resolve(
                        "data/cruciblecraft/worldgen/placed_feature/" + id + ".json");
                assertTrue(Files.isRegularFile(configured), id);
                assertTrue(Files.isRegularFile(placed), id);
                assertEquals(
                        "cruciblecraft:large_vein",
                        JsonParser.parseString(Files.readString(configured))
                                .getAsJsonObject()
                                .get("type")
                                .getAsString(),
                        id);
                assertEquals(
                        "cruciblecraft:" + id,
                        JsonParser.parseString(Files.readString(placed))
                                .getAsJsonObject()
                                .get("feature")
                                .getAsString(),
                        id);
            }
        }
    }

    @Test
    void oreCookingRecipesPreserveOneIngotEconomyWithoutVanillaDuplicates(
            @TempDir Path configDirectory) throws Exception {
        assertEquals(144, MaterialPrefixes.RAW_ORE.units());
        assertEquals(144, MaterialPrefixes.CRUSHED_ORE.units());

        Set<String> canonical = Set.of("copper", "iron", "gold");
        for (String material : canonical) {
            for (String process : List.of("smelting", "blasting")) {
                Path duplicate = RESOURCES.resolve(
                        "data/cruciblecraft/recipe/" + material + "_raw_ore_" + process + ".json");
                assertTrue(Files.notExists(duplicate), duplicate.toString());
                Path generatedDuplicate = GENERATED_RESOURCES.resolve(
                        "data/cruciblecraft/recipe/" + material + "_raw_ore_" + process + ".json");
                assertTrue(Files.notExists(generatedDuplicate), generatedDuplicate.toString());
            }
        }

        Set<String> expectedDerived = new HashSet<>();
        var materials = MaterialLoader.load(configDirectory).values();
        var registeredForms = MaterialRegistrationGate.load(materials);
        for (var material : materials) {
            if (!registeredForms.get(material.id()).contains(MaterialPrefixes.INGOT)) {
                continue;
            }
            if (registeredForms.get(material.id()).contains(MaterialPrefixes.CRUSHED_ORE)) {
                addExpectedCookingRecipes(expectedDerived, material.id(), MaterialPrefixes.CRUSHED_ORE);
            }
            if (registeredForms.get(material.id()).contains(MaterialPrefixes.RAW_ORE)
                    && !material.formItems().containsKey(MaterialPrefixes.RAW_ORE)) {
                addExpectedCookingRecipes(expectedDerived, material.id(), MaterialPrefixes.RAW_ORE);
            }
        }
        Path generatedRecipes = GENERATED_RESOURCES.resolve("data/cruciblecraft/recipe");
        Path generatedAdvancements = GENERATED_RESOURCES.resolve(
                "data/cruciblecraft/advancement/recipes");
        Set<String> generatedRecipeSet;
        try (var paths = Files.walk(generatedRecipes)) {
            generatedRecipeSet = paths
                    .filter(Files::isRegularFile)
                    .map(path -> generatedRecipes.relativize(path)
                            .toString().replace('\\', '/'))
                    .collect(Collectors.toUnmodifiableSet());
        }
        Set<String> coverRecipes = Set.of(
                "conveyor_cover.json",
                "pressure_valve_cover.json",
                "retriever_item_cover.json",
                "robot_arm_cover.json",
                "selector_manual_cover.json");
        Set<String> preStorageRecipeSet = generatedRecipeSet.stream()
                .filter(path -> path.startsWith("pipe_acquisition/")
                        || coverRecipes.contains(path))
                .collect(Collectors.toUnmodifiableSet());
        // Keep the pre-storage actual set distinct and derive the new total from
        // that set plus the exact 5-cover + 25-pipe generated acquisition set.
        assertEquals(30, preStorageRecipeSet.size());
        // Current generated baseline: 877 + wire cutter additions, tool
        // patterns/routes, and the catalog-driven machine/storage resources.
        // The hopper catalog adds 121 acquisition recipes (60 hopper, 60 queue,
        // 1 steel dust funnel). The machine catalog adds catalog-driven recipes
        // plus five source-backed casings beyond the early six. Storage adds 18
        // source-visible storage acquisition recipes. Display CPU adds 8 cover
        // recipes (4 shaped + 4 shapeless cycle).
        assertEquals(1_732, generatedRecipeSet.size() - preStorageRecipeSet.size());
        assertEquals(
                1_732 + preStorageRecipeSet.size(),
                generatedRecipeSet.size());
        assertEquals(48, countRegularFiles(COMPONENT_RULE_RESOURCES.resolve(
                "data/cruciblecraft/recipe")));
        assertEquals(0, countRegularFiles(generatedAdvancements));
        try (var paths = Files.walk(generatedRecipes)) {
            Set<String> actual = paths
                    .filter(Files::isRegularFile)
                    .map(path -> generatedRecipes.relativize(path).toString().replace('\\', '/'))
                    .filter(name -> name.matches(
                            ".+/(raw|crushed)_ore_(smelting|blasting)\\.json"))
                    .collect(Collectors.toUnmodifiableSet());
            assertEquals(expectedDerived, actual);
        }
        for (String recipeName : expectedDerived) {
            assertTrue(Files.notExists(RESOURCES.resolve(
                    "data/cruciblecraft/recipe/" + recipeName)));
        }
        var shortcutPolicy = JsonParser.parseString(Files.readString(ORE_CHAIN_INDEX))
                .getAsJsonObject()
                .getAsJsonObject("coverage_ledger")
                .getAsJsonObject("furnace_shortcut_policy");
        int rawPairs = Math.toIntExact(expectedDerived.stream()
                .filter(name -> name.endsWith("/raw_ore_smelting.json"))
                .count());
        int crushedPairs = Math.toIntExact(expectedDerived.stream()
                .filter(name -> name.endsWith("/crushed_ore_smelting.json"))
                .count());
        int smeltingFiles = Math.toIntExact(expectedDerived.stream()
                .filter(name -> name.endsWith("_smelting.json"))
                .count());
        int blastingFiles = Math.toIntExact(expectedDerived.stream()
                .filter(name -> name.endsWith("_blasting.json"))
                .count());
        assertEquals(rawPairs, shortcutPolicy.get("raw_pairs").getAsInt());
        assertEquals(crushedPairs, shortcutPolicy.get("crushed_pairs").getAsInt());
        assertEquals(smeltingFiles, shortcutPolicy.get("smelting_files").getAsInt());
        assertEquals(blastingFiles, shortcutPolicy.get("blasting_files").getAsInt());
        assertEquals(expectedDerived.size(), shortcutPolicy.get("total_files").getAsInt());
        assertEquals(
                COMPAT_SHORTCUT_GROUP,
                shortcutPolicy.get("group").getAsString());

        Path handwrittenRecipes = RESOURCES.resolve("data/cruciblecraft/recipe");
        try (var handwritten = Files.walk(handwrittenRecipes);
                var generated = Files.walk(generatedRecipes);
                Stream<Path> paths = Stream.concat(handwritten, generated)) {
            var oreCooking = paths
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().matches(
                            "(raw|crushed)_ore_(smelting|blasting)\\.json"))
                    .toList();
            assertEquals(expectedDerived.size(), oreCooking.size());
            for (Path recipe : oreCooking) {
                var document = JsonParser.parseString(Files.readString(recipe))
                        .getAsJsonObject();
                assertEquals(
                        COMPAT_SHORTCUT_GROUP,
                        document.get("group").getAsString(),
                        recipe.toString());
                var result = document.getAsJsonObject("result");
                assertEquals(1, result.get("count").getAsInt(), recipe.toString());
            }
        }
    }

    private static void assertResource(String relative) {
        assertTrue(Files.isRegularFile(RESOURCES.resolve(relative)), relative);
    }

    private static void assertWorldgenResource(String relative) {
        assertTrue(
                Files.isRegularFile(WORLDGEN_RESOURCES.resolve(relative)),
                relative);
    }

    private static String readString(Path path) {
        try {
            return Files.readString(path);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("Cannot read " + path, error);
        }
    }

    private static long countRegularFiles(Path root) throws java.io.IOException {
        if (Files.notExists(root)) {
            return 0;
        }
        try (var paths = Files.walk(root)) {
            return paths.filter(Files::isRegularFile).count();
        }
    }

    private static Set<String> tagValues(String json) {
        return JsonParser.parseString(json)
                .getAsJsonObject()
                .getAsJsonArray("values")
                .asList()
                .stream()
                .map(element -> element.getAsString())
                .collect(Collectors.toUnmodifiableSet());
    }

    private static void addExpectedCookingRecipes(
            Set<String> recipes,
            String materialId,
            MaterialPrefix form) {
        for (String process : List.of("smelting", "blasting")) {
            recipes.add(materialId + "/" + form.serializedName() + "_" + process + ".json");
        }
    }
}
