package com.masson.cruciblecraft.recipe.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonObject;
import com.masson.cruciblecraft.recipe.gt.PrefixMaterialItemCodecs;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;
import com.mojang.serialization.JsonOps;

import net.minecraft.world.item.crafting.Ingredient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class CraftingToolIngredientTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void craftingToolTagsRewriteOntoCustomIngredient() {
        JsonObject json = new JsonObject();
        json.addProperty("tag", "cruciblecraft:crafting_tools/wrench");
        Ingredient decoded = PrefixMaterialItemCodecs.INGREDIENT.parse(
                JsonOps.INSTANCE, json).getOrThrow();
        assertTrue(decoded.getCustomIngredient() instanceof CraftingToolIngredient);
        assertFalse(decoded.getCustomIngredient().isSimple());
    }

    @Test
    void knifeTagRewriteKeepsKnifeTag() {
        JsonObject json = new JsonObject();
        json.addProperty("tag", "cruciblecraft:crafting_tools/knife");
        Ingredient decoded = PrefixMaterialItemCodecs.INGREDIENT.parse(
                JsonOps.INSTANCE, json).getOrThrow();
        CraftingToolIngredient tool =
                (CraftingToolIngredient) decoded.getCustomIngredient();
        assertEquals("crafting_tools/knife", tool.tag().location().getPath());
    }
}
