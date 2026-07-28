package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.recipe.CrusherRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.RecipeHolder;

final class CrusherEmiRecipe implements EmiRecipe {
    private final RecipeHolder<CrusherRecipe> backing;
    private final String material;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;

    CrusherEmiRecipe(RecipeHolder<CrusherRecipe> backing, String material, Item input, Item output) {
        this.backing = backing;
        this.material = material;
        inputs = List.of(EmiStack.of(input));
        outputs = List.of(EmiStack.of(output, backing.value().outputCount()));
    }

    @Override public EmiRecipeCategory getCategory() { return CrucibleCraftEmiPlugin.CRUSHER; }
    @Override public ResourceLocation getId() {
        return backing.id().withSuffix("/" + material);
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
    @Override public RecipeHolder<?> getBackingRecipe() { return backing; }
}
