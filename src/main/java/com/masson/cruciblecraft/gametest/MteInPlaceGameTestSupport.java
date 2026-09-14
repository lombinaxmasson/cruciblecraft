package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.MteInPlaceBlock;
import com.masson.cruciblecraft.content.item.CatalogNamedBlockItem;
import com.masson.cruciblecraft.content.mte.MteInPlaceKind;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;

/** Shared assertions for sequential in-place MTE GameTests. */
public final class MteInPlaceGameTestSupport {
    private MteInPlaceGameTestSupport() {}

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.get(id(path));
    }

    public static void assertLive(
            GameTestHelper helper,
            String path,
            MteInPlaceKind kind) {
        Item item = item(path);
        helper.assertTrue(
                item instanceof CatalogNamedBlockItem,
                path + " is still a dummy item");
        helper.assertTrue(
                ((MteInPlaceBlock) ((CatalogNamedBlockItem) item).getBlock())
                        .spec()
                        .kind()
                        == kind,
                path + " kind drifted");
        helper.assertTrue(
                ModItems.smelterMteItemsById().get(id(path)) == null,
                path + " dummy CatalogNamedItem is still registered");
    }
}
