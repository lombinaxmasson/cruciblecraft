package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.AnvilRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.render.EmiTexture;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;

final class AnvilEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;
    private final RecipeHolder<AnvilRecipe> backing;

    AnvilEmiRecipe(
            RecipeHolder<AnvilRecipe> backing,
            String materialId,
            Item input,
            Item secondInput,
            Item output,
            Item secondaryOutput,
            ItemStack hammer,
            ItemStack anvil) {
        AnvilRecipe recipe = backing.value();
        ResourceLocation recipeId = backing.id();
        id = ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "anvil/" + recipeId.getNamespace() + "/" + recipeId.getPath() + "/" + materialId);
        var inputList = new java.util.ArrayList<EmiIngredient>();
        inputList.add(EmiStack.of(input, recipe.inputCount()));
        if (secondInput != null) {
            inputList.add(EmiStack.of(secondInput, recipe.secondInputCount()));
        }
        inputs = List.copyOf(inputList);
        catalysts = List.of(EmiStack.of(hammer), EmiStack.of(anvil));
        var outputList = new java.util.ArrayList<EmiStack>();
        outputList.add(EmiStack.of(output, recipe.outputCount()));
        if (secondaryOutput != null) {
            outputList.add(EmiStack.of(secondaryOutput, recipe.secondaryOutputCount()));
        }
        outputs = List.copyOf(outputList);
        this.backing = backing;
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
        String mode = backing.value().mode().serializedName().replace('_', ' ');
        String chance = backing.value().secondaryOutput().isPresent()
                ? " · " + Math.round(backing.value().secondaryChance() * 100.0) + "%"
                : "";
        widgets.addText(Component.literal(mode + chance), 28, 29, 0xFF404040, false);
        widgets.addSlot(outputs.getFirst(), 66, 4).recipeContext(this);
        if (outputs.size() > 1) {
            widgets.addSlot(outputs.get(1), 66, 22).recipeContext(this);
        }
    }

    @Override
    public RecipeHolder<?> getBackingRecipe() {
        return backing;
    }
}
