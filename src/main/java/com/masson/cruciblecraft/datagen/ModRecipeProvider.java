package com.masson.cruciblecraft.datagen;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.T2ChainRules;
import com.masson.cruciblecraft.recipe.rule.T3ComponentRules;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeEntry;
import com.masson.cruciblecraft.registry.ModFluids;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.neoforged.neoforge.common.conditions.ICondition;
import net.neoforged.neoforge.fluids.FluidStack;

/** Recipes derived mechanically from the bootstrapped material catalog. */
public final class ModRecipeProvider extends RecipeProvider {
    private static final String COMPAT_SHORTCUT_GROUP = "cruciblecraft:compat_shortcut";
    private static final float ORE_EXPERIENCE = 0.7F;
    private static final int SMELTING_TIME = 200;
    private static final int BLASTING_TIME = 100;

    public ModRecipeProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    protected void buildRecipes(RecipeOutput output) {
        RecipeOutput recipesOnly = new AdvancementFreeRecipeOutput(output);
        addMachineRecipes(recipesOnly);
        MaterialCatalog.startupValues().stream()
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .forEach(material -> addDerivedOreRecipes(recipesOnly, material));
    }

    private static void addMachineRecipes(RecipeOutput output) {
        machineCrafting(output, ModItems.SLUICE.get(), "sluice");
        machineCrafting(output, ModItems.BATH.get(), "bath");
        machineCrafting(output, ModItems.CENTRIFUGE.get(), "centrifuge");
        machineCrafting(output, ModItems.SHREDDER.get(), "shredder");
        machineCrafting(output, ModItems.SIFTER.get(), "sifter");
        machineCrafting(output, ModItems.SMELTER.get(), "smelter");
        machineCrafting(output, ModItems.MORTAR.get(), "mortar");
        machineCrafting(output, ModItems.EXTRUDER.get(), "extruder");
        machineCrafting(output, ModItems.CUTTER.get(), "cutter");
        machineCrafting(output, ModItems.LATHE.get(), "lathe");
        machineCrafting(output, ModItems.ROLLINGMILL.get(), "rollingmill");
        machineCrafting(output, ModItems.ROLLBENDER.get(), "rollbender");
        machineCrafting(output, ModItems.WIREMILL.get(), "wiremill");
        machineCrafting(output, ModItems.BENDER.get(), "bender");
        machineCrafting(output, ModItems.ASSEMBLER.get(), "assembler");
        machineCrafting(output, ModItems.WELDER.get(), "welder");
        machineCrafting(output, ModItems.PRESS.get(), "press");
        output.accept(
                id("coke_oven/coal"),
                new GTRecipeEntry(
                        ModRecipeMaps.COKE_OVEN.id(),
                        new GTRecipe(
                                List.of(Ingredient.of(Items.COAL)),
                                List.of(1),
                                List.of(new ItemStack(ModItems.COAL_COKE.get())),
                                List.of(),
                                List.of(new FluidStack(ModFluids.CREOSOTE_SOURCE.get(), 500)),
                                List.of(GTRecipe.GUARANTEED_CHANCE),
                                3_600,
                                0L,
                                0L)),
                null);
        output.accept(
                id("anvil/ingot_to_plate"),
                materialRule(ModRecipeMaps.ANVIL.id(), MaterialPrefixes.INGOT,
                        MaterialPrefixes.PLATE, 1, 1, 1, 10_000, 4, Map.of()),
                null);
        output.accept(
                id("anvil/plate_to_rods"),
                materialRule(ModRecipeMaps.ANVIL.id(), MaterialPrefixes.PLATE,
                        MaterialPrefixes.ROD, 1, 2, 1, 10_000, 5, Map.of()),
                null);
        output.accept(
                id("anvil/rod_to_bolts"),
                materialRule(ModRecipeMaps.ANVIL.id(), MaterialPrefixes.ROD,
                        MaterialPrefixes.BOLT, 1, 4, 1, 10_000, 3, Map.of()),
                null);
        output.accept(
                id("anvil/raw_ore_to_crushed_ore"),
                new MaterialRuleRecipe(T2ChainRules.ANVIL_RAW_TO_CRUSHED),
                null);
        T2ChainRules.ALL.stream()
                .filter(definition ->
                        !T2ChainRules.CONCRETE_ORE_CHAIN_PATHS.contains(definition.path()))
                .forEach(definition -> output.accept(
                        id(definition.path()), new MaterialRuleRecipe(definition.rule()), null));
        T3ComponentRules.ALL.forEach(definition -> output.accept(
                id(definition.path()), new MaterialRuleRecipe(definition.rule()), null));
    }

    private static void machineCrafting(RecipeOutput output, Item result, String id) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result)
                .pattern("CCC")
                .pattern("CFC")
                .pattern("CCC")
                .define('C', Items.COPPER_INGOT)
                .define('F', Items.FURNACE)
                .unlockedBy("has_copper", has(Items.COPPER_INGOT))
                .save(output, ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID, "machines/" + id));
    }

    private static void addDerivedOreRecipes(
            RecipeOutput output,
            MaterialDefinition material) {
        if (!MaterialCatalog.isFormRegistered(material, MaterialPrefixes.INGOT)) {
            return;
        }
        if (MaterialCatalog.isFormRegistered(material, MaterialPrefixes.CRUSHED_ORE)) {
            addOreRecipes(output, material, MaterialPrefixes.CRUSHED_ORE);
        }
        if (MaterialCatalog.isFormRegistered(material, MaterialPrefixes.RAW_ORE)
                && !material.formItems().containsKey(MaterialPrefixes.RAW_ORE)) {
            addOreRecipes(output, material, MaterialPrefixes.RAW_ORE);
        }
    }

    private static void addOreRecipes(
            RecipeOutput output,
            MaterialDefinition material,
            MaterialPrefix inputForm) {
        Item ore = MaterialLookup.item(material.id(), inputForm)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing " + inputForm.serializedName() + " item for " + material.id()));
        Item ingot = MaterialLookup.item(material.id(), MaterialPrefixes.INGOT)
                .orElseThrow(() -> new IllegalStateException(
                        "Missing ingot item for " + material.id()));
        Ingredient ingredient = Ingredient.of(ore);
        String unlockName = "has_" + material.registryName(inputForm);

        SimpleCookingRecipeBuilder.smelting(
                        ingredient,
                        RecipeCategory.MISC,
                        ingot,
                        ORE_EXPERIENCE,
                        SMELTING_TIME)
                .group(COMPAT_SHORTCUT_GROUP)
                .unlockedBy(unlockName, has(ore))
                .save(output, recipeId(material, inputForm, "smelting"));
        SimpleCookingRecipeBuilder.blasting(
                        ingredient,
                        RecipeCategory.MISC,
                        ingot,
                        ORE_EXPERIENCE,
                        BLASTING_TIME)
                .group(COMPAT_SHORTCUT_GROUP)
                .unlockedBy(unlockName, has(ore))
                .save(output, recipeId(material, inputForm, "blasting"));
    }

    private static ResourceLocation recipeId(
            MaterialDefinition material,
            MaterialPrefix inputForm,
            String process) {
        return ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                material.registryName(inputForm) + "_" + process);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, path);
    }

    private static MaterialRuleRecipe materialRule(
            ResourceLocation target,
            MaterialPrefix input,
            MaterialPrefix output,
            int inputCount,
            int outputCount,
            int duration,
            long eut,
            long specialValue,
            Map<String, Integer> durationOverrides) {
        Map<String, MaterialRule.MaterialOverride> overrides = durationOverrides.entrySet().stream()
                .collect(java.util.stream.Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> new MaterialRule.MaterialOverride(
                                java.util.Optional.of(Integer.toString(entry.getValue())),
                                java.util.Optional.empty(),
                                java.util.Optional.empty(),
                                Map.of(), Map.of(), Map.of(), Map.of())));
        return new MaterialRuleRecipe(new MaterialRule(
                java.util.Optional.of(target),
                List.of(ruleItem(input, inputCount)),
                List.of(ruleItem(output, outputCount)),
                List.of(),
                List.of(),
                Integer.toString(duration),
                Long.toString(eut),
                Long.toString(specialValue),
                true,
                java.util.Optional.empty(),
                overrides,
                List.of(),
                java.util.Optional.empty(),
                List.of()));
    }

    private record AdvancementFreeRecipeOutput(RecipeOutput delegate) implements RecipeOutput {
        @Override
        public void accept(
                ResourceLocation id,
                Recipe<?> recipe,
                AdvancementHolder advancement) {
            delegate.accept(id, recipe, null);
        }

        @Override
        public void accept(
                ResourceLocation id,
                Recipe<?> recipe,
                AdvancementHolder advancement,
                ICondition... conditions) {
            delegate.accept(id, recipe, null, conditions);
        }

        @Override
        public Advancement.Builder advancement() {
            return delegate.advancement();
        }
    }

    private static MaterialRule.ItemResource ruleItem(
            MaterialPrefix prefix,
            int count) {
        return new MaterialRule.ItemResource(
                java.util.Optional.of(prefix.serializedId()),
                java.util.Optional.empty(),
                Integer.toString(count),
                "10000");
    }

}
