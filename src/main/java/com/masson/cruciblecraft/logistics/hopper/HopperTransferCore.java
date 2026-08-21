package com.masson.cruciblecraft.logistics.hopper;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * BlockEntity-free Hopper / Queue Hopper transfer semantics.
 *
 * <p>GT6 {@code ST.move} is one source-slot to one dest-slot. Hopper loops that
 * move until the 64-item tick cap (or exact-mode first success). Queue emits
 * from the last slot up to {@code slotSize} items and advances FIFO inward.
 */
public final class HopperTransferCore {
    public static final int TICK_ITEM_CAP = 64;
    public static final int MIN_MODE = 0;
    public static final int MAX_MODE = 64;
    public static final int MIN_SLOT_SIZE = 1;
    public static final int MAX_SLOT_SIZE = 64;

    private HopperTransferCore() {}

    public static void requireMode(int mode) {
        if (mode < MIN_MODE || mode > MAX_MODE) {
            throw new IllegalArgumentException("Hopper mode must be 0..64");
        }
    }

    public static void requireSlotSize(int slotSize) {
        if (slotSize < MIN_SLOT_SIZE || slotSize > MAX_SLOT_SIZE) {
            throw new IllegalArgumentException(
                    "Queue slot size must be 1..64");
        }
    }

    public static int stackLimit(int mode) {
        requireMode(mode);
        if (mode <= 0) {
            return TICK_ITEM_CAP;
        }
        return mode * Math.max(1, TICK_ITEM_CAP / mode);
    }

    public static int count(IItemHandler handler) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            total += handler.getStackInSlot(slot).getCount();
        }
        return total;
    }

    public static ItemStackHandler limitedInventory(int slots, int slotLimit) {
        if (slots < 1 || slots > 36) {
            throw new IllegalArgumentException("Hopper slots must be 1..36");
        }
        requireSlotSize(slotLimit);
        return new ItemStackHandler(slots) {
            @Override
            public int getSlotLimit(int slot) {
                return slotLimit;
            }
        };
    }

    public static ItemStackHandler copyOf(IItemHandler source) {
        ItemStackHandler copy = new ItemStackHandler(source.getSlots()) {
            @Override
            public int getSlotLimit(int slot) {
                return source.getSlotLimit(slot);
            }
        };
        for (int slot = 0; slot < source.getSlots(); slot++) {
            copy.setStackInSlot(slot, source.getStackInSlot(slot).copy());
        }
        return copy;
    }

    /**
     * Normal hopper facing push. {@code mode=0} moves up to 64 (or one stack
     * when exact). {@code mode=n} moves complete n-batches, total ≤ 64; exact
     * stops after the first complete n.
     */
    public static int push(
            IItemHandler source,
            IItemHandler dest,
            int mode,
            boolean exact,
            boolean simulate) {
        requireMode(mode);
        int moved = 0;
        while (moved + (mode <= 0 ? 1 : mode) <= TICK_ITEM_CAP) {
            int maxMove = mode <= 0 ? TICK_ITEM_CAP - moved : mode;
            int minMove = mode <= 0 ? 1 : mode;
            int step = moveOnce(source, dest, maxMove, minMove, simulate);
            if (step < minMove) {
                break;
            }
            moved += step;
            if (exact) {
                break;
            }
        }
        return moved;
    }

    /** Queue output: last slot only, up to {@code slotSize} items this tick. */
    public static int pushQueue(
            IItemHandler source,
            IItemHandler dest,
            int slotSize,
            boolean simulate) {
        requireSlotSize(slotSize);
        if (source.getSlots() < 1) {
            return 0;
        }
        int last = source.getSlots() - 1;
        int moved = 0;
        while (moved < slotSize) {
            int step = moveFromSlot(
                    source, last, dest, slotSize - moved, 1, simulate);
            if (step < 1) {
                break;
            }
            moved += step;
        }
        return moved;
    }

    /** One GT6 {@code ST.move} from the block above into any hopper slot. */
    public static int pull(
            IItemHandler above,
            IItemHandler hopper,
            boolean simulate) {
        return moveOnce(above, hopper, TICK_ITEM_CAP, 1, simulate);
    }

    /** Queue input: first slot only. */
    public static int pullQueue(
            IItemHandler above,
            IItemHandler queue,
            boolean simulate) {
        if (queue.getSlots() < 1) {
            return 0;
        }
        return moveIntoSlot(above, queue, 0, TICK_ITEM_CAP, 1, simulate);
    }

    /** Merge later slots into earlier matching slots, GT6 hopper compact. */
    public static int compact(IItemHandler inventory, boolean simulate) {
        IItemHandler target = simulate ? copyOf(inventory) : inventory;
        int moved = 0;
        int slots = target.getSlots();
        for (int earlier = 0; earlier < slots; earlier++) {
            for (int later = earlier + 1; later < slots; later++) {
                moved += moveSlotToSlot(target, later, earlier, false);
            }
        }
        return moved;
    }

    /**
     * FIFO advance: slot {@code i-1} toward slot {@code i} until stable.
     * Items enter slot 0 and leave the last slot.
     */
    public static int advanceQueue(IItemHandler inventory, boolean simulate) {
        IItemHandler target = simulate ? copyOf(inventory) : inventory;
        int moved = 0;
        int previous = -1;
        while (previous != moved) {
            previous = moved;
            for (int slot = 1; slot < target.getSlots(); slot++) {
                moved += moveSlotToSlot(target, slot - 1, slot, false);
            }
        }
        return moved;
    }

    static int moveOnce(
            IItemHandler source,
            IItemHandler dest,
            int maxMove,
            int minMove,
            boolean simulate) {
        for (int from = 0; from < source.getSlots(); from++) {
            int moved = moveFromSlot(
                    source, from, dest, maxMove, minMove, simulate);
            if (moved >= minMove) {
                return moved;
            }
        }
        return 0;
    }

    static int moveIntoSlot(
            IItemHandler source,
            IItemHandler dest,
            int destSlot,
            int maxMove,
            int minMove,
            boolean simulate) {
        for (int from = 0; from < source.getSlots(); from++) {
            int moved = transferPair(
                    source, from, dest, destSlot, maxMove, minMove, simulate);
            if (moved >= minMove) {
                return moved;
            }
        }
        return 0;
    }

    static int moveFromSlot(
            IItemHandler source,
            int from,
            IItemHandler dest,
            int maxMove,
            int minMove,
            boolean simulate) {
        for (int to = 0; to < dest.getSlots(); to++) {
            int moved = transferPair(
                    source, from, dest, to, maxMove, minMove, simulate);
            if (moved >= minMove) {
                return moved;
            }
        }
        return 0;
    }

    static int moveSlotToSlot(
            IItemHandler inventory,
            int from,
            int to,
            boolean simulate) {
        ItemStack offered = inventory.getStackInSlot(from);
        if (offered.isEmpty()) {
            return 0;
        }
        return transferPair(
                inventory,
                from,
                inventory,
                to,
                offered.getCount(),
                1,
                simulate);
    }

    private static int transferPair(
            IItemHandler source,
            int from,
            IItemHandler dest,
            int to,
            int maxMove,
            int minMove,
            boolean simulate) {
        if (maxMove < minMove || minMove <= 0) {
            return 0;
        }
        ItemStack present = source.getStackInSlot(from);
        if (present.isEmpty() || present.getCount() < minMove) {
            return 0;
        }
        int request = Math.min(maxMove, present.getCount());
        ItemStack simulatedExtract = source.extractItem(from, request, true);
        if (!validExtract(present, simulatedExtract, minMove)) {
            return 0;
        }
        int offeredCount = simulatedExtract.getCount();
        ItemStack simulatedRemainder = dest.insertItem(
                to, simulatedExtract.copy(), true);
        if (!validRemainder(simulatedExtract, simulatedRemainder)) {
            return 0;
        }
        int accepted = offeredCount - simulatedRemainder.getCount();
        if (accepted < minMove) {
            return 0;
        }
        ItemStack planned = simulatedExtract.copyWithCount(accepted);
        ItemStack plannedRemainder = dest.insertItem(to, planned.copy(), true);
        if (!plannedRemainder.isEmpty()
                || !validRemainder(planned, plannedRemainder)) {
            return 0;
        }
        if (simulate) {
            return accepted;
        }
        ItemStack extracted = source.extractItem(from, accepted, false);
        if (!validExecutedExtract(planned, extracted)) {
            if (!extracted.isEmpty()) {
                ItemStack leftover = source.insertItem(from, extracted, false);
                if (!leftover.isEmpty()) {
                    dest.insertItem(to, leftover, false);
                }
            }
            return 0;
        }
        ItemStack executedRemainder = dest.insertItem(to, extracted, false);
        if (!executedRemainder.isEmpty()) {
            source.insertItem(from, executedRemainder, false);
            return 0;
        }
        return accepted;
    }

    private static boolean validExtract(
            ItemStack present, ItemStack extracted, int minMove) {
        return extracted != null
                && !extracted.isEmpty()
                && extracted.getCount() >= minMove
                && extracted.getCount() <= present.getCount()
                && ItemStack.isSameItemSameComponents(present, extracted);
    }

    private static boolean validExecutedExtract(
            ItemStack planned, ItemStack extracted) {
        return extracted != null
                && extracted.getCount() == planned.getCount()
                && ItemStack.isSameItemSameComponents(planned, extracted);
    }

    static boolean validRemainder(ItemStack offered, ItemStack remainder) {
        return remainder != null
                && remainder.getCount() >= 0
                && remainder.getCount() <= offered.getCount()
                && (remainder.isEmpty()
                        || ItemStack.isSameItemSameComponents(
                                offered, remainder));
    }
}
