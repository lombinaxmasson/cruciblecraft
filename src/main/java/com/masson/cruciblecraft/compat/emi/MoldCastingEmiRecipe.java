package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

final class MoldCastingEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    MoldCastingEmiRecipe(
            String materialId,
            String form,
            ItemStack input,
            Item mold,
            ItemStack output,
            int count) {
        id = EmiIds.synthetic(ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "mold_casting/" + materialId + "/" + form));
        texture = Gt6EmiGui.texture("default");
        inputs = List.of(EmiStack.of(input));
        catalysts = List.of(EmiStack.of(mold));
        ItemStack result = output.copy();
        result.setCount(count);
        outputs = List.of(EmiStack.of(result));
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
        return ProcessingEmiLayout.PANEL_WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return ProcessingEmiLayout.NEI_HEIGHT;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        Gt6EmiGui.addPanel(widgets, texture);
        Gt6EmiGui.addProgress(
                widgets,
                texture,
                new ProcessingEmiLayout.Rect(
                        Gt6BasicMachineGui.PROGRESS_X,
                        Gt6BasicMachineGui.PROGRESS_Y,
                        Gt6BasicMachineGui.PROGRESS_WIDTH,
                        Gt6BasicMachineGui.PROGRESS_HEIGHT),
                20);
        var inSlot = Gt6BasicMachineGui.inputSlots(1, 0).getFirst();
        Gt6EmiGui.slot(widgets, inputs.getFirst(), inSlot.x(), inSlot.y());
        Gt6EmiGui.catalyst(
                widgets,
                catalysts.getFirst(),
                Gt6BasicMachineGui.SPECIAL_SLOT_X,
                Gt6BasicMachineGui.SPECIAL_SLOT_Y);
        var outSlot = Gt6BasicMachineGui.outputSlots(1, 0).getFirst();
        Gt6EmiGui.output(widgets, outputs.getFirst(), this, outSlot.x(), outSlot.y());
    }
}
