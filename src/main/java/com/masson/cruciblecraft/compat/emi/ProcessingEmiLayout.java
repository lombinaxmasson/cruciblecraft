package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.Set;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

/**
 * Pure layout policy translating machine UI coordinates into EMI bounds.
 *
 * <p>The source coordinates come from {@link ProcessingMachineSpec.UiLayout}
 * and match GT6 {@code NEI_RecipeMap}: machine-GUI slots, a 176×83 recipe
 * panel, 176×166 NEI chrome, progress overlay at {@code (176,0)}, and the NEI
 * workstation slot at {@code (152,83)}. EMI blits follow GT6
 * {@code NEI_RecipeMap.drawBackground}: chrome at {@code (0,0)} and the
 * machine crop 176×79 from {@code v=3} at {@code (0,8)}. A tank is shifted to the right only when the source
 * UI overlaps another visible recipe widget, which occurs in the densest
 * configured layouts.
 */
public record ProcessingEmiLayout(
        int width,
        int height,
        List<ItemSlot> itemSlots,
        List<FluidTank> fluidTanks,
        Rect progress,
        Rect workstation,
        int costsTextY,
        int powerTextY,
        int durationTextY) {
    public static final int ITEM_SLOT_SIZE = 18;
    public static final int PANEL_WIDTH = 176;
    public static final int PANEL_HEIGHT = 83;
    public static final int NEI_HEIGHT = 166;
    public static final int TEXTURE_SIZE = 256;
    public static final Rect WORKSTATION = new Rect(152, 83, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE);
    private static final int PADDING = 4;
    private static final int TEXT_LINE_HEIGHT = 10;

    public ProcessingEmiLayout {
        itemSlots = List.copyOf(itemSlots);
        fluidTanks = List.copyOf(fluidTanks);
        Objects.requireNonNull(progress, "progress");
        Objects.requireNonNull(workstation, "workstation");
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
                sourceProgress.width(),
                sourceProgress.height());

        List<Rect> occupied = new ArrayList<>();
        items.stream().map(ItemSlot::bounds).forEach(occupied::add);
        occupied.add(progress);
        occupied.add(WORKSTATION);
        List<FluidTank> tanks = positionFluids(spec, data, occupied);

        int contentRight = java.util.stream.Stream.concat(
                        items.stream().map(ItemSlot::bounds),
                        tanks.stream().map(FluidTank::bounds))
                .mapToInt(Rect::right)
                .max()
                .orElse(progress.right());
        contentRight = Math.max(contentRight, Math.max(progress.right(), WORKSTATION.right()));
        int contentBottom = java.util.stream.Stream.concat(
                        items.stream().map(ItemSlot::bounds),
                        tanks.stream().map(FluidTank::bounds))
                .mapToInt(Rect::bottom)
                .max()
                .orElse(progress.bottom());
        contentBottom = Math.max(contentBottom, progress.bottom());
        int textTop = Math.max(contentBottom, PANEL_HEIGHT) + PADDING;
        int costsY = textTop;
        int powerY = costsY + TEXT_LINE_HEIGHT;
        int durationY = powerY + TEXT_LINE_HEIGHT;
        int lastTextY = data.specialValue() == 0L
                ? durationY
                : durationY + TEXT_LINE_HEIGHT;
        return new ProcessingEmiLayout(
                Math.max(PANEL_WIDTH, contentRight + PADDING),
                Math.max(NEI_HEIGHT, lastTextY + TEXT_LINE_HEIGHT),
                items,
                tanks,
                progress,
                WORKSTATION,
                costsY,
                powerY,
                durationY);
    }

    public List<Rect> visibleBounds() {
        return java.util.stream.Stream.concat(
                        java.util.stream.Stream.concat(
                                itemSlots.stream().map(ItemSlot::bounds),
                                fluidTanks.stream().map(FluidTank::bounds)),
                        java.util.stream.Stream.of(progress, workstation))
                .toList();
    }

    private static List<ItemSlot> positionItems(
            ProcessingMachineSpec spec,
            ProcessingEmiRecipeData data) {
        Set<Integer> available = new LinkedHashSet<>(spec.items().inputs());
        List<ItemSlot> result = new ArrayList<>();
        int overflow = 0;
        for (ProcessingEmiRecipeData.ItemInput input : data.consumedInputs()) {
            OptionalInt machineSlot = takeSlot(
                    spec,
                    available,
                    ProcessingMachineSpec.SlotRole.MATERIAL);
            if (machineSlot.isPresent()) {
                result.add(itemSlot(
                        spec, ItemKind.INPUT, input.recipeIndex(), machineSlot.getAsInt()));
            } else {
                result.add(overflowItemSlot(
                        spec, ItemKind.INPUT, input.recipeIndex(), overflow++));
            }
        }
        for (ProcessingEmiRecipeData.ItemInput catalyst : data.catalysts()) {
            OptionalInt machineSlot = takeSlot(
                    spec,
                    available,
                    ProcessingMachineSpec.SlotRole.TOOL);
            if (machineSlot.isPresent()) {
                result.add(itemSlot(
                        spec,
                        ItemKind.CATALYST,
                        catalyst.recipeIndex(),
                        machineSlot.getAsInt()));
            } else {
                result.add(overflowItemSlot(
                        spec, ItemKind.CATALYST, catalyst.recipeIndex(), overflow++));
            }
        }
        for (int index = 0; index < data.itemOutputs().size(); index++) {
            ProcessingEmiRecipeData.ItemOutput output = data.itemOutputs().get(index);
            if (index < spec.items().outputs().size()) {
                result.add(itemSlot(
                        spec,
                        ItemKind.OUTPUT,
                        output.recipeIndex(),
                        spec.items().outputs().get(index)));
            } else {
                result.add(overflowItemSlot(
                        spec, ItemKind.OUTPUT, output.recipeIndex(), overflow++));
            }
        }
        return List.copyOf(result);
    }

    private static OptionalInt takeSlot(
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
            return OptionalInt.of(preferred);
        }
        if (available.isEmpty()) {
            return OptionalInt.empty();
        }
        int fallback = available.iterator().next();
        available.remove(fallback);
        return OptionalInt.of(fallback);
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

    private static ItemSlot overflowItemSlot(
            ProcessingMachineSpec spec,
            ItemKind kind,
            int recipeIndex,
            int overflowIndex) {
        int gap = 2;
        int columns = 9;
        int col = overflowIndex % columns;
        int row = overflowIndex / columns;
        int baseY = Math.max(
                spec.ui().machineSlots().stream()
                        .mapToInt(slot -> slot.y() + ITEM_SLOT_SIZE)
                        .max()
                        .orElse(0),
                WORKSTATION.bottom());
        int x = PADDING + col * (ITEM_SLOT_SIZE + gap);
        int y = baseY + PADDING + row * (ITEM_SLOT_SIZE + gap);
        return new ItemSlot(
                kind,
                recipeIndex,
                spec.items().slotCount() + overflowIndex,
                new Rect(x, y, ITEM_SLOT_SIZE, ITEM_SLOT_SIZE));
    }

    private static List<FluidTank> positionFluids(
            ProcessingMachineSpec spec,
            ProcessingEmiRecipeData data,
            List<Rect> occupied) {
        List<FluidTank> result = new ArrayList<>();
        int overflow = 0;
        for (int index = 0; index < data.fluidInputs().size(); index++) {
            if (index < spec.fluids().inputs().size()) {
                addTank(
                        spec,
                        FluidKind.INPUT,
                        data.fluidInputs().get(index).recipeIndex(),
                        spec.fluids().inputs().get(index),
                        occupied,
                        result);
            } else {
                addOverflowTank(
                        spec,
                        FluidKind.INPUT,
                        data.fluidInputs().get(index).recipeIndex(),
                        overflow++,
                        occupied,
                        result);
            }
        }
        for (int index = 0; index < data.fluidOutputs().size(); index++) {
            if (index < spec.fluids().outputs().size()) {
                addTank(
                        spec,
                        FluidKind.OUTPUT,
                        data.fluidOutputs().get(index).recipeIndex(),
                        spec.fluids().outputs().get(index),
                        occupied,
                        result);
            } else {
                addOverflowTank(
                        spec,
                        FluidKind.OUTPUT,
                        data.fluidOutputs().get(index).recipeIndex(),
                        overflow++,
                        occupied,
                        result);
            }
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

    private static void addOverflowTank(
            ProcessingMachineSpec spec,
            FluidKind kind,
            int recipeIndex,
            int overflowIndex,
            List<Rect> occupied,
            List<FluidTank> result) {
        int width = ITEM_SLOT_SIZE;
        int height = ITEM_SLOT_SIZE;
        long capacity = 1_000L;
        if (!spec.ui().tanks().isEmpty()) {
            ProcessingMachineSpec.TankPosition source = spec.ui().tanks().getFirst();
            width = source.width();
            height = source.height();
        }
        if (!spec.fluids().all().isEmpty()) {
            capacity = spec.fluids().all().getFirst().capacity();
        }
        int gap = 2;
        int baseX = occupied.stream().mapToInt(Rect::right).max().orElse(PADDING);
        Rect bounds = new Rect(
                baseX + PADDING + overflowIndex * (width + gap),
                PADDING,
                width,
                height);
        while (occupied.stream().anyMatch(bounds::overlaps)) {
            bounds = new Rect(
                    bounds.x() + 1, bounds.y(), bounds.width(), bounds.height());
        }
        occupied.add(bounds);
        result.add(new FluidTank(
                kind,
                recipeIndex,
                spec.fluids().tankCount() + overflowIndex,
                capacity,
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
