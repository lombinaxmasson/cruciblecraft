package com.masson.cruciblecraft.recipe.gt;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;

/**
 * Extracts conservative secondary-index keys for component predicates.
 * Ingredient matching remains authoritative after candidate lookup.
 */
public final class ComponentIngredientIndex {
    private static final Set<ResourceLocation> INDEXABLE_COMPONENTS = Set.of(
            ModComponents.TOOL_MATERIAL.getId(),
            ModComponents.MACHINE_MATERIAL.getId(),
            ModComponents.CIRCUIT_CONFIG.getId());

    private ComponentIngredientIndex() {}

    public static boolean isIndexableStringComponent(ResourceLocation componentId) {
        return INDEXABLE_COMPONENTS.contains(componentId);
    }

    public static Optional<DataComponentType<String>> stringComponentType(
            ResourceLocation componentId) {
        if (componentId.equals(ModComponents.TOOL_MATERIAL.getId())) {
            return Optional.of(ModComponents.TOOL_MATERIAL.get());
        }
        if (componentId.equals(ModComponents.MACHINE_MATERIAL.getId())) {
            return Optional.of(ModComponents.MACHINE_MATERIAL.get());
        }
        return Optional.empty();
    }

    public static Extraction extract(Ingredient ingredient) {
        if (!(ingredient.getCustomIngredient()
                instanceof DataComponentIngredient componentIngredient)) {
            return Extraction.unsupported();
        }
        Set<Key> keys = new HashSet<>();
        for (var entry : componentIngredient.components().asPatch().entrySet()) {
            ResourceLocation componentId =
                    BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(entry.getKey());
            if (!INDEXABLE_COMPONENTS.contains(componentId)
                    || entry.getValue().isEmpty()) {
                return Extraction.unsupported();
            }
            String value = indexableValue(
                    componentId, entry.getValue().orElseThrow());
            if (value == null) {
                return Extraction.unsupported();
            }
            for (var item : componentIngredient.items()) {
                    keys.add(new Key(
                            item.value(),
                            componentId,
                            value));
            }
        }
        return keys.isEmpty()
                ? Extraction.unsupported()
                : new Extraction(true, List.copyOf(keys));
    }

    public static List<Key> keys(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        Set<Key> keys = new HashSet<>();
        for (TypedDataComponent<?> component : stack.getComponents()) {
            ResourceLocation componentId =
                    BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(component.type());
            if (INDEXABLE_COMPONENTS.contains(componentId)) {
                String value = indexableValue(componentId, component.value());
                if (value != null) {
                    keys.add(new Key(stack.getItem(), componentId, value));
                }
            }
        }
        return List.copyOf(keys);
    }

    private static String indexableValue(ResourceLocation componentId, Object value) {
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof Integer integer
                && componentId.equals(ModComponents.CIRCUIT_CONFIG.getId())) {
            return Integer.toString(integer);
        }
        return null;
    }

    public static List<String> unsupportedIngredientTypes(GTRecipe recipe) {
        return recipe.itemInputs().stream()
                .filter(ingredient -> !ingredient.isSimple())
                .filter(ingredient -> !extract(ingredient).supported())
                .map(ingredient -> ingredient.getCustomIngredient() == null
                        ? ingredient.getClass().getName()
                        : ingredient.getCustomIngredient().getClass().getName())
                .distinct()
                .sorted()
                .toList();
    }

    public record Key(Item item, ResourceLocation componentId, String value) {}

    public record Extraction(boolean supported, List<Key> keys) {
        private static Extraction unsupported() {
            return new Extraction(false, List.of());
        }
    }
}
