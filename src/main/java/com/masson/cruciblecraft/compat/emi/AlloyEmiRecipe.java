package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

final class AlloyEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    AlloyEmiRecipe(String materialId, List<EmiIngredient> inputs, EmiStack output) {
        id = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "/alloy/" + materialId);
        this.inputs = List.copyOf(inputs);
        outputs = List.of(output);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.CRUCIBLE;
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
        return 134;
    }

    @Override
    public int getDisplayHeight() {
        return Math.max(26, ((inputs.size() + 2) / 3) * 18);
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (int index = 0; index < inputs.size(); index++) {
            widgets.addSlot(inputs.get(index), (index % 3) * 18, (index / 3) * 18);
        }
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 72, 5);
        widgets.addSlot(outputs.getFirst(), 108, 4).recipeContext(this);
    }
}
