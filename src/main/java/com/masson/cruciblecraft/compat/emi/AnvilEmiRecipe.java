package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

final class AnvilEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final AnvilMode mode;
    private final GTRecipe recipe;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    AnvilEmiRecipe(
            ResourceLocation id,
            AnvilMode mode,
            GTRecipe recipe,
            ItemStack hammer,
            ItemStack anvil) {
        this.id = id;
        this.mode = mode;
        this.recipe = recipe;
        inputs = java.util.stream.IntStream.range(0, recipe.itemInputs().size())
                .mapToObj(index -> EmiIngredient.of(
                        recipe.itemInputs().get(index),
                        recipe.itemInputCounts().get(index)))
                .toList();
        catalysts = List.of(EmiStack.of(hammer), EmiStack.of(anvil));
        outputs = recipe.itemOutputs().stream().map(EmiStack::of).toList();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.ANVIL;
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
        return 92;
    }

    @Override
    public int getDisplayHeight() {
        return 44;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.getFirst(), 0, 4);
        if (inputs.size() > 1) {
            widgets.addSlot(inputs.get(1), 18, 4);
        }
        widgets.addSlot(catalysts.getFirst(), 0, 22).catalyst(true);
        widgets.addSlot(catalysts.get(1), 18, 22).catalyst(true);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 28, 5);
        String modeName = mode.serializedName().replace('_', ' ');
        String chance = outputs.size() > 1
                ? " · " + recipe.outputChances().get(1) / 100.0 + "%"
                : "";
        widgets.addText(
                Component.literal(modeName + " · " + recipe.specialValue() + " hits" + chance),
                28,
                29,
                0xFF404040,
                false);
        widgets.addSlot(outputs.getFirst(), 66, 4).recipeContext(this);
        if (outputs.size() > 1) {
            widgets.addSlot(outputs.get(1), 66, 22).recipeContext(this);
        }
    }
}
