package com.masson.cruciblecraft.content.menu;

import java.util.function.Predicate;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.IItemHandlerModifiable;
import net.neoforged.neoforge.items.SlotItemHandler;

/** Spec-driven slots, transfer ranges, progress, power, and status vocabulary. */
public abstract class ProcessingMachineMenu extends AbstractContainerMenu {
    private final ProcessingMachineSpec spec;
    private final ContainerData data;
    private final Predicate<Player> validity;

    protected ProcessingMachineMenu(
            MenuType<?> type,
            int id,
            Inventory playerInventory,
            IItemHandler machineItems,
            ContainerData data,
            ProcessingMachineSpec spec,
            Predicate<Player> validity) {
        this(type, id, (Container) playerInventory, machineItems, data, spec, validity);
    }

    ProcessingMachineMenu(
            MenuType<?> type,
            int id,
            Container playerInventory,
            IItemHandler machineItems,
            ContainerData data,
            ProcessingMachineSpec spec,
            Predicate<Player> validity) {
        super(type, id);
        this.spec = spec;
        this.data = data;
        this.validity = validity;
        for (int slot = 0; slot < spec.items().slotCount(); slot++) {
            ProcessingMachineSpec.SlotPosition position =
                    spec.ui().machineSlots().get(slot);
            boolean output = spec.items().outputs().contains(slot);
            addSlot(new NotifyingSlotItemHandler(
                    machineItems, slot, position.x(), position.y()) {
                @Override public boolean mayPlace(ItemStack stack) {
                    return !output && super.mayPlace(stack);
                }
            });
        }
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(
                        playerInventory,
                        column + row * 9 + 9,
                        8 + column * 18,
                        84 + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, 142));
        }
        addDataSlots(data);
    }

    public final int scaledProgress(int width) {
        return data.get(1) <= 0 ? 0 : data.get(0) * width / data.get(1);
    }

    public final int powerDemand() {
        return data.getCount() > 2 ? data.get(2) : 0;
    }

    public final String status() {
        if (data.getCount() <= 3 || spec.ui().statuses().isEmpty()) {
            return "idle";
        }
        int index = Math.max(0, Math.min(
                data.get(3), spec.ui().statuses().size() - 1));
        return spec.ui().statuses().get(index);
    }

    public final int tankAmount() {
        return data.getCount() > 4 ? Math.max(0, data.get(4)) : 0;
    }

    public final int tankCapacity() {
        return data.getCount() > 5 ? Math.max(0, data.get(5)) : 0;
    }

    public final ProcessingMachineSpec machineSpec() {
        return spec;
    }

    @Override public boolean stillValid(Player player) {
        return validity.test(player);
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        ProcessingMenuRanges ranges = ProcessingMenuRanges.forSpec(spec);
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        boolean moved;
        if (index < ranges.machineEnd()) {
            moved = moveItemStackTo(source, ranges.playerStart(), ranges.playerEnd(), true);
        } else {
            moved = false;
            for (int input : ranges.inputSlots()) {
                if (moveItemStackTo(source, input, input + 1, false)) {
                    moved = true;
                    if (source.isEmpty()) {
                        break;
                    }
                }
            }
        }
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, original);
        return original;
    }

    static class NotifyingSlotItemHandler extends SlotItemHandler {
        private boolean notifying;
        private ItemStack lastNotified;

        NotifyingSlotItemHandler(
                IItemHandler itemHandler,
                int index,
                int xPosition,
                int yPosition) {
            super(itemHandler, index, xPosition, yPosition);
            lastNotified = getItem().copy();
        }

        @Override public void set(ItemStack stack) {
            if (!(getItemHandler() instanceof IItemHandlerModifiable modifiable)) {
                super.set(stack);
                return;
            }
            notifying = true;
            try {
                modifiable.setStackInSlot(index, stack);
                lastNotified = stack.copy();
            } finally {
                notifying = false;
            }
        }

        @Override public void setChanged() {
            if (notifying) {
                return;
            }
            if (!(getItemHandler() instanceof IItemHandlerModifiable modifiable)) {
                super.setChanged();
                return;
            }
            ItemStack current = getItem().copy();
            if (ItemStack.matches(lastNotified, current)) {
                return;
            }
            notifying = true;
            try {
                // moveItemStackTo mutates SlotItemHandler's live stack in place.
                // Re-setting a copy makes ItemStackHandler observe that mutation.
                modifiable.setStackInSlot(index, current);
                lastNotified = current;
            } finally {
                notifying = false;
            }
        }
    }
}
