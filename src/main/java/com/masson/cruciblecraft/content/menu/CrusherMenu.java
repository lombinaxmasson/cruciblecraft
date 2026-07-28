package com.masson.cruciblecraft.content.menu;

import com.masson.cruciblecraft.content.blockentity.CrusherBlockEntity;
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
import net.neoforged.neoforge.items.SlotItemHandler;

public final class CrusherMenu extends AbstractContainerMenu {
    private final CrusherBlockEntity blockEntity;
    private final ContainerData data;
    public CrusherMenu(int id, Inventory inv) { this(id, inv, null, new ItemStackHandler(2), new SimpleContainerData(3)); }
    public CrusherMenu(int id, Inventory inv, CrusherBlockEntity be) { this(id, inv, be, be.inventory(), be.data()); }
    private CrusherMenu(int id, Inventory inv, CrusherBlockEntity be, IItemHandler items, ContainerData data) {
        super(ModMenus.CRUSHER.get(), id); this.blockEntity = be; this.data = data;
        addSlot(new SlotItemHandler(items, 0, 56, 35));
        addSlot(new SlotItemHandler(items, 1, 116, 35) {
            @Override public boolean mayPlace(ItemStack stack) { return false; }
        });
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(inv, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(inv, col, 8 + col * 18, 142));
        addDataSlots(data);
    }
    public int scaledProgress(int width) { return data.get(1) <= 0 ? 0 : data.get(0) * width / data.get(1); }
    public int powerDemand() { return data.get(2); }
    @Override public boolean stillValid(Player player) { return blockEntity == null || blockEntity.stillValid(player); }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index); if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack source = slot.getItem(), original = source.copy();
        if (index < 2 ? !moveItemStackTo(source, 2, slots.size(), true) : !moveItemStackTo(source, 0, 1, false))
            return ItemStack.EMPTY;
        if (source.isEmpty()) slot.set(ItemStack.EMPTY); else slot.setChanged();
        return original;
    }
}
