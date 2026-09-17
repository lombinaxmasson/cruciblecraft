package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.MassStorageBlock;
import com.masson.cruciblecraft.content.block.StorageHostBlock;
import com.masson.cruciblecraft.content.storage.ILogisticsStorage;
import com.masson.cruciblecraft.content.storage.MassStorageClicks;
import com.masson.cruciblecraft.content.storage.MassStorageHandler;
import com.masson.cruciblecraft.content.storage.MassStorageSidedHandler;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

public final class MassStorageBlockEntity extends BlockEntity
        implements ILogisticsStorage {
    private static final int AUTO_OUTPUT = 1;
    private static final int RESET_FILTER = 2;
    private static final int EMIT_OVERFLOW = 4;

    private final StorageVariant variant;
    private final MassStorageHandler inventory;
    private int mode;
    private boolean inventoryChanged;

    public MassStorageBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.MASS_STORAGE.get(), pos, state, variantOf(state));
    }

    public MassStorageBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            StorageVariant variant) {
        super(type, pos, state);
        this.variant = variant;
        this.inventory = new MassStorageHandler(variant.capacity(), () -> {
            inventoryChanged = true;
            setChanged();
        });
    }

    public StorageVariant variant() {
        return variant;
    }

    public MassStorageHandler inventory() {
        return inventory;
    }

    public IItemHandler itemHandler(Direction side) {
        return new MassStorageSidedHandler(
                inventory, autoOutput() && side == Direction.DOWN);
    }

    public ILogisticsStorage logisticsStorage() {
        return variant.logistics() ? this : null;
    }

    @Override
    public int getLogisticsPriorityItem() {
        if (!variant.logistics()) {
            return 0;
        }
        return inventory.filter().isEmpty() ? 1 : 2;
    }

    @Override
    public ItemStack getLogisticsFilterItem() {
        return variant.logistics() ? inventory.filter() : ItemStack.EMPTY;
    }

    public boolean autoOutput() {
        return (mode & AUTO_OUTPUT) != 0;
    }

    public boolean emitOverflow() {
        return (mode & EMIT_OVERFLOW) != 0;
    }

    public boolean resetFilterWhenEmpty() {
        return (mode & RESET_FILTER) != 0;
    }

    public void toggleAutoOutput() {
        mode ^= AUTO_OUTPUT;
        setChanged();
    }

    public void toggleResetFilterWhenEmpty() {
        mode ^= RESET_FILTER;
        inventory.setKeepFilterWhenEmpty(!resetFilterWhenEmpty());
        setChanged();
    }

    public void toggleOverflow() {
        mode ^= EMIT_OVERFLOW;
        inventory.setOverflowBonus(
                emitOverflow() ? MassStorageHandler.OVERFLOW_BONUS : 0);
        setChanged();
    }

    public Component autoOutputMessage() {
        return Component.translatable(
                autoOutput()
                        ? "message.cruciblecraft.mass_storage.auto_output_on"
                        : "message.cruciblecraft.mass_storage.auto_output_off");
    }

    public Component filterMessage() {
        return Component.translatable(
                resetFilterWhenEmpty()
                        ? "message.cruciblecraft.mass_storage.filter_reset"
                        : "message.cruciblecraft.mass_storage.filter_stay");
    }

    public Component overflowMessage() {
        return Component.translatable(
                emitOverflow()
                        ? "message.cruciblecraft.mass_storage.overflow_on"
                        : "message.cruciblecraft.mass_storage.overflow_off");
    }

    public boolean onActivated(Player player, ItemStack held, BlockHitResult hit) {
        Direction facing = getBlockState().getValue(StorageHostBlock.FACING);
        if (hit.getDirection() != facing) {
            return false;
        }
        float[] uv = MassStorageClicks.facingUv(
                facing, worldPosition, hit.getLocation());
        if (!MassStorageClicks.onFace(uv)) {
            return false;
        }
        inventory.convertPartials();
        int amount = MassStorageClicks.amount(uv);
        if (!inventory.filter().isEmpty()) {
            if (amount > 0) {
                ejectInFront(inventory.extractStored(amount, false));
            } else if (!held.isEmpty()) {
                ItemStack leftover = inventory.insertAll(held.copy(), false);
                held.setCount(leftover.getCount());
            } else if (amount == -1) {
                dumpPlayerInventory(player);
            }
        } else if (!held.isEmpty()) {
            ItemStack leftover = inventory.insertAll(held.copy(), false);
            held.setCount(leftover.getCount());
        }
        inventory.convertPartials();
        setChanged();
        return true;
    }

    public void giveToPlayer(Player player) {
        ItemStack partial = inventory.takePartials();
        if (!partial.isEmpty()) {
            int leftover = moveIntoPlayerMain(player, partial);
            if (leftover > 0) {
                inventory.insertAll(partial.copyWithCount(leftover), false);
            }
        }
        while (inventory.stored() > 0) {
            ItemStack taken = inventory.extractStored(64, false);
            if (taken.isEmpty()) {
                break;
            }
            int leftover = moveIntoPlayerMain(player, taken);
            if (leftover > 0) {
                inventory.insertAll(taken.copyWithCount(leftover), false);
                break;
            }
        }
        setChanged();
    }

    public void dumpInFront() {
        while (inventory.stored() > 0) {
            int chunk = Math.min(
                    inventory.stored(),
                    Math.max(1, inventory.filter().getMaxStackSize()));
            ItemStack taken = inventory.extractStored(chunk, false);
            if (taken.isEmpty()) {
                break;
            }
            ejectInFront(taken);
        }
        ItemStack partial = inventory.takePartials();
        if (!partial.isEmpty()) {
            ejectInFront(partial);
        }
        inventory.clearAll();
        setChanged();
    }

    public void clearContents() {
        inventory.clearAll();
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        while (inventory.stored() > 0) {
            ItemStack extracted = inventory.extractItem(0, 64, false);
            if (extracted.isEmpty()) {
                break;
            }
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(), extracted);
        }
        ItemStack partial = inventory.takePartials();
        if (!partial.isEmpty()) {
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(), partial);
        }
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            MassStorageBlockEntity storage) {
        if (level.isClientSide) {
            return;
        }
        storage.inventory.convertPartials();
        boolean pulse = storage.inventoryChanged || level.getGameTime() % 100L == 0L;
        storage.inventoryChanged = false;
        if (!pulse) {
            return;
        }
        if (storage.autoOutput() && storage.inventory.stored() > 0) {
            storage.pushBelow(level, pos);
        } else if (storage.emitOverflow()
                && storage.inventory.stored() > storage.inventory.capacity()) {
            storage.emitOverflowBelow(level, pos);
        }
    }

    public CompoundTag saveForTest(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        saveAdditional(tag, registries);
        return tag;
    }

    public void loadForTest(CompoundTag tag, HolderLookup.Provider registries) {
        loadAdditional(tag, registries);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!inventory.filter().isEmpty()) {
            tag.put("filter", inventory.filter().save(registries));
        }
        tag.putInt("stored", inventory.stored());
        tag.putLong("partial", inventory.partialUnits());
        tag.putByte("mode", (byte) mode);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        mode = tag.getByte("mode");
        inventory.setKeepFilterWhenEmpty(!resetFilterWhenEmpty());
        inventory.setOverflowBonus(
                emitOverflow() ? MassStorageHandler.OVERFLOW_BONUS : 0);
        ItemStack filter = tag.contains("filter")
                ? ItemStack.parseOptional(registries, tag.getCompound("filter"))
                : ItemStack.EMPTY;
        inventory.load(filter, tag.getInt("stored"), tag.getLong("partial"));
    }

    private void dumpPlayerInventory(Player player) {
        var items = player.getInventory().items;
        for (int slot = 0; slot < 36; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty() || !inventory.sameType(stack)) {
                continue;
            }
            ItemStack leftover = inventory.insertAll(stack.copy(), false);
            stack.setCount(leftover.getCount());
            if (!leftover.isEmpty()) {
                break;
            }
        }
        player.getInventory().setChanged();
    }

    private static int moveIntoPlayerMain(Player player, ItemStack stack) {
        int remaining = stack.getCount();
        var items = player.getInventory().items;
        for (int slot = 9; slot < 36 && remaining > 0; slot++) {
            ItemStack dest = items.get(slot);
            if (dest.isEmpty()) {
                int put = Math.min(remaining, stack.getMaxStackSize());
                items.set(slot, stack.copyWithCount(put));
                remaining -= put;
                continue;
            }
            if (!ItemStack.isSameItemSameComponents(dest, stack)) {
                continue;
            }
            int put = Math.min(
                    remaining, dest.getMaxStackSize() - dest.getCount());
            if (put <= 0) {
                continue;
            }
            dest.grow(put);
            remaining -= put;
        }
        player.getInventory().setChanged();
        return remaining;
    }

    private void pushBelow(Level level, BlockPos pos) {
        IItemHandler below = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        if (below == null) {
            return;
        }
        ItemStack taken = inventory.extractItem(0, 64, false);
        if (!taken.isEmpty()) {
            insertInto(below, taken);
        }
    }

    private void emitOverflowBelow(Level level, BlockPos pos) {
        IItemHandler below = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        if (below == null) {
            return;
        }
        while (inventory.stored() > inventory.capacity()) {
            int extra = Math.min(64, inventory.stored() - inventory.capacity());
            ItemStack taken = inventory.extractStored(extra, false);
            if (taken.isEmpty()) {
                break;
            }
            int leftover = insertInto(below, taken);
            if (leftover >= extra) {
                break;
            }
        }
    }

    private int insertInto(IItemHandler dest, ItemStack stack) {
        if (stack.isEmpty()) {
            return 0;
        }
        ItemStack leftover = stack.copy();
        for (int slot = 0; slot < dest.getSlots() && !leftover.isEmpty(); slot++) {
            leftover = dest.insertItem(slot, leftover, false);
        }
        if (!leftover.isEmpty()) {
            inventory.insertAll(leftover, false);
        }
        return leftover.getCount();
    }

    private void ejectInFront(ItemStack stack) {
        if (stack.isEmpty() || level == null) {
            return;
        }
        Direction facing = getBlockState().getValue(StorageHostBlock.FACING);
        double x = worldPosition.getX() + 0.5 + facing.getStepX() * 0.75;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5 + facing.getStepZ() * 0.75;
        int remaining = stack.getCount();
        int max = Math.max(1, stack.getMaxStackSize());
        while (remaining > 0) {
            int chunk = Math.min(max, remaining);
            Containers.dropItemStack(level, x, y, z, stack.copyWithCount(chunk));
            remaining -= chunk;
        }
    }

    private static StorageVariant variantOf(BlockState state) {
        if (state.getBlock() instanceof MassStorageBlock block) {
            return block.variant();
        }
        throw new IllegalStateException("Mass storage BE missing host variant");
    }
}
