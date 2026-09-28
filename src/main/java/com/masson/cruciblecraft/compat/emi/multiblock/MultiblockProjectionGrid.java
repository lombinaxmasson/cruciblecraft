package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.compat.emi.EmiIds;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Element;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PalettePredicate;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * One structure, one grid. Air is omitted. A uniform group shares one
 * example block. Material rows stay per palette key so EMI search still
 * sees tags.
 */
public final class MultiblockProjectionGrid {
    private static final ResourceLocation CATEGORY =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "multiblock_blueprint");
    private static final Comparator<ResourceLocation> BY_ID =
            Comparator.comparing(ResourceLocation::toString);

    private final ResourceLocation structureId;
    private final List<Cell> cells;
    private final List<Material> materials;
    private final List<ResourceLocation> controllers;
    private final List<Integer> layers;

    private MultiblockProjectionGrid(
            ResourceLocation structureId,
            List<Cell> cells,
            List<Material> materials,
            List<ResourceLocation> controllers,
            List<Integer> layers) {
        this.structureId = structureId;
        this.cells = cells;
        this.materials = materials;
        this.controllers = controllers;
        this.layers = layers;
    }

    public static MultiblockProjectionGrid project(
            ResourceLocation structureId,
            MultiblockStructureDefinition definition,
            BlockFormIndex index) {
        Objects.requireNonNull(structureId, "structureId");
        Objects.requireNonNull(definition, "definition");
        Objects.requireNonNull(index, "index");
        Map<String, ResourceLocation> groupModels =
                groupModels(definition, index);
        List<Cell> cells = new ArrayList<>();
        for (Element element : definition.structure()) {
            PalettePredicate predicate = definition.predicate(element);
            if (predicate.kind() == PredicateKind.AIR) {
                continue;
            }
            ResourceLocation model = model(predicate, groupModels, index);
            if (model == null) {
                continue;
            }
            cells.add(new Cell(
                    element.offset(),
                    element.predicate(),
                    model,
                    predicate.description()));
        }
        List<Material> materials = new ArrayList<>();
        definition.palette().forEach((key, predicate) -> {
            if (predicate.kind() == PredicateKind.AIR) {
                return;
            }
            List<ResourceLocation> items = itemForms(predicate, index);
            if (items.isEmpty()) {
                return;
            }
            int count = (int) definition.structure().stream()
                    .filter(element -> element.predicate().equals(key))
                    .count();
            materials.add(new Material(key, count, items));
        });
        return new MultiblockProjectionGrid(
                structureId,
                List.copyOf(cells),
                List.copyOf(materials),
                controllers(definition, index),
                layers(cells));
    }

    public static ResourceLocation recipeId(ResourceLocation structureId) {
        return EmiIds.synthetic(CATEGORY, structureId);
    }

    public ResourceLocation structureId() {
        return structureId;
    }

    public List<Cell> cells() {
        return cells;
    }

    public List<Material> materials() {
        return materials;
    }

    public List<ResourceLocation> controllers() {
        return controllers;
    }

    public List<Integer> layers() {
        return layers;
    }

    public Optional<Cell> cell(Offset offset) {
        return cells.stream()
                .filter(cell -> cell.offset().equals(offset))
                .findFirst();
    }

    /** Local offset after the same horizontal turn the validator uses. */
    public Offset presented(Cell cell, Direction facing) {
        return cell.offset().rotate(facing);
    }

    private static Map<String, ResourceLocation> groupModels(
            MultiblockStructureDefinition definition,
            BlockFormIndex index) {
        Map<String, List<String>> keysByGroup = new LinkedHashMap<>();
        definition.palette().forEach((key, predicate) ->
                predicate.uniformGroup().ifPresent(group ->
                        keysByGroup.computeIfAbsent(group, ignored ->
                                new ArrayList<>()).add(key)));
        Map<String, ResourceLocation> models = new LinkedHashMap<>();
        keysByGroup.forEach((group, keys) -> {
            ResourceLocation model = representative(keys, definition, index);
            if (model != null) {
                models.put(group, model);
            }
        });
        return models;
    }

    private static ResourceLocation representative(
            List<String> keys,
            MultiblockStructureDefinition definition,
            BlockFormIndex index) {
        List<ResourceLocation> explicit = new ArrayList<>();
        List<Set<ResourceLocation>> tagSets = new ArrayList<>();
        for (String key : keys) {
            PalettePredicate predicate = definition.palette().get(key);
            if (predicate.kind() == PredicateKind.AIR) {
                continue;
            }
            if (predicate.block().isPresent()) {
                explicit.add(predicate.block().orElseThrow());
            } else if (predicate.tag().isPresent()) {
                tagSets.add(new HashSet<>(
                        index.blocksInTag(predicate.tag().orElseThrow())));
            }
        }
        if (!explicit.isEmpty()) {
            explicit.sort(BY_ID);
            return explicit.getFirst();
        }
        if (tagSets.isEmpty()) {
            return null;
        }
        Set<ResourceLocation> intersection = new HashSet<>(tagSets.getFirst());
        for (int i = 1; i < tagSets.size(); i++) {
            intersection.retainAll(tagSets.get(i));
        }
        return intersection.stream().min(BY_ID).orElse(null);
    }

    private static ResourceLocation model(
            PalettePredicate predicate,
            Map<String, ResourceLocation> groupModels,
            BlockFormIndex index) {
        if (predicate.uniformGroup().isPresent()) {
            return groupModels.get(predicate.uniformGroup().orElseThrow());
        }
        if (predicate.block().isPresent()) {
            return predicate.block().orElseThrow();
        }
        if (predicate.tag().isPresent()) {
            return index.blocksInTag(predicate.tag().orElseThrow()).stream()
                    .min(BY_ID)
                    .orElse(null);
        }
        return null;
    }

    private static List<ResourceLocation> itemForms(
            PalettePredicate predicate,
            BlockFormIndex index) {
        if (predicate.block().isPresent()) {
            ResourceLocation block = predicate.block().orElseThrow();
            return index.hasItemForm(block) ? List.of(block) : List.of();
        }
        if (predicate.tag().isPresent()) {
            return index.blocksInTag(predicate.tag().orElseThrow()).stream()
                    .filter(index::hasItemForm)
                    .sorted(BY_ID)
                    .toList();
        }
        return List.of();
    }

    private static List<ResourceLocation> controllers(
            MultiblockStructureDefinition definition,
            BlockFormIndex index) {
        PalettePredicate controller = definition.palette().values().stream()
                .filter(predicate -> predicate.kind() == PredicateKind.CONTROLLER)
                .findFirst()
                .orElseThrow();
        if (controller.block().isPresent()) {
            return List.of(controller.block().orElseThrow());
        }
        return index.blocksInTag(controller.tag().orElseThrow()).stream()
                .sorted(BY_ID)
                .toList();
    }

    private static List<Integer> layers(List<Cell> cells) {
        return cells.stream()
                .map(cell -> cell.offset().y())
                .distinct()
                .sorted()
                .toList();
    }

    public record Cell(
            Offset offset,
            String paletteKey,
            ResourceLocation block,
            String description) {
        public Cell {
            Objects.requireNonNull(offset, "offset");
            Objects.requireNonNull(paletteKey, "paletteKey");
            Objects.requireNonNull(block, "block");
            Objects.requireNonNull(description, "description");
        }
    }

    public record Material(
            String paletteKey,
            int count,
            List<ResourceLocation> itemBlocks) {
        public Material {
            Objects.requireNonNull(paletteKey, "paletteKey");
            itemBlocks = List.copyOf(itemBlocks);
            if (count < 1 || itemBlocks.isEmpty()) {
                throw new IllegalArgumentException(
                        "A material row needs a count and an item form");
            }
        }
    }
}
