package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.item.CatalogNamedItem;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated converter host fold. Run with
 * {@code -PgameTestGrid=content}.
 */
@GameTestHolder(MteConverterHostFoldGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteConverterHostFoldGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_content";
    private static final String TEMPLATE = "empty";

    private MteConverterHostFoldGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadBoilerAndTantalumBoxFoldOntoLiveHosts(
            GameTestHelper helper) {
        helper.assertTrue(
                liveConverter("lead_boiler")
                        && liveConverter(
                                "tantalum_hafnium_carbide_burning_box_solid"),
                "folded converter live BlockItems disappeared");
        helper.assertTrue(
                withdrawn("steam/boiler_tank_lead")
                        && withdrawn(
                                "tantalum_hafnium_carbide/burning_box_solid"),
                "folded converter dummies are still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void steamTurbinesStayDummy(GameTestHelper helper) {
        helper.assertTrue(
                dummy("steam/turbine_bronze")
                        && dummy("steam/turbine_steel")
                        && dummy("steam/turbine_steeleaf")
                        && dummy("steam/turbine_titanium")
                        && dummy("steam/turbine_trinitanium")
                        && dummy("steam/turbine_graphene"),
                "steam turbine identities were folded without a live host");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void luvAndZpmBatteryBoxesStayDummy(GameTestHelper helper) {
        helper.assertTrue(
                dummy("boxwood/battery_luv") && dummy("boxwood/battery_zpm"),
                "LuV/ZPM battery boxes were folded without a live host");
        helper.succeed();
    }

    private static boolean liveConverter(String path) {
        Item item = item(path);
        return item instanceof BlockItem
                && !(item instanceof CatalogNamedItem)
                && ModItems.converterItemsById().containsKey(
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", path));
    }

    private static boolean dummy(String path) {
        Item item = item(path);
        if (item instanceof CatalogNamedItem) {
            return true;
        }
        return item instanceof CatalogNamedBlockItem
                && !ModItems.converterItemsById().containsKey(
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", path));
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
