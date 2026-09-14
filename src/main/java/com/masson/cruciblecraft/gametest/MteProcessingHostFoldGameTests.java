package com.masson.cruciblecraft.gametest;

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
 * Isolated processing-machine host fold. Run with
 * {@code -PwaveRecipes=content/gt6-mte-processing-host-fold}.
 */
@GameTestHolder(MteProcessingHostFoldGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteProcessingHostFoldGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_processing_host_fold";
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
                dummy("processing/automatic_hammer_bronze")
                        && dummy("processing/automatic_hammer_steel")
                        && dummy("processing/automatic_hammer_titanium")
                        && dummy("processing/automatic_hammer_tungstensteel"),
                "automatic hammers were folded without a machine_tiers host");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void squeezerPolarizerMagSepLaserStayDummy(
            GameTestHelper helper) {
        helper.assertTrue(
                dummy("processing/squeezer_bronze")
                        && dummy("steel/polarizer_galvanized")
                        && dummy("steel/magnetic_separator_galvanized")
                        && dummy("processing/laser_engraver_t1")
                        && dummy("processing/laser_welder_t1"),
                "ungated processing identities were folded without sourceId hosts");
        helper.succeed();
    }

    private static boolean liveMachine(String path) {
        Item item = item(path);
        return item instanceof BlockItem
                && !(item instanceof CatalogNamedItem)
                && ModItems.tieredProcessingItemsById().containsKey(
                        ResourceLocation.fromNamespaceAndPath(
                                "cruciblecraft", path));
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
