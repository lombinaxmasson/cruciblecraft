package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.registry.ModBlocks;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

final class AlloyEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final EmiStack workstation;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    AlloyEmiRecipe(String materialId, List<EmiIngredient> inputs, EmiStack output) {
        id = EmiIds.synthetic(ResourceLocation.fromNamespaceAndPath(
                CrucibleCraft.MODID,
                "alloy/" + materialId));
        texture = Gt6EmiGui.texture("alloying");
        workstation = EmiStack.of(ModBlocks.CRUCIBLE.get());
        this.inputs = List.copyOf(inputs);
        catalysts = List.of(workstation);
        outputs = List.of(output);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.CRUCIBLE;
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
        var inSlots = Gt6BasicMachineGui.inputSlots(12, 0);
        for (int index = 0; index < inputs.size() && index < inSlots.size(); index++) {
            Gt6EmiGui.slot(
                    widgets,
                    inputs.get(index),
                    inSlots.get(index).x(),
                    inSlots.get(index).y());
        }
        var outSlots = Gt6BasicMachineGui.outputSlots(12, 0);
        Gt6EmiGui.output(
                widgets,
                outputs.getFirst(),
                this,
                outSlots.getFirst().x(),
                outSlots.getFirst().y());
        Gt6EmiGui.catalyst(
                widgets,
                workstation,
                ProcessingEmiLayout.WORKSTATION.x(),
                ProcessingEmiLayout.WORKSTATION.y());
    }
}
