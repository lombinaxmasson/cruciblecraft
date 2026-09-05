package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

/** EMI view for converter fuel maps (engine / burn / fluid-bed). */
final class FuelMapEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final EmiRecipeCategory category;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    FuelMapEmiRecipe(
            ResourceLocation id, EmiRecipeCategory category, GTRecipe recipe) {
        this.id = id;
        this.category = category;
        List<EmiIngredient> displayedInputs = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            int count = index < recipe.itemInputCounts().size()
                    ? recipe.itemInputCounts().get(index)
                    : 1;
            displayedInputs.add(EmiIngredient.of(
                    recipe.itemInputs().get(index), count));
        }
        recipe.fluidInputs().forEach(stack ->
                displayedInputs.add(EmiStack.of(
                        stack.getFluid(), stack.getAmount())));
        inputs = List.copyOf(displayedInputs);
        List<EmiStack> displayedOutputs = new ArrayList<>();
        recipe.itemOutputs().forEach(stack ->
                displayedOutputs.add(EmiStack.of(stack)));
        recipe.fluidOutputs().forEach(stack ->
                displayedOutputs.add(EmiStack.of(
                        stack.getFluid(), stack.getAmount())));
        outputs = List.copyOf(displayedOutputs);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
    }

    @Override
    public ResourceLocation getId() {
        return id;
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
        return 132;
    }

    @Override
    public int getDisplayHeight() {
        return 28;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 0;
        for (EmiIngredient input : inputs) {
            widgets.addSlot(input, x, 5);
            x += 20;
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, x, 5);
        x += 24;
        if (outputs.isEmpty()) {
            widgets.addSlot(x, 5);
            return;
        }
        for (EmiStack output : outputs) {
            widgets.addSlot(output, x, 5).recipeContext(this);
            x += 20;
        }
    }
}
