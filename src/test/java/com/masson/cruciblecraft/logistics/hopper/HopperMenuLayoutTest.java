package com.masson.cruciblecraft.logistics.hopper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    private static final int[] GT6_COUNTS = {
        1, 2, 3, 4, 5, 6, 7, 8, 9, 12, 14, 15, 16, 18, 27, 36
    };

    @Test
    void fiveSlotMatchesVanillaHopper() {
        List<SlotPos> five = HopperMenuLayout.hopperSlots(5);
        assertEquals(List.of(44, 62, 80, 98, 116), five.stream().map(SlotPos::x).toList());
        assertEquals(35, five.get(0).y());
        assertEquals(35, five.get(4).y());
        assertEquals(84, HopperMenuLayout.playerInventoryY(5));
        assertEquals(142, HopperMenuLayout.hotbarY(5));
        assertEquals(166, HopperMenuLayout.imageHeight(5));
    }

    @Test
    void nineSlotIsCenteredThreeByThreeNotAChestRow() {
        List<SlotPos> nine = HopperMenuLayout.hopperSlots(9);
        assertEquals(62, nine.get(0).x());
        assertEquals(17, nine.get(0).y());
        assertEquals(98, nine.get(2).x());
        assertEquals(53, nine.get(8).y());
        assertTrue(HopperMenuLayout.hasChestBackground(9));
    }

    @Test
    void dedicatedCountsHaveNoGhostSlots() {
        for (int slots : GT6_COUNTS) {
            assertEquals(slots, HopperMenuLayout.hopperSlots(slots).size());
            assertTrue(HopperMenuLayout.hasChestBackground(slots));
            assertEquals(166, HopperMenuLayout.imageHeight(slots));
            assertEquals(84, HopperMenuLayout.playerInventoryY(slots));
        }
        assertFalse(HopperMenuLayout.hasChestBackground(10));
        assertEquals(10, HopperMenuLayout.hopperSlots(10).size());
        List<SlotPos> ten = HopperMenuLayout.hopperSlots(10);
        assertEquals(80, ten.get(9).x());
        assertEquals(44, ten.get(9).y());
    }

    @Test
    void playerInventoryStaysAtGt6Offset() {
        assertEquals(36, HopperMenuLayout.playerSlots(5).size());
        assertEquals(
                HopperMenuLayout.PLAYER_INVENTORY_Y,
                HopperMenuLayout.playerSlots(36).get(0).y());
        assertEquals(
                HopperMenuLayout.hotbarY(36) + HopperMenuLayout.HOTBAR_BOTTOM_PAD,
                HopperMenuLayout.imageHeight(36));
        assertEquals(4, HopperMenuLayout.titleLabelY(5));
        assertTrue(HopperMenuLayout.titleLabelY(36) < 0);
        assertTrue(HopperMenuLayout.playerInventoryLabelY(36) < 0);
        assertEquals(72, HopperMenuLayout.playerInventoryLabelY(5));
    }

    @Test
    void machineSlotsStayAbovePlayerInventory() {
        for (int slots : GT6_COUNTS) {
            int lowest = HopperMenuLayout.hopperSlots(slots).stream()
                    .mapToInt(SlotPos::y)
                    .max()
                    .orElseThrow();
            assertTrue(lowest + 16 <= HopperMenuLayout.playerInventoryY(slots));
        }
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
        assertEquals(71, two.get(0).x());
        assertEquals(89, two.get(1).x());
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
