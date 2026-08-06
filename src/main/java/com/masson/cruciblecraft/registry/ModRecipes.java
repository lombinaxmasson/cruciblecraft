package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.AnvilRecipe;
import com.masson.cruciblecraft.recipe.AnvilRecipeSerializer;
import com.masson.cruciblecraft.recipe.CrusherRecipe;
import com.masson.cruciblecraft.recipe.CrusherRecipeSerializer;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntrySerializer;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleSerializer;

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

    /**
     * Legacy addon compatibility type. New data should use
     * {@code cruciblecraft:material_rule}; this type and its serializer remain
     * loadable until an addon/datapack dependency audit finds no consumers and
     * a documented compatibility window has elapsed.
     *
     * @deprecated Migrate authored recipes to {@code material_rule}.
     */
    @Deprecated(forRemoval = false)
    public static final DeferredHolder<RecipeType<?>, RecipeType<AnvilRecipe>> ANVIL_TYPE =
            RECIPE_TYPES.register(
                    "anvil",
                    () -> RecipeType.<AnvilRecipe>simple(ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "anvil")));

    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AnvilRecipe>> ANVIL_SERIALIZER =
            RECIPE_SERIALIZERS.register("anvil", AnvilRecipeSerializer::new);

    /**
     * Legacy addon compatibility type. New data should use
     * {@code cruciblecraft:material_rule}; this type and its serializer remain
     * loadable until an addon/datapack dependency audit finds no consumers and
     * a documented compatibility window has elapsed.
     *
     * @deprecated Migrate authored recipes to {@code material_rule}.
     */
    @Deprecated(forRemoval = false)
    public static final DeferredHolder<RecipeType<?>, RecipeType<CrusherRecipe>> CRUSHER_TYPE =
            RECIPE_TYPES.register(
                    "crusher",
                    () -> RecipeType.<CrusherRecipe>simple(ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "crusher")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrusherRecipe>>
            CRUSHER_SERIALIZER =
                    RECIPE_SERIALIZERS.register("crusher", CrusherRecipeSerializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<GTRecipeEntry>> GT_RECIPE_TYPE =
            RECIPE_TYPES.register(
                    "gt_recipe",
                    () -> RecipeType.<GTRecipeEntry>simple(ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "gt_recipe")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GTRecipeEntry>>
            GT_RECIPE_SERIALIZER =
                    RECIPE_SERIALIZERS.register("gt_recipe", GTRecipeEntrySerializer::new);

    public static final DeferredHolder<RecipeType<?>, RecipeType<MaterialRuleRecipe>> MATERIAL_RULE_TYPE =
            RECIPE_TYPES.register(
                    "material_rule",
                    () -> RecipeType.<MaterialRuleRecipe>simple(ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            "material_rule")));
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<MaterialRuleRecipe>>
            MATERIAL_RULE_SERIALIZER =
                    RECIPE_SERIALIZERS.register("material_rule", MaterialRuleSerializer::new);

    private ModRecipes() {}
}
