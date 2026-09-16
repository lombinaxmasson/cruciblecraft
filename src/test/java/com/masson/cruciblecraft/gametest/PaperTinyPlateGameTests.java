package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated paper tiny_plate gate. Run with
 * {@code -PwaveRecipes=content/gt6-paper-tiny-plate}.
 */
@GameTestHolder(PaperTinyPlateGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PaperTinyPlateGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_paper_tiny_plate";
    private static final String TEMPLATE = "empty";

    private PaperTinyPlateGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void paperTinyPlateItemIsRegistered(GameTestHelper helper) {
        helper.assertTrue(
                MaterialCatalog.registeredForms("paper").contains(
                        MaterialPrefixCatalog.require("tiny_plate")),
                "paper:tiny_plate is not in the registration gate");
        Item item = BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "paper/tiny_plate"));
        helper.assertTrue(
                item != null && item != Items.AIR,
                "cruciblecraft:paper/tiny_plate is missing");
        helper.succeed();
    }
}
