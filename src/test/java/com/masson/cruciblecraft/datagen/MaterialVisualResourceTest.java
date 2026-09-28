package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.MaterialCreativeTab;

class MaterialVisualResourceTest {
    private static final long EXPECTED_STORAGE_BLOCKS = 495L;

    @Test
    void placeableStorageBlocksUseSharedTintedCube(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        long storage = materials.stream()
                .filter(material -> registered.get(material.id())
                        .contains(MaterialPrefixes.BLOCK))
                .filter(material ->
                        !material.formItems().containsKey(MaterialPrefixes.BLOCK))
                .count();
        assertEquals(EXPECTED_STORAGE_BLOCKS, storage);

        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/blockstates/aluminium/block.json"));
        JsonObject aluminiumState = json(clientFiles.get(
                "assets/cruciblecraft/blockstates/aluminium/block.json"));
        assertEquals(
                "cruciblecraft:block/material_storage",
                aluminiumState.getAsJsonObject("variants")
                        .getAsJsonObject("")
                        .get("model")
                        .getAsString());
        JsonObject aluminiumItem = json(clientFiles.get(
                "assets/cruciblecraft/models/item/aluminium/block.json"));
        assertEquals(
                "cruciblecraft:block/material_storage",
                aluminiumItem.get("parent").getAsString());
        assertTrue(serverFiles.containsKey(
                "data/cruciblecraft/loot_table/blocks/aluminium/block.json"));
        assertTrue(serverFiles.containsKey(
                "data/c/tags/block/storage_blocks/aluminium.json"));
        assertTrue(serverFiles.containsKey(
                "data/c/tags/item/storage_blocks/aluminium.json"));
        assertTrue(json(clientFiles.get("assets/cruciblecraft/lang/en_us.json"))
                .get("block.cruciblecraft.aluminium.block")
                .getAsString()
                .equals("Aluminium Block"));
        assertTrue(json(clientFiles.get("assets/cruciblecraft/lang/zh_cn.json"))
                .get("block.cruciblecraft.aluminium.block")
                .getAsString()
                .equals("铝块"));

        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/blockstates/copper/block.json"));
        assertTrue(serverFiles.containsKey(
                "data/cruciblecraft/loot_table/blocks/copper/block.json"));
    }

    @Test
    void placeableMachineCasingsUseSharedTintedCubes(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        long casings = materials.stream()
                .filter(material -> registered.get(material.id())
                        .contains(MaterialPrefixes.MACHINE_CASING))
                .filter(material -> !material.formItems().containsKey(
                        MaterialPrefixes.MACHINE_CASING))
                .count();
        assertEquals(212, casings);

        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/blockstates/aluminium/machine_casing.json"));
        JsonObject aluminiumState = json(clientFiles.get(
                "assets/cruciblecraft/blockstates/aluminium/machine_casing.json"));
        assertEquals(
                "cruciblecraft:block/machine_casing",
                aluminiumState.getAsJsonObject("variants")
                        .getAsJsonObject("")
                        .get("model")
                        .getAsString());
        JsonObject aluminiumItem = json(clientFiles.get(
                "assets/cruciblecraft/models/item/aluminium/machine_casing.json"));
        assertEquals(
                "cruciblecraft:block/machine_casing",
                aluminiumItem.get("parent").getAsString());
        assertTrue(serverFiles.containsKey(
                "data/cruciblecraft/loot_table/blocks/aluminium/machine_casing.json"));
        assertTrue(serverFiles.containsKey(
                "data/c/tags/block/machine_casings/aluminium.json"));
        assertTrue(serverFiles.containsKey(
                "data/c/tags/item/machine_casings/aluminium.json"));
        assertEquals(
                "Aluminium Machine Casing",
                json(clientFiles.get("assets/cruciblecraft/lang/en_us.json"))
                        .get("block.cruciblecraft.aluminium.machine_casing")
                        .getAsString());
        assertEquals(
                "铝机器外壳",
                json(clientFiles.get("assets/cruciblecraft/lang/zh_cn.json"))
                        .get("block.cruciblecraft.aluminium.machine_casing")
                        .getAsString());

        JsonObject steelDouble = json(clientFiles.get(
                "assets/cruciblecraft/blockstates/steel/machine_casing_double.json"));
        assertEquals(
                "cruciblecraft:block/machine_casing_double",
                steelDouble.getAsJsonObject("variants")
                        .getAsJsonObject("")
                        .get("model")
                        .getAsString());
        assertTrue(serverFiles.containsKey(
                "data/cruciblecraft/loot_table/blocks/steel/machine_casing_double.json"));
        assertEquals(
                "Steel Double Machine Casing",
                json(clientFiles.get("assets/cruciblecraft/lang/en_us.json"))
                        .get("block.cruciblecraft.steel.machine_casing_double")
                        .getAsString());
    }

    @Test
    void realGt6ItemOverlaysAreWiredAsUntintedLayer1(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        JsonObject crushed = json(clientFiles.get(
                "assets/cruciblecraft/models/item/crushed_ore.json"));
        JsonObject textures = crushed.getAsJsonObject("textures");
        assertEquals(
                "cruciblecraft:item/material/crushed_ore",
                textures.get("layer0").getAsString());
        assertEquals(
                "cruciblecraft:item/material/crushed_ore_overlay",
                textures.get("layer1").getAsString());
        assertFalse(textures.has("layer2"));

        JsonObject dust = json(clientFiles.get(
                "assets/cruciblecraft/models/item/copper/dust.json"));
        assertEquals(
                "cruciblecraft:item/material/dust",
                dust.getAsJsonObject("textures").get("layer0").getAsString());
        assertFalse(dust.getAsJsonObject("textures").has("layer1"));
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/dust.json"));

        String div72Path = clientFiles.keySet().stream()
                .filter(path -> path.endsWith("/dust_div72.json"))
                .filter(path -> path.contains("/models/item/"))
                .findFirst()
                .orElseThrow();
        JsonObject div72Textures = json(clientFiles.get(div72Path))
                .getAsJsonObject("textures");
        assertEquals(
                "cruciblecraft:item/material/dust_div72",
                div72Textures.get("layer0").getAsString());
        assertEquals(
                "cruciblecraft:item/material/dust_div72_overlay",
                div72Textures.get("layer1").getAsString());
        String storagePath = "assets/cruciblecraft/models/item/coal_coke/storage_dust.json";
        assertTrue(clientFiles.containsKey(storagePath));
        JsonObject storageItem = json(clientFiles.get(storagePath));
        assertEquals(
                "cruciblecraft:block/material_storage_dust",
                storageItem.get("parent").getAsString());
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/storage_dust.json"));
        JsonObject storageState = json(clientFiles.get(
                "assets/cruciblecraft/blockstates/coal_coke/storage_dust.json"));
        assertEquals(
                "cruciblecraft:block/material_storage_dust",
                storageState.getAsJsonObject("variants")
                        .getAsJsonObject("")
                        .get("model")
                        .getAsString());
        String platePath = "assets/cruciblecraft/models/item/iron/storage_plate.json";
        assertTrue(clientFiles.containsKey(platePath));
        JsonObject plateItem = json(clientFiles.get(platePath));
        assertEquals(
                "cruciblecraft:block/material_storage_plate",
                plateItem.get("parent").getAsString());
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/storage_plate.json"));
        JsonObject plateState = json(clientFiles.get(
                "assets/cruciblecraft/blockstates/iron/storage_plate.json"));
        assertEquals(
                "cruciblecraft:block/material_storage_plate",
                plateState.getAsJsonObject("variants")
                        .getAsJsonObject("")
                        .get("model")
                        .getAsString());
    }

    @Test
    void leftoverMiscPrefixesUseGt6MaterialIcons(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);
        Path textures = Path.of(
                "src/main/resources/assets/cruciblecraft/textures/item/material");
        var overlays = java.util.Set.of(
                "arrow_gt_plastic",
                "arrow_gt_wood");
        for (String prefix : java.util.List.of(
                "arrow_gt_plastic",
                "arrow_gt_wood",
                "scrap",
                "rail_gt")) {
            assertEquals(
                    MaterialCreativeTab.MISC,
                    MaterialCreativeTab.forPrefix(
                            MaterialPrefixCatalog.require(prefix)));
            JsonObject texturesJson = json(clientFiles.get(
                    "assets/cruciblecraft/models/item/" + prefix + ".json"))
                    .getAsJsonObject("textures");
            assertEquals(
                    "cruciblecraft:item/material/" + prefix,
                    texturesJson.get("layer0").getAsString(),
                    prefix);
            assertTrue(Files.isRegularFile(textures.resolve(prefix + ".png")), prefix);
            if (overlays.contains(prefix)) {
                assertEquals(
                        "cruciblecraft:item/material/" + prefix + "_overlay",
                        texturesJson.get("layer1").getAsString(),
                        prefix);
                assertTrue(
                        Files.isRegularFile(textures.resolve(prefix + "_overlay.png")),
                        prefix);
            } else {
                assertFalse(texturesJson.has("layer1"), prefix);
            }
        }
    }

    @Test
    void multiWireItemsUseStrandBundlesWithoutBlockTags(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        var expected = java.util.Map.of(
                "wire", "cruciblecraft:conductor/wiregt01_item",
                "double_wire", "cruciblecraft:conductor/wiregt02_item",
                "triple_wire", "cruciblecraft:conductor/wiregt03_item",
                "quadruple_wire", "cruciblecraft:conductor/wiregt04_item",
                "quintuple_wire", "cruciblecraft:conductor/wiregt05_item",
                "sextuple_wire", "cruciblecraft:conductor/wiregt06_item",
                "octuple_wire", "cruciblecraft:conductor/wiregt08_item",
                "dodecuple_wire", "cruciblecraft:conductor/wiregt12_item",
                "hexadecuple_wire", "cruciblecraft:conductor/wiregt16_item");
        expected.forEach((form, parent) -> {
            JsonObject model = json(clientFiles.get(
                    "assets/cruciblecraft/models/item/copper/" + form + ".json"));
            assertEquals(parent, model.get("parent").getAsString(), form);
        });
        assertTrue(serverFiles.containsKey(
                "data/c/tags/block/wires/copper.json"));
        assertTrue(serverFiles.containsKey(
                "data/c/tags/item/dusts/copper.json"));
        assertTrue(json(serverFiles.get("data/c/tags/item/dusts.json"))
                .toString()
                .contains("#c:dusts/copper"));
    }

    @Test
    void generatedClientPackPublishesIdentityAndToolHeadFormNames(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);
        JsonObject english = json(clientFiles.get(
                "assets/cruciblecraft/lang/en_us.json"));
        JsonObject chinese = json(clientFiles.get(
                "assets/cruciblecraft/lang/zh_cn.json"));
        assertEquals(
                "Lighter (Empty)",
                english.get(
                        "item.cruciblecraft.tool.lighter_empty_requires_canning_machine_to_be_filled")
                        .getAsString());
        assertEquals(
                "%s斧头",
                chinese.get("item.cruciblecraft.material_form.tool_head_axe")
                        .getAsString());
        assertEquals(
                "%s Twig",
                english.get("item.cruciblecraft.material_form.plant_gt_twig")
                        .getAsString());
        assertEquals(
                "%s枝条",
                chinese.get("item.cruciblecraft.material_form.plant_gt_twig")
                        .getAsString());
        assertFalse(english.has("item.cruciblecraft.water.plant_gt_twig"));
        assertFalse(chinese.has("item.cruciblecraft.water.plant_gt_twig"));
        assertTrue(english.has("block.cruciblecraft.planks2.blue"));
        assertEquals(
                "Blue Spruce Planks",
                english.get("block.cruciblecraft.planks2.blue").getAsString());
        assertEquals(
                "蓝云杉木板",
                chinese.get("block.cruciblecraft.planks2.blue").getAsString());
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/planks2/blue.json"));
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/blockstates/asphalt/white.json"));
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/block/asphalt/white.json"));
    }

    private static JsonObject json(String document) {
        return JsonParser.parseString(document).getAsJsonObject();
    }
}
