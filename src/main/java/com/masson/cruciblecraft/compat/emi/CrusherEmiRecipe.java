package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

final class CrusherEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    CrusherEmiRecipe(ResourceLocation id, GTRecipe recipe) {
        this.id = id;
        inputs = java.util.stream.IntStream.range(0, recipe.itemInputs().size())
                .mapToObj(index -> EmiIngredient.of(
                        recipe.itemInputs().get(index),
                        recipe.itemInputCounts().get(index)))
                .toList();
        outputs = recipe.itemOutputs().stream().map(EmiStack::of).toList();
    }

    @Override public EmiRecipeCategory getCategory() { return CrucibleCraftEmiPlugin.CRUSHER; }
    @Override public ResourceLocation getId() {
        return id;
    }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public int getDisplayWidth() { return 78; }
    @Override public int getDisplayHeight() { return 28; }
    @Override public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.getFirst(), 0, 5);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 26, 5);
        widgets.addSlot(outputs.getFirst(), 58, 5).recipeContext(this);
    }
}
