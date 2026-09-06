package com.masson.cruciblecraft.machine.processing;

import java.util.Set;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

/**
 * Registry-safe classification for reusable crafting inputs.
 *
 * <p>Exact registry paths cover built-in and planned CrucibleCraft tools without
 * requiring their item fields to exist yet. Tags provide an extension point for
 * compatibility items, while the pattern prefix preserves the existing selector
 * family.
 */
public final class CraftingCatalystPolicy {
    public static final TagKey<Item> WEAR_CATALYSTS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "crafting_wear_catalysts"));
    public static final TagKey<Item> PRESERVED_PATTERNS = TagKey.create(
            Registries.ITEM,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "crafting_patterns"));

    private static final Set<String> WEAR_PATHS = Set.of(
            "material_file",
            "smithing_hammer",
            "flint_knife",
            "material_screwdriver",
            "material_wrench",
            "material_monkey_wrench");
    private static final String TOOL_PATTERN_PREFIX = "tool_pattern_";

    private CraftingCatalystPolicy() {}

    public static boolean isWearCatalyst(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(WEAR_CATALYSTS)
                || isWearCatalyst(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    public static boolean isWearCatalyst(ResourceLocation itemId) {
        return itemId != null
                && CrucibleCraft.MODID.equals(itemId.getNamespace())
                && WEAR_PATHS.contains(itemId.getPath());
    }

    public static boolean isPreservedPattern(ItemStack stack) {
        return !stack.isEmpty()
                && (stack.is(PRESERVED_PATTERNS)
                || isPreservedPattern(BuiltInRegistries.ITEM.getKey(stack.getItem())));
    }

    public static boolean isPreservedPattern(ResourceLocation itemId) {
        return itemId != null
                && CrucibleCraft.MODID.equals(itemId.getNamespace())
                && itemId.getPath().startsWith(TOOL_PATTERN_PREFIX)
                && itemId.getPath().length() > TOOL_PATTERN_PREFIX.length();
    }

    public static boolean isProgrammingCircuit(ItemStack stack) {
        return !stack.isEmpty()
                && isProgrammingCircuit(BuiltInRegistries.ITEM.getKey(stack.getItem()));
    }

    public static boolean isProgrammingCircuit(ResourceLocation itemId) {
        return itemId != null
                && CrucibleCraft.MODID.equals(itemId.getNamespace())
                && "programmed_circuit".equals(itemId.getPath());
    }

    public static boolean isCatalyst(ItemStack stack) {
        return isWearCatalyst(stack)
                || isPreservedPattern(stack)
                || isProgrammingCircuit(stack);
    }

    public static boolean acceptsMaterialSlot(ItemStack stack) {
        return !stack.isEmpty() && !isCatalyst(stack);
    }

    public static boolean acceptsToolSlot(ItemStack stack) {
        return isCatalyst(stack);
    }

    public static boolean acceptsIngredient(
            Ingredient ingredient,
            ItemInputAction.Kind action) {
        ItemStack[] candidates = ingredient.getItems();
        if (candidates.length == 0) {
            return false;
        }
        return switch (action) {
            case CONSUME -> java.util.Arrays.stream(candidates)
                    .noneMatch(CraftingCatalystPolicy::isCatalyst);
            case WEAR -> java.util.Arrays.stream(candidates)
                    .allMatch(CraftingCatalystPolicy::isWearCatalyst);
            case PRESERVE -> java.util.Arrays.stream(candidates)
                    .allMatch(stack -> isPreservedPattern(stack)
                            || isProgrammingCircuit(stack));
        };
    }
}
