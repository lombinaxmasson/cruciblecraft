package com.masson.cruciblecraft.compat.emi;

import java.util.List;

import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.recipe.AnvilMode;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

final class AnvilEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final ResourceLocation texture;
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
        this.id = EmiIds.synthetic(id);
        this.mode = mode;
        this.recipe = recipe;
        texture = Gt6EmiGui.texture(switch (mode) {
            case BEND_SMALL -> "anvilbendingsmall";
            case BEND_BIG -> "anvilbendingbig";
            case ANVIL -> "anvil";
        });
        inputs = java.util.stream.IntStream.range(0, recipe.itemInputs().size())
                .mapToObj(index -> EmiIngredient.of(
                        recipe.itemInputs().get(index),
                        recipe.itemInputCounts().get(index)))
                .toList();
        catalysts = List.of(EmiStacks.ofItem(hammer), EmiStacks.ofItem(anvil));
        outputs = recipe.itemOutputs().stream().map(EmiStacks::ofItem).toList();
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return CrucibleCraftEmiPlugin.ANVIL;
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
                Math.max(1, (int) recipe.specialValue()));
        var inSlots = Gt6BasicMachineGui.inputSlots(2, 0);
        for (int index = 0; index < inputs.size() && index < inSlots.size(); index++) {
            Gt6EmiGui.slot(
                    widgets,
                    inputs.get(index),
                    inSlots.get(index).x(),
                    inSlots.get(index).y());
        }
        Gt6EmiGui.catalyst(
                widgets,
                catalysts.getFirst(),
                Gt6BasicMachineGui.SPECIAL_SLOT_X,
                Gt6BasicMachineGui.SPECIAL_SLOT_Y);
        var outSlots = Gt6BasicMachineGui.outputSlots(2, 0);
        for (int index = 0; index < outputs.size() && index < outSlots.size(); index++) {
            Gt6EmiGui.output(
                    widgets,
                    outputs.get(index),
                    this,
                    outSlots.get(index).x(),
                    outSlots.get(index).y());
        }
        Gt6EmiGui.catalyst(
                widgets,
                catalysts.get(1),
                ProcessingEmiLayout.WORKSTATION.x(),
                ProcessingEmiLayout.WORKSTATION.y());
        String modeName = mode.serializedName().replace('_', ' ');
        widgets.addText(
                outputs.size() > 1
                        ? Component.translatable(
                                "emi.cruciblecraft.anvil.hits_with_chance",
                                modeName,
                                recipe.specialValue(),
                                recipe.outputChances().get(1) / 100.0)
                        : Component.translatable(
                                "emi.cruciblecraft.anvil.hits",
                                modeName,
                                recipe.specialValue()),
                8,
                86,
                0xFF000000,
                false);
    }
}
