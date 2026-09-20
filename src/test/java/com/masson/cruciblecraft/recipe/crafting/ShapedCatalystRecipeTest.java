package com.masson.cruciblecraft.recipe.crafting;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Ingredient;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ShapedCatalystRecipeTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void remainingItemsCoverEmiThreeColumnRemainderProbe() {
        ShapedCatalystRecipe recipe = new ShapedCatalystRecipe(
                List.of(" x ", " P ", "   "),
                Map.of("P", Ingredient.of(Items.IRON_INGOT)),
                Map.of("x", Ingredient.of(Items.STICK)),
                new ItemStack(Items.IRON_NUGGET));
        List<ItemStack> padded = new ArrayList<>();
        for (int index = 0; index < 9; index++) {
            padded.add(ItemStack.EMPTY);
        }
        padded.set(1, new ItemStack(Items.STICK));
        padded.set(4, new ItemStack(Items.IRON_INGOT));
        CraftingInput input = CraftingInput.of(3, 3, padded);
        NonNullList<ItemStack> remaining = recipe.getRemainingItems(input);
        assertTrue(remaining.size() >= 9, remaining.size() + "");
        int catalystSlot = 1;
        int emiIndex = (catalystSlot / 3) * input.width() + (catalystSlot % 3);
        assertDoesNotThrow(() -> remaining.get(emiIndex));
        int plateSlot = 4;
        int plateIndex = (plateSlot / 3) * input.width() + (plateSlot % 3);
        assertDoesNotThrow(() -> remaining.get(plateIndex));
    }
}
