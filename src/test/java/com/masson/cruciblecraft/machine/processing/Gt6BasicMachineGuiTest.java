package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.registry.ModProcessingMachines;

class Gt6BasicMachineGuiTest {
    @Test
    void oneInOneOutUsesGt6DefaultRow() {
        Gt6BasicMachineGui.Layout layout = Gt6BasicMachineGui.layout(
                1, 1, 0, 0, 1, 1, 0, 0, -1);
        assertEquals(
                List.of(slot(53, 25), slot(107, 25)),
                layout.itemSlots());
        assertEquals(List.of(), layout.tanks());
        assertEquals(Gt6BasicMachineGui.PROGRESS, layout.progress());
    }

    @Test
    void crusherUsesFirstOfTwelveOutputs() {
        assertEquals(
                List.of(slot(53, 25), slot(107, 7)),
                Gt6BasicMachineGui.layout(1, 12, 0, 0, 1, 1, 0, 0, -1)
                        .itemSlots());
    }

    @Test
    void mixerKeepsSixFluidItemShift() {
        Gt6BasicMachineGui.Layout layout = Gt6BasicMachineGui.layout(
                6, 1, 6, 2, 4, 1, 3, 2, -1);
        assertEquals(
                List.of(
                        slot(17, 7),
                        slot(35, 7),
                        slot(53, 7),
                        slot(17, 25),
                        slot(107, 25)),
                layout.itemSlots());
        assertEquals(
                List.of(
                        tank(0, 53, 63),
                        tank(1, 35, 63),
                        tank(2, 17, 63),
                        tank(3, 107, 63),
                        tank(4, 125, 63)),
                layout.tanks());
    }

    @Test
    void extruderInsertsSpecialSlotBetweenMaterialAndOutput() {
        assertEquals(
                List.of(slot(35, 25), slot(80, 43), slot(107, 25), slot(125, 25)),
                Gt6BasicMachineGui.layout(2, 2, 0, 0, 1, 2, 0, 0, 1)
                        .itemSlots());
        assertEquals(
                Gt6BasicMachineGui.layout(2, 2, 0, 0, 1, 2, 0, 0, 1).itemSlots(),
                ModProcessingMachines.EXTRUDER.ui().machineSlots());
    }

    @Test
    void welderNineInputsFillGt6ThreeByThreeGrid() {
        Gt6BasicMachineGui.Layout layout = Gt6BasicMachineGui.layout(
                9, 1, 1, 0, 9, 1, 1, 0, -1);
        assertEquals(
                List.of(
                        slot(17, 7),
                        slot(35, 7),
                        slot(53, 7),
                        slot(17, 25),
                        slot(35, 25),
                        slot(53, 25),
                        slot(17, 43),
                        slot(35, 43),
                        slot(53, 43),
                        slot(107, 25)),
                layout.itemSlots());
        assertEquals(List.of(tank(0, 53, 63)), layout.tanks());
    }

    @Test
    void smelterUsesLargerOutputGridWhenCcHasMoreSlots() {
        Gt6BasicMachineGui.Layout layout = Gt6BasicMachineGui.layout(
                1, 1, 1, 1, 1, 4, 0, 1, -1);
        assertEquals(
                List.of(
                        slot(53, 25),
                        slot(107, 16),
                        slot(125, 16),
                        slot(107, 34),
                        slot(125, 34)),
                layout.itemSlots());
        assertEquals(List.of(tank(0, 107, 63)), layout.tanks());
    }

    private static ProcessingMachineSpec.SlotPosition slot(int x, int y) {
        return new ProcessingMachineSpec.SlotPosition(x, y);
    }

    private static ProcessingMachineSpec.TankPosition tank(
            int tank, int x, int y) {
        return new ProcessingMachineSpec.TankPosition(tank, x, y, 18, 18);
    }
}
