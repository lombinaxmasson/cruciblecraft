package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.AnvilRecipe;
import com.masson.cruciblecraft.recipe.AnvilRecipeSerializer;
import com.masson.cruciblecraft.recipe.CokeOvenRecipe;
import com.masson.cruciblecraft.recipe.CokeOvenRecipeSerializer;
import com.masson.cruciblecraft.recipe.CrusherRecipe;
import com.masson.cruciblecraft.recipe.CrusherRecipeSerializer;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModRecipes {
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, CrucibleCraft.MODID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, CrucibleCraft.MODID);

    public static final DeferredHolder<RecipeType<?>, RecipeType<AnvilRecipe>> ANVIL_TYPE =
            RECIPE_TYPES.register(
                    "anvil",
                    () -> RecipeType.<AnvilRecipe>simple(ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "anvil")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AnvilRecipe>> ANVIL_SERIALIZER =
            RECIPE_SERIALIZERS.register("anvil", AnvilRecipeSerializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CokeOvenRecipe>> COKE_OVEN_TYPE =
            RECIPE_TYPES.register(
                    "coke_oven",
                    () -> RecipeType.<CokeOvenRecipe>simple(ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "coke_oven")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CokeOvenRecipe>>
            COKE_OVEN_SERIALIZER =
                    RECIPE_SERIALIZERS.register("coke_oven", CokeOvenRecipeSerializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrusherRecipe>> CRUSHER_TYPE =
            RECIPE_TYPES.register(
                    "crusher",
                    () -> RecipeType.<CrusherRecipe>simple(ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "crusher")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrusherRecipe>>
            CRUSHER_SERIALIZER =
                    RECIPE_SERIALIZERS.register("crusher", CrusherRecipeSerializer::new);

    private ModRecipes() {}
}
