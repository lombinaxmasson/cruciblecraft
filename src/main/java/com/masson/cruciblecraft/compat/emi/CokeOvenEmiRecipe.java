package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.recipe.CokeOvenRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.RecipeHolder;

final class CokeOvenEmiRecipe implements EmiRecipe {
    private final RecipeHolder<CokeOvenRecipe> backing;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    CokeOvenEmiRecipe(RecipeHolder<CokeOvenRecipe> backing) {
        this.backing = backing;
        CokeOvenRecipe recipe = backing.value();
        inputs = List.of(EmiIngredient.of(recipe.input()));
        outputs = List.of(
                EmiStack.of(recipe.output()),
                EmiStack.of(
                        recipe.fluidOutput().getFluid(),
                        recipe.fluidOutput().getAmount()));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.COKE_OVEN;
    }

    @Override
    public ResourceLocation getId() {
        return backing.id();
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return inputs;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 116;
    }

    @Override
    public int getDisplayHeight() {
        return 28;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.getFirst(), 0, 5);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 28, 5);
        widgets.addSlot(outputs.getFirst(), 66, 5).recipeContext(this);
        widgets.addSlot(outputs.get(1), 90, 5).recipeContext(this);
    }

    @Override
    public RecipeHolder<?> getBackingRecipe() {
        return backing;
    }
}
