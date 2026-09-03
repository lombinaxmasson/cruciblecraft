package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;

class MaterialVisualResourceTest {
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
        assertEquals(483, storage);

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
                .get("block.cruciblecraft.aluminium/block")
                .getAsString()
                .equals("Aluminium Block"));
        assertTrue(json(clientFiles.get("assets/cruciblecraft/lang/zh_cn.json"))
                .get("block.cruciblecraft.aluminium/block")
                .getAsString()
                .equals("铝块"));

        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/blockstates/copper/block.json"));
        assertTrue(serverFiles.containsKey(
                "data/cruciblecraft/loot_table/blocks/copper/block.json"));
    }

    @Test
    void realGt6ItemOverlaysAreWiredAsUntintedLayer1(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        JsonObject crushed = json(clientFiles.get(
                "assets/cruciblecraft/models/item/copper/crushed_ore.json"));
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
                "double_wire", "cruciblecraft:item/material/wire_bundle_2",
                "quadruple_wire", "cruciblecraft:item/material/wire_bundle_4",
                "octuple_wire", "cruciblecraft:item/material/wire_bundle_8",
                "dodecuple_wire", "cruciblecraft:item/material/wire_bundle_12",
                "hexadecuple_wire", "cruciblecraft:item/material/wire_bundle_16");
        expected.forEach((form, parent) -> {
            JsonObject model = json(clientFiles.get(
                    "assets/cruciblecraft/models/item/copper/" + form + ".json"));
            assertEquals(parent, model.get("parent").getAsString(), form);
            if (!form.equals("wire")) {
                assertFalse(
                        serverFiles.containsKey(
                                "data/c/tags/block/" + form + "s/copper.json"),
                        form);
                assertFalse(
                        serverFiles.containsKey(
                                "data/cruciblecraft/tags/block/"
                                        + form + "s/copper.json"),
                        form);
                assertFalse(
                        serverFiles.containsKey(
                                "data/cruciblecraft/loot_table/blocks/copper/"
                                        + form + ".json"),
                        form);
            }
        });
        assertTrue(serverFiles.containsKey(
                "data/c/tags/block/wires/copper.json"));
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
                        "item.cruciblecraft.gt_multiitem.multiitem_randomtools_m5004")
                        .getAsString());
        assertEquals(
                "%s 斧头",
                chinese.get("item.cruciblecraft.material_form.tool_head_axe")
                        .getAsString());
        assertTrue(english.has("block.cruciblecraft.gt_block.planks2_m11"));
        assertTrue(chinese.has("block.cruciblecraft.gt_block.planks2_m11"));
        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/gt_block/planks2_m11.json"));
    }

    private static JsonObject json(String document) {
        return JsonParser.parseString(document).getAsJsonObject();
    }
}
