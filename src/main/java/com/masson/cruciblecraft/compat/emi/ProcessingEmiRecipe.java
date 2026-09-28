package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import com.masson.cruciblecraft.gui.MachineGuiTextures;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Generic EMI renderer for every configured processing-machine spec. */
final class ProcessingEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final EmiRecipeCategory category;
    private final ResourceLocation texture;
    private final EmiStack workstation;
    private final ProcessingMachineSpec spec;
    private final GTRecipe recipe;
    private final EmiIngredient[] itemInputs;
    private final EmiStack[] itemOutputs;
    private final EmiStack[] fluidInputs;
    private final EmiStack[] fluidOutputs;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;
    private ProcessingEmiRecipeData data;
    private volatile ProcessingEmiLayout layout;

    ProcessingEmiRecipe(
            ResourceLocation id,
            EmiRecipeCategory category,
            ProcessingMachineSpec spec,
            GTRecipe recipe,
            EmiStack workstation) {
        this.category = Objects.requireNonNull(category, "category");
        this.id = EmiIds.synthetic(this.category.getId(), id);
        this.spec = Objects.requireNonNull(spec, "spec");
        this.recipe = Objects.requireNonNull(recipe, "recipe");
        texture = MachineGuiTextures.forMachine(spec.id());
        this.workstation = Objects.requireNonNull(workstation, "workstation");

        int inputCount = recipe.itemInputs().size();
        itemInputs = new EmiIngredient[inputCount];
        List<EmiIngredient> consumed = new ArrayList<>();
        List<EmiIngredient> catalystInputs = new ArrayList<>();
        for (int index = 0; index < inputCount; index++) {
            ItemInputAction action = recipe.itemInputActions().get(index);
            long amount = action.kind() == ItemInputAction.Kind.CONSUME
                    ? recipe.itemInputCounts().get(index)
                    : 1L;
            EmiIngredient ingredient = EmiStacks.ofIngredient(
                    recipe.itemInputs().get(index), amount);
            itemInputs[index] = ingredient;
            if (action.kind() == ItemInputAction.Kind.CONSUME) {
                consumed.add(ingredient);
            } else {
                catalystInputs.add(ingredient);
            }
        }
        List<FluidStack> inputFluids = recipe.fluidInputsView();
        fluidInputs = new EmiStack[inputFluids.size()];
        for (int index = 0; index < inputFluids.size(); index++) {
            fluidInputs[index] = EmiStacks.ofFluid(inputFluids.get(index));
            consumed.add(fluidInputs[index]);
        }
        inputs = List.copyOf(consumed);

        catalystInputs.add(this.workstation);
        catalysts = List.copyOf(catalystInputs);

        List<ItemStack> outputStacks = recipe.itemOutputsView();
        itemOutputs = new EmiStack[outputStacks.size()];
        List<EmiStack> displayedOutputs = new ArrayList<>(
                outputStacks.size() + recipe.fluidOutputsView().size());
        for (int index = 0; index < outputStacks.size(); index++) {
            EmiStack stack = EmiStacks.ofItem(outputStacks.get(index));
            int chance = recipe.outputChances().get(index);
            if (chance != GTRecipe.GUARANTEED_CHANCE) {
                stack.setChance(chance / (float) GTRecipe.GUARANTEED_CHANCE);
            }
            itemOutputs[index] = stack;
            displayedOutputs.add(stack);
        }
        List<FluidStack> outputFluids = recipe.fluidOutputsView();
        fluidOutputs = new EmiStack[outputFluids.size()];
        for (int index = 0; index < outputFluids.size(); index++) {
            fluidOutputs[index] = EmiStacks.ofFluid(outputFluids.get(index));
            displayedOutputs.add(fluidOutputs[index]);
        }
        outputs = List.copyOf(displayedOutputs);
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return category;
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
    public boolean hideCraftable() {
        return true;
    }

    @Override
    public int getDisplayWidth() {
        return displayed().width();
    }

    @Override
    public int getDisplayHeight() {
        return displayed().height();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        ProcessingEmiLayout shown = displayed();
        Gt6EmiGui.addPanel(widgets, texture);
        Gt6EmiGui.addProgress(widgets, texture, shown.progress(), data.durationTicks());
        for (ProcessingEmiLayout.ItemSlot slot : shown.itemSlots()) {
            ProcessingEmiLayout.Rect bounds = slot.bounds();
            switch (slot.kind()) {
                case INPUT -> Gt6EmiGui.slot(
                        widgets,
                        requireItemInput(slot.recipeIndex()),
                        bounds.x(),
                        bounds.y());
                case CATALYST -> {
                    ProcessingEmiRecipeData.ItemInput input = data.catalysts().stream()
                            .filter(candidate ->
                                    candidate.recipeIndex() == slot.recipeIndex())
                            .findFirst()
                            .orElseThrow();
                    Gt6EmiGui.catalyst(
                                    widgets,
                                    requireItemInput(slot.recipeIndex()),
                                    bounds.x(),
                                    bounds.y())
                            .appendTooltip(switch (input.action().kind()) {
                                case PRESERVE -> Component.translatable(
                                        "emi.cruciblecraft.processing.preserved");
                                case WEAR -> Component.translatable(
                                        "emi.cruciblecraft.processing.wear",
                                        input.action().damage());
                                case CONSUME -> throw new IllegalStateException(
                                        "Consumed input was rendered as a catalyst");
                            });
                }
                case OUTPUT -> {
                    ProcessingEmiRecipeData.ItemOutput output =
                            data.itemOutputs().get(slot.recipeIndex());
                    SlotWidget widget = Gt6EmiGui.output(
                            widgets,
                            requireItemOutput(slot.recipeIndex()),
                            this,
                            bounds.x(),
                            bounds.y());
                    if (output.chance() != GTRecipe.GUARANTEED_CHANCE) {
                        widget.appendTooltip(Component.translatable(
                                "emi.cruciblecraft.processing.chance",
                                String.format(
                                        Locale.ROOT,
                                        "%.2f",
                                        output.chance() / 100.0D)));
                    }
                }
            }
        }
        for (ProcessingEmiLayout.FluidTank tank : shown.fluidTanks()) {
            ProcessingEmiLayout.Rect bounds = tank.bounds();
            SlotWidget widget = Gt6EmiGui.tank(
                    widgets,
                    tank.kind() == ProcessingEmiLayout.FluidKind.INPUT
                            ? requireFluidInput(tank.recipeIndex())
                            : requireFluidOutput(tank.recipeIndex()),
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height());
            if (tank.kind() == ProcessingEmiLayout.FluidKind.OUTPUT) {
                widget.recipeContext(this);
            }
        }
        Gt6EmiGui.workstation(widgets, workstation);
        Gt6EmiGui.addStats(
                widgets,
                data,
                shown.costsTextY(),
                shown.powerTextY(),
                shown.durationTextY());
    }

    private ProcessingEmiLayout displayed() {
        ProcessingEmiLayout current = layout;
        if (current == null) {
            data = ProcessingEmiRecipeData.from(spec, recipe);
            current = ProcessingEmiLayout.create(spec, data);
            layout = current;
        }
        return current;
    }

    private EmiIngredient requireItemInput(int recipeIndex) {
        return Objects.requireNonNull(
                itemInputs[recipeIndex], "missing item input " + recipeIndex);
    }

    private EmiStack requireItemOutput(int recipeIndex) {
        return Objects.requireNonNull(
                itemOutputs[recipeIndex], "missing item output " + recipeIndex);
    }

    private EmiStack requireFluidInput(int recipeIndex) {
        return Objects.requireNonNull(
                fluidInputs[recipeIndex], "missing fluid input " + recipeIndex);
    }

    private EmiStack requireFluidOutput(int recipeIndex) {
        return Objects.requireNonNull(
                fluidOutputs[recipeIndex], "missing fluid output " + recipeIndex);
    }
}
