package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.registry.ModBlocks;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

final class CrusherEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final EmiStack workstation;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;
    private final int durationTicks;

    CrusherEmiRecipe(ResourceLocation id, GTRecipe recipe) {
        this.id = EmiIds.synthetic(id);
        texture = Gt6EmiGui.texture("crusher");
        workstation = EmiStack.of(ModBlocks.BRONZE_CRUSHER.get());
        inputs = java.util.stream.IntStream.range(0, recipe.itemInputs().size())
                .mapToObj(index -> EmiStacks.ofIngredient(
                        recipe.itemInputs().get(index),
                        recipe.itemInputCounts().get(index)))
                .toList();
        catalysts = List.of(workstation);
        outputs = recipe.itemOutputs().stream().map(EmiStacks::ofItem).toList();
        durationTicks = recipe.duration();
    }

    @Override public EmiRecipeCategory getCategory() { return CrucibleCraftEmiPlugin.CRUSHER; }
    @Override public boolean hideCraftable() { return true; }
    @Override public ResourceLocation getId() {
        return id;
    }
    @Override public List<EmiIngredient> getInputs() { return inputs; }
    @Override public List<EmiIngredient> getCatalysts() { return catalysts; }
    @Override public List<EmiStack> getOutputs() { return outputs; }
    @Override public int getDisplayWidth() { return ProcessingEmiLayout.PANEL_WIDTH; }
    @Override public int getDisplayHeight() { return ProcessingEmiLayout.NEI_HEIGHT; }

    @Override public void addWidgets(WidgetHolder widgets) {
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
        var inSlots = Gt6BasicMachineGui.inputSlots(1, 0);
        for (int index = 0; index < inputs.size() && index < inSlots.size(); index++) {
            Gt6EmiGui.slot(
                    widgets,
                    inputs.get(index),
                    inSlots.get(index).x(),
                    inSlots.get(index).y());
        }
        var outSlots = Gt6BasicMachineGui.outputSlots(12, 0);
        for (int index = 0; index < outputs.size() && index < outSlots.size(); index++) {
            Gt6EmiGui.output(
                    widgets,
                    outputs.get(index),
                    this,
                    outSlots.get(index).x(),
                    outSlots.get(index).y());
        }
        Gt6EmiGui.workstation(widgets, workstation);
    }
}
