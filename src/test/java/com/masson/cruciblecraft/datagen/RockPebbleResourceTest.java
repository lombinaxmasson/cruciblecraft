package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.gen.GeneratedMaterialPack;
import com.masson.cruciblecraft.recipe.crafting.RockCobbleCrafting;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tracked GT6 {@code OP.rockGt} recipes, lang, rockgt art, and copy-below
 * pebble models.
 */
class RockPebbleResourceTest {
    private static final Path RECIPES = Path.of(
            "src/generated/resources/data/cruciblecraft/recipe/rocks");
    private static final Path EN = Path.of(
            "src/generated/resources/assets/cruciblecraft/lang/en_us.json");
    private static final Path ZH = Path.of(
            "src/generated/resources/assets/cruciblecraft/lang/zh_cn.json");
    private static final Path TEXTURES = Path.of(
            "src/main/resources/assets/cruciblecraft/textures/item/material");
    private static final Path MODELS = Path.of(
            "src/main/resources/assets/cruciblecraft/models/block");

    @Test
    void fourRocksCraftMatchingCobbleJson() throws Exception {
        Map<String, String> expected = Map.ofEntries(
                Map.entry("stone", "minecraft:cobblestone"),
                Map.entry("netherrack", "minecraft:netherrack"),
                Map.entry("endstone", "minecraft:end_stone"),
                Map.entry(
                        "granite_black",
                        "cruciblecraft:granite_black/cobble"));
        for (var entry : RockCobbleCrafting.cobbleResults().entrySet()) {
            JsonObject recipe = object(
                    RECIPES.resolve(entry.getKey() + "_to_cobble.json"));
            assertEquals("minecraft:crafting_shaped", recipe.get("type").getAsString());
            assertEquals("XX", recipe.getAsJsonArray("pattern").get(0).getAsString());
            assertEquals("XX", recipe.getAsJsonArray("pattern").get(1).getAsString());
            assertEquals(
                    "cruciblecraft:" + entry.getKey() + "/rock",
                    recipe.getAsJsonObject("key")
                            .getAsJsonObject("X")
                            .get("item")
                            .getAsString());
            JsonObject result = recipe.getAsJsonObject("result");
            assertEquals(entry.getValue().toString(), result.get("id").getAsString());
            assertEquals(1, result.get("count").getAsInt());
        }
        assertEquals(18, RockCobbleCrafting.cobbleResults().size());
        for (var entry : expected.entrySet()) {
            assertEquals(
                    entry.getValue(),
                    RockCobbleCrafting.cobbleResults()
                            .get(entry.getKey())
                            .toString());
        }
    }

    @Test
    void rockNamesUseGt6SpecialsAndShiZi() throws Exception {
        JsonObject english = object(EN);
        JsonObject chinese = object(ZH);
        assertEquals("%s Rock", english.get("item.cruciblecraft.material_form.rock")
                .getAsString());
        assertEquals("%s石子", chinese.get("item.cruciblecraft.material_form.rock")
                .getAsString());
        assertEquals("Rock", english.get("item.cruciblecraft.rock.stone").getAsString());
        assertEquals("石子", chinese.get("item.cruciblecraft.rock.stone").getAsString());
        assertEquals(
                "Nether Rock",
                english.get("item.cruciblecraft.rock.netherrack").getAsString());
        assertEquals(
                "下界石子",
                chinese.get("item.cruciblecraft.rock.netherrack").getAsString());
        assertEquals(
                "End Rock",
                english.get("item.cruciblecraft.rock.endstone").getAsString());
        assertEquals(
                "末地石子",
                chinese.get("item.cruciblecraft.rock.endstone").getAsString());
        assertEquals(
                "Meteorite",
                english.get("item.cruciblecraft.rock.meteorite").getAsString());
        assertEquals(
                "陨石",
                chinese.get("item.cruciblecraft.rock.meteorite").getAsString());
        assertEquals(
                "Indicates occurrence of %s",
                english.get("tooltip.cruciblecraft.rock.indicates").getAsString());
        assertEquals(
                "表明存在 %s",
                chinese.get("tooltip.cruciblecraft.rock.indicates").getAsString());
        assertEquals(
                "黑花岗岩",
                chinese.get("material.cruciblecraft.granite_black").getAsString());
        assertEquals(
                "页岩",
                chinese.get("material.cruciblecraft.shale").getAsString());
    }

    @Test
    void pebbleModelsCopyContactAndKeepRockgt(@TempDir Path configDirectory)
            throws Exception {
        assertTrue(Files.isRegularFile(TEXTURES.resolve("rock.png")));
        assertTrue(Files.isRegularFile(TEXTURES.resolve("rock_overlay.png")));
        for (String model : new String[] {
                "material_rock.json",
                "gt_surface_rock_stone.json",
                "gt_surface_rock_sandstone.json",
                "gt_surface_rock_cobble.json"
        }) {
            JsonObject json = object(MODELS.resolve(model));
            assertEquals(
                    "cruciblecraft:positional_pebble",
                    json.get("loader").getAsString(),
                    model);
            assertTrue(json.get("copy_below").getAsBoolean(), model);
        }
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var clientFiles = GeneratedMaterialPack.planClientFiles(materials, registered);
        JsonObject item = json(clientFiles.get(
                "assets/cruciblecraft/models/item/stone/rock.json"));
        assertEquals("minecraft:item/generated", item.get("parent").getAsString());
        JsonObject textures = item.getAsJsonObject("textures");
        assertEquals(
                "cruciblecraft:item/material/rock",
                textures.get("layer0").getAsString());
        assertEquals(
                "cruciblecraft:item/material/rock_overlay",
                textures.get("layer1").getAsString());
    }

    private static JsonObject object(Path path) throws Exception {
        assertTrue(Files.isRegularFile(path), path.toString());
        return JsonParser.parseString(Files.readString(path)).getAsJsonObject();
    }

    private static JsonObject json(String document) {
        return JsonParser.parseString(document).getAsJsonObject();
    }
}
