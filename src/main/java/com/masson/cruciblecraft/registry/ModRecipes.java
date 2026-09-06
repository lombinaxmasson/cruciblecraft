package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.gt.CompactDedupRuleEntry;
import com.masson.cruciblecraft.recipe.gt.CompactDedupRuleSerializer;
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilyEntry;
import com.masson.cruciblecraft.recipe.gt.CompactGTRecipeFamilySerializer;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationPolicyEntry;
import com.masson.cruciblecraft.recipe.gt.CompactPublicationPolicySerializer;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntrySerializer;
import com.masson.cruciblecraft.recipe.crafting.BatteryCellCraftingRecipe;
import com.masson.cruciblecraft.recipe.crafting.BatteryCellCraftingRecipeSerializer;
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

    public static final DeferredHolder<RecipeType<?>, RecipeType<CompactGTRecipeFamilyEntry>>
            COMPACT_GT_RECIPE_FAMILY_TYPE =
                    RECIPE_TYPES.register(
                            "compact_gt_recipe_family",
                            () -> RecipeType.<CompactGTRecipeFamilyEntry>simple(
                                    ResourceLocation.fromNamespaceAndPath(
                                            CrucibleCraft.MODID,
                                            "compact_gt_recipe_family")));
    public static final DeferredHolder<
                    RecipeSerializer<?>, RecipeSerializer<CompactGTRecipeFamilyEntry>>
            COMPACT_GT_RECIPE_FAMILY_SERIALIZER =
                    RECIPE_SERIALIZERS.register(
                            "compact_gt_recipe_family",
                            CompactGTRecipeFamilySerializer::new);

    public static final DeferredHolder<
                    RecipeType<?>, RecipeType<CompactPublicationPolicyEntry>>
            COMPACT_PUBLICATION_POLICY_TYPE =
                    RECIPE_TYPES.register(
                            "compact_publication_policy",
                            () -> RecipeType.<CompactPublicationPolicyEntry>simple(
                                    ResourceLocation.fromNamespaceAndPath(
                                            CrucibleCraft.MODID,
                                            "compact_publication_policy")));
    public static final DeferredHolder<
                    RecipeSerializer<?>,
                    RecipeSerializer<CompactPublicationPolicyEntry>>
            COMPACT_PUBLICATION_POLICY_SERIALIZER =
                    RECIPE_SERIALIZERS.register(
                            "compact_publication_policy",
                            CompactPublicationPolicySerializer::new);

    public static final DeferredHolder<
                    RecipeType<?>, RecipeType<CompactDedupRuleEntry>>
            COMPACT_DEDUP_RULE_TYPE =
                    RECIPE_TYPES.register(
                            "compact_dedup_rule",
                            () -> RecipeType.<CompactDedupRuleEntry>simple(
                                    ResourceLocation.fromNamespaceAndPath(
                                            CrucibleCraft.MODID,
                                            "compact_dedup_rule")));
    public static final DeferredHolder<
                    RecipeSerializer<?>,
                    RecipeSerializer<CompactDedupRuleEntry>>
            COMPACT_DEDUP_RULE_SERIALIZER =
                    RECIPE_SERIALIZERS.register(
                            "compact_dedup_rule",
                            CompactDedupRuleSerializer::new);

    public static final DeferredHolder<
            RecipeType<?>, RecipeType<BatteryCellCraftingRecipe>>
            BATTERY_CELL_CRAFTING_TYPE =
                    RECIPE_TYPES.register(
                            "battery_cell_crafting",
                            () -> RecipeType.<BatteryCellCraftingRecipe>simple(
                                    ResourceLocation.fromNamespaceAndPath(
                                            CrucibleCraft.MODID,
                                            "battery_cell_crafting")));
    public static final DeferredHolder<
            RecipeSerializer<?>, RecipeSerializer<BatteryCellCraftingRecipe>>
            BATTERY_CELL_CRAFTING_SERIALIZER =
                    RECIPE_SERIALIZERS.register(
                            "battery_cell_crafting",
                            BatteryCellCraftingRecipeSerializer::new);

    private ModRecipes() {}
}
