package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.item.CatalogNamedItem;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated processing-machine host fold. Run with
 * {@code -PgameTestGrid=content}.
 */
@GameTestHolder(MteProcessingHostFoldGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteProcessingHostFoldGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private MteProcessingHostFoldGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void bronzeSifterFoldsOntoLiveHost(GameTestHelper helper) {
        helper.assertTrue(
                liveMachine("sifter"),
                "bronze sifter lost its live processing BlockItem");
        helper.assertTrue(
                withdrawn("processing/sifter_bronze"),
                "bronze sifter dummy is still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void automaticHammersStayDummy(GameTestHelper helper) {
        helper.assertTrue(
                liveMachine("automatic_hammer")
                        && liveMachine("steel_automatic_hammer")
                        && liveMachine("titanium_automatic_hammer")
                        && liveMachine("tungstensteel_automatic_hammer"),
                "automatic hammers lost their live hosts");
        helper.assertTrue(
                withdrawn("processing/automatic_hammer_bronze")
                        && withdrawn("processing/automatic_hammer_steel")
                        && withdrawn("processing/automatic_hammer_titanium")
                        && withdrawn("processing/automatic_hammer_tungstensteel"),
                "automatic hammer dummies are still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void polarizerMagSepFoldOntoLiveHosts(GameTestHelper helper) {
        helper.assertTrue(
                liveMachine("steel_galvanized_polarizer")
                        && liveMachine("steel_galvanized_magnetic_separator"),
                "polarizer/magsep lost their live processing BlockItems");
        helper.assertTrue(
                withdrawn("steel/polarizer_galvanized")
                        && withdrawn("steel/magnetic_separator_galvanized"),
                "polarizer/magsep dummies are still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void squeezerLaserStayDummy(GameTestHelper helper) {
        helper.assertTrue(
                liveMachine("squeezer")
                        && liveMachine("steel_squeezer")
                        && liveMachine("titanium_squeezer")
                        && liveMachine("tungstensteel_squeezer")
                        && liveMachine("laser_engraver")
                        && liveMachine("aluminium_laser_engraver")
                        && liveMachine("stainless_steel_laser_engraver")
                        && liveMachine("chromium_laser_engraver")
                        && liveMachine("titanium_laser_engraver")
                        && liveMachine("laser_welder")
                        && liveMachine("aluminium_laser_welder")
                        && liveMachine("stainless_steel_laser_welder")
                        && liveMachine("chromium_laser_welder")
                        && liveMachine("titanium_laser_welder"),
                "squeezer and laser hosts lost their live registrations");
        helper.assertTrue(
                withdrawn("processing/squeezer_bronze")
                        && withdrawn("processing/laser_engraver_t1")
                        && withdrawn("processing/laser_welder_t1"),
                "squeezer and laser dummies are still registered");
        helper.succeed();
    }

    private static boolean liveMachine(String path) {
        Item item = item(path);
        return item instanceof BlockItem
                && !(item instanceof CatalogNamedItem);
    }

    private static boolean dummy(String path) {
        return item(path) instanceof CatalogNamedItem;
    }

    private static boolean withdrawn(String path) {
        return !BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }

    private static Item item(String path) {
        return BuiltInRegistries.ITEM.get(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }
}
