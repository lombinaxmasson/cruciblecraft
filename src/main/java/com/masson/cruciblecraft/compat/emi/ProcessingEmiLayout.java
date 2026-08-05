package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

/**
 * Pure layout policy translating machine UI coordinates into EMI bounds.
 *
 * <p>The source coordinates come from {@link ProcessingMachineSpec.UiLayout}.
 * A tank is shifted to the right only when the source UI overlaps another
 * visible recipe widget, which occurs in the densest configured layouts.
 */
public record ProcessingEmiLayout(
        int width,
        int height,
        List<ItemSlot> itemSlots,
        List<FluidTank> fluidTanks,
        Rect progress,
        int durationTextY,
        int powerTextY) {
    public static final int ITEM_SLOT_SIZE = 18;
    public static final int ARROW_WIDTH = 24;
    public static final int ARROW_HEIGHT = 17;
    private static final int MINIMUM_WIDTH = 176;
    private static final int PADDING = 4;
    private static final int TEXT_LINE_HEIGHT = 10;

    public ProcessingEmiLayout {
        itemSlots = List.copyOf(itemSlots);
        fluidTanks = List.copyOf(fluidTanks);
        Objects.requireNonNull(progress, "progress");
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("Layout bounds must be positive");
        }
    }

    public static ProcessingEmiLayout create(
            ProcessingMachineSpec spec,
            ProcessingEmiRecipeData data) {
        Objects.requireNonNull(spec, "spec");
        Objects.requireNonNull(data, "data");
        List<ItemSlot> items = positionItems(spec, data);
        ProcessingMachineSpec.ProgressBar sourceProgress = spec.ui().progress();
        Rect progress = new Rect(
                sourceProgress.x(),
                sourceProgress.y(),
                ARROW_WIDTH,
                ARROW_HEIGHT);

        List<Rect> occupied = new ArrayList<>();
        items.stream().map(ItemSlot::bounds).forEach(occupied::add);
        occupied.add(progress);
        List<FluidTank> tanks = positionFluids(spec, data, occupied);

        int contentRight = java.util.stream.Stream.concat(
                        items.stream().map(ItemSlot::bounds),
                        tanks.stream().map(FluidTank::bounds))
                .mapToInt(Rect::right)
                .max()
                .orElse(progress.right());
        contentRight = Math.max(contentRight, progress.right());
        int contentBottom = java.util.stream.Stream.concat(
                        items.stream().map(ItemSlot::bounds),
                        tanks.stream().map(FluidTank::bounds))
                .mapToInt(Rect::bottom)
                .max()
                .orElse(progress.bottom());
        contentBottom = Math.max(contentBottom, progress.bottom());
        int durationY = contentBottom + PADDING;
        int powerY = durationY + TEXT_LINE_HEIGHT;
        return new ProcessingEmiLayout(
                Math.max(MINIMUM_WIDTH, contentRight + PADDING),
                powerY + TEXT_LINE_HEIGHT,
                items,
                tanks,
                progress,
                durationY,
                powerY);
    }

    public List<Rect> visibleBounds() {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.concat(
                                itemSlots.stream().map(ItemSlot::bounds),
                                fluidTanks.stream().map(FluidTank::bounds)),
                        java.util.stream.Stream.of(progress))
                .toList();
    }

    private static List<ItemSlot> positionItems(
            ProcessingMachineSpec spec,
            ProcessingEmiRecipeData data) {
        Set<Integer> available = new LinkedHashSet<>(spec.items().inputs());
        List<ItemSlot> result = new ArrayList<>();
        for (ProcessingEmiRecipeData.ItemInput input : data.consumedInputs()) {
            int machineSlot = takeSlot(
                    spec,
                    available,
                    ProcessingMachineSpec.SlotRole.MATERIAL);
            result.add(itemSlot(spec, ItemKind.INPUT, input.recipeIndex(), machineSlot));
        }
        for (ProcessingEmiRecipeData.ItemInput catalyst : data.catalysts()) {
            int machineSlot = takeSlot(
                    spec,
                    available,
                    ProcessingMachineSpec.SlotRole.TOOL);
            result.add(itemSlot(
                    spec, ItemKind.CATALYST, catalyst.recipeIndex(), machineSlot));
        }
        if (data.itemOutputs().size() > spec.items().outputs().size()) {
            throw new IllegalArgumentException(
                    "Recipe has more item outputs than " + spec.id() + " can display");
        }
        for (int index = 0; index < data.itemOutputs().size(); index++) {
            ProcessingEmiRecipeData.ItemOutput output = data.itemOutputs().get(index);
            result.add(itemSlot(
                    spec,
                    ItemKind.OUTPUT,
                    output.recipeIndex(),
                    spec.items().outputs().get(index)));
        }
        return List.copyOf(result);
    }

    private static int takeSlot(
            ProcessingMachineSpec spec,
            Set<Integer> available,
            ProcessingMachineSpec.SlotRole preferredRole) {
        Integer preferred = null;
        for (int slot : available) {
            if (spec.items().role(slot) == preferredRole) {
                preferred = slot;
                break;
            }
        }
        if (preferred != null) {
            available.remove(preferred);
            return preferred;
        }
        if (available.isEmpty()) {
            throw new IllegalArgumentException(
                    "Recipe has more item inputs than " + spec.id() + " can display");
        }
        int fallback = available.iterator().next();
        available.remove(fallback);
        return fallback;
    }

    private static ItemSlot itemSlot(
            ProcessingMachineSpec spec,
            ItemKind kind,
            int recipeIndex,
            int machineSlot) {
        ProcessingMachineSpec.SlotPosition position =
                spec.ui().machineSlots().get(machineSlot);
        return new ItemSlot(
                kind,
                recipeIndex,
                machineSlot,
                new Rect(position.x(), position.y(), ITEM_SLOT_SIZE, ITEM_SLOT_SIZE));
    }

    private static List<FluidTank> positionFluids(
            ProcessingMachineSpec spec,
            ProcessingEmiRecipeData data,
            List<Rect> occupied) {
        if (data.fluidInputs().size() > spec.fluids().inputs().size()
                || data.fluidOutputs().size() > spec.fluids().outputs().size()) {
            throw new IllegalArgumentException(
                    "Recipe has more fluid resources than " + spec.id() + " can display");
        }
        List<FluidTank> result = new ArrayList<>();
        for (int index = 0; index < data.fluidInputs().size(); index++) {
            addTank(
                    spec,
                    FluidKind.INPUT,
                    data.fluidInputs().get(index).recipeIndex(),
                    spec.fluids().inputs().get(index),
                    occupied,
                    result);
        }
        for (int index = 0; index < data.fluidOutputs().size(); index++) {
            addTank(
                    spec,
                    FluidKind.OUTPUT,
                    data.fluidOutputs().get(index).recipeIndex(),
                    spec.fluids().outputs().get(index),
                    occupied,
                    result);
        }
        return List.copyOf(result);
    }

    private static void addTank(
            ProcessingMachineSpec spec,
            FluidKind kind,
            int recipeIndex,
            ProcessingMachineSpec.TankSpec tank,
            List<Rect> occupied,
            List<FluidTank> result) {
        ProcessingMachineSpec.TankPosition position = spec.ui().tanks().stream()
                .filter(candidate -> candidate.tank() == tank.index())
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "Missing UI position for tank " + tank.index()));
        Rect bounds = new Rect(
                position.x(), position.y(), position.width(), position.height());
        while (occupied.stream().anyMatch(bounds::overlaps)) {
            bounds = new Rect(
                    bounds.x() + 1, bounds.y(), bounds.width(), bounds.height());
        }
        occupied.add(bounds);
        result.add(new FluidTank(
                kind,
                recipeIndex,
                tank.index(),
                tank.capacity(),
                bounds));
    }

    public enum ItemKind {
        INPUT,
        CATALYST,
        OUTPUT
    }

    public enum FluidKind {
        INPUT,
        OUTPUT
    }

    public record ItemSlot(
            ItemKind kind,
            int recipeIndex,
            int machineSlot,
            Rect bounds) {
        public ItemSlot {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(bounds, "bounds");
            if (recipeIndex < 0 || machineSlot < 0) {
                throw new IllegalArgumentException("Invalid item slot index");
            }
        }
    }

    public record FluidTank(
            FluidKind kind,
            int recipeIndex,
            int tank,
            long capacity,
            Rect bounds) {
        public FluidTank {
            Objects.requireNonNull(kind, "kind");
            Objects.requireNonNull(bounds, "bounds");
            if (recipeIndex < 0 || tank < 0 || capacity <= 0L) {
                throw new IllegalArgumentException("Invalid fluid tank");
            }
        }
    }

    public record Rect(int x, int y, int width, int height) {
        public Rect {
            if (x < 0 || y < 0 || width <= 0 || height <= 0) {
                throw new IllegalArgumentException("Invalid widget rectangle");
            }
        }

        public int right() {
            return Math.addExact(x, width);
        }

        public int bottom() {
            return Math.addExact(y, height);
        }

        public boolean overlaps(Rect other) {
            Objects.requireNonNull(other, "other");
            return x < other.right()
                    && right() > other.x
                    && y < other.bottom()
                    && bottom() > other.y;
        }
    }
}
