package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.item.CatalogNamedItem;
import com.masson.cruciblecraft.content.item.HopperBlockItem;
import com.masson.cruciblecraft.registry.ModBlocks;
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
 * Isolated hopper host fold. Run with
 * {@code -PwaveRecipes=content/gt6-mte-hopper-host-fold}.
 */
@GameTestHolder(MteHopperHostFoldGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class MteHopperHostFoldGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_content_gt6_mte_hopper_host_fold";
    private static final String TEMPLATE = "empty";

    private MteHopperHostFoldGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void leadHopperAndQueueFoldOntoLiveHosts(
            GameTestHelper helper) {
        helper.assertTrue(
                liveHopper("lead_hopper") && liveHopper("lead_queue_hopper"),
                "folded hopper live BlockItems disappeared");
        helper.assertTrue(
                withdrawn("lead/hopper") && withdrawn("lead/queue_hopper"),
                "folded hopper dummies are still registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void dustFunnelFoldsOntoSteelDustFunnel(
            GameTestHelper helper) {
        Item funnel = item("steel_dust_funnel");
        helper.assertTrue(
                funnel instanceof BlockItem
                        && !(funnel instanceof CatalogNamedItem)
                        && ModBlocks.STEEL_DUST_FUNNEL.get()
                                == ((BlockItem) funnel).getBlock(),
                "steel dust funnel lost its live BlockItem");
        helper.assertTrue(
                withdrawn("hopper/dust_funnel"),
                "dust funnel dummy is still registered");
        helper.succeed();
    }

    private static boolean liveHopper(String path) {
        Item item = item(path);
        return item instanceof HopperBlockItem
                && ModItems.hopperItemsById().containsKey(
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
