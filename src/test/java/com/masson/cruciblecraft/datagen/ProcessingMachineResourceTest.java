package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

class ProcessingMachineResourceTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path MAIN = Path.of("src/main/resources");
    private static final List<String> MACHINES = ModMachineVariants.ALL
            .stream().map(variant -> variant.id().getPath()).toList();

    @Test
    void configuredMachinesHaveLootAndMiningTags() throws Exception {
        var pickaxe = values(
                GENERATED,
                "data/minecraft/tags/block/mineable/pickaxe.json");
        var stone = values(
                GENERATED,
                "data/minecraft/tags/block/needs_stone_tool.json");
        for (String machine : MACHINES) {
            String id = "cruciblecraft:" + machine;
            assertTrue(pickaxe.contains(id), id + " must be pickaxe-mineable");
            assertTrue(stone.contains(id), id + " must require a stone-tier tool");
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "data/cruciblecraft/loot_table/blocks/" + machine + ".json")));
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "data/cruciblecraft/recipe/machines/" + machine + ".json")));
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "assets/cruciblecraft/blockstates/" + machine + ".json")));
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + machine + ".json")));
            var blockstate = JsonParser.parseString(Files.readString(GENERATED.resolve(
                    "assets/cruciblecraft/blockstates/" + machine + ".json"))).getAsJsonObject();
            assertEquals(
                    Set.of("facing=north", "facing=east", "facing=south", "facing=west"),
                    blockstate.getAsJsonObject("variants").keySet());
            var north = blockstate.getAsJsonObject("variants").get("facing=north");
            var northVariant = north.isJsonArray()
                    ? north.getAsJsonArray().get(0).getAsJsonObject()
                    : north.getAsJsonObject();
            String modelId = northVariant.get("model").getAsString();
            assertTrue(modelId.startsWith("cruciblecraft:"), machine);
            String modelPath = "assets/cruciblecraft/models/"
                    + modelId.substring("cruciblecraft:".length())
                    + ".json";
            Path blockModel = MAIN.resolve(modelPath);
            if (!Files.isRegularFile(blockModel)) {
                blockModel = GENERATED.resolve(modelPath);
            }
            assertTrue(Files.isRegularFile(blockModel), machine + " " + modelPath);
            var model = JsonParser.parseString(Files.readString(blockModel)).getAsJsonObject();
            var textures = model.getAsJsonObject("textures");
            String parent = model.has("parent") ? model.get("parent").getAsString() : "";
            if ("cruciblecraft:block/machine_cube_2_layer".equals(parent)) {
                assertTrue(
                        textures.has("bot_north") && textures.has("top_north"),
                        machine + " needs colored and overlay front textures");
                assertNotEquals(
                        textures.get("bot_north").getAsString(),
                        textures.get("bot_east").getAsString());
            } else if ("minecraft:block/orientable".equals(parent)) {
                assertTrue(
                        textures.has("front"),
                        machine + " needs a visible front texture");
                assertNotEquals(
                        textures.get("front").getAsString(),
                        textures.get("side").getAsString());
            } else {
                assertTrue(model.has("elements"), machine + " shaped GT6 model");
                assertTrue(
                        textures.has("texture") || textures.has("particle"),
                        machine + " shaped GT6 model needs a texture");
            }
            var lang = JsonParser.parseString(Files.readString(GENERATED.resolve(
                    "assets/cruciblecraft/lang/en_us.json"))).getAsJsonObject();
            assertTrue(lang.has("block.cruciblecraft." + machine));
        }
    }

    @Test
    void chineseLanguageFileDeclaresOnlyItsRealTranslationCoverage() throws Exception {
        var english = JsonParser.parseString(Files.readString(GENERATED.resolve(
                "assets/cruciblecraft/lang/en_us.json"))).getAsJsonObject();
        var chinese = JsonParser.parseString(Files.readString(GENERATED.resolve(
                "assets/cruciblecraft/lang/zh_cn.json"))).getAsJsonObject();

        assertTrue(english.keySet().containsAll(chinese.keySet()));
        assertTrue(chinese.keySet().containsAll(Set.of(
                "screen.cruciblecraft.processing.status.unsupported_version",
                "screen.cruciblecraft.processing.status.material_quarantined",
                "screen.cruciblecraft.processing.status.inventory_layout_quarantined",
                "screen.cruciblecraft.processing.status.unknown",
                "screen.cruciblecraft.processing.tank_named",
                "message.cruciblecraft.anvil_material_quarantined",
                "message.cruciblecraft.crucible_casing_quarantined",
                "tooltip.cruciblecraft.invalid_tool_material",
                "tooltip.cruciblecraft.invalid_machine_material",
                "config.jade.plugin_cruciblecraft.cable",
                "jade.cruciblecraft.material_quarantined",
                "item.cruciblecraft.material_pickaxe",
                "item.cruciblecraft.material_shovel",
                "item.cruciblecraft.material_axe",
                "item.cruciblecraft.material_hoe",
                "item.cruciblecraft.material_sword",
                "item.cruciblecraft.smithing_hammer",
                "item.cruciblecraft.material_file",
                "item.cruciblecraft.material_chisel",
                "item.cruciblecraft.material_saw",
                "item.cruciblecraft.material_screwdriver",
                "item.cruciblecraft.material_wrench",
                "item.cruciblecraft.portable_fluid_tank",
                "block.cruciblecraft.anvil",
                "block.cruciblecraft.crucible",
                "block.cruciblecraft.centrifuge",
                "block.cruciblecraft.sifter",
                "block.cruciblecraft.extruder",
                "block.cruciblecraft.electrolyzer",
                "fluid_type.cruciblecraft.oxygen",
                "emi.category.cruciblecraft.electrolyzer",
                "jade.cruciblecraft.processing_tank")));
        long translated = chinese.keySet().stream()
                .filter(key -> !english.get(key).getAsString()
                        .equals(chinese.get(key).getAsString()))
                .count();
        Set<String> missing = new HashSet<>(english.keySet());
        missing.removeAll(chinese.keySet());
        long missingMaterialNames = missing.stream()
                .filter(key -> key.startsWith("material.cruciblecraft."))
                .count();
        assertEquals(7_622, english.size(), "current generated en_us key count");
        assertEquals(2_887L, translated, "declared Chinese translation coverage");
        assertEquals(2_401, missing.size(), "visible zh_cn localization debt");
        assertEquals(1_566L, missingMaterialNames,
                "missing generated material-name translations");
        assertEquals(
                2_334L,
                chinese.size() - translated,
                "copied English keys still occupying zh_cn");
        assertTrue(english.size() > translated,
                "partial localization must remain visibly partial until separately completed");
    }

    @Test
    void toolRecipesPersistOnlyMaterialIdentity() throws Exception {
        Path toolRoot = GENERATED.resolve(
                "data/cruciblecraft/recipe/tool/assembler");
        Set<String> materialIdentityLiterals = new HashSet<>();
        Pattern materialIs = Pattern.compile("material\\.is\\(\"([^\"]+)\"\\)");
        boolean foundWear = false;
        boolean foundPreserve = false;
        int rules = 0;
        try (var paths = Files.walk(toolRoot)) {
            for (Path path : paths.filter(Files::isRegularFile).toList()) {
                rules++;
                var recipe = JsonParser.parseString(
                        Files.readString(path)).getAsJsonObject();
                for (var condition : recipe.getAsJsonArray("conditions")) {
                    var matcher = materialIs.matcher(condition.getAsString());
                    while (matcher.find()) {
                        materialIdentityLiterals.add(matcher.group(1));
                    }
                }
                var output = recipe.getAsJsonArray("item_outputs")
                        .get(0).getAsJsonObject();
                var components = output.getAsJsonObject("string_components");
                assertEquals(
                        Set.of("cruciblecraft:tool_material"),
                        components.keySet(),
                        "derived tool facts must not be serialized");
                assertEquals(
                        "$material",
                        components.get("cruciblecraft:tool_material").getAsString());
                for (var inputValue : recipe.getAsJsonArray("item_inputs")) {
                    var input = inputValue.getAsJsonObject();
                    if (!input.has("input_action")) {
                        continue;
                    }
                    String kind = input.getAsJsonObject("input_action")
                            .get("kind").getAsString();
                    foundWear |= kind.equals("wear");
                    foundPreserve |= kind.equals("preserve");
                }
            }
        }
        assertEquals(23, rules, "all tool route rules must be generated");
        assertEquals(Set.of("stone", "wood"), materialIdentityLiterals,
                "material.is literals must stay explicitly budgeted");
        assertTrue(foundWear, "Tool rules must retain WEAR catalysts");
        assertTrue(foundPreserve, "Tool rules must retain pattern selectors");
        var fileRecipe = JsonParser.parseString(Files.readString(GENERATED.resolve(
                "data/cruciblecraft/recipe/tools/iron_file.json"))).getAsJsonObject();
        assertTrue(!fileRecipe.getAsJsonObject("result").has("components"),
                "the iron File recipe must rely on runtime derivation");
    }

    @Test
    void hammerRecipesPersistMaterialIdentityOnly() throws Exception {
        Map<String, String> recipes = Map.of(
                "smithing_hammer.json", "iron",
                "bronze_smithing_hammer.json", "bronze",
                "steel_smithing_hammer.json", "steel");

        for (var entry : recipes.entrySet()) {
            var recipe = JsonParser.parseString(Files.readString(MAIN.resolve(
                    "data/cruciblecraft/recipe/" + entry.getKey()))).getAsJsonObject();
            var components = recipe.getAsJsonObject("result")
                    .getAsJsonObject("components");
            assertEquals(
                    Set.of("cruciblecraft:tool_material"),
                    components.keySet(),
                    entry.getKey() + " must not persist runtime-derived durability");
            assertEquals(
                    entry.getValue(),
                    components.get("cruciblecraft:tool_material").getAsString());
        }
    }

    private static List<String> values(Path rootPath, String path) throws Exception {
        var root = JsonParser.parseString(
                Files.readString(rootPath.resolve(path))).getAsJsonObject();
        return root.getAsJsonArray("values").asList().stream()
                .map(value -> value.getAsString()).toList();
    }
}
