package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.crafting.CraftingToolIngredient;

import net.neoforged.neoforge.common.crafting.IngredientType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

public final class ModIngredientTypes {
    public static final DeferredRegister<IngredientType<?>> INGREDIENT_TYPES =
            DeferredRegister.create(
                    NeoForgeRegistries.Keys.INGREDIENT_TYPES, CrucibleCraft.MODID);

    public static final DeferredHolder<
            IngredientType<?>, IngredientType<CraftingToolIngredient>> CRAFTING_TOOL =
            INGREDIENT_TYPES.register(
                    "crafting_tool",
                    () -> new IngredientType<>(CraftingToolIngredient.CODEC));

    private ModIngredientTypes() {}
}
