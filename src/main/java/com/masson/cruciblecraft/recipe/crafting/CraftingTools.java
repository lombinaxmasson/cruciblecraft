package com.masson.cruciblecraft.recipe.crafting;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * GT6 lowercase CR letters are OreDict tools (any material). Recipe JSON keeps
 * {@code cruciblecraft:crafting_tools/*} tags so datagen stays small.
 * Decode rewrites those tags onto {@link CraftingToolIngredient} so EMI can
 * cycle routed material variants instead of the shared item's default iron
 * stack.
 */
public final class CraftingTools {
    public static final TagKey<Item> AXE = tag("axe");
    public static final TagKey<Item> CROWBAR = tag("crowbar");
    public static final TagKey<Item> SCREWDRIVER = tag("screwdriver");
    public static final TagKey<Item> FILE = tag("file");
    public static final TagKey<Item> HAMMER = tag("hammer");
    public static final TagKey<Item> KNIFE = tag("knife");
    public static final TagKey<Item> MONKEY_WRENCH = tag("monkey_wrench");
    public static final TagKey<Item> BENDING_CYLINDER_SMALL =
            tag("bending_cylinder_small");
    public static final TagKey<Item> SCISSORS = tag("scissors");
    public static final TagKey<Item> SOFT_HAMMER = tag("soft_hammer");
    public static final TagKey<Item> SAW = tag("saw");
    public static final TagKey<Item> WRENCH = tag("wrench");
    public static final TagKey<Item> WIRE_CUTTER = tag("wire_cutter");
    public static final TagKey<Item> CHISEL = tag("chisel");
    public static final TagKey<Item> BENDING_CYLINDER = tag("bending_cylinder");
    public static final TagKey<Item> ROLLING_PIN = tag("rolling_pin");

    private static final Map<String, TagKey<Item>> BY_PATH = Map.ofEntries(
            Map.entry("material_axe", AXE),
            Map.entry("material_crowbar", CROWBAR),
            Map.entry("material_screwdriver", SCREWDRIVER),
            Map.entry("material_file", FILE),
            Map.entry("smithing_hammer", HAMMER),
            Map.entry("material_knife", KNIFE),
            Map.entry("material_monkey_wrench", MONKEY_WRENCH),
            Map.entry("material_bending_cylinder_small", BENDING_CYLINDER_SMALL),
            Map.entry("material_scissors", SCISSORS),
            Map.entry("material_soft_hammer", SOFT_HAMMER),
            Map.entry("material_saw", SAW),
            Map.entry("material_wrench", WRENCH),
            Map.entry("material_wire_cutter", WIRE_CUTTER),
            Map.entry("material_chisel", CHISEL),
            Map.entry("material_bending_cylinder", BENDING_CYLINDER),
            Map.entry("material_rolling_pin", ROLLING_PIN));
    private static final Map<ResourceLocation, TagKey<Item>> BY_TAG_LOCATION;

    static {
        Map<ResourceLocation, TagKey<Item>> tags = new LinkedHashMap<>();
        for (TagKey<Item> tag : BY_PATH.values()) {
            tags.put(tag.location(), tag);
        }
        BY_TAG_LOCATION = Map.copyOf(tags);
    }

    private CraftingTools() {}

    public static Optional<Ingredient> tryIngredient(String logicalId) {
        return tagId(logicalId).map(Ingredient::of);
    }

    public static Optional<Ingredient> tryTag(ResourceLocation tagId) {
        if (tagId == null) {
            return Optional.empty();
        }
        TagKey<Item> tag = BY_TAG_LOCATION.get(tagId);
        if (tag == null) {
            return Optional.empty();
        }
        return Optional.of(new CraftingToolIngredient(tag).toVanilla());
    }

    public static Optional<TagKey<Item>> tagId(String logicalId) {
        ResourceLocation location = ResourceLocation.tryParse(logicalId);
        if (location == null
                || !CrucibleCraft.MODID.equals(location.getNamespace())) {
            return Optional.empty();
        }
        return Optional.ofNullable(BY_PATH.get(location.getPath()));
    }

    public static Ingredient of(Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        TagKey<Item> tag = BY_PATH.get(id.getPath());
        return tag == null ? Ingredient.of(item) : Ingredient.of(tag);
    }

    public static boolean belongs(TagKey<Item> tag, Item item) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(item);
        if (id == null || !CrucibleCraft.MODID.equals(id.getNamespace())) {
            return false;
        }
        TagKey<Item> owned = BY_PATH.get(id.getPath());
        if (owned == null) {
            return false;
        }
        return tag.equals(owned)
                || (WRENCH.equals(tag) && MONKEY_WRENCH.equals(owned));
    }

    static List<String> memberPaths(TagKey<Item> tag) {
        List<String> paths = new ArrayList<>();
        BY_PATH.forEach((path, owned) -> {
            if (tag.equals(owned)
                    || (WRENCH.equals(tag) && MONKEY_WRENCH.equals(owned))) {
                paths.add(path);
            }
        });
        return paths;
    }

    private static TagKey<Item> tag(String path) {
        return TagKey.create(
                Registries.ITEM,
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "crafting_tools/" + path));
    }
}
