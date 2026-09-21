package com.masson.cruciblecraft.api.agriculture;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

/**
 * Shared agriculture classification. Crop cards and the optional Foods addon
 * publish into these tags; Farmer's Delight and other farms read them. Core
 * does not hardcode foreign recipes.
 */
public final class AgricultureTags {
    public static final TagKey<Item> GT_BUSH_BERRIES = local("gt_bush_berries");
    public static final TagKey<Item> FOOD = common("foods");
    public static final TagKey<Item> FRUIT = common("fruits");
    public static final TagKey<Item> BERRY = common("foods/berry");
    public static final TagKey<Item> GRAIN = common("grain");
    public static final TagKey<Item> VEGETABLE = common("vegetables");
    public static final TagKey<Item> CROP = common("crops");

    private AgricultureTags() {}

    private static TagKey<Item> local(String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path));
    }

    private static TagKey<Item> common(String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath("c", path));
    }
}
