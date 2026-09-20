package com.masson.cruciblecraft.content.menu;

import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import com.masson.cruciblecraft.machine.processing.Gt6BasicMachineGui;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class CokeOvenMenu extends AbstractContainerMenu {
    public static final int MACHINE_SLOTS = CokeOvenBlockEntity.SLOT_COUNT;
    public static final Gt6BasicMachineGui.Layout LAYOUT = Gt6BasicMachineGui.layout(
            1, 9, 0, 1, 1, 9, 0, 1, -1);
    private final CokeOvenBlockEntity blockEntity;
    private final ContainerData data;

    public CokeOvenMenu(int containerId, Inventory playerInventory) {
        this(
                containerId,
                playerInventory,
                null,
                new ItemStackHandler(MACHINE_SLOTS),
                new SimpleContainerData(6));
    }

    public CokeOvenMenu(
            int containerId,
            Inventory playerInventory,
            CokeOvenBlockEntity blockEntity) {
        this(
                containerId,
                playerInventory,
                blockEntity,
                blockEntity.inventory(),
                blockEntity.data());
    }

    private CokeOvenMenu(
            int containerId,
            Inventory playerInventory,
            CokeOvenBlockEntity blockEntity,
            IItemHandler machineInventory,
            ContainerData data) {
        super(ModMenus.COKE_OVEN.get(), containerId);
        this.blockEntity = blockEntity;
        this.data = data;
        checkContainerDataCount(data, 6);

        addSlot(new ProcessingMachineMenu.NotifyingSlotItemHandler(
                machineInventory,
                CokeOvenBlockEntity.INPUT_SLOT,
                LAYOUT.itemSlots().get(0).x(),
                LAYOUT.itemSlots().get(0).y()));
        for (int output = 0; output < CokeOvenBlockEntity.OUTPUT_SLOT_COUNT; output++) {
            int slot = CokeOvenBlockEntity.OUTPUT_SLOT + output;
            addSlot(new ProcessingMachineMenu.NotifyingSlotItemHandler(
                    machineInventory,
                    slot,
                    LAYOUT.itemSlots().get(slot).x(),
                    LAYOUT.itemSlots().get(slot).y()) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return false;
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
            addSlot(new Slot(
                    playerInventory,
                    column,
                    8 + column * 18,
                    142));
        }
        addDataSlots(data);
    }

    public int progress() {
        return data.get(0);
    }

    public int duration() {
        return data.get(1);
    }

    public int tankAmount() {
        return data.get(2);
    }

    public int tankCapacity() {
        return data.get(3);
    }

    public boolean structureValid() {
        return data.get(4) != 0;
    }

    public boolean ignited() {
        return data.get(5) != 0;
    }

    public int scaledProgress(int width) {
        return duration() <= 0 ? 0 : progress() * width / duration();
    }

    public int scaledTank(int height) {
        return tankCapacity() <= 0 ? 0 : tankAmount() * height / tankCapacity();
    }

    @Override
    public boolean stillValid(Player player) {
        return blockEntity == null || blockEntity.stillValid(player);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack source = slot.getItem();
        ItemStack original = source.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(source, MACHINE_SLOTS, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (!moveItemStackTo(source, 0, 1, false)) {
            return ItemStack.EMPTY;
        }

        if (source.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, source);
        return original;
    }
}
