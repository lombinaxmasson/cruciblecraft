package com.masson.cruciblecraft.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperMenuLayout;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.items.ItemStackHandler;

class HopperMenuTest {
    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void quickMoveStaysInsideArbitrarySlotCounts() {
        for (int slots : new int[] {1, 5, 9, 18, 36}) {
            ItemStackHandler machine = new ItemStackHandler(slots);
            machine.setStackInSlot(0, new ItemStack(Items.IRON_INGOT, 8));
            SimpleContainer player = new SimpleContainer(36);
            HopperMenu menu = new HopperMenu(
                    null,
                    0,
                    player,
                    machine,
                    HopperKind.HOPPER,
                    slots,
                    ignored -> true);
            assertEquals(slots, menu.machineSlots());
            assertEquals(slots + 36, menu.slots.size());
            ItemStack moved = menu.quickMoveStack(null, 0);
            assertEquals(8, moved.getCount());
            assertTrue(machine.getStackInSlot(0).isEmpty());
            int playerCount = 0;
            for (int slot = 0; slot < player.getContainerSize(); slot++) {
                playerCount += player.getItem(slot).getCount();
            }
            assertEquals(8, playerCount);
            assertEquals(
                    slots,
                    HopperMenuLayout.quickMoveDestination(slots, 0, false));
            assertEquals(
                    0,
                    HopperMenuLayout.quickMoveDestination(slots, slots, false));
            assertEquals(
                    -1,
                    HopperMenuLayout.quickMoveDestination(slots, -1, false));
        }
    }

    @Test
    void queueMenuUsesTheSameSlotCountWithoutGhostPadding() {
        ItemStackHandler machine = new ItemStackHandler(2);
        SimpleContainer player = new SimpleContainer(36);
        HopperMenu menu = new HopperMenu(
                null,
                0,
                player,
                machine,
                HopperKind.QUEUE_HOPPER,
                2,
                ignored -> false);
        assertEquals(HopperKind.QUEUE_HOPPER, menu.kind());
        assertEquals(2, menu.machineSlots());
        assertEquals(38, menu.slots.size());
        assertEquals(
                HopperMenuLayout.imageHeight(2),
                menu.imageHeight());
    }
}
