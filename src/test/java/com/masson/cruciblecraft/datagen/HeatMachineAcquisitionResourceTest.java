package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HeatMachineAcquisitionResourceTest {
    private static final Path GENERATED = Path.of("src/generated/resources");
    private static final Path RECIPES = GENERATED.resolve(
            "data/cruciblecraft/recipe");

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void independentNineRecipeSetIsBidirectionalWithSelectedHeatVariants() {
        List<RecipeSpec> expected = recipes();
        Set<String> expectedResults = expected.stream()
                .map(RecipeSpec::result)
                .collect(Collectors.toSet());
        Set<String> selected = ModMachineVariants.SELECTED_HEAT_VARIANTS.stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toSet());

        assertEquals(9, expected.size());
        assertEquals(9, selected.size());
        assertEquals(selected, expectedResults);
        expected.forEach(HeatMachineAcquisitionResourceTest::assertRecipe);
    }

    @Test
    void selectedModelsLanguagesLootAndTagsUseRealNamesAndTextures()
            throws Exception {
        Set<String> selected = ModMachineVariants.SELECTED_HEAT_VARIANTS.stream()
                .map(variant -> variant.id().getPath())
                .collect(Collectors.toSet());
        var english = document("assets/cruciblecraft/lang/en_us.json");
        var chinese = document("assets/cruciblecraft/lang/zh_cn.json");
        Set<String> pickaxe = values(
                "data/minecraft/tags/block/mineable/pickaxe.json");
        Set<String> stone = values(
                "data/minecraft/tags/block/needs_stone_tool.json");

        for (String id : selected) {
            String key = "block.cruciblecraft." + id;
            assertEquals(englishName(id), english.get(key).getAsString(), id);
            assertEquals(chineseName(id), chinese.get(key).getAsString(), id);
            assertTrue(pickaxe.contains("cruciblecraft:" + id), id);
            assertTrue(stone.contains("cruciblecraft:" + id), id);
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "data/cruciblecraft/loot_table/blocks/" + id + ".json")), id);
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "assets/cruciblecraft/blockstates/" + id + ".json")), id);
            assertTrue(Files.isRegularFile(GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + id + ".json")), id);

            var model = document(
                    "assets/cruciblecraft/models/block/" + id + ".json");
            assertEquals(
                    "cruciblecraft:block/machine_cube_2_layer",
                    model.get("parent").getAsString(),
                    id);
            String textureKind = textureKind(id);
            model.getAsJsonObject("textures").entrySet().forEach(entry ->
                    assertTrue(
                            entry.getValue().getAsString().contains(
                                    "cruciblecraft:block/machine/"
                                            + textureKind + "/"),
                            id + " must reuse the real " + textureKind
                                    + " texture set"));
        }
    }

    private static List<RecipeSpec> recipes() {
        return List.of(
                distillery(
                        "distillery", "steel",
                        "#c:double_wires/constantan",
                        "steel/machine_casing_double",
                        ModItems.DISTILLERY.getId().toString()),
                distillery(
                        "invar_distillery", "invar",
                        "#c:quadruple_wires/kanthal",
                        "steel/machine_casing_double",
                        ModItems.INVAR_DISTILLERY.getId().toString()),
                distillery(
                        "titanium_distillery", "titanium",
                        "#c:octuple_wires/nichrome",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_DISTILLERY.getId().toString()),
                heatBody(
                        "drying", "steel",
                        "steel/machine_casing_double",
                        ModItems.DRYING.getId().toString()),
                heatBody(
                        "invar_drying", "invar",
                        "steel/machine_casing_double",
                        ModItems.INVAR_DRYING.getId().toString()),
                heatBody(
                        "titanium_drying", "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_DRYING.getId().toString()),
                heatBody(
                        "smelter", "steel",
                        "steel/machine_casing_double",
                        ModItems.SMELTER.getId().toString()),
                heatBody(
                        "invar_smelter", "invar",
                        "steel/machine_casing_double",
                        ModItems.INVAR_SMELTER.getId().toString()),
                heatBody(
                        "titanium_smelter", "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_SMELTER.getId().toString()));
    }

    private static RecipeSpec distillery(
            String id,
            String material,
            String wire,
            String casing,
            String registration) {
        return recipe(
                id,
                casing,
                registration,
                List.of("GPG", "WMW", " C "),
                Map.of(
                        "C", "cruciblecraft:copper/double_plate",
                        "G", "minecraft:glass",
                        "M", "cruciblecraft:" + casing,
                        "P", "cruciblecraft:" + material + "/plate",
                        "W", wire));
    }

    private static RecipeSpec heatBody(
            String id,
            String material,
            String casing,
            String registration) {
        boolean smelter = kind(id).equals("smelter");
        Map<String, String> key = new LinkedHashMap<>();
        key.put("B", "minecraft:bricks");
        key.put("C", "cruciblecraft:copper/double_plate");
        key.put("M", "cruciblecraft:" + casing);
        key.put("P", "cruciblecraft:" + material + "/plate");
        if (smelter) {
            key.put("U", "#cruciblecraft:smelting_crucibles");
        }
        return recipe(
                id,
                casing,
                registration,
                smelter
                        ? List.of(" U ", "PMP", "BCB")
                        : List.of(" P ", "BMB", "BCB"),
                Map.copyOf(key));
    }

    private static RecipeSpec recipe(
            String id,
            String casing,
            String registration,
            List<String> pattern,
            Map<String, String> key) {
        return new RecipeSpec(
                "machines/" + id,
                "cruciblecraft:" + id,
                "cruciblecraft:" + casing,
                registration,
                pattern,
                key);
    }

    private static void assertRecipe(RecipeSpec expected) {
        try {
            var recipe = JsonParser.parseString(Files.readString(
                    RECIPES.resolve(expected.path() + ".json")))
                    .getAsJsonObject();
            assertEquals("minecraft:crafting_shaped",
                    recipe.get("type").getAsString(), expected.path());
            assertEquals(expected.pattern(), recipe.getAsJsonArray("pattern")
                    .asList().stream().map(value -> value.getAsString()).toList());
            Map<String, String> key = new LinkedHashMap<>();
            recipe.getAsJsonObject("key").entrySet().forEach(entry ->
                    key.put(entry.getKey(), slotId(entry.getValue().getAsJsonObject())));
            assertEquals(expected.key(), key, expected.path());
            assertEquals(expected.result(),
                    recipe.getAsJsonObject("result").get("id").getAsString(),
                    expected.path());
            assertEquals(expected.result(), expected.registration(), expected.path());
            assertEquals(
                    1L,
                    expected.key().values().stream()
                            .filter(expected.casing()::equals)
                            .count(),
                    expected.path());
        } catch (Exception error) {
            throw new AssertionError(expected.path(), error);
        }
    }

    private static String slotId(JsonObject slot) {
        if (slot.has("tag")) {
            return "#" + slot.get("tag").getAsString();
        }
        if (slot.has("item")) {
            return slot.get("item").getAsString();
        }
        if (slot.has("items") && slot.has("components")) {
            var items = slot.get("items");
            String item = items.isJsonArray()
                    ? items.getAsJsonArray().get(0).getAsString()
                    : items.getAsString();
            var components = slot.getAsJsonObject("components");
            if (components.has("cruciblecraft:prefix_material")) {
                String material = components
                        .get("cruciblecraft:prefix_material")
                        .getAsString();
                String prefix = item.substring(item.indexOf(':') + 1);
                return "cruciblecraft:" + material + "/" + prefix;
            }
            return item;
        }
        throw new IllegalStateException("unsupported recipe slot " + slot);
    }

    private static JsonObject document(String path)
            throws Exception {
        return JsonParser.parseString(
                Files.readString(GENERATED.resolve(path))).getAsJsonObject();
    }

    private static Set<String> values(String path) throws Exception {
        return document(path).getAsJsonArray("values").asList().stream()
                .map(value -> value.getAsString())
                .collect(Collectors.toSet());
    }

    private static String kind(String id) {
        return id.replaceFirst("^(invar|titanium)_", "");
    }

    private static String textureKind(String id) {
        return kind(id).equals("drying") ? "dryer" : kind(id);
    }

    private static String englishName(String id) {
        String material = id.startsWith("invar_")
                ? "Invar"
                : id.startsWith("titanium_") ? "Titanium" : "Steel";
        return material + " " + switch (kind(id)) {
            case "distillery" -> "Distillery";
            case "drying" -> "Drying Machine";
            case "smelter" -> "Smelter";
            default -> throw new IllegalArgumentException(id);
        };
    }

    private static String chineseName(String id) {
        String material = id.startsWith("invar_")
                ? "殷钢"
                : id.startsWith("titanium_") ? "钛制" : "钢制";
        return material + switch (kind(id)) {
            case "distillery" -> "蒸馏机";
            case "drying" -> "干燥机";
            case "smelter" -> "熔炼炉";
            default -> throw new IllegalArgumentException(id);
        };
    }

    private record RecipeSpec(
            String path,
            String result,
            String casing,
            String registration,
            List<String> pattern,
            Map<String, String> key) {}
}
