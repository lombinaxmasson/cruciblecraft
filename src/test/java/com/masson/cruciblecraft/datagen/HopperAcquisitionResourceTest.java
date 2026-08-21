package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.logistics.hopper.HopperVariantCatalog;

class HopperAcquisitionResourceTest {
    private static final Path GENERATED = Path.of("src/generated/resources");

    @Test
    void oneHundredTwentyOneIdentitiesHaveRecipesLootLangAndModels()
            throws Exception {
        List<HopperVariant> variants = HopperVariantCatalog.variants();
        assertEquals(120, variants.size());
        var english = document("assets/cruciblecraft/lang/en_us.json");
        var chinese = document("assets/cruciblecraft/lang/zh_cn.json");
        Set<String> pickaxe = values(
                "data/minecraft/tags/block/mineable/pickaxe.json");
        for (HopperVariant variant : variants) {
            String path = variant.id().getPath();
            assertRecipe(variant);
            assertGeneratedFiles(path);
            String key = "block.cruciblecraft." + path;
            assertTrue(english.has(key), key);
            if (variant.kind() == HopperKind.HOPPER
                    && "lead".equals(variant.materialPath())) {
                assertEquals("Lead Hopper", english.get(key).getAsString());
                assertEquals("铅制料斗", chinese.get(key).getAsString());
            }
            if (variant.kind() == HopperKind.QUEUE_HOPPER
                    && "lead".equals(variant.materialPath())) {
                assertEquals("Lead Queue Hopper", english.get(key).getAsString());
                assertEquals("铅制队列料斗", chinese.get(key).getAsString());
            }
            assertTrue(pickaxe.contains(variant.id().toString()), path);
            JsonObject blockstate = document(
                    "assets/cruciblecraft/blockstates/" + path + ".json");
            assertTrue(blockstate.getAsJsonObject("variants").size() >= 6, path);
            JsonObject itemModel = document(
                    "assets/cruciblecraft/models/item/" + path + ".json");
            String parent = itemModel.get("parent").getAsString();
            assertTrue(
                    parent.endsWith("/hopper")
                            || parent.endsWith("/queue_hopper"),
                    path + " " + parent);
            assertFalse(Files.exists(Path.of(
                    "src/main/resources/assets/cruciblecraft/textures/block/"
                            + path + ".png")));
        }
        assertGeneratedFiles("steel_dust_funnel");
        JsonObject funnelRecipe = document(
                "data/cruciblecraft/recipe/hoppers/steel_dust_funnel.json");
        assertEquals(
                "minecraft:crafting_shaped",
                funnelRecipe.get("type").getAsString());
        assertEquals(
                "cruciblecraft:steel_dust_funnel",
                funnelRecipe.getAsJsonObject("result").get("id").getAsString());
        assertEquals(
                "Steel Dust Funnel",
                english.get("block.cruciblecraft.steel_dust_funnel").getAsString());
        assertEquals(
                "钢制粉末漏斗",
                chinese.get("block.cruciblecraft.steel_dust_funnel").getAsString());
        assertTrue(pickaxe.contains("cruciblecraft:steel_dust_funnel"));
        assertTrue(english.has("tooltip.cruciblecraft.hopper.slots"));
        assertTrue(chinese.has("message.cruciblecraft.dust_funnel.mode"));
    }

    private static void assertRecipe(HopperVariant variant) throws Exception {
        JsonObject recipe = document(
                "data/cruciblecraft/recipe/hoppers/"
                        + variant.id().getPath()
                        + ".json");
        assertEquals(
                "minecraft:crafting_shaped",
                recipe.get("type").getAsString());
        assertEquals(
                variant.id().toString(),
                recipe.getAsJsonObject("result").get("id").getAsString());
        List<String> pattern = recipe.getAsJsonArray("pattern").asList().stream()
                .map(element -> element.getAsString())
                .toList();
        if (variant.kind() == HopperKind.HOPPER) {
            assertEquals(List.of("P P", "PCP", " P "), pattern);
        } else {
            assertEquals(List.of("PPP", "C C", "P P"), pattern);
        }
        String joined = String.join("", pattern);
        assertEquals(5, joined.chars().filter(ch -> ch == 'P').count());
        assertFalse(joined.contains("W"));
        assertFalse(recipe.toString().contains("plate_curved"));
    }

    private static void assertGeneratedFiles(String path) {
        for (Path file : List.of(
                GENERATED.resolve("assets/cruciblecraft/blockstates/" + path + ".json"),
                GENERATED.resolve("assets/cruciblecraft/models/item/" + path + ".json"),
                GENERATED.resolve(
                        "data/cruciblecraft/loot_table/blocks/" + path + ".json"))) {
            assertTrue(Files.isRegularFile(file), file.toString());
        }
    }

    private static JsonObject document(String relative) throws Exception {
        return JsonParser.parseString(
                Files.readString(GENERATED.resolve(relative))).getAsJsonObject();
    }

    private static Set<String> values(String relative) throws Exception {
        JsonArray values = document(relative).getAsJsonArray("values");
        return values.asList().stream()
                .map(element -> element.getAsString())
                .collect(Collectors.toSet());
    }
}
