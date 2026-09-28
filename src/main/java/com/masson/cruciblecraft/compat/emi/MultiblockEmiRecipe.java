package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.client.multiblockpreview.WorldPreviewRenderer;
import com.masson.cruciblecraft.compat.emi.multiblock.MultiblockProjectionGrid;
import com.masson.cruciblecraft.compat.emi.multiblock.MultiblockProjectionView;
import com.masson.cruciblecraft.compat.emi.multiblock.MultiblockProjectionWidget;
import com.masson.cruciblecraft.compat.emi.multiblock.ProjectionButtonWidget;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * One data-owned multiblock, laid out like GTCEu's multiblock info page:
 * the controller is the output, structure blocks are the counted inputs.
 * The projection is a picture; the slots are what EMI indexes.
 */
final class MultiblockEmiRecipe implements EmiRecipe {
    private static final int CELL = 18;
    private static final int TOP = 11;
    private static final int VIEW_W = 132;
    private static final int VIEW_H = 96;
    private static final int BUTTON = 12;
    private static final int BUTTON_STEP = BUTTON + 1;
    private static final int BUTTONS_LEFT = VIEW_W + 2;
    private static final int MATERIALS_LEFT = BUTTONS_LEFT + BUTTON + 4;
    private static final int MATERIAL_COLUMNS = 3;

    private final ResourceLocation id;
    private final EmiRecipeCategory category;
    private final MultiblockProjectionGrid grid;
    private final MultiblockProjectionView view = new MultiblockProjectionView();
    private final List<EmiIngredient> inputs;
    private final List<EmiStack> outputs;
    private final EmiIngredient catalyst;
    private final int width;
    private final int height;

    MultiblockEmiRecipe(
            MultiblockProjectionGrid grid,
            EmiRecipeCategory category) {
        this.grid = Objects.requireNonNull(grid, "grid");
        this.category = Objects.requireNonNull(category, "category");
        this.id = MultiblockProjectionGrid.recipeId(grid.structureId());
        List<EmiIngredient> materials = new ArrayList<>();
        for (MultiblockProjectionGrid.Material material : grid.materials()) {
            EmiIngredient ingredient = ingredient(
                    material.itemBlocks(), material.count());
            if (ingredient != null) {
                materials.add(ingredient);
            }
        }
        inputs = List.copyOf(materials);
        outputs = controllerOutputs(grid.controllers());
        catalyst = ingredient(grid.controllers(), 1);
        int slots = inputs.size() + (catalyst == null ? 0 : 1);
        int rows = Math.max(1, (slots + MATERIAL_COLUMNS - 1) / MATERIAL_COLUMNS);
        width = MATERIALS_LEFT + MATERIAL_COLUMNS * CELL;
        height = TOP + Math.max(VIEW_H, rows * CELL);
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
        return catalyst == null ? List.of() : List.of(catalyst);
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
        return width;
    }

    @Override
    public int getDisplayHeight() {
        return height;
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        Font font = Minecraft.getInstance().font;
        Component title = Component.translatable(
                "emi.cruciblecraft.multiblock.blueprint", controllerName());
        widgets.addText(
                Language.getInstance().getVisualOrder(
                        font.substrByWidth(title, width)),
                0,
                0,
                0xFF404040,
                false);
        widgets.add(new MultiblockProjectionWidget(
                grid, view, 0, TOP, VIEW_W, VIEW_H));

        int buttonY = TOP;
        buttonY = addButton(widgets, buttonY, "Y", "cycle_layer", () ->
                view.cycleLayer(grid.layers().size()));
        buttonY = addButton(widgets, buttonY, "R", "reset", view::reset);
        addButton(widgets, buttonY, "W", "world_preview", () ->
                WorldPreviewRenderer.toggle(grid));

        int index = 0;
        for (EmiIngredient ingredient : inputs) {
            addMaterialSlot(widgets, ingredient, index++);
        }
        if (catalyst != null) {
            int column = index % MATERIAL_COLUMNS;
            int row = index / MATERIAL_COLUMNS;
            widgets.addSlot(catalyst, MATERIALS_LEFT + column * CELL, TOP + row * CELL)
                    .catalyst(true);
        }
    }

    private static void addMaterialSlot(
            WidgetHolder widgets, EmiIngredient ingredient, int index) {
        int column = index % MATERIAL_COLUMNS;
        int row = index / MATERIAL_COLUMNS;
        widgets.addSlot(ingredient, MATERIALS_LEFT + column * CELL, TOP + row * CELL);
    }

    private static int addButton(
            WidgetHolder widgets,
            int y,
            String label,
            String name,
            Runnable action) {
        widgets.add(new ProjectionButtonWidget(
                BUTTONS_LEFT,
                y,
                BUTTON,
                label,
                Component.translatable("emi.cruciblecraft.multiblock." + name),
                action));
        return y + BUTTON_STEP;
    }

    private Component controllerName() {
        for (ResourceLocation id : grid.controllers()) {
            var block = BuiltInRegistries.BLOCK.getOptional(id);
            if (block.isPresent()) {
                return block.get().getName();
            }
        }
        return Component.literal(grid.structureId().toString());
    }

    /**
     * GTCEu indexes the controller as the recipe output, so View Recipes
     * on the core opens this page. View Uses comes from the catalyst.
     */
    private static List<EmiStack> controllerOutputs(List<ResourceLocation> blocks) {
        List<EmiStack> stacks = new ArrayList<>();
        for (ResourceLocation id : blocks) {
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent(block ->
                    addItemChoice(stacks, block));
        }
        return List.copyOf(stacks);
    }

    private static EmiIngredient ingredient(
            List<ResourceLocation> blocks,
            int amount) {
        List<EmiStack> choices = new ArrayList<>();
        for (ResourceLocation id : blocks) {
            BuiltInRegistries.BLOCK.getOptional(id).ifPresent(block ->
                    addItemChoice(choices, block));
        }
        if (choices.isEmpty()) {
            return null;
        }
        return EmiIngredient.of(choices, amount);
    }

    private static void addItemChoice(List<EmiStack> choices, Block block) {
        ItemStack stack = new ItemStack(block);
        if (!stack.isEmpty()) {
            choices.add(EmiStack.of(stack));
        }
    }
}
