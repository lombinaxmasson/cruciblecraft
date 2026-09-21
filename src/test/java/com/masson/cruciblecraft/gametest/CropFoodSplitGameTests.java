package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.agriculture.AgricultureTags;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.crops.CropBlockEntity;
import com.masson.cruciblecraft.crops.CropCatalog;
import com.masson.cruciblecraft.crops.CropRegistries;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialFormHosts;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Crop addon overlay, crop-stick harvest, Foods tags, and no ItemEntity scatter.
 * Lives on the default GameTest namespace so a bare {@code runGameTestServer}
 * exercises it.
 */
@PrefixGameTestTemplate(false)
public final class CropFoodSplitGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(2, 2, 2);

    private CropFoodSplitGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void namedPlantFormsUseSharedLongTail(GameTestHelper helper) {
        helper.assertTrue(
                ModList.get().isLoaded("cruciblecraft_crops"),
                "crops addon must load in the default GameTest run");
        var pairs = java.util.List.of(
                java.util.Map.entry("indigo", "plant_gt_blossom"),
                java.util.Map.entry("iron", "plant_gt_blossom"),
                java.util.Map.entry("gold", "plant_gt_blossom"),
                java.util.Map.entry("copper", "plant_gt_fiber"),
                java.util.Map.entry("tin", "plant_gt_twig"));
        for (var pair : pairs) {
            var material = MaterialCatalog.require(pair.getKey());
            var prefix = MaterialPrefixCatalog.require(pair.getValue());
            helper.assertTrue(
                    MaterialCatalog.isFormRegistered(material, prefix),
                    "missing overlay " + pair);
            helper.assertTrue(
                    MaterialFormHosts.isSharedInventoryForm(material, prefix),
                    "not shared " + pair);
            helper.assertFalse(
                    MaterialFormHosts.isUniqueInventoryForm(material, prefix),
                    "unique backend " + pair);
            helper.assertTrue(
                    MaterialLookup.tryStack(pair.getKey(), prefix, 1).isPresent(),
                    "no live stack " + pair);
            helper.assertTrue(
                    BuiltInRegistries.ITEM.getOptional(
                                    ResourceLocation.fromNamespaceAndPath(
                                            CrucibleCraft.MODID,
                                            pair.getKey() + "/" + pair.getValue()))
                            .isEmpty(),
                    "unique slash item " + pair);
        }
        helper.assertFalse(
                MaterialCatalog.isFormRegistered(
                        MaterialCatalog.require("oil"),
                        MaterialPrefixCatalog.require("plant_gt_berry")),
                "oil plant overlay must stay blocked on metadata-only oil");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cropStickHarvestsIndigoBlossom(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.FARMLAND);
        helper.setBlock(POS.above(), CropRegistries.CROP_STICK.get().defaultBlockState());
        CropBlockEntity crop = (CropBlockEntity) helper.getBlockEntity(POS.above());
        helper.assertTrue(crop != null, "crop entity missing");
        crop.plant(CropCatalog.require("indigo"));
        crop.setSizeForTest(4);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        helper.assertTrue(crop.harvest(player), "indigo harvest failed");
        boolean found = false;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            if (MaterialLookup.matches(
                    player.getInventory().getItem(slot),
                    "indigo",
                    MaterialPrefixCatalog.require("plant_gt_blossom"))) {
                found = true;
                break;
            }
        }
        helper.assertTrue(found, "indigo blossom was not harvested");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void foodsAddonPublishesTomatoAndSkipsStandInApple(GameTestHelper helper) {
        helper.assertTrue(
                ModList.get().isLoaded("cruciblecraft_foods"),
                "foods addon must load in the default GameTest run");
        var tomato = BuiltInRegistries.ITEM.getOptional(
                ResourceLocation.fromNamespaceAndPath("cruciblecraft_foods", "tomato"));
        helper.assertTrue(tomato.isPresent(), "tomato missing");
        helper.assertTrue(
                new ItemStack(tomato.orElseThrow()).is(AgricultureTags.VEGETABLE),
                "tomato not in c:vegetables");
        helper.assertTrue(
                BuiltInRegistries.ITEM.getOptional(
                                ResourceLocation.fromNamespaceAndPath(
                                        "cruciblecraft_foods", "apple_red"))
                        .isEmpty(),
                "red apple must stay minecraft:apple");
        helper.assertTrue(CropCatalog.find("rye").isPresent(), "rye card missing with foods");
        helper.assertTrue(
                CropCatalog.find("desert_nova").isEmpty(),
                "ARS Desert Nova must stay unregistered without ARS");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cropObtainDoesNotUseItemEntityScatter(GameTestHelper helper) {
        long scatter = BuiltInRegistries.FEATURE.stream()
                .map(BuiltInRegistries.FEATURE::getKey)
                .filter(id -> id != null && CrucibleCraft.MODID.equals(id.getNamespace()))
                .filter(id -> id.getPath().contains("scatter"))
                .count();
        helper.assertTrue(scatter == 0, "crop obtain must not use ItemEntity scatter");
        helper.succeed();
    }
}
