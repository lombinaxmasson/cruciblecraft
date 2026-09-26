package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.item.PipeBlockItem;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 restrictive item-pipe runtime. Run with
 * {@code -PgameTestGrid=logistics}.
 */
@GameTestHolder(RestrictiveItemPipeRuntimeGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class RestrictiveItemPipeRuntimeGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_logistics";
    private static final String TEMPLATE = "empty";

    private RestrictiveItemPipeRuntimeGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void restrictiveStepSizeIsTimes100(GameTestHelper helper) {
        var ordinary = PipeCatalog.require(
                "brass", MaterialPrefixes.ITEM_PIPE, PipeCatalog.Kind.ITEM);
        var restrictive = PipeCatalog.require(
                "brass",
                MaterialPrefixes.RESTRICTIVE_ITEM_PIPE,
                PipeCatalog.Kind.ITEM);
        helper.assertTrue(
                ordinary.item().stepSize() == 32768L
                        && restrictive.item().stepSize() == 3_276_800L
                        && restrictive.item().stacksPerSecond()
                                == ordinary.item().stacksPerSecond(),
                "brass restrictive stepSize is not ordinary x100");
        helper.assertTrue(
                PipeCatalog.require(
                                        "brass",
                                        MaterialPrefixes.LARGE_RESTRICTIVE_ITEM_PIPE,
                                        PipeCatalog.Kind.ITEM)
                                .item()
                                .stepSize()
                        == 1_638_400L
                        && PipeCatalog.require(
                                        "brass",
                                        MaterialPrefixes.HUGE_RESTRICTIVE_ITEM_PIPE,
                                        PipeCatalog.Kind.ITEM)
                                .item()
                                .stepSize()
                        == 819_200L,
                "large/huge restrictive stepSize drifted from x100");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void restrictiveUsesRestrictorOverlayNotOrdinaryAlias(
            GameTestHelper helper) {
        var ordinary = PipeCatalog.require(
                "brass", MaterialPrefixes.ITEM_PIPE, PipeCatalog.Kind.ITEM);
        var restrictive = PipeCatalog.require(
                "brass",
                MaterialPrefixes.RESTRICTIVE_ITEM_PIPE,
                PipeCatalog.Kind.ITEM);
        helper.assertTrue(
                ordinary.width() == 8
                        && restrictive.width() == 8
                        && "8".equals(ordinary.textureKey())
                        && "restrictive_8".equals(restrictive.textureKey())
                        && restrictive.item().stepSize()
                                != ordinary.item().stepSize(),
                "restrictive was aliased onto the ordinary item-pipe host");
        helper.assertTrue(
                ModBlocks.hasPipeBlock(
                        "brass",
                        MaterialPrefixes.RESTRICTIVE_ITEM_PIPE,
                        PipeCatalog.Kind.ITEM)
                        && ModItems.hasMaterialItem(
                                "brass", MaterialPrefixes.RESTRICTIVE_ITEM_PIPE)
                        && ModItems.materialItem(
                                        "brass",
                                        MaterialPrefixes.RESTRICTIVE_ITEM_PIPE)
                                .get() instanceof PipeBlockItem,
                "brass restrictive is not a live PipeBlockItem");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void restrictiveCatalogDummiesFoldOntoLiveHost(
            GameTestHelper helper) {
        helper.assertTrue(
                liveRestrictive(
                        "elven_elementium",
                        MaterialPrefixes.RESTRICTIVE_ITEM_PIPE)
                        && liveRestrictive(
                                "elven_elementium",
                                MaterialPrefixes.LARGE_RESTRICTIVE_ITEM_PIPE)
                        && liveRestrictive(
                                "elven_elementium",
                                MaterialPrefixes.HUGE_RESTRICTIVE_ITEM_PIPE)
                        && liveRestrictive(
                                "vibranium_silver",
                                MaterialPrefixes.RESTRICTIVE_ITEM_PIPE),
                "live restrictive BlockItems disappeared");
        helper.assertTrue(
                withdrawn("item_pipe_tile/restrictive_elementium_item_pipe")
                        && withdrawn(
                                "item_pipe_tile/restrictive_large_elementium_item_pipe")
                        && withdrawn(
                                "item_pipe_tile/restrictive_huge_elementium_item_pipe")
                        && withdrawn(
                                "item_pipe_tile/restrictive_vibranium_silver_item_pipe"),
                "folded restrictive dummies are still registered");
        helper.succeed();
    }

    private static boolean liveRestrictive(
            String material,
            com.masson.cruciblecraft.api.material.MaterialPrefix form) {
        return ModBlocks.hasPipeBlock(material, form, PipeCatalog.Kind.ITEM)
                && ModBlocks.pipeBlock(material, form, PipeCatalog.Kind.ITEM)
                        .get() instanceof ItemPipeBlock
                && ModItems.hasMaterialItem(material, form)
                && ModItems.materialItem(material, form).get()
                        instanceof PipeBlockItem;
    }

    private static boolean withdrawn(String path) {
        return !BuiltInRegistries.ITEM.containsKey(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft", path));
    }
}
