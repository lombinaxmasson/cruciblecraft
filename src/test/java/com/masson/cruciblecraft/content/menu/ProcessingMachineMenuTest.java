package com.masson.cruciblecraft.content.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.items.ItemStackHandler;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingMachineMenuTest {
    private static final ProcessingMachineSpec SPEC = spec();

    @BeforeAll
    static void bootstrap() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void shiftMergeIntoExistingMachineInputNotifiesHandler() {
        CountingItemStackHandler machine = new CountingItemStackHandler(2);
        machine.setStackInSlot(0, new ItemStack(Items.COAL, 10));
        SimpleContainer player = new SimpleContainer(36);
        player.setItem(9, new ItemStack(Items.COAL, 5));
        TestMenu menu = new TestMenu(player, machine);
        machine.resetChanges();

        ItemStack moved = menu.quickMoveStack(null, 2);

        assertEquals(5, moved.getCount());
        assertEquals(15, machine.getStackInSlot(0).getCount());
        assertTrue(player.getItem(9).isEmpty());
        assertEquals(1, machine.changes());
        assertEquals(15, itemCount(machine, player));
    }

    @Test
    void partialShiftFromMachineOutputNotifiesLiveSourceMutation() {
        CountingItemStackHandler machine = new CountingItemStackHandler(2);
        machine.setStackInSlot(1, new ItemStack(Items.COAL, 10));
        SimpleContainer player = blockedPlayerInventory(8);
        player.setItem(8, new ItemStack(Items.COAL, 60));
        TestMenu menu = new TestMenu(player, machine);
        AtomicReference<ItemStack> taken = new AtomicReference<>();
        ProcessingMachineMenu.NotifyingSlotItemHandler sourceSlot =
                new ProcessingMachineMenu.NotifyingSlotItemHandler(machine, 1, 18, 0) {
                    @Override public void onTake(Player takingPlayer, ItemStack stack) {
                        taken.set(stack.copy());
                        super.onTake(takingPlayer, stack);
                    }
                };
        menu.slots.set(1, sourceSlot);
        machine.resetChanges();

        ItemStack moved = menu.quickMoveStack(null, 1);

        assertEquals(10, moved.getCount());
        assertEquals(6, machine.getStackInSlot(1).getCount());
        assertEquals(10, taken.get().getCount(),
                "onTake receives the original stack while the slot retains its remainder");
        assertEquals(64, player.getItem(8).getCount());
        assertEquals(1, machine.changes());
        assertEquals(70, coalCount(machine, player));
    }

    @Test
    void completeShiftFromMachineOutputStillClearsAndNotifies() {
        CountingItemStackHandler machine = new CountingItemStackHandler(2);
        machine.setStackInSlot(1, new ItemStack(Items.COAL, 10));
        SimpleContainer player = new SimpleContainer(36);
        TestMenu menu = new TestMenu(player, machine);
        machine.resetChanges();

        ItemStack moved = menu.quickMoveStack(null, 1);

        assertEquals(10, moved.getCount());
        assertTrue(machine.getStackInSlot(1).isEmpty());
        assertEquals(1, machine.changes());
        assertEquals(10, coalCount(machine, player));
    }

    private static SimpleContainer blockedPlayerInventory(int openSlot) {
        SimpleContainer player = new SimpleContainer(36);
        for (int slot = 0; slot < player.getContainerSize(); slot++) {
            if (slot != openSlot) {
                player.setItem(slot, new ItemStack(Items.DIRT, 64));
            }
        }
        return player;
    }

    private static int itemCount(
            CountingItemStackHandler machine,
            SimpleContainer player) {
        int total = 0;
        for (int slot = 0; slot < machine.getSlots(); slot++) {
            total += machine.getStackInSlot(slot).getCount();
        }
        for (int slot = 0; slot < player.getContainerSize(); slot++) {
            total += player.getItem(slot).getCount();
        }
        return total;
    }

    private static int coalCount(
            CountingItemStackHandler machine,
            SimpleContainer player) {
        int total = 0;
        for (int slot = 0; slot < machine.getSlots(); slot++) {
            if (machine.getStackInSlot(slot).is(Items.COAL)) {
                total += machine.getStackInSlot(slot).getCount();
            }
        }
        for (int slot = 0; slot < player.getContainerSize(); slot++) {
            if (player.getItem(slot).is(Items.COAL)) {
                total += player.getItem(slot).getCount();
            }
        }
        return total;
    }

    private static ProcessingMachineSpec spec() {
        ResourceLocation id =
                ResourceLocation.fromNamespaceAndPath("test", "menu");
        return new ProcessingMachineSpec(
                id,
                id,
                () -> new RecipeMap(id),
                new ProcessingMachineSpec.SlotLayout(
                        2, List.of(0), List.of(1)),
                new ProcessingMachineSpec.TankLayout(List.of(), List.of()),
                new ProcessingMachineSpec.EnergySpec(
                        EnergyType.KINETIC,
                        ProcessingMachineSpec.EnergyMode.BUFFERED,
                        100,
                        16),
                new ProcessingMachineSpec.SidedIoPolicy(
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE,
                        (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE),
                recipe -> Optional.empty(),
                ProcessingMachineSpec.BufferPolicy.PAUSE,
                new ProcessingMachineSpec.UiLayout(
                        List.of(
                                new ProcessingMachineSpec.SlotPosition(0, 0),
                                new ProcessingMachineSpec.SlotPosition(18, 0)),
                        new ProcessingMachineSpec.ProgressBar(0, 0, 1, 1),
                        List.of(),
                        List.of("idle")));
    }

    private static final class TestMenu extends ProcessingMachineMenu {
        private TestMenu(
                SimpleContainer player,
                CountingItemStackHandler machine) {
            super(
                    null,
                    0,
                    player,
                    machine,
                    new SimpleContainerData(0),
                    SPEC,
                    ignored -> true);
        }
    }

    private static final class CountingItemStackHandler extends ItemStackHandler {
        private int changes;

        private CountingItemStackHandler(int size) {
            super(size);
        }

        @Override
        protected void onContentsChanged(int slot) {
            changes++;
        }

        private int changes() {
            return changes;
        }

        private void resetChanges() {
            changes = 0;
        }
    }
}
