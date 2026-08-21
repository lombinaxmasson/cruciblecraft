package com.masson.cruciblecraft.logistics.hopper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.logistics.hopper.HopperMenuLayout.SlotPos;
import com.masson.cruciblecraft.logistics.hopper.HopperMenuLayout.SlotRole;

class HopperMenuLayoutTest {
    @Test
    void rowBandsMatchOneToThirtySix() {
        assertEquals(1, HopperMenuLayout.rows(1));
        assertEquals(1, HopperMenuLayout.rows(9));
        assertEquals(2, HopperMenuLayout.rows(10));
        assertEquals(2, HopperMenuLayout.rows(18));
        assertEquals(3, HopperMenuLayout.rows(19));
        assertEquals(3, HopperMenuLayout.rows(27));
        assertEquals(4, HopperMenuLayout.rows(28));
        assertEquals(4, HopperMenuLayout.rows(36));
    }

    @Test
    void lastRowIsNotPaddedWithGhostSlots() {
        assertEquals(1, HopperMenuLayout.hopperSlots(1).size());
        assertEquals(2, HopperMenuLayout.hopperSlots(2).size());
        assertEquals(10, HopperMenuLayout.hopperSlots(10).size());
        assertEquals(1, HopperMenuLayout.lastRowCount(1));
        assertEquals(1, HopperMenuLayout.lastRowCount(10));
        assertEquals(9, HopperMenuLayout.lastRowCount(18));
        List<SlotPos> ten = HopperMenuLayout.hopperSlots(10);
        assertEquals(HopperMenuLayout.MACHINE_X, ten.get(9).x());
        assertEquals(
                HopperMenuLayout.MACHINE_Y + HopperMenuLayout.SLOT,
                ten.get(9).y());
    }

    @Test
    void playerInventoryMovesDownWithMachineRows() {
        int one = HopperMenuLayout.playerInventoryY(1);
        int ten = HopperMenuLayout.playerInventoryY(10);
        int thirtysix = HopperMenuLayout.playerInventoryY(36);
        assertTrue(ten > one);
        assertTrue(thirtysix > ten);
        assertEquals(
                HopperMenuLayout.playerInventoryY(9) + HopperMenuLayout.SLOT,
                HopperMenuLayout.playerInventoryY(10));
        assertEquals(36, HopperMenuLayout.playerSlots(5).size());
        assertEquals(
                HopperMenuLayout.hotbarY(36) + HopperMenuLayout.HOTBAR_BOTTOM_PAD,
                HopperMenuLayout.imageHeight(36));
    }

    @Test
    void quickMoveRangesAreExact() {
        var ranges = HopperMenuLayout.ranges(5);
        assertEquals(0, ranges.machineStart());
        assertEquals(5, ranges.machineEnd());
        assertEquals(5, ranges.playerStart());
        assertEquals(41, ranges.playerEnd());
        assertEquals(
                5,
                HopperMenuLayout.quickMoveDestination(5, 0, false));
        assertEquals(
                0,
                HopperMenuLayout.quickMoveDestination(5, 5, false));
        assertEquals(-1, HopperMenuLayout.quickMoveDestination(5, 0, true));
        assertEquals(-1, HopperMenuLayout.quickMoveDestination(5, 41, false));
    }

    @Test
    void queueMarksInputAndOutputOrder() {
        List<SlotPos> two = HopperMenuLayout.queueSlots(2);
        assertEquals(SlotRole.QUEUE_INPUT, two.get(0).role());
        assertEquals(SlotRole.QUEUE_OUTPUT, two.get(1).role());
        List<SlotPos> six = HopperMenuLayout.queueSlots(6);
        assertEquals(SlotRole.QUEUE_INPUT, six.get(0).role());
        assertEquals(SlotRole.QUEUE_BUFFER, six.get(2).role());
        assertEquals(SlotRole.QUEUE_OUTPUT, six.get(5).role());
        assertNotEquals(
                SlotRole.QUEUE_INPUT,
                HopperMenuLayout.hopperSlots(6).get(0).role());
    }

    @Test
    void slotIndexesAreUniqueAndDense() {
        Set<Integer> seen = new HashSet<>();
        for (SlotPos slot : HopperMenuLayout.hopperSlots(36)) {
            assertTrue(seen.add(slot.index()));
        }
        assertEquals(36, seen.size());
        assertThrows(
                IllegalArgumentException.class,
                () -> HopperMenuLayout.hopperSlots(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> HopperMenuLayout.hopperSlots(37));
    }
}
