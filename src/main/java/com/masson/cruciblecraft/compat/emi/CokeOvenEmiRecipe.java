package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.content.menu.CokeOvenMenu;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.registry.ModBlocks;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.resources.ResourceLocation;

final class CokeOvenEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final EmiStack workstation;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> itemOutputs;
    private final List<EmiStack> fluidOutputs;
    private final List<EmiStack> outputs;
    private final int durationTicks;

    CokeOvenEmiRecipe(ResourceLocation id, GTRecipe recipe) {
        this.id = EmiIds.synthetic(id);
        texture = Gt6EmiGui.texture("coke_oven");
        workstation = EmiStack.of(ModBlocks.COKE_OVEN.get());
        inputs = java.util.stream.IntStream.range(0, recipe.itemInputs().size())
                .mapToObj(index -> EmiStacks.ofIngredient(
                        recipe.itemInputs().get(index),
                        recipe.itemInputCounts().get(index)))
                .toList();
        catalysts = List.of(workstation);
        itemOutputs = recipe.itemOutputs().stream()
                .map(EmiStacks::ofItem)
                .toList();
        fluidOutputs = recipe.fluidOutputs().stream()
                .map(EmiStacks::ofFluid)
                .toList();
        var displayed = new java.util.ArrayList<EmiStack>();
        displayed.addAll(itemOutputs);
        displayed.addAll(fluidOutputs);
        outputs = List.copyOf(displayed);
        durationTicks = recipe.duration();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.COKE_OVEN;
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
        var layout = CokeOvenMenu.LAYOUT;
        Gt6EmiGui.addProgress(
                widgets,
                texture,
                new ProcessingEmiLayout.Rect(
                        layout.progress().x(),
                        layout.progress().y(),
                        layout.progress().width(),
                        layout.progress().height()),
                durationTicks);
        if (!inputs.isEmpty()) {
            var slot = layout.itemSlots().getFirst();
            Gt6EmiGui.slot(widgets, inputs.getFirst(), slot.x(), slot.y());
        }
        int outputSlots = Math.min(itemOutputs.size(), layout.itemSlots().size() - 1);
        for (int index = 0; index < outputSlots; index++) {
            var slot = layout.itemSlots().get(index + 1);
            Gt6EmiGui.output(widgets, itemOutputs.get(index), this, slot.x(), slot.y());
        }
        if (!fluidOutputs.isEmpty() && !layout.tanks().isEmpty()) {
            var tank = layout.tanks().getFirst();
            Gt6EmiGui.tank(
                    widgets,
                    fluidOutputs.getFirst(),
                    tank.x(),
                    tank.y(),
                    tank.width(),
                    tank.height(),
                    Math.max(1, (int) fluidOutputs.getFirst().getAmount()))
                    .recipeContext(this);
        }
        Gt6EmiGui.workstation(widgets, workstation);
    }
}
