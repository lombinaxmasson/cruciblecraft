package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.SlotWidget;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.fluids.FluidStack;

/** Generic EMI renderer for every configured processing-machine spec. */
final class ProcessingEmiRecipe implements EmiRecipe {
    private final ResourceLocation id;
    private final EmiRecipeCategory category;
    private final ProcessingEmiRecipeData data;
    private final ProcessingEmiLayout layout;
    private final Map<Integer, EmiIngredient> itemInputs;
    private final Map<Integer, EmiStack> itemOutputs;
    private final Map<Integer, EmiStack> fluidInputs;
    private final Map<Integer, EmiStack> fluidOutputs;
    private final List<EmiIngredient> inputs;
    private final List<EmiIngredient> catalysts;
    private final List<EmiStack> outputs;

    ProcessingEmiRecipe(
            ResourceLocation id,
            EmiRecipeCategory category,
            ProcessingMachineSpec spec,
            GTRecipe recipe) {
        this.id = Objects.requireNonNull(id, "id");
        this.category = Objects.requireNonNull(category, "category");
        data = ProcessingEmiRecipeData.from(spec, recipe);
        layout = ProcessingEmiLayout.create(spec, data);

        itemInputs = new HashMap<>();
        data.consumedInputs().forEach(input -> itemInputs.put(
                input.recipeIndex(),
                EmiIngredient.of(input.ingredient(), input.displayAmount())));
        data.catalysts().forEach(input -> itemInputs.put(
                input.recipeIndex(),
                EmiIngredient.of(input.ingredient(), input.displayAmount())));
        itemOutputs = new HashMap<>();
        data.itemOutputs().forEach(output -> itemOutputs.put(
                output.recipeIndex(),
                EmiStack.of(output.stack()).setChance(output.chanceFraction())));
        fluidInputs = new HashMap<>();
        data.fluidInputs().forEach(input ->
                fluidInputs.put(input.recipeIndex(), fluidStack(input.stack())));
        fluidOutputs = new HashMap<>();
        data.fluidOutputs().forEach(output ->
                fluidOutputs.put(output.recipeIndex(), fluidStack(output.stack())));

        List<EmiIngredient> displayedInputs = new ArrayList<>();
        data.consumedInputs().forEach(input ->
                displayedInputs.add(requireItemInput(input.recipeIndex())));
        data.fluidInputs().forEach(input ->
                displayedInputs.add(requireFluidInput(input.recipeIndex())));
        inputs = List.copyOf(displayedInputs);
        catalysts = data.catalysts().stream()
                .map(input -> requireItemInput(input.recipeIndex()))
                .toList();
        List<EmiStack> displayedOutputs = new ArrayList<>();
        data.itemOutputs().forEach(output ->
                displayedOutputs.add(requireItemOutput(output.recipeIndex())));
        data.fluidOutputs().forEach(output ->
                displayedOutputs.add(requireFluidOutput(output.recipeIndex())));
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
    public int getDisplayWidth() {
        return layout.width();
    }

    @Override
    public int getDisplayHeight() {
        return layout.height();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        for (ProcessingEmiLayout.ItemSlot slot : layout.itemSlots()) {
            ProcessingEmiLayout.Rect bounds = slot.bounds();
            switch (slot.kind()) {
                case INPUT -> widgets.addSlot(
                        requireItemInput(slot.recipeIndex()), bounds.x(), bounds.y());
                case CATALYST -> {
                    ProcessingEmiRecipeData.ItemInput input = data.catalysts().stream()
                            .filter(candidate ->
                                    candidate.recipeIndex() == slot.recipeIndex())
                            .findFirst()
                            .orElseThrow();
                    widgets.addSlot(
                                    requireItemInput(slot.recipeIndex()),
                                    bounds.x(),
                                    bounds.y())
                            .catalyst(true)
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
                    SlotWidget widget = widgets.addSlot(
                                    requireItemOutput(slot.recipeIndex()),
                                    bounds.x(),
                                    bounds.y())
                            .recipeContext(this);
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
        for (ProcessingEmiLayout.FluidTank tank : layout.fluidTanks()) {
            ProcessingEmiLayout.Rect bounds = tank.bounds();
            SlotWidget widget = widgets.addTank(
                    tank.kind() == ProcessingEmiLayout.FluidKind.INPUT
                            ? requireFluidInput(tank.recipeIndex())
                            : requireFluidOutput(tank.recipeIndex()),
                    bounds.x(),
                    bounds.y(),
                    bounds.width(),
                    bounds.height(),
                    Math.toIntExact(tank.capacity()));
            if (tank.kind() == ProcessingEmiLayout.FluidKind.OUTPUT) {
                widget.recipeContext(this);
            }
        }
        widgets.addFillingArrow(
                layout.progress().x(),
                layout.progress().y(),
                (int) Math.min(Integer.MAX_VALUE, (long) data.durationTicks() * 50L));
        widgets.addText(
                Component.translatable(
                        "emi.cruciblecraft.processing.duration",
                        data.durationTicks(),
                        String.format(
                                Locale.ROOT,
                                "%.2f",
                                data.durationTicks() / 20.0D)),
                4,
                layout.durationTextY(),
                0xFF404040,
                false);
        widgets.addText(
                Component.translatable(
                        "emi.cruciblecraft.processing.power."
                                + data.energyType().name().toLowerCase(Locale.ROOT),
                        data.eut()),
                4,
                layout.powerTextY(),
                0xFF404040,
                false);
    }

    private EmiIngredient requireItemInput(int recipeIndex) {
        return Objects.requireNonNull(
                itemInputs.get(recipeIndex), "missing item input " + recipeIndex);
    }

    private EmiStack requireItemOutput(int recipeIndex) {
        return Objects.requireNonNull(
                itemOutputs.get(recipeIndex), "missing item output " + recipeIndex);
    }

    private EmiStack requireFluidInput(int recipeIndex) {
        return Objects.requireNonNull(
                fluidInputs.get(recipeIndex), "missing fluid input " + recipeIndex);
    }

    private EmiStack requireFluidOutput(int recipeIndex) {
        return Objects.requireNonNull(
                fluidOutputs.get(recipeIndex), "missing fluid output " + recipeIndex);
    }

    private static EmiStack fluidStack(FluidStack stack) {
        return EmiStack.of(
                stack.getFluid(),
                stack.getComponentsPatch(),
                stack.getAmount());
    }
}
