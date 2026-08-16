package com.masson.cruciblecraft.content.menu;

import java.util.function.Predicate;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineDisplayData;
import com.masson.cruciblecraft.machine.processing.ProcessingProgressSync;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
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

    public int progress() {
        return data.getCount() > 0 ? Math.max(0, data.get(0)) : 0;
    }

    public int duration() {
        return data.getCount() > 1 ? Math.max(0, data.get(1)) : 0;
    }

    public int progressPermille() {
        int dataIndex = progressPermilleDataIndex();
        if (dataIndex >= 0 && data.getCount() > dataIndex) {
            return Math.max(
                    0,
                    Math.min(
                            ProcessingProgressSync.COMPLETE_PERMILLE,
                            data.get(dataIndex)));
        }
        return ProcessingProgressSync.permille(progress(), duration());
    }

    public final int scaledProgress(int width) {
        if (width <= 0) {
            return 0;
        }
        if (progressPermilleDataIndex() < 0) {
            int duration = duration();
            return duration <= 0
                    ? 0
                    : (int) Math.min(
                            width,
                            (long) progress() * width / duration);
        }
        return (int) Math.min(
                width,
                (long) progressPermille() * width
                        / ProcessingProgressSync.COMPLETE_PERMILLE);
    }

    public final String status() {
        int dataIndex = statusDataIndex();
        if (dataIndex < 0
                || data.getCount() <= dataIndex
                || spec.ui().statuses().isEmpty()) {
            return "idle";
        }
        int index = data.get(dataIndex);
        if (index < 0 || index >= spec.ui().statuses().size()) {
            return ProcessingMachineDisplayData.UNKNOWN;
        }
        return spec.ui().statuses().get(index);
    }

    public final int statusArgument() {
        int dataIndex = statusArgumentDataIndex();
        return dataIndex >= 0 && data.getCount() > dataIndex
                ? Math.max(0, data.get(dataIndex))
                : 0;
    }

    protected int statusDataIndex() {
        return 3;
    }

    protected int statusArgumentDataIndex() {
        return 6;
    }

    protected int progressPermilleDataIndex() {
        return -1;
    }

    public int tankAmount(int tank) {
        if (tank < 0 || tank >= spec.fluids().tankCount()) {
            return 0;
        }
        return tankFluid(tank).getAmount();
    }

    public int tankCapacity(int tank) {
        if (tank < 0 || tank >= spec.fluids().tankCount()) {
            return 0;
        }
        return spec.fluids().all().stream()
                .filter(configured -> configured.index() == tank)
                .mapToInt(ProcessingMachineSpec.TankSpec::capacity)
                .findFirst()
                .orElse(0);
    }

    public final ProcessingMachineSpec machineSpec() {
        return spec;
    }

    /** Client-visible tank identity supplied by configured machine menus. */
    public FluidStack tankFluid(int tank) {
        return FluidStack.EMPTY;
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
        // Match vanilla quick-move semantics: onTake observes the live remainder,
        // which is empty when the complete stack moved.
        slot.onTake(player, source);
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
            // External machine writes can make lastNotified stale, but
            // moveItemStackTo only calls this method after mutating this slot.
            // Re-publishing current therefore reports the real transfer once.
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
