package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;

class PrefixMaterialIdentityTest {
    @Test
    void publicExchangePrefixesEmitPerMaterialTagsAndUniqueModels(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        assertTrue(serverFiles.containsKey("data/c/tags/item/dusts/copper.json"));
        assertTrue(serverFiles.containsKey("data/c/tags/item/plates/bronze.json"));
        assertTrue(serverFiles.containsKey("data/c/tags/item/plates/rubber.json"));
        assertTrue(serverFiles.containsKey("data/c/tags/item/long_rods/copper.json"));
        assertTrue(serverFiles.containsKey("data/c/tags/item/bolts/copper.json"));
        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/copper/dust.json"));
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/dust.json"));
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/ingot.json"));
        var dusts = JsonParser.parseString(
                        serverFiles.get("data/c/tags/item/dusts.json"))
                .getAsJsonObject()
                .getAsJsonArray("values");
        assertFalse(dusts.toString().contains("cruciblecraft:dust"));
        assertTrue(dusts.toString().contains("#c:dusts/copper")
                || dusts.toString().contains("cruciblecraft:copper/dust"));
        assertTrue(registered.get("copper").contains(MaterialPrefixes.DUST));
        var copperDust = JsonParser.parseString(
                        serverFiles.get("data/c/tags/item/dusts/copper.json"))
                .getAsJsonObject()
                .getAsJsonArray("values");
        assertEquals("[\"cruciblecraft:copper/dust\"]", copperDust.toString());
    }

    @Test
    void sharedInventoryPrefixesDoNotEmitPerMaterialTags(
            @TempDir Path configDirectory) {
        longTailInventoryPrefixesStayShared(configDirectory);
    }

    @Test
    void longTailInventoryPrefixesStayShared(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        assertFalse(serverFiles.containsKey(
                "data/c/tags/item/crushed_ores/copper.json"));
        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/crushed_ore.json"));
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/copper/crushed_ore.json"));
        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/dust_div72.json"));
        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/purified_dust.json"));
        assertTrue(registered.get("copper").contains(MaterialPrefixes.CRUSHED_ORE));
        assertTrue(registered.get("copper").contains(MaterialPrefixes.DUST));
        assertEquals(
                "minecraft:sugar",
                materials.stream()
                        .filter(material -> "sugar".equals(material.id()))
                        .findFirst()
                        .orElseThrow()
                        .formItems()
                        .get(MaterialPrefixes.DUST));
        assertTrue(registered.get("sugar").contains(MaterialPrefixes.DUST));
        assertTrue(registered.get("sugar").contains(MaterialPrefixes.STORAGE_DUST));
    }

    @Test
    void uniqueHostedFormsKeepPerMaterialIds(@TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);

        assertTrue(serverFiles.containsKey(
                "data/c/tags/item/storage_blocks/aluminium.json"));
        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/aluminium/block.json"));
        assertTrue(serverFiles.containsKey(
                "data/c/tags/block/wires/copper.json"));
        assertTrue(serverFiles.containsKey(
                "data/c/tags/item/storage_dusts/coal_coke.json"));
        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/blockstates/coal_coke/storage_dust.json"));
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/storage_dust.json"));
        assertTrue(serverFiles.get(
                "data/minecraft/tags/block/mineable/shovel.json")
                .contains("cruciblecraft:coal_coke/storage_dust"));
        assertTrue(serverFiles.containsKey(
                "data/c/tags/item/storage_plates/iron.json"));
        assertTrue(clientFiles.containsKey(
                "assets/cruciblecraft/blockstates/iron/storage_plate.json"));
        assertFalse(clientFiles.containsKey(
                "assets/cruciblecraft/models/item/storage_plate.json"));
        assertTrue(serverFiles.get(
                "data/minecraft/tags/block/mineable/pickaxe.json")
                .contains("cruciblecraft:iron/storage_plate"));
    }

    @Test
    void uniqueHostedPrefixPathDenylistMatchesPipesWiresAndStorage() {
        assertTrue(MaterialFormHosts.isUniqueHostedPrefixPath("fluid_pipe"));
        assertTrue(MaterialFormHosts.isUniqueHostedPrefixPath("wire"));
        assertTrue(MaterialFormHosts.isUniqueHostedPrefixPath("block"));
        assertTrue(MaterialFormHosts.isUniqueHostedPrefixPath("storage_dust"));
        assertTrue(MaterialFormHosts.isUniqueHostedPrefixPath("storage_plate"));
        assertFalse(MaterialFormHosts.isUniqueHostedPrefixPath("dust"));
        assertFalse(MaterialFormHosts.isUniqueHostedPrefixPath("fine_wire"));
        assertFalse(MaterialFormHosts.isUniqueHostedPrefixPath("ingot"));
        assertTrue(MaterialFormHosts.isPublicExchangePrefixPath("dust"));
        assertTrue(MaterialFormHosts.isPublicExchangePrefixPath("fine_wire"));
        assertEquals(16, MaterialFormHosts.PUBLIC_EXCHANGE_PREFIX_PATHS.size());
    }

    @Test
    void sugarDustUnifiesToVanillaSugar(@TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var sugar = materials.stream()
                .filter(material -> "sugar".equals(material.id()))
                .findFirst()
                .orElseThrow();
        assertEquals("minecraft:sugar", sugar.formItems().get(MaterialPrefixes.DUST));
        assertTrue(registered.get("sugar").contains(MaterialPrefixes.DUST));
        assertTrue(registered.get("sugar").contains(MaterialPrefixes.TINY_DUST));
        assertTrue(registered.get("sugar").contains(MaterialPrefixes.STORAGE_DUST));
        var serverFiles = GeneratedMaterialPack.planServerFiles(materials, registered);
        var dusts = JsonParser.parseString(
                        serverFiles.get("data/c/tags/item/dusts/sugar.json"))
                .getAsJsonObject()
                .getAsJsonArray("values");
        assertEquals("[\"minecraft:sugar\"]", dusts.toString());
    }
}
