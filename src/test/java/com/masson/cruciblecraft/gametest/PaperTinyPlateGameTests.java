package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated paper tiny_plate gate. Run with
 * {@code -PgameTestGrid=content}.
 */
@GameTestHolder(PaperTinyPlateGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class PaperTinyPlateGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private PaperTinyPlateGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void paperTinyPlateItemIsRegistered(GameTestHelper helper) {
        var tinyPlate = MaterialPrefixCatalog.require("tiny_plate");
        helper.assertTrue(
                MaterialCatalog.registeredForms("paper").contains(tinyPlate),
                "paper:tiny_plate is not in the registration gate");
        helper.assertTrue(
                ModItems.hasMaterialItem("paper", tinyPlate),
                "paper tiny_plate has no live item");
        helper.succeed();
    }
}
