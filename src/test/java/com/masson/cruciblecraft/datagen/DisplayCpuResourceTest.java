package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class DisplayCpuResourceTest {
    private static final Path ROOT = Path.of("src/main/resources");
    private static final Path GENERATED =
            Path.of("src/generated/resources");
    private static final Path MANIFEST = ROOT.resolve(
            "assets/cruciblecraft/display_cpu_art_manifest.json");
    private static final List<String> ITEMS = List.of(
            "logistics_display_cpu_logic_cover",
            "logistics_display_cpu_control_cover",
            "logistics_display_cpu_storage_cover",
            "logistics_display_cpu_conversion_cover");

    @Test
    void manifestCopiesLocalGt6WTextures() throws Exception {
        JsonObject manifest = json(MANIFEST);
        assertEquals(1, manifest.get("schema_version").getAsInt());
        assertEquals(
                "gt6_referencable_port_code/gregtech6_w",
                manifest.get("source").getAsString());
        JsonArray imports = manifest.getAsJsonArray("imports");
        assertEquals(52, imports.size());
        for (var element : imports) {
            JsonObject row = element.getAsJsonObject();
            String destination = row.get("destination").getAsString();
            String source = row.get("gt6_source").getAsString();
            assertTrue(destination.startsWith("assets/cruciblecraft/textures/"));
            assertTrue(destination.contains("/gt6_import/"));
            assertTrue(source.startsWith("assets/gregtech/textures/"));
            assertFalse(source.contains("github"));
            assertTrue(
                    Files.isRegularFile(ROOT.resolve(destination)),
                    destination);
        }
    }

    @Test
    void displayCoversHaveRecipeModelAndBothLanguages() throws Exception {
        JsonObject english = json(
                GENERATED.resolve("assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(
                GENERATED.resolve("assets/cruciblecraft/lang/zh_cn.json"));
        assertEquals(
                "Emits Redstone and Displays Status of Logistics Core.",
                english.get("tooltip.cruciblecraft.cover.display_cpu")
                        .getAsString());
        for (String item : ITEMS) {
            Path recipe = GENERATED.resolve(
                    "data/cruciblecraft/recipe/" + item + ".json");
            Path cycle = GENERATED.resolve(
                    "data/cruciblecraft/recipe/"
                            + item.replace("_cover", "_cycle_cover")
                            + ".json");
            Path model = GENERATED.resolve(
                    "assets/cruciblecraft/models/item/" + item + ".json");
            assertTrue(Files.isRegularFile(recipe), recipe.toString());
            assertTrue(Files.isRegularFile(cycle), cycle.toString());
            assertTrue(Files.isRegularFile(model), model.toString());
            assertEquals(
                    "cruciblecraft:" + item,
                    json(recipe).getAsJsonObject("result")
                            .get("id").getAsString());
            assertEquals(
                    switch (item) {
                        case "logistics_display_cpu_logic_cover" ->
                                "Logistics Display (CPU Logic)";
                        case "logistics_display_cpu_control_cover" ->
                                "Logistics Display (CPU Control)";
                        case "logistics_display_cpu_storage_cover" ->
                                "Logistics Display (CPU Storage)";
                        case "logistics_display_cpu_conversion_cover" ->
                                "Logistics Display (CPU Conversion)";
                        default -> throw new IllegalStateException(item);
                    },
                    english.get("item.cruciblecraft." + item).getAsString());
            String zh = chinese.get("item.cruciblecraft." + item)
                    .getAsString();
            assertEquals(
                    switch (item) {
                        case "logistics_display_cpu_logic_cover" ->
                                "物流监视器(逻辑处理器)";
                        case "logistics_display_cpu_control_cover" ->
                                "物流监视器(控制处理器)";
                        case "logistics_display_cpu_storage_cover" ->
                                "物流监视器(存储处理器)";
                        case "logistics_display_cpu_conversion_cover" ->
                                "物流监视器(转换处理器)";
                        default -> throw new IllegalStateException(item);
                    },
                    zh);
            assertFalse(zh.contains("Cover"));
            assertEquals(
                    "cruciblecraft:item/gt6_import/" + item,
                    json(model).getAsJsonObject("textures")
                            .get("layer0")
                            .getAsString());
        }
        Path sidecar = Path.of(
                "src/main/resources/data/cruciblecraft/"
                        + "logistics_display_cpu_cover_definitions.json");
        JsonObject sidecarJson = json(sidecar);
        assertEquals(4, sidecarJson.getAsJsonArray("definitions").size());
        String ns = "src/main/resources/data/"
                + "cruciblecraft_wave_runtime_display_cpu";
        assertTrue(Files.isRegularFile(Path.of(ns + "/structure/empty.nbt")));
        assertTrue(Files.isRegularFile(Path.of(
                ns + "/gametest/structure/empty.nbt")));
    }

    private static JsonObject json(Path path) throws Exception {
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }
}
