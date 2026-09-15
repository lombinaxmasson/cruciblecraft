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

/** Fluid-forward EMI view for the 18 fusion rows. */
final class FusionEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    FusionEmiRecipe(ResourceLocation id, GTRecipe recipe) {
        this.id = EmiIds.synthetic(id);
        List<EmiIngredient> displayedInputs = new ArrayList<>();
        List<EmiIngredient> displayedCatalysts = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            int count = recipe.itemInputCounts().get(index);
            EmiIngredient ingredient = EmiIngredient.of(
                    recipe.itemInputs().get(index), Math.max(1, count));
            if (count == 0) {
                displayedCatalysts.add(ingredient);
            } else {
                displayedInputs.add(ingredient);
            }
        }
        recipe.fluidInputs().forEach(stack ->
                displayedInputs.add(EmiStacks.ofFluid(stack)));
        inputs = List.copyOf(displayedInputs);
        catalysts = List.copyOf(displayedCatalysts);
        List<EmiStack> displayedOutputs = new ArrayList<>();
        recipe.itemOutputs().forEach(stack ->
                displayedOutputs.add(EmiStacks.ofItem(stack)));
        recipe.fluidOutputs().forEach(stack ->
                displayedOutputs.add(EmiStacks.ofFluid(stack)));
        outputs = List.copyOf(displayedOutputs);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.FUSION;
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
    public List<EmiIngredient> getCatalysts() {
        return catalysts;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return outputs;
    }

    @Override
    public int getDisplayWidth() {
        return 176;
    }

    @Override
    public int getDisplayHeight() {
        return 54;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        int x = 0;
        for (EmiIngredient catalyst : catalysts) {
            widgets.addSlot(catalyst, x, 5).catalyst(true);
            x += 20;
        }
        for (EmiIngredient input : inputs) {
            widgets.addSlot(input, x, 5);
            x += 20;
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, Math.min(x, 72), 24);
        int outX = 0;
        for (EmiStack output : outputs) {
            widgets.addSlot(output, outX, 32).recipeContext(this);
            outX += 20;
        }
    }
}
