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
    /** GT6 {@code OD.craftingFirestarter}. */
    public static final TagKey<Item> CRAFTING_FIRESTARTER = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "crafting_firestarter"));
    /** GT6 {@code OD.craftingChest}. */
    public static final TagKey<Item> CRAFTING_CHEST = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "crafting_chest"));
    /** GT6 {@code OD.craftingPistonIngot}. */
    public static final TagKey<Item> CRAFTING_PISTON_INGOT = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "crafting_piston_ingot"));
    /** GT6 {@code OD.craftingPistonGlue}. */
    public static final TagKey<Item> CRAFTING_PISTON_GLUE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "crafting_piston_glue"));
    /** GT6 {@code OD.craftingFurnace}. */
    public static final TagKey<Item> CRAFTING_FURNACE = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "crafting_furnace"));
    public static final TagKey<Item> SMELTING_CRUCIBLES = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "smelting_crucibles"));
    /** GT6 {@code OD.beamWood}: vanilla stripped wood plus GT-tree beams. */
    public static final TagKey<Item> WOODEN_BEAMS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "wooden_beams"));

    private ModItemTags() {}
}
