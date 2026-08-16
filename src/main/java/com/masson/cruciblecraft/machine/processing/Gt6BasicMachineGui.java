package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.List;

/**
 * Ports GT6 {@code ContainerCommonBasicMachine} slot tables into CC
 * {@link ProcessingMachineSpec.UiLayout} coordinates.
 *
 * <p>GT6 paints each machine GUI for the RecipeMap panel size
 * ({@code mInputItemsCount} / {@code mOutputItemsCount} / fluid counts). CC
 * often exposes fewer slots; this helper lays out the GT6 panel then keeps
 * the first used slots. When CC has more slots than GT6 painted, the larger
 * CC grid is used as a whole so mixed switch-tables cannot overlap.
 *
 * <p>The Python converter {@code tools/gt6_gui_layout.py} implements the same
 * arithmetic and dumps {@code tools/gt6_basic_machine_layouts.json}.
 */
public final class Gt6BasicMachineGui {
    public static final int PROGRESS_X = 78;
    public static final int PROGRESS_Y = 24;
    public static final int PROGRESS_WIDTH = 20;
    public static final int PROGRESS_HEIGHT = 18;
    public static final int PROGRESS_U = 176;
    public static final int PROGRESS_V = 0;
    public static final int SPECIAL_SLOT_X = 80;
    public static final int SPECIAL_SLOT_Y = 43;
    public static final int FLUID_SLOT = 18;
    public static final ProcessingMachineSpec.ProgressBar PROGRESS =
            new ProcessingMachineSpec.ProgressBar(
                    PROGRESS_X, PROGRESS_Y, PROGRESS_WIDTH, PROGRESS_HEIGHT);
    public static final ProcessingMachineSpec.SlotPosition SPECIAL_SLOT =
            new ProcessingMachineSpec.SlotPosition(
                    SPECIAL_SLOT_X, SPECIAL_SLOT_Y);

    private Gt6BasicMachineGui() {}

    public static ProcessingMachineSpec.UiLayout ui(
            int panelInItems,
            int panelOutItems,
            int panelInFluids,
            int panelOutFluids,
            int usedInItems,
            int usedOutItems,
            int usedInFluids,
            int usedOutFluids,
            List<String> statuses) {
        return ui(
                panelInItems,
                panelOutItems,
                panelInFluids,
                panelOutFluids,
                usedInItems,
                usedOutItems,
                usedInFluids,
                usedOutFluids,
                -1,
                statuses);
    }

    public static ProcessingMachineSpec.UiLayout ui(
            int panelInItems,
            int panelOutItems,
            int panelInFluids,
            int panelOutFluids,
            int usedInItems,
            int usedOutItems,
            int usedInFluids,
            int usedOutFluids,
            int specialSlotIndex,
            List<String> statuses) {
        Layout layout = layout(
                panelInItems,
                panelOutItems,
                panelInFluids,
                panelOutFluids,
                usedInItems,
                usedOutItems,
                usedInFluids,
                usedOutFluids,
                specialSlotIndex);
        return new ProcessingMachineSpec.UiLayout(
                layout.itemSlots(),
                layout.progress(),
                layout.tanks(),
                statuses);
    }

    public static Layout layout(
            int panelInItems,
            int panelOutItems,
            int panelInFluids,
            int panelOutFluids,
            int usedInItems,
            int usedOutItems,
            int usedInFluids,
            int usedOutFluids,
            int specialSlotIndex) {
        int shiftInFluids = Math.max(panelInFluids, usedInFluids);
        int shiftOutFluids = Math.max(panelOutFluids, usedOutFluids);
        List<ProcessingMachineSpec.SlotPosition> items = new ArrayList<>();
        items.addAll(takeOrExtend(
                inputSlots(panelInItems, shiftInFluids),
                usedInItems,
                inputSlots(usedInItems, shiftInFluids)));
        items.addAll(takeOrExtend(
                outputSlots(panelOutItems, shiftOutFluids),
                usedOutItems,
                outputSlots(usedOutItems, shiftOutFluids)));
        if (specialSlotIndex >= 0) {
            items.add(specialSlotIndex, SPECIAL_SLOT);
        }
        List<ProcessingMachineSpec.TankPosition> tanks = new ArrayList<>();
        int tank = 0;
        for (ProcessingMachineSpec.SlotPosition slot :
                fluidSlots(usedInFluids, false)) {
            tanks.add(tank(tank++, slot));
        }
        for (ProcessingMachineSpec.SlotPosition slot :
                fluidSlots(usedOutFluids, true)) {
            tanks.add(tank(tank++, slot));
        }
        return new Layout(List.copyOf(items), List.copyOf(tanks), PROGRESS);
    }

    static List<ProcessingMachineSpec.SlotPosition> inputSlots(
            int count, int inFluids) {
        int yHigh = inFluids > 6 ? 7 : 25;
        int y0 = inFluids > 3 ? 7 : 16;
        int y1 = inFluids > 3 ? 25 : 34;
        if (count <= 0) {
            return List.of();
        }
        if (count == 1) {
            return List.of(slot(53, yHigh));
        }
        if (count == 2) {
            return List.of(slot(35, yHigh), slot(53, yHigh));
        }
        if (count == 3) {
            return List.of(slot(17, yHigh), slot(35, yHigh), slot(53, yHigh));
        }
        if (count == 4) {
            return List.of(slot(35, y0), slot(53, y0), slot(35, y1), slot(53, y1));
        }
        if (count == 5) {
            return List.of(
                    slot(17, y0),
                    slot(35, y0),
                    slot(53, y0),
                    slot(35, y1),
                    slot(53, y1));
        }
        if (count == 6) {
            return List.of(
                    slot(17, y0),
                    slot(35, y0),
                    slot(53, y0),
                    slot(17, y1),
                    slot(35, y1),
                    slot(53, y1));
        }
        List<ProcessingMachineSpec.SlotPosition> grid = List.of(
                slot(17, 7), slot(35, 7), slot(53, 7),
                slot(17, 25), slot(35, 25), slot(53, 25),
                slot(17, 43), slot(35, 43), slot(53, 43),
                slot(17, 61), slot(35, 61), slot(53, 61));
        return grid.subList(0, Math.min(count, grid.size()));
    }

    static List<ProcessingMachineSpec.SlotPosition> outputSlots(
            int count, int outFluids) {
        int yHigh = outFluids > 6 ? 7 : 25;
        int y0 = outFluids > 3 ? 7 : 16;
        int y1 = outFluids > 3 ? 25 : 34;
        if (count <= 0) {
            return List.of();
        }
        if (count == 1) {
            return List.of(slot(107, yHigh));
        }
        if (count == 2) {
            return List.of(slot(107, yHigh), slot(125, yHigh));
        }
        if (count == 3) {
            return List.of(slot(107, yHigh), slot(125, yHigh), slot(143, yHigh));
        }
        if (count == 4) {
            return List.of(
                    slot(107, y0), slot(125, y0), slot(107, y1), slot(125, y1));
        }
        if (count == 5) {
            return List.of(
                    slot(107, y0),
                    slot(125, y0),
                    slot(143, y0),
                    slot(107, y1),
                    slot(125, y1));
        }
        if (count == 6) {
            return List.of(
                    slot(107, y0),
                    slot(125, y0),
                    slot(143, y0),
                    slot(107, y1),
                    slot(125, y1),
                    slot(143, y1));
        }
        List<ProcessingMachineSpec.SlotPosition> grid = List.of(
                slot(107, 7), slot(125, 7), slot(143, 7),
                slot(107, 25), slot(125, 25), slot(143, 25),
                slot(107, 43), slot(125, 43), slot(143, 43),
                slot(107, 61), slot(125, 61), slot(143, 61));
        return grid.subList(0, Math.min(count, grid.size()));
    }

    static List<ProcessingMachineSpec.SlotPosition> fluidSlots(
            int count, boolean outputs) {
        int originX = outputs ? 107 : 53;
        int sign = outputs ? 1 : -1;
        List<ProcessingMachineSpec.SlotPosition> slots = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            slots.add(slot(
                    originX + sign * (index % 3) * 18,
                    63 - (index / 3) * 18));
        }
        return slots;
    }

    private static List<ProcessingMachineSpec.SlotPosition> takeOrExtend(
            List<ProcessingMachineSpec.SlotPosition> panel,
            int usedCount,
            List<ProcessingMachineSpec.SlotPosition> usedLayout) {
        if (usedCount <= 0) {
            return List.of();
        }
        if (usedCount <= panel.size()) {
            return List.copyOf(panel.subList(0, usedCount));
        }
        if (usedLayout.size() < usedCount) {
            throw new IllegalArgumentException(
                    "Cannot extend " + panel.size() + " GT6 slots to " + usedCount);
        }
        return List.copyOf(usedLayout.subList(0, usedCount));
    }

    private static ProcessingMachineSpec.TankPosition tank(
            int index, ProcessingMachineSpec.SlotPosition slot) {
        return new ProcessingMachineSpec.TankPosition(
                index, slot.x(), slot.y(), FLUID_SLOT, FLUID_SLOT);
    }

    private static ProcessingMachineSpec.SlotPosition slot(int x, int y) {
        return new ProcessingMachineSpec.SlotPosition(x, y);
    }

    public record Layout(
            List<ProcessingMachineSpec.SlotPosition> itemSlots,
            List<ProcessingMachineSpec.TankPosition> tanks,
            ProcessingMachineSpec.ProgressBar progress) {
        public Layout {
            itemSlots = List.copyOf(itemSlots);
            tanks = List.copyOf(tanks);
        }
    }
}
