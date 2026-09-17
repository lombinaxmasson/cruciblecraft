package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.content.block.HopperBlock;
import com.masson.cruciblecraft.content.menu.HopperMenu;
import com.masson.cruciblecraft.logistics.hopper.HopperKind;
import com.masson.cruciblecraft.logistics.hopper.HopperSidedHandler;
import com.masson.cruciblecraft.logistics.hopper.HopperTransferCore;
import com.masson.cruciblecraft.logistics.hopper.HopperVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModMenus;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/** Shared Hopper / Queue Hopper host. Kind strategy comes from the block variant. */
public final class HopperBlockEntity extends BlockEntity implements MenuProvider {
    public static final int SCHEMA_VERSION = 1;
    private static final int MAX_MODE = 64;

    private final HopperVariant variant;
    private final ItemStackHandler inventory;
    private int mode;
    private boolean exactMode;
    private int slotSize = MAX_MODE;
    private boolean transferring;
    private boolean failClosed;
    private final List<ItemStack> overflow = new ArrayList<>();

    public HopperBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.HOPPER.get(), pos, state);
    }

    public HopperBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        this(type, pos, state, variantOf(state));
    }

    HopperBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            HopperVariant variant) {
        super(type, pos, state);
        this.variant = variant;
        this.inventory = new ItemStackHandler(variant.slots()) {
            @Override
            public int getSlotLimit(int slot) {
                return variant.kind().isQueue()
                        ? slotSize
                        : HopperTransferCore.stackLimit(mode);
            }

            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
        if (variant.kind().isQueue()) {
            this.mode = 0;
            this.slotSize = MAX_MODE;
        }
    }

    public HopperVariant variant() {
        return variant;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public int mode() {
        return variant.kind().isQueue() ? slotSize : mode;
    }

    public boolean exactMode() {
        return !variant.kind().isQueue() && exactMode;
    }

    public boolean failClosed() {
        return failClosed;
    }

    public List<ItemStack> overflow() {
        return List.copyOf(overflow);
    }

    public CompoundTag saveForTest(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    public void loadForTest(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    public IItemHandler itemHandler(Direction side) {
        Direction facing = getBlockState().getValue(HopperBlock.FACING);
        boolean facingSide = side == facing;
        return new HopperSidedHandler(
                inventory, variant.kind(), facingSide, transferring && facingSide);
    }

    public boolean stillValid(Player player) {
        return level != null
                && level.getBlockEntity(worldPosition) == this
                && player.distanceToSqr(
                        worldPosition.getX() + 0.5,
                        worldPosition.getY() + 0.5,
                        worldPosition.getZ() + 0.5) <= 64.0;
    }

    public void cycleMode(boolean reverse) {
        if (variant.kind().isQueue()) {
            slotSize += reverse ? -1 : 1;
            if (slotSize < 1) {
                slotSize = MAX_MODE;
            }
            if (slotSize > MAX_MODE) {
                slotSize = 1;
            }
        } else if (reverse) {
            mode--;
            if (mode < 0) {
                mode = MAX_MODE;
            }
        } else {
            mode++;
            if (mode > MAX_MODE) {
                mode = 0;
            }
        }
        setChanged();
    }

    public void toggleExactMode() {
        if (!variant.kind().isQueue()) {
            exactMode = !exactMode;
            setChanged();
        }
    }

    public void resetModes() {
        if (variant.kind().isQueue()) {
            slotSize = MAX_MODE;
            restackToSlotSize();
        } else {
            mode = 0;
            exactMode = false;
        }
        setChanged();
    }

    public Component statusMessage() {
        if (variant.kind().isQueue()) {
            return Component.translatable(
                    "message.cruciblecraft.hopper.queue_slot_size", slotSize);
        }
        if (mode <= 0) {
            return Component.translatable(
                    exactMode
                            ? "message.cruciblecraft.hopper.mode_stack"
                            : "message.cruciblecraft.hopper.mode_any");
        }
        return Component.translatable(
                exactMode
                        ? "message.cruciblecraft.hopper.mode_exact"
                        : "message.cruciblecraft.hopper.mode_divisible",
                mode);
    }

    public void dropContents() {
        if (level == null) {
            return;
        }
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            Containers.dropItemStack(
                    level,
                    worldPosition.getX(),
                    worldPosition.getY(),
                    worldPosition.getZ(),
                    inventory.getStackInSlot(slot));
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
        overflow.forEach(stack -> Containers.dropItemStack(
                level,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                stack));
        overflow.clear();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            HopperBlockEntity hopper) {
        if (hopper.failClosed || pausedByRedstone(level, pos)) {
            return;
        }
        hopper.pull(level, pos);
        hopper.suck(level, pos);
        if (hopper.variant.kind().isQueue()) {
            HopperTransferCore.advanceQueue(hopper.inventory, false);
        } else {
            HopperTransferCore.compact(hopper.inventory, false);
        }
        hopper.push(level, pos, state);
    }

    private static boolean pausedByRedstone(Level level, BlockPos pos) {
        for (Direction side : Direction.values()) {
            BlockPos neighbor = pos.relative(side);
            if (level.getBlockState(neighbor).is(BlockTags.RAILS)) {
                continue;
            }
            if (level.getSignal(neighbor, side) > 0) {
                return true;
            }
        }
        return false;
    }

    private void pull(Level level, BlockPos pos) {
        IItemHandler above = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
        if (above == null) {
            return;
        }
        if (variant.kind().isQueue()) {
            HopperTransferCore.pullQueue(above, inventory, false);
        } else {
            HopperTransferCore.pull(above, inventory, false);
        }
    }

    private void push(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(HopperBlock.FACING);
        if (facing == Direction.UP) {
            return;
        }
        IItemHandler dest = level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                pos.relative(facing),
                facing.getOpposite());
        if (dest == null) {
            return;
        }
        transferring = true;
        try {
            if (variant.kind() == HopperKind.QUEUE_HOPPER) {
                HopperTransferCore.pushQueue(inventory, dest, slotSize, false);
            } else {
                HopperTransferCore.push(inventory, dest, mode, exactMode, false);
            }
        } finally {
            transferring = false;
        }
    }

    private void suck(Level level, BlockPos pos) {
        IItemHandler above = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
        if (above != null) {
            return;
        }
        AABB box = new AABB(pos.above());
        for (ItemEntity entity : level.getEntitiesOfClass(ItemEntity.class, box)) {
            if (!entity.isAlive() || entity.getItem().isEmpty()) {
                continue;
            }
            ItemStack leftover = entity.getItem().copy();
            for (int slot = 0; slot < inventory.getSlots() && !leftover.isEmpty(); slot++) {
                leftover = inventory.insertItem(slot, leftover, false);
            }
            entity.setItem(leftover);
            if (leftover.isEmpty()) {
                entity.discard();
            }
        }
    }

    private void restackToSlotSize() {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.getCount() > slotSize) {
                ItemStack extra = stack.split(stack.getCount() - slotSize);
                inventory.setStackInSlot(slot, stack);
                overflow.add(extra);
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable(
                variant.kind().isQueue()
                        ? "container.cruciblecraft.queue_hopper"
                        : "container.cruciblecraft.hopper");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int id, Inventory playerInventory, Player player) {
        return new HopperMenu(ModMenus.HOPPER.get(), id, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("cc_hopper_schema", SCHEMA_VERSION);
        tag.putString("variant", variant.id().toString());
        tag.putInt("mode", mode);
        tag.putBoolean("exact", exactMode);
        tag.putInt("slot_size", slotSize);
        tag.put("inventory", inventory.serializeNBT(registries));
        if (!overflow.isEmpty()) {
            ListTag extra = new ListTag();
            overflow.forEach(stack -> extra.add(stack.save(registries)));
            tag.put("overflow", extra);
        }
        if (failClosed) {
            tag.putBoolean("fail_closed", true);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int schema = tag.contains("cc_hopper_schema")
                ? tag.getInt("cc_hopper_schema")
                : SCHEMA_VERSION;
        if (schema != SCHEMA_VERSION) {
            failClosed = true;
            loadOverflow(tag, registries);
            return;
        }
        if (tag.contains("inventory", Tag.TAG_COMPOUND)) {
            ItemStackHandler loaded = new ItemStackHandler(variant.slots());
            loaded.deserializeNBT(registries, tag.getCompound("inventory"));
            int limit = Math.min(loaded.getSlots(), inventory.getSlots());
            for (int slot = 0; slot < limit; slot++) {
                inventory.setStackInSlot(slot, loaded.getStackInSlot(slot));
            }
            for (int slot = limit; slot < loaded.getSlots(); slot++) {
                ItemStack extra = loaded.getStackInSlot(slot);
                if (!extra.isEmpty()) {
                    overflow.add(extra.copy());
                }
            }
        }
        mode = Math.max(0, Math.min(MAX_MODE, tag.getInt("mode")));
        exactMode = tag.getBoolean("exact");
        slotSize = tag.contains("slot_size")
                ? Math.max(1, Math.min(MAX_MODE, tag.getInt("slot_size")))
                : MAX_MODE;
        if (tag.contains("overflow", Tag.TAG_LIST)) {
            loadOverflowList(tag.getList("overflow", Tag.TAG_COMPOUND), registries);
        }
        failClosed = tag.getBoolean("fail_closed");
    }

    private void loadOverflow(CompoundTag tag, HolderLookup.Provider registries) {
        if (tag.contains("inventory", Tag.TAG_COMPOUND)) {
            ItemStackHandler loaded = new ItemStackHandler(36);
            loaded.deserializeNBT(registries, tag.getCompound("inventory"));
            for (int slot = 0; slot < loaded.getSlots(); slot++) {
                ItemStack extra = loaded.getStackInSlot(slot);
                if (!extra.isEmpty()) {
                    overflow.add(extra.copy());
                }
            }
        }
        if (tag.contains("overflow", Tag.TAG_LIST)) {
            loadOverflowList(tag.getList("overflow", Tag.TAG_COMPOUND), registries);
        }
    }

    private void loadOverflowList(ListTag list, HolderLookup.Provider registries) {
        for (int index = 0; index < list.size(); index++) {
            ItemStack stack = ItemStack.parseOptional(
                    registries, list.getCompound(index));
            if (!stack.isEmpty()) {
                overflow.add(stack);
            }
        }
    }

    private static HopperVariant variantOf(BlockState state) {
        if (!(state.getBlock() instanceof HopperBlock hopper)) {
            throw new IllegalArgumentException(
                    "Hopper block entity requires a HopperBlock");
        }
        return hopper.variant();
    }
}
