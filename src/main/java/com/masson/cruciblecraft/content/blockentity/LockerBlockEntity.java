package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.LockerBlock;
import com.masson.cruciblecraft.content.storage.StorageVariant;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

public final class LockerBlockEntity extends BlockEntity implements IEnergyHandler {
    private static final EquipmentSlot[] ARMOR = {
            EquipmentSlot.FEET,
            EquipmentSlot.LEGS,
            EquipmentSlot.CHEST,
            EquipmentSlot.HEAD
    };
    public static final long CHARGE_CAPACITY = 10_000L;
    private final StorageVariant variant;
    private final ItemStackHandler inventory;

    public LockerBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.LOCKER.get(), pos, state, variantOf(state));
    }

    public LockerBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            StorageVariant variant) {
        super(type, pos, state);
        this.variant = variant;
        this.inventory = new ItemStackHandler(variant.slots()) {
            @Override
            protected void onContentsChanged(int slot) {
                setChanged();
            }
        };
    }

    public StorageVariant variant() {
        return variant;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public IItemHandler itemHandler(Direction side) {
        return inventory;
    }

    public void swapArmor(Player player) {
        for (int slot = 0; slot < ARMOR.length; slot++) {
            ItemStack stored = inventory.getStackInSlot(slot);
            ItemStack worn = player.getItemBySlot(ARMOR[slot]);
            inventory.setStackInSlot(slot, worn.copy());
            player.setItemSlot(ARMOR[slot], stored.copy());
        }
        setChanged();
    }

    public void dropContents(Level level, BlockPos pos) {
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            Containers.dropItemStack(
                    level, pos.getX(), pos.getY(), pos.getZ(),
                    inventory.getStackInSlot(slot));
            inventory.setStackInSlot(slot, ItemStack.EMPTY);
        }
    }

    public static void tick(
            Level level,
            BlockPos pos,
            BlockState state,
            LockerBlockEntity locker) {
        // Charging is pull-based through IEnergyHandler.insert.
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
    public boolean handles(EnergyType type, Direction side) {
        return variant.charging() && type == EnergyType.ELECTRIC;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        if (!handles(type, side) || amount <= 0L || size == 0L) {
            return 0L;
        }
        long remaining = Math.abs(size) * amount;
        long acceptedPackets = 0L;
        for (int slot = 0; slot < inventory.getSlots() && remaining > 0L; slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.isEmpty() || !stack.has(ModComponents.ELECTRIC_CHARGE.get())) {
                continue;
            }
            long stored = stack.getOrDefault(ModComponents.ELECTRIC_CHARGE.get(), 0L);
            long room = Math.max(0L, CHARGE_CAPACITY - stored);
            long take = Math.min(remaining, room);
            if (take <= 0L) {
                continue;
            }
            long packets = take / Math.abs(size);
            if (packets <= 0L) {
                continue;
            }
            take = packets * Math.abs(size);
            if (!simulate) {
                stack.set(ModComponents.ELECTRIC_CHARGE.get(), stored + take);
                inventory.setStackInSlot(slot, stack);
            }
            remaining -= take;
            acceptedPackets += packets;
        }
        return acceptedPackets;
    }

    @Override
    public long stored(EnergyType type) {
        if (type != EnergyType.ELECTRIC) {
            return 0L;
        }
        long total = 0L;
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (stack.has(ModComponents.ELECTRIC_CHARGE.get())) {
                total += stack.getOrDefault(ModComponents.ELECTRIC_CHARGE.get(), 0L);
            }
        }
        return total;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.ELECTRIC
                ? CHARGE_CAPACITY * inventory.getSlots()
                : 0L;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("inventory", inventory.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        if (tag.contains("inventory")) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
    }

    private static StorageVariant variantOf(BlockState state) {
        if (state.getBlock() instanceof LockerBlock block) {
            return block.variant();
        }
        throw new IllegalStateException("Locker BE missing host variant");
    }
}
