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
import net.minecraft.world.item.Item;

final class MoldCastingEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    MoldCastingEmiRecipe(String materialId, String form, Item input, Item mold, Item output, int count) {
        id = EmiIds.synthetic(ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "mold_casting/" + materialId + "/" + form));
        inputs = List.of(EmiStack.of(input));
        catalysts = List.of(EmiStack.of(mold));
        outputs = List.of(EmiStack.of(output, count));
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.MOLD_CASTING;
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
        return 92;
    }

    @Override
    public int getDisplayHeight() {
        return 44;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        widgets.addSlot(inputs.getFirst(), 0, 4);
        widgets.addSlot(catalysts.getFirst(), 0, 22).catalyst(true);
        widgets.addTexture(EmiTexture.EMPTY_ARROW, 28, 5);
        widgets.addSlot(outputs.getFirst(), 66, 4).recipeContext(this);
    }
}
