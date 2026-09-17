package com.masson.cruciblecraft.content.blockentity;

import java.util.ArrayList;
import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.logistics.hopper.DustAmountLedger;
import com.masson.cruciblecraft.logistics.hopper.DustAmountLedger.Form;
import com.masson.cruciblecraft.logistics.hopper.HopperTransferCore;
import com.masson.cruciblecraft.registry.ModBlockEntities;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.wrapper.RangedWrapper;

/** Steel Dust Funnel: 36-unit dust/small/tiny ledger with top in / bottom out. */
public final class DustFunnelBlockEntity extends BlockEntity {
    public static final int SCHEMA_VERSION = 1;
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private final DustAmountLedger ledger = new DustAmountLedger();
    private final ItemStackHandler inventory = new ItemStackHandler(2) {
        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return slot == INPUT_SLOT && DustFunnelBlockEntity.isDustForm(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };
    private boolean failClosed;

    public DustFunnelBlockEntity(BlockPos pos, BlockState state) {
        this(ModBlockEntities.DUST_FUNNEL.get(), pos, state);
    }

    public DustFunnelBlockEntity(
            BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    public DustAmountLedger ledger() {
        return ledger;
    }

    public ItemStackHandler inventory() {
        return inventory;
    }

    public boolean failClosed() {
        return failClosed;
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
        if (side == Direction.UP) {
            return new RangedWrapper(inventory, INPUT_SLOT, INPUT_SLOT + 1);
        }
        if (side == Direction.DOWN) {
            return new RangedWrapper(inventory, OUTPUT_SLOT, OUTPUT_SLOT + 1) {
                @Override
                public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
                    return stack;
                }
            };
        }
        return null;
    }

    public boolean absorb(String material, Form form, int count) {
        return ledger.accept(material, form, count);
    }

    public boolean insertFromHand(ItemStack stack) {
        if (!isDustForm(stack)) {
            return false;
        }
        ItemStack leftover = inventory.insertItem(INPUT_SLOT, stack.copy(), false);
        int taken = stack.getCount() - leftover.getCount();
        if (taken <= 0) {
            return false;
        }
        stack.shrink(taken);
        convertInput();
        setChanged();
        return true;
    }

    public void cycleMode(boolean reverse) {
        ledger.cycleOutputMode(reverse);
        setChanged();
    }

    public void dropLedger() {
        convertInput();
        DustAmountLedger.Decomposition split = ledger.decompose();
        if (level != null) {
            ArrayList<ItemStack> drops = new ArrayList<>();
            addForm(drops, split.materialId(), MaterialPrefixes.DUST, split.dust());
            addForm(
                    drops,
                    split.materialId(),
                    MaterialPrefixes.SMALL_DUST,
                    split.smallDust());
            addForm(
                    drops,
                    split.materialId(),
                    MaterialPrefixes.TINY_DUST,
                    split.tinyDust());
            for (ItemStack drop : drops) {
                Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        drop);
            }
        }
        ledger.restore(new DustAmountLedger.Snapshot(
                DustAmountLedger.SCHEMA_VERSION, "", 0, ledger.outputMode()));
        setChanged();
    }

    public Component statusMessage() {
        return Component.translatable(
                "message.cruciblecraft.dust_funnel.mode",
                ledger.outputMode().name().toLowerCase());
    }

    public void readFromItem(ItemStack stack) {
        var snapshot = stack.get(ModComponents.DUST_FUNNEL.get());
        if (snapshot != null) {
            ledger.restore(snapshot);
        }
    }

    public void writeToItem(ItemStack stack) {
        if (!ledger.isEmpty() && !ledger.decompose().lossless()) {
            stack.set(ModComponents.DUST_FUNNEL.get(), ledger.snapshot());
        }
    }

    public void exportToDrops(List<ItemStack> drops, ItemStack blockItem) {
        convertInput();
        DustAmountLedger.Decomposition split = ledger.decompose();
        addForm(drops, split.materialId(), MaterialPrefixes.DUST, split.dust());
        addForm(drops, split.materialId(), MaterialPrefixes.SMALL_DUST, split.smallDust());
        addForm(drops, split.materialId(), MaterialPrefixes.TINY_DUST, split.tinyDust());
        for (int slot = 0; slot < inventory.getSlots(); slot++) {
            ItemStack stack = inventory.getStackInSlot(slot);
            if (!stack.isEmpty()) {
                drops.add(stack.copy());
                inventory.setStackInSlot(slot, ItemStack.EMPTY);
            }
        }
        if (split.leftoverUnits() > 0) {
            blockItem.set(
                    ModComponents.DUST_FUNNEL.get(),
                    new DustAmountLedger.Snapshot(
                            DustAmountLedger.SCHEMA_VERSION,
                            split.materialId(),
                            split.leftoverUnits(),
                            ledger.outputMode()));
        }
        ledger.restore(new DustAmountLedger.Snapshot(
                DustAmountLedger.SCHEMA_VERSION, "", 0, ledger.outputMode()));
        setChanged();
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            DustFunnelBlockEntity funnel) {
        if (funnel.failClosed) {
            return;
        }
        IItemHandler above = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.above(), Direction.DOWN);
        if (above != null) {
            HopperTransferCore.pull(above, funnel.inventory, false);
        }
        funnel.convertInput();
        funnel.emitOutput();
        IItemHandler below = level.getCapability(
                Capabilities.ItemHandler.BLOCK, pos.below(), Direction.UP);
        if (below != null && !funnel.inventory.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            ItemStackHandler output = new ItemStackHandler(1);
            output.setStackInSlot(0, funnel.inventory.getStackInSlot(OUTPUT_SLOT));
            int moved = HopperTransferCore.push(output, below, 0, false, false);
            if (moved > 0) {
                funnel.inventory.setStackInSlot(OUTPUT_SLOT, output.getStackInSlot(0));
            }
        }
        funnel.setChanged();
    }

    private void convertInput() {
        ItemStack input = inventory.getStackInSlot(INPUT_SLOT);
        if (input.isEmpty()) {
            return;
        }
        var resolved = MaterialUnits.resolve(input);
        if (resolved.isEmpty()) {
            return;
        }
        Form form = formOf(resolved.get().form());
        if (form == null) {
            return;
        }
        if (ledger.accept(resolved.get().materialId(), form, input.getCount())) {
            inventory.setStackInSlot(INPUT_SLOT, ItemStack.EMPTY);
        }
    }

    private void emitOutput() {
        ItemStack output = inventory.getStackInSlot(OUTPUT_SLOT);
        int space = output.isEmpty()
                ? 64
                : Math.max(0, output.getMaxStackSize() - output.getCount());
        int items = ledger.outputItemCount();
        if (items <= 0 || space < items) {
            return;
        }
        if (!output.isEmpty() && !matchesOutput(output)) {
            return;
        }
        if (!ledger.tryEmit(space)) {
            return;
        }
        ItemStack emitted = outputStack(items);
        if (output.isEmpty()) {
            inventory.setStackInSlot(OUTPUT_SLOT, emitted);
        } else {
            output.grow(items);
            inventory.setStackInSlot(OUTPUT_SLOT, output);
        }
    }

    private boolean matchesOutput(ItemStack stack) {
        var resolved = MaterialUnits.resolve(stack);
        return resolved.isPresent()
                && resolved.get().materialId().equals(ledger.materialId())
                && formOf(resolved.get().form()) == ledger.outputMode();
    }

    private ItemStack outputStack(int count) {
        MaterialPrefix prefix = switch (ledger.outputMode()) {
            case DUST -> MaterialPrefixes.DUST;
            case SMALL_DUST -> MaterialPrefixes.SMALL_DUST;
            case TINY_DUST -> MaterialPrefixes.TINY_DUST;
        };
        return new ItemStack(
                ModItems.materialItem(ledger.materialId(), prefix).get(),
                count);
    }

    private static void addForm(
            List<ItemStack> drops, String materialId, MaterialPrefix prefix, int count) {
        if (count <= 0 || materialId == null || materialId.isEmpty()) {
            return;
        }
        if (!ModItems.hasMaterialItem(materialId, prefix)) {
            return;
        }
        drops.add(new ItemStack(ModItems.materialItem(materialId, prefix).get(), count));
    }

    public static boolean isDustForm(ItemStack stack) {
        return MaterialUnits.resolve(stack)
                .map(entry -> formOf(entry.form()) != null)
                .orElse(false);
    }

    private static Form formOf(MaterialPrefix prefix) {
        if (prefix.equals(MaterialPrefixes.DUST)) {
            return Form.DUST;
        }
        if (prefix.equals(MaterialPrefixes.SMALL_DUST)) {
            return Form.SMALL_DUST;
        }
        if (prefix.equals(MaterialPrefixes.TINY_DUST)) {
            return Form.TINY_DUST;
        }
        return null;
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("cc_dust_funnel_schema", SCHEMA_VERSION);
        tag.put("inventory", inventory.serializeNBT(registries));
        DustAmountLedger.Snapshot snapshot = ledger.snapshot();
        tag.putString("material", snapshot.materialId());
        tag.putInt("units", snapshot.units());
        tag.putString("mode", snapshot.outputMode().name());
        tag.putBoolean("fail_closed", failClosed);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        int schema = tag.contains("cc_dust_funnel_schema")
                ? tag.getInt("cc_dust_funnel_schema")
                : SCHEMA_VERSION;
        if (tag.contains("inventory", Tag.TAG_COMPOUND)) {
            inventory.deserializeNBT(registries, tag.getCompound("inventory"));
        }
        try {
            DustAmountLedger loaded = DustAmountLedger.load(
                    new DustAmountLedger.Snapshot(
                            DustAmountLedger.SCHEMA_VERSION,
                            tag.getString("material"),
                            tag.getInt("units"),
                            Form.valueOf(
                                    tag.contains("mode")
                                            ? tag.getString("mode")
                                            : Form.DUST.name())));
            ledger.restore(loaded.snapshot());
        } catch (IllegalArgumentException ignored) {
            failClosed = true;
        }
        failClosed = failClosed
                || schema != SCHEMA_VERSION
                || tag.getBoolean("fail_closed");
    }
}
