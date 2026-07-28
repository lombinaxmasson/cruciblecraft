package com.masson.cruciblecraft.recipe;

import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.heat.HeatComponent;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipes;

import net.minecraft.core.HolderLookup;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

public record AnvilRecipe(
        MaterialForm input,
        int inputCount,
        Optional<MaterialForm> secondInput,
        int secondInputCount,
        MaterialForm output,
        int outputCount,
        int hits,
        Optional<String> material,
        AnvilMode mode,
        Optional<MaterialForm> secondaryOutput,
        int secondaryOutputCount,
        double secondaryChance,
        long recipePower) implements Recipe<AnvilRecipeInput> {

    public AnvilRecipe(MaterialForm input, MaterialForm output, int outputCount, int hits) {
        this(input, 1, Optional.empty(), 1, output, outputCount, hits, Optional.empty(),
                AnvilMode.ANVIL, Optional.empty(), 1, 0.0, 10_000L);
    }

    public AnvilRecipe(
            MaterialForm input,
            MaterialForm output,
            int outputCount,
            int hits,
            Optional<String> material) {
        this(input, 1, Optional.empty(), 1, output, outputCount, hits, material,
                AnvilMode.ANVIL, Optional.empty(), 1, 0.0, 10_000L);
    }

    public AnvilRecipe {
        material = AnvilRecipeRules.normalizeMaterial(material);
        AnvilRecipeRules.validate(
                input, inputCount, secondInput, secondInputCount, output, outputCount,
                hits, secondaryOutput, secondaryOutputCount, secondaryChance, recipePower);
    }

    @Override
    public boolean matches(AnvilRecipeInput recipeInput, Level level) {
        return recipeInput.mode() == mode && matchPlan(recipeInput).isPresent();
    }

    @Override
    public ItemStack assemble(AnvilRecipeInput recipeInput, HolderLookup.Provider registries) {
        var plan = matchPlan(recipeInput);
        if (plan.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = recipeInput.getItem(plan.get().primarySlot());
        var entry = MaterialUnits.resolve(source);
        if (entry.isEmpty()) {
            return ItemStack.EMPTY;
        }

        var outputItem = MaterialLookup.item(entry.get().material().id(), output);
        if (outputItem.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack result = new ItemStack(outputItem.get(), outputCount);
        HeatComponent heat = source.get(ModComponents.HEAT.get());
        if (heat != null) {
            result.set(ModComponents.HEAT.get(), heat);
        }
        return result;
    }

    public ItemStack assembleSecondary(AnvilRecipeInput recipeInput) {
        if (secondaryOutput.isEmpty()) {
            return ItemStack.EMPTY;
        }
        Optional<MatchPlan> plan = matchPlan(recipeInput);
        if (plan.isEmpty()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = recipeInput.getItem(plan.get().primarySlot());
        var entry = MaterialUnits.resolve(source);
        if (entry.isEmpty()) {
            return ItemStack.EMPTY;
        }
        return MaterialLookup.item(entry.get().material().id(), secondaryOutput.get())
                .map(item -> new ItemStack(item, secondaryOutputCount))
                .orElse(ItemStack.EMPTY);
    }

    public Optional<MatchPlan> matchPlan(AnvilRecipeInput recipeInput) {
        Optional<MatchPlan> direct = matchOrder(recipeInput.first(), recipeInput.second(), 0, 1);
        return direct.isPresent() || secondInput.isEmpty()
                ? direct
                : matchOrder(recipeInput.second(), recipeInput.first(), 1, 0);
    }

    private Optional<MatchPlan> matchOrder(
            ItemStack primary,
            ItemStack secondary,
            int primarySlot,
            int secondarySlot) {
        var primaryEntry = MaterialUnits.resolve(primary);
        if (primaryEntry.isEmpty()
                || primaryEntry.get().form() != input
                || primary.getCount() < inputCount
                || material.filter(value -> !value.equals(primaryEntry.get().material().id())).isPresent()
                || MaterialLookup.item(primaryEntry.get().material().id(), output).isEmpty()
                || secondaryOutput
                        .filter(form -> MaterialLookup.item(primaryEntry.get().material().id(), form).isEmpty())
                        .isPresent()) {
            return Optional.empty();
        }
        if (secondInput.isEmpty()) {
            return Optional.of(new MatchPlan(primarySlot, inputCount, -1, 0));
        }
        var secondaryEntry = MaterialUnits.resolve(secondary);
        if (secondaryEntry.isEmpty()
                || secondaryEntry.get().form() != secondInput.get()
                || secondary.getCount() < secondInputCount
                || !secondaryEntry.get().material().id().equals(primaryEntry.get().material().id())) {
            return Optional.empty();
        }
        return Optional.of(new MatchPlan(
                primarySlot, inputCount, secondarySlot, secondInputCount));
    }

    public record MatchPlan(
            int primarySlot,
            int primaryCount,
            int secondarySlot,
            int secondaryCount) {}

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return true;
    }

    @Override
    public ItemStack getResultItem(HolderLookup.Provider registries) {
        return ItemStack.EMPTY;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.ANVIL_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipes.ANVIL_TYPE.get();
    }
}
