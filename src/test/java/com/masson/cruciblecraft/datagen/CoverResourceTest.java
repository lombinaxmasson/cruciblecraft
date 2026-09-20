package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class CoverResourceTest {
    private static final Path GENERATED =
            Path.of("src/generated/resources");
    private static final Map<String, String> CHINESE_COVER_NAMES = Map.of(
            "conveyor_cover", "传送带盖板",
            "retriever_item_cover", "物品抽取覆盖板",
            "robot_arm_cover", "机械臂",
            "pressure_valve_cover", "释压安全阀",
            "selector_manual_cover", "手动选择面板",
            "pipe_filter_cover", "物品过滤覆盖板",
            "pipe_valve_cover", "管道封闭覆盖板",
            "pipe_pump_cover", "管道输出泵盖板");

    @Test
    void selectedFiveHaveRecipeModelAndBothLanguages() throws Exception {
        JsonObject english = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/zh_cn.json"));
        for (var entry : CHINESE_COVER_NAMES.entrySet()) {
            String item = entry.getKey();
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + item + ".json");
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            assertTrue(Files.isRegularFile(recipe), recipe.toString());
            assertTrue(Files.isRegularFile(model), model.toString());
            assertEquals(
                    "cruciblecraft:" + item,
                    json(recipe).getAsJsonObject("result")
                            .get("id").getAsString());
            assertTrue(english.has("item.cruciblecraft." + item));
            assertEquals(
                    entry.getValue(),
                    chinese.get("item.cruciblecraft." + item).getAsString());
        }
        for (String key : Set.of(
                "jade.cruciblecraft.pipe_covers",
                "tooltip.cruciblecraft.cover.behavior",
                "tooltip.cruciblecraft.cover.parameters",
                "tooltip.cruciblecraft.cover.interval",
                "tooltip.cruciblecraft.cover.retriever",
                "tooltip.cruciblecraft.cover.display_cpu")) {
            assertTrue(english.has(key));
            assertTrue(chinese.has(key));
        }
        assertFalse(chinese.get("jade.cruciblecraft.pipe_covers")
                .getAsString()
                .contains("Cover"));
        assertFalse(chinese.get("jade.cruciblecraft.item_pipe")
                .getAsString()
                .contains("Cover"));
        assertEquals(
                "离心机",
                chinese.get("block.cruciblecraft.centrifuge").getAsString());
        assertEquals(
                "筛选机",
                chinese.get("block.cruciblecraft.sifter").getAsString());
        assertEquals(
                "挤压机",
                chinese.get("block.cruciblecraft.extruder").getAsString());
        for (String value : CHINESE_COVER_NAMES.values()) {
            assertFalse(value.contains("Cover"));
        }
    }

    @Test
    void definitionCatalogAndSchemaExposeStrictBounds() throws Exception {
        Path catalog = Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "cover_definitions.json");
        Path schema = Path.of(
                "src/main/resources/data/cruciblecraft/schema/"
                        + "cover_definitions.schema.json");
        JsonObject catalogJson = json(catalog);
        assertEquals(9, catalogJson.getAsJsonArray("definitions").size());
        catalogJson.getAsJsonArray("definitions").forEach(row -> {
            JsonObject object = row.getAsJsonObject();
            assertFalse(object.has("translationKey"));
            assertFalse(object.get("id").getAsString().contains("logistics_"));
        });
        Path sidecar = Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "item_network_cover_definitions.json");
        JsonObject sidecarJson = json(sidecar);
        assertEquals(3, sidecarJson.getAsJsonArray("definitions").size());
        sidecarJson.getAsJsonArray("definitions").forEach(row -> {
            JsonObject object = row.getAsJsonObject();
            assertFalse(object.has("translationKey"));
            assertTrue(object.get("id").getAsString()
                    .startsWith("cruciblecraft:logistics_item_"));
        });
        JsonObject schemaJson = json(schema);
        JsonObject definitionSchema = schemaJson.getAsJsonObject("$defs")
                .getAsJsonObject("definition");
        assertFalse(definitionSchema.getAsJsonObject("properties")
                .has("translationKey"));
        assertFalse(definitionSchema.getAsJsonArray("required").asList()
                .stream().anyMatch(field ->
                        field.getAsString().equals("translationKey")));
        String schemaText = Files.readString(schema);
        assertTrue(schemaText.contains("\"additionalProperties\": false"));
        assertTrue(schemaText.contains("\"maximum\": 65536000"));
        assertTrue(schemaText.contains("\"maximum\": 1000000"));
        assertTrue(schemaText.contains("\"maximum\": 64"));
        assertTrue(schemaText.contains("\"network_id\""));
    }

    @Test
    void itemNetworkCoversHaveRecipeModelAndBothLanguages() throws Exception {
        JsonObject english = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/zh_cn.json"));
        Map<String, String> names = Map.of(
                "logistics_item_storage_cover", "过滤物流存储总线(物品)",
                "logistics_item_import_cover", "过滤物流输入总线(物品)",
                "logistics_item_export_cover", "过滤物流输出总线(物品)");
        for (var entry : names.entrySet()) {
            String item = entry.getKey();
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + item + ".json");
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            assertTrue(Files.isRegularFile(recipe), recipe.toString());
            assertTrue(Files.isRegularFile(model), model.toString());
            assertEquals(
                    "cruciblecraft:" + item,
                    json(recipe).getAsJsonObject("result")
                            .get("id").getAsString());
            assertTrue(english.has("item.cruciblecraft." + item));
            assertEquals(
                    entry.getValue(),
                    chinese.get("item.cruciblecraft." + item).getAsString());
        }
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_item_network_core";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }

    @Test
    void fluidNetworkCoversHaveRecipeModelAndBothLanguages() throws Exception {
        JsonObject english = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/zh_cn.json"));
        Map<String, String> names = Map.of(
                "logistics_fluid_storage_cover", "过滤物流存储总线(流体)",
                "logistics_fluid_import_cover", "过滤物流输入总线(流体)",
                "logistics_fluid_export_cover", "过滤物流输出总线(流体)");
        for (var entry : names.entrySet()) {
            String item = entry.getKey();
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + item + ".json");
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            assertTrue(Files.isRegularFile(recipe), recipe.toString());
            assertTrue(Files.isRegularFile(model), model.toString());
            assertEquals(
                    "cruciblecraft:" + item,
                    json(recipe).getAsJsonObject("result")
                            .get("id").getAsString());
            assertTrue(english.has("item.cruciblecraft." + item));
            assertEquals(
                    entry.getValue(),
                    chinese.get("item.cruciblecraft." + item).getAsString());
            assertFalse(entry.getValue().contains("Cover"));
        }
        Path sidecar = Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "fluid_network_cover_definitions.json");
        JsonObject sidecarJson = json(sidecar);
        assertEquals(3, sidecarJson.getAsJsonArray("definitions").size());
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_fluid_network_basic_transfer";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }

    @Test
    void genericNetworkCoversHaveRecipeModelAndBothLanguages() throws Exception {
        JsonObject english = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/zh_cn.json"));
        Map<String, String> names = Map.of(
                "logistics_generic_storage_cover", "通用物流存储总线",
                "logistics_generic_import_cover", "通用物流输入总线",
                "logistics_generic_export_cover", "通用物流输出总线");
        for (var entry : names.entrySet()) {
            String item = entry.getKey();
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + item + ".json");
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            assertTrue(Files.isRegularFile(recipe), recipe.toString());
            assertTrue(Files.isRegularFile(model), model.toString());
            assertEquals(
                    "cruciblecraft:" + item,
                    json(recipe).getAsJsonObject("result")
                            .get("id").getAsString());
            assertTrue(english.has("item.cruciblecraft." + item));
            assertEquals(
                    entry.getValue(),
                    chinese.get("item.cruciblecraft." + item).getAsString());
            assertFalse(entry.getValue().contains("Cover"));
        }
        Path sidecar = Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "generic_network_cover_definitions.json");
        JsonObject sidecarJson = json(sidecar);
        assertEquals(3, sidecarJson.getAsJsonArray("definitions").size());
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_generic_network_core";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }

    @Test
    void dumpCoverHasRecipeModelAndBothLanguages() throws Exception {
        JsonObject english = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/zh_cn.json"));
        String item = "logistics_generic_dump_cover";
        Path recipe = GENERATED.resolve(
                "data/cruciblecraft/recipe/" + item + ".json");
        Path model = GENERATED.resolve(
                "assets/cruciblecraft/models/item/" + item + ".json");
        assertTrue(Files.isRegularFile(recipe), recipe.toString());
        assertTrue(Files.isRegularFile(model), model.toString());
        assertEquals(
                "cruciblecraft:" + item,
                json(recipe).getAsJsonObject("result")
                        .get("id").getAsString());
        assertEquals(
                "Logistics Dump Bus (Item)",
                english.get("item.cruciblecraft." + item).getAsString());
        assertEquals(
                "物流回收总线(物品)",
                chinese.get("item.cruciblecraft." + item).getAsString());
        JsonObject dumpModel = json(model);
        assertEquals(
                "cruciblecraft:item/gt6_import/" + item,
                dumpModel.getAsJsonObject("textures")
                        .get("layer0")
                        .getAsString());
        Path sidecar = Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "logistics_dump_cover_definitions.json");
        JsonObject sidecarJson = json(sidecar);
        assertEquals(1, sidecarJson.getAsJsonArray("definitions").size());
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_logistics_core";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }

    @Test
    void compactElectricCoversHaveRecipeModelAndBothLanguages()
            throws Exception {
        JsonObject english = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve(
                        "assets/cruciblecraft/lang/zh_cn.json"));
        Path sidecar = Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "cover_component_tier_definitions.json");
        JsonObject sidecarJson = json(sidecar);
        assertEquals(
                30,
                sidecarJson.getAsJsonArray("definitions").size());
        for (String family : java.util.List.of(
                "compact_electric_conveyor",
                "compact_electric_robot_arm",
                "compact_electric_pump")) {
            for (String vn : java.util.List.of(
                    "ulv", "lv", "mv", "hv", "ev",
                    "iv", "luv", "zpm", "uv", "puv1")) {
                String item = family + "_" + vn;
                Path recipe = GENERATED.resolve(
                        "data/cruciblecraft/recipe/" + item + ".json");
                Path model = GENERATED.resolve(
                        "assets/cruciblecraft/models/item/" + item + ".json");
                assertTrue(Files.isRegularFile(recipe), recipe.toString());
                assertTrue(Files.isRegularFile(model), model.toString());
                assertEquals(
                        "cruciblecraft:" + item,
                        json(recipe).getAsJsonObject("result")
                                .get("id").getAsString());
                assertTrue(english.has("item.cruciblecraft." + item));
                assertTrue(chinese.has("item.cruciblecraft." + item));
                assertFalse(
                        chinese.get("item.cruciblecraft." + item)
                                .getAsString()
                                .contains("Cover"));
            }
        }
        JsonObject hvPump = null;
        for (com.google.gson.JsonElement element
                : sidecarJson.getAsJsonArray("definitions")) {
            JsonObject row = element.getAsJsonObject();
            if ("cruciblecraft:pump_hv".equals(
                    row.get("id").getAsString())) {
                hvPump = row;
                break;
            }
        }
        if (hvPump == null) {
            throw new AssertionError("missing pump_hv");
        }
        assertEquals(
                16_000,
                hvPump.getAsJsonObject("values").get("rate").getAsInt());
        assertEquals(
                20,
                hvPump.getAsJsonObject("values").get("interval").getAsInt());
    }

    @Test
    void logisticsCoversUseImportedGt6IconsNotBorrowedCovers() throws Exception {
        for (String item : java.util.List.of(
                "logistics_item_storage_cover",
                "logistics_item_import_cover",
                "logistics_item_export_cover",
                "logistics_fluid_storage_cover",
                "logistics_fluid_import_cover",
                "logistics_fluid_export_cover",
                "logistics_generic_storage_cover",
                "logistics_generic_import_cover",
                "logistics_generic_export_cover",
                "logistics_generic_dump_cover")) {
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            JsonObject textures = json(model).getAsJsonObject("textures");
            assertEquals(
                    "cruciblecraft:item/gt6_import/" + item,
                    textures.get("layer0").getAsString());
            assertTrue(Files.isRegularFile(Path.of(
                    "src/main/resources/assets/cruciblecraft/textures/item/"
                            + "gt6_import/" + item + ".png")),
                    item);
        }
        Path manifest = Path.of(
                "src/main/resources/assets/cruciblecraft/"
                        + "logistics_cover_art_manifest.json");
        JsonObject manifestJson = json(manifest);
        assertEquals(10, manifestJson.getAsJsonArray("imports").size());
        assertEquals(
                "gt6_referencable_port_code/gregtech6_w",
                manifestJson.get("source").getAsString());
    }

    @Test
    void pipeCoversUseImportedGt6IconsNotBareItemSheets() throws Exception {
        for (String item : java.util.List.of(
                "conveyor_cover",
                "robot_arm_cover",
                "retriever_item_cover",
                "pressure_valve_cover",
                "pipe_pump_cover")) {
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            JsonObject textures = json(model).getAsJsonObject("textures");
            assertEquals(
                    "cruciblecraft:item/gt6_import/" + item,
                    textures.get("layer0").getAsString());
            Path png = Path.of(
                    "src/main/resources/assets/cruciblecraft/textures/item/"
                            + "gt6_import/" + item + ".png");
            assertTrue(Files.isRegularFile(png), png.toString());
        }
        for (String item : java.util.List.of(
                "conveyor_cover", "robot_arm_cover", "pipe_pump_cover")) {
            Path mcmeta = Path.of(
                    "src/main/resources/assets/cruciblecraft/textures/item/"
                            + "gt6_import/" + item + ".png.mcmeta");
            assertTrue(Files.isRegularFile(mcmeta), mcmeta.toString());
        }
        Path pumpIcon = Path.of(
                "src/main/resources/assets/cruciblecraft/textures/item/"
                        + "gt6_import/compact_electric_pump.png");
        assertTrue(Files.isRegularFile(pumpIcon), pumpIcon.toString());
        Path pumpMcmeta = Path.of(
                "src/main/resources/assets/cruciblecraft/textures/item/"
                        + "gt6_import/compact_electric_pump.png.mcmeta");
        assertTrue(Files.isRegularFile(pumpMcmeta), pumpMcmeta.toString());
        for (String face : java.util.List.of(
                "base",
                "conveyor_in",
                "retriever_normal",
                "retriever_inverted",
                "pump_in")) {
            Path png = Path.of(
                    "src/main/resources/assets/cruciblecraft/textures/block/"
                            + "gt6_import/covers/" + face + ".png");
            assertTrue(Files.isRegularFile(png), png.toString());
        }
        Path manifest = Path.of(
                "src/main/resources/assets/cruciblecraft/"
                        + "pipe_cover_art_manifest.json");
        JsonObject manifestJson = json(manifest);
        assertEquals(20, manifestJson.getAsJsonArray("imports").size());
        assertEquals(
                "gt6_referencable_port_code/gregtech6_w",
                manifestJson.get("source").getAsString());
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(
                Files.readString(path)).getAsJsonObject();
    }
}
