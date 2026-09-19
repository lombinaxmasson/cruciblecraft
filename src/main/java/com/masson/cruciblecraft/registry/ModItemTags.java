package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

public final class ModItemTags {
    public static final TagKey<Item> EXTRUDER_SHAPES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "extruder_shapes"));
    /** GT6 OD.craftingFirestarter. Furnace substitutes stay deferred. */
    public static final TagKey<Item> CRAFTING_FIRESTARTER = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "crafting_firestarter"));
    public static final TagKey<Item> SMELTING_CRUCIBLES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "smelting_crucibles"));

    private ModItemTags() {}
}
