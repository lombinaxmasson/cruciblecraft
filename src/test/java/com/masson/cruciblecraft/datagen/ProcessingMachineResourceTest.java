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
import com.masson.cruciblecraft.localization.LanguageNames;
import com.masson.cruciblecraft.machine.processing.MachineTierCatalog;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.resources.ResourceLocation;

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
            assertTrue(
                    Files.isRegularFile(GENERATED.resolve(
                            "data/cruciblecraft/loot_table/blocks/" + machine + ".json"))
                            || Files.isRegularFile(MAIN.resolve(
                                    "data/cruciblecraft/loot_table/blocks/"
                                            + machine + ".json")),
                    machine + " missing loot table");
            if (!MachineTierCatalog.acquisitionBlocked(
                    ResourceLocation.fromNamespaceAndPath("cruciblecraft", machine))) {
                assertTrue(
                        Files.isRegularFile(GENERATED.resolve(
                                "data/cruciblecraft/recipe/machines/" + machine + ".json")),
                        machine + " missing generated acquisition recipe");
            }
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
                assertTrue(
                        textures.get("bot_east").getAsString().endsWith("/left"),
                        machine + " east must be GT6 left");
                assertTrue(
                        textures.get("bot_west").getAsString().endsWith("/right"),
                        machine + " west must be GT6 right");
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
                "config.jade.plugin_cruciblecraft.transformer",
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
                "item.cruciblecraft.material_monkey_wrench",
                "item.cruciblecraft.material_knife",
                "item.cruciblecraft.material_club",
                "item.cruciblecraft.material_crowbar",
                "item.cruciblecraft.material_plunger",
                "item.cruciblecraft.material_soft_hammer",
                "item.cruciblecraft.material_spade",
                "item.cruciblecraft.portable_fluid_tank",
                "block.cruciblecraft.anvil",
                "block.cruciblecraft.foundry.smelting_crucible_steel",
                "block.cruciblecraft.centrifuge",
                "block.cruciblecraft.sifter",
                "block.cruciblecraft.extruder",
                "block.cruciblecraft.electrolyzer",
                "fluid_type.cruciblecraft.oxygen",
                "emi.category.cruciblecraft.electrolyzer",
                "jade.cruciblecraft.processing_tank",
                "jade.cruciblecraft.temperature_k")));
        long translated = chinese.keySet().stream()
                .filter(key -> !english.get(key).getAsString()
                        .equals(chinese.get(key).getAsString()))
                .count();
        Set<String> missing = new HashSet<>(english.keySet());
        missing.removeAll(chinese.keySet());
        long missingMaterialNames = missing.stream()
                .filter(key -> key.startsWith("material.cruciblecraft."))
                .count();
        assertTrue(translated > 0, "zh_cn must contain real translations");
        assertTrue(missingMaterialNames > 0,
                "partial localization must keep visible material-name debt");
        assertTrue(english.size() >= chinese.size());
        assertTrue(chinese.size() >= translated);
        assertTrue(chinese.entrySet().stream()
                .allMatch(entry -> !entry.getValue().getAsString().isBlank()),
                "zh_cn must not contain blank translation values");
        assertTrue(chinese.entrySet().stream().noneMatch(entry ->
                        LanguageNames.isEnglishCopy(
                                entry.getValue().getAsString(),
                                english.get(entry.getKey()).getAsString())),
                "zh_cn must not copy English as a fake translation");
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
        assertEquals(25, rules, "all tool route rules must be generated");
        assertEquals(Set.of("stone", "wood"), materialIdentityLiterals,
                "material.is literals must stay explicitly budgeted");
        assertTrue(foundWear, "Tool rules must retain WEAR catalysts");
        assertTrue(foundPreserve, "Tool rules must retain pattern selectors");
        var fileRecipe = JsonParser.parseString(Files.readString(GENERATED.resolve(
                "data/cruciblecraft/recipe/tools/iron/tool_head_file.json")))
                .getAsJsonObject();
        assertEquals(
                "cruciblecraft:shaped_catalyst",
                fileRecipe.get("type").getAsString());
        assertEquals(
                List.of(" P ", " Pk", "   "),
                fileRecipe.getAsJsonArray("pattern").asList().stream()
                        .map(value -> value.getAsString()).toList());
        assertEquals(
                "cruciblecraft:iron/plate",
                fileRecipe.getAsJsonObject("ingredients")
                        .getAsJsonObject("P").get("item").getAsString());
        assertEquals(
                "cruciblecraft:flint_knife",
                fileRecipe.getAsJsonObject("catalysts")
                        .getAsJsonObject("k").get("item").getAsString());
        var fileResult = fileRecipe.getAsJsonObject("result");
        assertEquals("cruciblecraft:iron/tool_head_file", fileResult.get("id").getAsString());
        assertTrue(!fileResult.has("components"));
    }

    @Test
    void hammerRecipesPersistMaterialIdentityOnly() throws Exception {
        Map<String, String> headRecipes = Map.of(
                "tools/iron/tool_head_hammer.json", "cruciblecraft:iron/tool_head_hammer",
                "tools/bronze/tool_head_hammer.json", "cruciblecraft:bronze/tool_head_hammer",
                "tools/steel/tool_head_hammer.json", "cruciblecraft:steel/tool_head_hammer");

        for (var entry : headRecipes.entrySet()) {
            var recipe = JsonParser.parseString(Files.readString(GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + entry.getKey()))).getAsJsonObject();
            assertEquals(
                    "cruciblecraft:shaped_catalyst",
                    recipe.get("type").getAsString(),
                    entry.getKey());
            assertEquals(
                    List.of("II ", "IIh", "II "),
                    recipe.getAsJsonArray("pattern").asList().stream()
                            .map(value -> value.getAsString()).toList(),
                    entry.getKey());
            assertEquals(
                    "cruciblecraft:smithing_hammer",
                    recipe.getAsJsonObject("catalysts")
                            .getAsJsonObject("h").get("item").getAsString(),
                    entry.getKey());
            var result = recipe.getAsJsonObject("result");
            assertEquals(entry.getValue(), result.get("id").getAsString(), entry.getKey());
            assertTrue(
                    !result.has("components"),
                    entry.getKey() + " head must not persist tool_material");
        }
        var stone = JsonParser.parseString(Files.readString(GENERATED.resolve(
                "data/cruciblecraft/recipe/tools/stone/smithing_hammer.json")))
                .getAsJsonObject();
        assertEquals("cruciblecraft:shaped_catalyst", stone.get("type").getAsString());
        assertEquals(
                List.of("XX ", "XXS", "XX "),
                stone.getAsJsonArray("pattern").asList().stream()
                        .map(value -> value.getAsString()).toList());
        assertTrue(
                !stone.has("catalysts")
                        || stone.getAsJsonObject("catalysts").isEmpty());
        assertEquals(
                "stone",
                stone.getAsJsonObject("result")
                        .getAsJsonObject("components")
                        .get("cruciblecraft:tool_material")
                        .getAsString());
    }

    @Test
    void remainingWorkbenchToolsKeepGt6Shapes() throws Exception {
        assertShapedCatalyst(
                "tools/iron/wrench.json",
                List.of("PhP", " P ", " P "),
                Map.of("P", "cruciblecraft:iron/plate"),
                Map.of("h", "cruciblecraft:smithing_hammer"),
                "cruciblecraft:material_wrench",
                "iron");
        assertShapedHead(
                "tools/iron/tool_head_screwdriver.json",
                List.of("hS ", "Sf ", "   "),
                Map.of("S", "cruciblecraft:iron/rod"),
                Map.of(
                        "h", "cruciblecraft:smithing_hammer",
                        "f", "cruciblecraft:material_file"),
                "cruciblecraft:iron/tool_head_screwdriver");
        assertShapedHead(
                "tools/iron/tool_head_saw.json",
                List.of("PP ", "fh ", "   "),
                Map.of("P", "cruciblecraft:iron/plate"),
                Map.of(
                        "f", "cruciblecraft:material_file",
                        "h", "cruciblecraft:smithing_hammer"),
                "cruciblecraft:iron/tool_head_saw");
        assertShapedHead(
                "tools/iron/tool_head_chisel.json",
                List.of("hPf", " S ", "   "),
                Map.of(
                        "P", "cruciblecraft:iron/plate",
                        "S", "cruciblecraft:iron/rod"),
                Map.of(
                        "h", "cruciblecraft:smithing_hammer",
                        "f", "cruciblecraft:material_file"),
                "cruciblecraft:iron/tool_head_chisel");
        assertShapedHead(
                "tools/iron/tool_head_pickaxe.json",
                List.of("PII", "f h", "   "),
                Map.of(
                        "P", "cruciblecraft:iron/plate",
                        "I", "minecraft:iron_ingot"),
                Map.of(
                        "f", "cruciblecraft:material_file",
                        "h", "cruciblecraft:smithing_hammer"),
                "cruciblecraft:iron/tool_head_pickaxe");
        assertShapedCatalyst(
                "tools/iron/wire_cutter.json",
                List.of("PfP", "hPd", "STS"),
                Map.of(
                        "P", "cruciblecraft:iron/plate",
                        "S", "cruciblecraft:iron/rod",
                        "T", "cruciblecraft:iron/screw"),
                Map.of(
                        "f", "cruciblecraft:material_file",
                        "h", "cruciblecraft:smithing_hammer",
                        "d", "cruciblecraft:material_screwdriver"),
                "cruciblecraft:material_wire_cutter",
                "iron");
        assertShapedCatalyst(
                "tools/iron/monkey_wrench.json",
                List.of("PPd", "hPT", " P "),
                Map.of(
                        "P", "cruciblecraft:iron/plate",
                        "T", "cruciblecraft:iron/screw"),
                Map.of(
                        "d", "cruciblecraft:material_screwdriver",
                        "h", "cruciblecraft:smithing_hammer"),
                "cruciblecraft:material_monkey_wrench",
                "iron");
        assertShapedCatalyst(
                "tools/iron/knife.json",
                List.of("fP ", "hH ", "   "),
                Map.of(
                        "P", "cruciblecraft:iron/plate",
                        "H", "minecraft:stick"),
                Map.of(
                        "f", "cruciblecraft:material_file",
                        "h", "cruciblecraft:smithing_hammer"),
                "cruciblecraft:material_knife",
                "iron");
        assertShapedCatalyst(
                "tools/iron/club.json",
                List.of(" II", "III", "HI "),
                Map.of(
                        "I", "minecraft:iron_ingot",
                        "H", "minecraft:stick"),
                Map.of(),
                "cruciblecraft:material_club",
                "iron");
        assertShapedCatalyst(
                "tools/iron/crowbar.json",
                List.of("hVS", "VSV", "SVf"),
                Map.of(
                        "S", "cruciblecraft:iron/rod",
                        "V", "minecraft:blue_dye"),
                Map.of(
                        "h", "cruciblecraft:smithing_hammer",
                        "f", "cruciblecraft:material_file"),
                "cruciblecraft:material_crowbar",
                "iron");
        assertShapedCatalyst(
                "tools/iron/plunger.json",
                List.of("xVV", " SV", "S f"),
                Map.of(
                        "S", "cruciblecraft:iron/rod",
                        "V", "cruciblecraft:rubber/plate"),
                Map.of(
                        "x", "cruciblecraft:material_wire_cutter",
                        "f", "cruciblecraft:material_file"),
                "cruciblecraft:material_plunger",
                "iron");
        assertShapedHead(
                "tools/bronze/tool_head_file.json",
                List.of(" P ", " Pk", "   "),
                Map.of("P", "cruciblecraft:bronze/plate"),
                Map.of("k", "cruciblecraft:flint_knife"),
                "cruciblecraft:bronze/tool_head_file");
        var assemble = JsonParser.parseString(Files.readString(GENERATED.resolve(
                "data/cruciblecraft/recipe/tools/assemble/pickaxe.json")))
                .getAsJsonObject();
        assertEquals("cruciblecraft:tool_head_assembly", assemble.get("type").getAsString());
        assertEquals("tool_head_pickaxe", assemble.get("head_prefix").getAsString());
        assertEquals(
                "cruciblecraft:material_pickaxe",
                assemble.get("result").getAsString());
        assertShapedCatalyst(
                "tools/flint_pickaxe.json",
                List.of("XXX", " S ", "   "),
                Map.of(
                        "X", "minecraft:flint",
                        "S", "minecraft:stick"),
                Map.of(),
                "cruciblecraft:material_pickaxe",
                "flint");
        assertShapedCatalyst(
                "tools/stone/hoe.json",
                List.of("XX ", " S ", "   "),
                Map.of(
                        "X", "cruciblecraft:stone/rock",
                        "S", "minecraft:stick"),
                Map.of(),
                "cruciblecraft:material_hoe",
                "stone");
        assertShapedCatalyst(
                "tools/granite/pickaxe.json",
                List.of("XXX", " S ", "   "),
                Map.of(
                        "X", "cruciblecraft:granite/rock",
                        "S", "minecraft:stick"),
                Map.of(),
                "cruciblecraft:material_pickaxe",
                "granite");
        for (String material : List.of("iron", "bronze", "steel")) {
            for (String tool : List.of(
                    "wrench",
                    "tool_head_screwdriver",
                    "tool_head_saw",
                    "tool_head_chisel",
                    "wire_cutter",
                    "monkey_wrench",
                    "tool_head_pickaxe")) {
                assertTrue(Files.isRegularFile(GENERATED.resolve(
                        "data/cruciblecraft/recipe/tools/"
                                + material + "/" + tool + ".json")),
                        material + " " + tool);
            }
        }
    }

    private static void assertShapedCatalyst(
            String relative,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId,
            String material) throws Exception {
        var recipe = JsonParser.parseString(Files.readString(
                GENERATED.resolve("data/cruciblecraft/recipe/" + relative)))
                .getAsJsonObject();
        assertEquals("cruciblecraft:shaped_catalyst", recipe.get("type").getAsString(), relative);
        assertEquals(
                pattern,
                recipe.getAsJsonArray("pattern").asList().stream()
                        .map(value -> value.getAsString()).toList(),
                relative);
        Map<String, String> actualIngredients = new java.util.LinkedHashMap<>();
        recipe.getAsJsonObject("ingredients").entrySet().forEach(entry ->
                actualIngredients.put(
                        entry.getKey(),
                        entry.getValue().getAsJsonObject().get("item").getAsString()));
        assertEquals(ingredients, actualIngredients, relative);
        if (catalysts.isEmpty()) {
            assertTrue(
                    !recipe.has("catalysts")
                            || recipe.getAsJsonObject("catalysts").isEmpty(),
                    relative);
        } else {
            Map<String, String> actualCatalysts = new java.util.LinkedHashMap<>();
            recipe.getAsJsonObject("catalysts").entrySet().forEach(entry ->
                    actualCatalysts.put(
                            entry.getKey(),
                            entry.getValue().getAsJsonObject().get("item").getAsString()));
            assertEquals(catalysts, actualCatalysts, relative);
        }
        var result = recipe.getAsJsonObject("result");
        assertEquals(resultId, result.get("id").getAsString(), relative);
        if (result.has("components")) {
            assertEquals(
                    material,
                    result.getAsJsonObject("components")
                            .get("cruciblecraft:tool_material")
                            .getAsString(),
                    relative);
        } else {
            assertEquals(
                    "iron",
                    material,
                    relative + " relies on MaterialToolItem's iron default");
        }
    }

    private static void assertShapedHead(
            String relative,
            List<String> pattern,
            Map<String, String> ingredients,
            Map<String, String> catalysts,
            String resultId) throws Exception {
        var recipe = JsonParser.parseString(Files.readString(
                GENERATED.resolve("data/cruciblecraft/recipe/" + relative)))
                .getAsJsonObject();
        assertEquals("cruciblecraft:shaped_catalyst", recipe.get("type").getAsString(), relative);
        assertEquals(
                pattern,
                recipe.getAsJsonArray("pattern").asList().stream()
                        .map(value -> value.getAsString()).toList(),
                relative);
        Map<String, String> actualIngredients = new java.util.LinkedHashMap<>();
        recipe.getAsJsonObject("ingredients").entrySet().forEach(entry ->
                actualIngredients.put(
                        entry.getKey(),
                        entry.getValue().getAsJsonObject().get("item").getAsString()));
        assertEquals(ingredients, actualIngredients, relative);
        Map<String, String> actualCatalysts = new java.util.LinkedHashMap<>();
        recipe.getAsJsonObject("catalysts").entrySet().forEach(entry ->
                actualCatalysts.put(
                        entry.getKey(),
                        entry.getValue().getAsJsonObject().get("item").getAsString()));
        assertEquals(catalysts, actualCatalysts, relative);
        var result = recipe.getAsJsonObject("result");
        assertEquals(resultId, result.get("id").getAsString(), relative);
        assertTrue(!result.has("components"), relative);
    }

    private static List<String> values(Path rootPath, String path) throws Exception {
        var root = JsonParser.parseString(
                Files.readString(rootPath.resolve(path))).getAsJsonObject();
        return root.getAsJsonArray("values").asList().stream()
                .map(value -> value.getAsString()).toList();
    }
}
