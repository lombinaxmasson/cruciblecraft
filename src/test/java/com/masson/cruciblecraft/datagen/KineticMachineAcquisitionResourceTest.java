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

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class KineticMachineAcquisitionResourceTest {
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
    void independentFifteenRecipeSetIsBidirectionalWithSelectedVariants() {
        List<RecipeSpec> expected = recipes();
        Set<String> expectedResults = expected.stream()
                .map(RecipeSpec::result)
                .collect(Collectors.toSet());
        Set<String> selected = ModMachineVariants.SELECTED_KINETIC_VARIANTS.stream()
                .map(variant -> variant.id().toString())
                .collect(Collectors.toSet());

        assertEquals(15, expected.size());
        assertEquals(15, selected.size());
        assertEquals(selected, expectedResults);
        expected.forEach(KineticMachineAcquisitionResourceTest::assertRecipe);
    }

    @Test
    void selectedModelsLanguagesLootAndTagsAreCompleteWithoutFallbacks()
            throws Exception {
        Set<String> selected = ModMachineVariants.SELECTED_KINETIC_VARIANTS.stream()
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
            String textureKind = kind(id);
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
                machine("lathe", "bronze",
                        "bronze/machine_casing_double",
                        ModItems.LATHE.getId().toString()),
                machine("steel_lathe", "steel",
                        "steel/machine_casing_double",
                        ModItems.STEEL_LATHE.getId().toString()),
                machine("titanium_lathe", "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_LATHE.getId().toString()),
                machine("rollingmill", "bronze",
                        "bronze/machine_casing_double",
                        ModItems.ROLLINGMILL.getId().toString()),
                machine("steel_rollingmill", "steel",
                        "steel/machine_casing_double",
                        ModItems.STEEL_ROLLINGMILL.getId().toString()),
                machine("titanium_rollingmill", "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_ROLLINGMILL.getId().toString()),
                machine("wiremill", "bronze",
                        "bronze/machine_casing_double",
                        ModItems.WIREMILL.getId().toString()),
                machine("steel_wiremill", "steel",
                        "steel/machine_casing_double",
                        ModItems.STEEL_WIREMILL.getId().toString()),
                machine("titanium_wiremill", "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_WIREMILL.getId().toString()),
                machine("shredder", "bronze",
                        "bronze/machine_casing_double",
                        ModItems.SHREDDER.getId().toString()),
                machine("steel_shredder", "steel",
                        "steel/machine_casing_double",
                        ModItems.STEEL_SHREDDER.getId().toString()),
                machine("titanium_shredder", "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_SHREDDER.getId().toString()),
                machine("press", "bronze",
                        "bronze/machine_casing_double",
                        ModItems.PRESS.getId().toString()),
                machine("steel_press", "steel",
                        "steel/machine_casing_double",
                        ModItems.STEEL_PRESS.getId().toString()),
                machine("titanium_press", "titanium",
                        "titanium/machine_casing_double",
                        ModItems.TITANIUM_PRESS.getId().toString()));
    }

    private static RecipeSpec machine(
            String id,
            String material,
            String casing,
            String registration) {
        Map<String, String> key = new LinkedHashMap<>();
        key.put("C", "cruciblecraft:" + casing);
        key.put("G", "cruciblecraft:" + material + "/gear");
        List<String> pattern;
        switch (kind(id)) {
            case "lathe" -> {
                pattern = List.of("TDS", " CG");
                key.put("D", "cruciblecraft:diamond/gem");
                key.put("S", "cruciblecraft:" + material + "/small_gear");
                key.put("T", "cruciblecraft:" + material + "/screw");
            }
            case "rollingmill" -> pattern = List.of("G ", "C ", "G ");
            case "wiremill" -> {
                pattern = List.of("SGS", " C ");
                key.put("S", "cruciblecraft:" + material + "/small_gear");
            }
            case "shredder" -> {
                pattern = List.of("GDG", " C ");
                key.put("D", "cruciblecraft:diamond/gem");
            }
            case "press" -> {
                pattern = List.of("RS", "PC", "P ");
                key.remove("G");
                key.put("P", "cruciblecraft:" + material + "/double_plate");
                key.put("R", "cruciblecraft:" + material + "/rod");
                key.put("S", "cruciblecraft:" + material + "/spring");
            }
            default -> throw new IllegalArgumentException(id);
        }
        return new RecipeSpec(
                "machines/" + id,
                "cruciblecraft:" + id,
                "cruciblecraft:" + casing,
                registration,
                pattern,
                Map.copyOf(key));
    }

    private static void assertRecipe(RecipeSpec expected) {
        try {
            var recipe = JsonParser.parseString(Files.readString(
                    RECIPES.resolve(expected.path() + ".json"))).getAsJsonObject();
            assertEquals("minecraft:crafting_shaped",
                    recipe.get("type").getAsString(), expected.path());
            assertEquals(expected.pattern(), recipe.getAsJsonArray("pattern")
                    .asList().stream().map(value -> value.getAsString()).toList());
            Map<String, String> key = new LinkedHashMap<>();
            recipe.getAsJsonObject("key").entrySet().forEach(entry ->
                    key.put(entry.getKey(), entry.getValue().getAsJsonObject()
                            .get("item").getAsString()));
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

    private static com.google.gson.JsonObject document(String path)
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
        return id.replaceFirst("^(steel|titanium)_", "");
    }

    private static String englishName(String id) {
        String material = id.startsWith("steel_")
                ? "Steel"
                : id.startsWith("titanium_") ? "Titanium" : "Bronze";
        return material + " " + switch (kind(id)) {
            case "lathe" -> "Lathe";
            case "rollingmill" -> "Rolling Mill";
            case "wiremill" -> "Wire Mill";
            case "shredder" -> "Shredder";
            case "press" -> "Press";
            default -> throw new IllegalArgumentException(id);
        };
    }

    private static String chineseName(String id) {
        String material = id.startsWith("steel_")
                ? "钢制"
                : id.startsWith("titanium_") ? "钛制" : "青铜";
        return material + switch (kind(id)) {
            case "lathe" -> "车床";
            case "rollingmill" -> "轧机";
            case "wiremill" -> "线材轧机";
            case "shredder" -> "粉碎机";
            case "press" -> "压机";
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
