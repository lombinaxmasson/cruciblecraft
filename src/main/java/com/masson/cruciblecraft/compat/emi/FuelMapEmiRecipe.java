package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

/** EMI view for converter fuel maps (engine / burn / fluid-bed). */
final class FuelMapEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final EmiRecipeCategory category;
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private final int durationTicks;

    FuelMapEmiRecipe(
            ResourceLocation id, EmiRecipeCategory category, GTRecipe recipe) {
        this.id = EmiIds.synthetic(id);
        this.category = category;
        texture = Gt6EmiGui.texture("default");
        List<EmiIngredient> displayedInputs = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            int count = index < recipe.itemInputCounts().size()
                    ? recipe.itemInputCounts().get(index)
                    : 1;
            displayedInputs.add(EmiStacks.ofIngredient(
                    recipe.itemInputs().get(index), count));
        }
        recipe.fluidInputs().forEach(stack ->
                displayedInputs.add(EmiStacks.ofFluid(stack)));
        inputs = List.copyOf(displayedInputs);
        List<EmiStack> displayedOutputs = new ArrayList<>();
        recipe.itemOutputs().forEach(stack ->
                displayedOutputs.add(EmiStacks.ofItem(stack)));
        recipe.fluidOutputs().forEach(stack ->
                displayedOutputs.add(EmiStacks.ofFluid(stack)));
        outputs = List.copyOf(displayedOutputs);
        durationTicks = recipe.duration();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
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
                durationTicks);
        var inSlots = Gt6BasicMachineGui.inputSlots(Math.max(1, inputs.size()), 0);
        for (int index = 0; index < inputs.size() && index < inSlots.size(); index++) {
            Gt6EmiGui.slot(
                    widgets,
                    inputs.get(index),
                    inSlots.get(index).x(),
                    inSlots.get(index).y());
        }
        if (outputs.isEmpty()) {
            var out = Gt6BasicMachineGui.outputSlots(1, 0).getFirst();
            Gt6EmiGui.emptySlot(widgets, out.x(), out.y());
            return;
        }
        var outSlots = Gt6BasicMachineGui.outputSlots(Math.max(1, outputs.size()), 0);
        for (int index = 0; index < outputs.size() && index < outSlots.size(); index++) {
            Gt6EmiGui.output(
                    widgets,
                    outputs.get(index),
                    this,
                    outSlots.get(index).x(),
                    outSlots.get(index).y());
        }
    }
}
