package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.registry.ModBlocks;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

/** Fluid-forward EMI view for the 18 fusion rows. */
final class FusionEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final ResourceLocation texture;
    private final EmiStack workstation;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;
    private final GTRecipe recipe;

    FusionEmiRecipe(ResourceLocation id, GTRecipe recipe) {
        this.id = EmiIds.synthetic(id);
        this.recipe = recipe;
        texture = Gt6EmiGui.texture("fusion");
        workstation = EmiStack.of(ModBlocks.FUSION_REACTOR.get());
        List<EmiIngredient> displayedInputs = new ArrayList<>();
        List<EmiIngredient> displayedCatalysts = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            int count = recipe.itemInputCounts().get(index);
            EmiIngredient ingredient = EmiIngredient.of(
                    recipe.itemInputs().get(index), Math.max(1, count));
            if (count == 0) {
                displayedCatalysts.add(ingredient);
            } else {
                displayedInputs.add(ingredient);
            }
        }
        recipe.fluidInputs().forEach(stack ->
                displayedInputs.add(EmiStacks.ofFluid(stack)));
        displayedCatalysts.add(workstation);
        inputs = List.copyOf(displayedInputs);
        catalysts = List.copyOf(displayedCatalysts);
        List<EmiStack> displayedOutputs = new ArrayList<>();
        recipe.itemOutputs().forEach(stack ->
                displayedOutputs.add(EmiStacks.ofItem(stack)));
        recipe.fluidOutputs().forEach(stack ->
                displayedOutputs.add(EmiStacks.ofFluid(stack)));
        outputs = List.copyOf(displayedOutputs);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.FUSION;
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
                recipe.duration());
        List<EmiIngredient> itemInputs = new ArrayList<>();
        List<EmiStack> fluidInputs = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            int count = recipe.itemInputCounts().get(index);
            EmiIngredient ingredient = EmiIngredient.of(
                    recipe.itemInputs().get(index), Math.max(1, count));
            if (count == 0) {
                Gt6EmiGui.catalyst(
                        widgets,
                        ingredient,
                        Gt6BasicMachineGui.SPECIAL_SLOT_X,
                        Gt6BasicMachineGui.SPECIAL_SLOT_Y);
            } else {
                itemInputs.add(ingredient);
            }
        }
        recipe.fluidInputs().forEach(stack -> fluidInputs.add(EmiStacks.ofFluid(stack)));
        var inSlots = Gt6BasicMachineGui.inputSlots(2, 2);
        for (int index = 0; index < itemInputs.size() && index < inSlots.size(); index++) {
            Gt6EmiGui.slot(
                    widgets,
                    itemInputs.get(index),
                    inSlots.get(index).x(),
                    inSlots.get(index).y());
        }
        var inTanks = Gt6BasicMachineGui.fluidSlots(2, false);
        for (int index = 0; index < fluidInputs.size() && index < inTanks.size(); index++) {
            var tank = inTanks.get(index);
            Gt6EmiGui.tank(
                    widgets,
                    fluidInputs.get(index),
                    tank.x(),
                    tank.y(),
                    Gt6BasicMachineGui.FLUID_SLOT,
                    Gt6BasicMachineGui.FLUID_SLOT,
                    Math.max(1, (int) fluidInputs.get(index).getAmount()));
        }
        List<EmiStack> itemOutputs = recipe.itemOutputs().stream()
                .map(EmiStacks::ofItem)
                .toList();
        List<EmiStack> fluidOutputs = recipe.fluidOutputs().stream()
                .map(EmiStacks::ofFluid)
                .toList();
        var outSlots = Gt6BasicMachineGui.outputSlots(6, 6);
        for (int index = 0; index < itemOutputs.size() && index < outSlots.size(); index++) {
            Gt6EmiGui.output(
                    widgets,
                    itemOutputs.get(index),
                    this,
                    outSlots.get(index).x(),
                    outSlots.get(index).y());
        }
        var outTanks = Gt6BasicMachineGui.fluidSlots(6, true);
        for (int index = 0; index < fluidOutputs.size() && index < outTanks.size(); index++) {
            var tank = outTanks.get(index);
            Gt6EmiGui.tank(
                    widgets,
                    fluidOutputs.get(index),
                    tank.x(),
                    tank.y(),
                    Gt6BasicMachineGui.FLUID_SLOT,
                    Gt6BasicMachineGui.FLUID_SLOT,
                    Math.max(1, (int) fluidOutputs.get(index).getAmount()))
                    .recipeContext(this);
        }
        Gt6EmiGui.workstation(widgets, workstation);
        widgets.addText(
                Component.translatable(
                        recipe.eut() < 0L
                                ? "emi.cruciblecraft.processing.gain"
                                : "emi.cruciblecraft.processing.costs",
                        Long.toString(Math.abs(recipe.eut()) * (long) recipe.duration()),
                        "EU"),
                8,
                86,
                0xFF000000,
                false);
        widgets.addText(Gt6EmiGui.timeComponent(recipe.duration()), 8, 96, 0xFF000000, false);
        if (recipe.specialValue() != 0L) {
            widgets.addText(
                    Component.translatable(
                            "emi.cruciblecraft.fusion.start",
                            Long.toString(recipe.specialValue())),
                    8,
                    106,
                    0xFF000000,
                    false);
        }
    }
}
