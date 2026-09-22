package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Element;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PalettePredicate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;

import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.stack.ListEmiIngredient;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * A clickable, layer-by-layer material blueprint for one data-owned
 * multiblock. Every displayed cell is an EMI ingredient, so both the compact
 * material list and the actual blueprint can be used as EMI search targets.
 */
final class MultiblockEmiRecipe implements EmiRecipe {
    private static final int CELL = 18;
    private static final int LEFT = 8;
    private static final int TOP = 22;
    private static final int MATERIALS_LEFT = 120;
    private static final int MATERIALS_TOP = 22;

    private final ResourceLocation id;
    private final EmiRecipeCategory category;
    private final ResourceLocation structureId;
    private final MultiblockStructureDefinition definition;
    private final Map<String, EmiIngredient> paletteIngredients;
    private final List<EmiIngredient> inputs;
    private final EmiStack controller;
    private final int layer;
    private final int width;
    private final int height;

    MultiblockEmiRecipe(
            ResourceLocation structureId,
            MultiblockStructureDefinition definition,
            EmiRecipeCategory category,
            EmiStack controller,
            int layer) {
        this.structureId = Objects.requireNonNull(structureId, "structureId");
        this.definition = Objects.requireNonNull(definition, "definition");
        this.category = Objects.requireNonNull(category, "category");
        this.controller = Objects.requireNonNull(controller, "controller");
        this.layer = layer;
        this.id = EmiIds.synthetic(
                category.getId(),
                ResourceLocation.fromNamespaceAndPath(
                        structureId.getNamespace(),
                        structureId.getPath() + "/layer_" + layer));

        paletteIngredients = new LinkedHashMap<>();
        List<EmiIngredient> materialInputs = new ArrayList<>();
        definition.palette().forEach((key, predicate) -> {
            if (predicate.kind() == PredicateKind.AIR) {
                return;
            }
            EmiIngredient cellIngredient = ingredient(predicate, 1);
            EmiIngredient materialIngredient = ingredient(predicate, count(key));
            if (cellIngredient != null && materialIngredient != null) {
                paletteIngredients.put(key, cellIngredient);
                materialInputs.add(materialIngredient);
            }
        });
        inputs = List.copyOf(materialInputs);

        int minX = definition.structure().stream()
                .mapToInt(element -> element.offset().x()).min().orElse(0);
        int maxX = definition.structure().stream()
                .mapToInt(element -> element.offset().x()).max().orElse(0);
        int minZ = definition.structure().stream()
                .mapToInt(element -> element.offset().z()).min().orElse(0);
        int maxZ = definition.structure().stream()
                .mapToInt(element -> element.offset().z()).max().orElse(0);
        int gridWidth = maxX - minX + 1;
        int gridDepth = maxZ - minZ + 1;
        int materialLines = Math.max(1, materialInputs.size() + 1);
        width = Math.max(MATERIALS_LEFT + 100, LEFT + gridWidth * CELL + 8);
        height = Math.max(
                MATERIALS_TOP + materialLines * CELL + 10,
                TOP + gridDepth * CELL + 16);
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
        return List.of(controller);
    }

    @Override
    public List<EmiStack> getOutputs() {
        return List.of();
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
        widgets.addText(
                net.minecraft.network.chat.Component.translatable(
                        "emi.cruciblecraft.multiblock.blueprint",
                        structureId.toString()),
                LEFT,
                4,
                0xFF000000,
                false);
        widgets.addText(
                net.minecraft.network.chat.Component.translatable(
                        "emi.cruciblecraft.multiblock.materials"),
                MATERIALS_LEFT,
                4,
                0xFF000000,
                false);

        int minX = definition.structure().stream()
                .mapToInt(element -> element.offset().x()).min().orElse(0);
        int minY = definition.structure().stream()
                .mapToInt(element -> element.offset().y()).min().orElse(0);
        int minZ = definition.structure().stream()
                .mapToInt(element -> element.offset().z()).min().orElse(0);
        int maxZ = definition.structure().stream()
                .mapToInt(element -> element.offset().z()).max().orElse(0);
        int gridWidth = definition.structure().stream()
                .mapToInt(element -> element.offset().x()).max().orElse(0)
                - minX + 1;
        int gridDepth = maxZ - minZ + 1;

        Map<Integer, List<Element>> byLayer = new java.util.TreeMap<>();
        definition.structure().forEach(element ->
                byLayer.computeIfAbsent(element.offset().y(), ignored ->
                        new ArrayList<>()).add(element));
        List<Element> elements = byLayer.getOrDefault(layer, List.of());
        int y = TOP;
        widgets.addText(
                net.minecraft.network.chat.Component.translatable(
                        "emi.cruciblecraft.multiblock.layer", layer),
                LEFT,
                y - 10,
                0xFF000000,
                false);
        for (Element element : elements) {
            EmiIngredient ingredient = paletteIngredients.get(
                    element.predicate());
            if (ingredient == null) {
                continue;
            }
            int x = LEFT + (element.offset().x() - minX) * CELL;
            int z = y + (element.offset().z() - minZ) * CELL;
            widgets.addSlot(ingredient, x, z).drawBack(false);
        }

        int materialY = MATERIALS_TOP;
        for (EmiIngredient ingredient : inputs) {
            widgets.addSlot(ingredient, MATERIALS_LEFT, materialY)
                    .drawBack(false);
            materialY += CELL;
        }
        widgets.addSlot(controller, MATERIALS_LEFT, materialY)
                .drawBack(false)
                .catalyst(true);
    }

    private int count(String paletteKey) {
        return (int) definition.structure().stream()
                .filter(element -> element.predicate().equals(paletteKey))
                .count();
    }

    private static EmiIngredient ingredient(
            PalettePredicate predicate,
            int amount) {
        List<EmiStack> choices = new ArrayList<>();
        if (predicate.block().isPresent()) {
            addBlockChoice(choices, predicate.block().orElseThrow());
        } else if (predicate.tag().isPresent()) {
            var tag = net.minecraft.tags.TagKey.create(
                    net.minecraft.core.registries.Registries.BLOCK,
                    predicate.tag().orElseThrow());
            BuiltInRegistries.BLOCK.forEach(block -> {
                if (block.builtInRegistryHolder().is(tag)) {
                    addItemChoice(choices, block);
                }
            });
        }
        return choices.isEmpty() ? null : new ListEmiIngredient(choices, amount);
    }

    private static void addBlockChoice(
            List<EmiStack> choices,
            ResourceLocation id) {
        BuiltInRegistries.BLOCK.getOptional(id).ifPresent(
                block -> addItemChoice(choices, block));
    }

    private static void addItemChoice(List<EmiStack> choices, Block block) {
        ItemStack stack = new ItemStack(block);
        if (!stack.isEmpty()) {
            choices.add(EmiStack.of(stack));
        }
    }
}
