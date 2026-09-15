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

final class CokeOvenEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    CokeOvenEmiRecipe(ResourceLocation id, GTRecipe recipe) {
        this.id = EmiIds.synthetic(id);
        inputs = java.util.stream.IntStream.range(0, recipe.itemInputs().size())
                .mapToObj(index -> EmiIngredient.of(
                        recipe.itemInputs().get(index),
                        recipe.itemInputCounts().get(index)))
                .toList();
        var displayedOutputs = new java.util.ArrayList<EmiStack>();
        recipe.itemOutputs().forEach(stack -> displayedOutputs.add(EmiStacks.ofItem(stack)));
        recipe.fluidOutputs().forEach(stack -> displayedOutputs.add(EmiStacks.ofFluid(stack)));
        outputs = List.copyOf(displayedOutputs);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.COKE_OVEN;
    }

    @Override
    public boolean hideCraftable() {
        return true;
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
}
