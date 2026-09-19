package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.client.screen.MachineGuiTextures;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.registry.ModBlocks;

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
    private final ResourceLocation texture;
    private final EmiStack workstation;
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
        this.category = Objects.requireNonNull(category, "category");
        this.id = EmiIds.synthetic(this.category.getId(), id);
        texture = MachineGuiTextures.forMachine(spec.id());
        workstation = EmiStack.of(ModBlocks.configuredProcessingBlock(spec));
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
                EmiStacks.ofItem(output.stack()).setChance(output.chanceFraction())));
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
        List<EmiIngredient> displayedCatalysts = new ArrayList<>();
        data.catalysts().forEach(input ->
                displayedCatalysts.add(requireItemInput(input.recipeIndex())));
        displayedCatalysts.add(workstation);
        catalysts = List.copyOf(displayedCatalysts);
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
    public boolean hideCraftable() {
        return true;
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
        Gt6EmiGui.addPanel(widgets, texture);
        Gt6EmiGui.addProgress(widgets, texture, layout.progress(), data.durationTicks());
        for (ProcessingEmiLayout.ItemSlot slot : layout.itemSlots()) {
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
                    Math.toIntExact(tank.capacity()))
                    .drawBack(false);
            if (tank.kind() == ProcessingEmiLayout.FluidKind.OUTPUT) {
                widget.recipeContext(this);
            }
        }
        Gt6EmiGui.catalyst(
                widgets,
                workstation,
                layout.workstation().x(),
                layout.workstation().y());
        Gt6EmiGui.addStats(
                widgets,
                data,
                layout.costsTextY(),
                layout.powerTextY(),
                layout.durationTextY());
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
        return EmiStacks.ofFluid(stack);
    }
}
