package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.ZpmDechargerBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.registry.ModBlockEntities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 {@code MultiTileEntityZPMDechargerEU} / {@code TileEntityBase10EnergyBatBox}.
 *
 * <p>The single slot accepts only {@code IL.ZPM}. That item is unmapped, so
 * the slot rejects every stack and the machine neither accepts QU nor emits
 * EU. Packet sizes stay at GT6 {@code V[7]}.
 */
public final class ZpmDechargerBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final EnergyConverterProfile profile;
    private final long packet;
    private final Slot slot = new Slot(this::setChanged);
    private long stored;
    private boolean emitsEnergy;
    private String status = "no_zpm";

    public ZpmDechargerBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ZPM_DECHARGER.get(), pos, state);
        if (!(state.getBlock() instanceof EnergyConverterHost host)) {
            throw new IllegalArgumentException(
                    "ZPM decharger requires a catalog block");
        }
        profile = host.converterProfile();
        packet = profile.inputPacket().size();
        if (packet != profile.outputPacket().size() || packet <= 0L) {
            throw new IllegalStateException(
                    "ZPM decharger packet sizes drifted");
        }
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ZpmDechargerBlockEntity decharger) {
        decharger.emit(level, pos, state);
    }

    private void emit(Level level, BlockPos pos, BlockState state) {
        emitsEnergy = false;
        if (slot.stack.isEmpty() || stored < packet) {
            status = slot.stack.isEmpty() ? "no_zpm" : "charging";
            setLit(level, pos, state, false);
            return;
        }
        Direction front = front();
        long delivered = EnergyEmitter.pushToSide(
                level, pos, EnergyType.ELECTRIC, packet, 1L, front);
        if (delivered > 0L) {
            stored -= packet * delivered;
            emitsEnergy = true;
            status = "running";
        } else {
            status = "blocked";
        }
        setLit(level, pos, state, emitsEnergy);
        setChanged();
    }

    public boolean use(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (slot.stack.isEmpty()) {
                return false;
            }
            player.setItemInHand(hand, slot.stack);
            slot.stack = ItemStack.EMPTY;
            setChanged();
            return true;
        }
        if (!slot.stack.isEmpty() || !slot.isItemValid(0, held)) {
            return false;
        }
        slot.stack = held.split(1);
        setChanged();
        return true;
    }

    public IItemHandler items() {
        return slot;
    }

    @Override
    public boolean handles(EnergyType type, Direction side) {
        Direction front = front();
        if (front == null || side == null) {
            return false;
        }
        if (type == EnergyType.QUANTUM && side != front) {
            return true;
        }
        return type == EnergyType.ELECTRIC && side == front;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        // GT6 doInject returns 0 unless a chargeable item set mReceivablePower.
        if (type != EnergyType.QUANTUM
                || !handles(type, side)
                || amount <= 0L
                || slot.stack.isEmpty()) {
            return 0L;
        }
        if (EnergyPackets.magnitude(size) > packet) {
            return amount;
        }
        long room = profile.inputCapacity() - stored;
        long accepted = Math.min(
                amount, EnergyPackets.packetsForUnits(size, room));
        if (!simulate && accepted > 0L) {
            stored += EnergyPackets.units(size, accepted);
            setChanged();
        }
        return accepted;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        return type == EnergyType.ELECTRIC
                        && handles(type, side)
                        && !slot.stack.isEmpty()
                        && stored >= packet
                ? packet
                : 0L;
    }

    @Override
    public long extract(
            EnergyType type,
            long size,
            long maximum,
            Direction side,
            boolean simulate) {
        if (outputSize(type, side) == 0L
                || EnergyPackets.magnitude(size) != packet
                || maximum <= 0L) {
            return 0L;
        }
        long packets = Math.min(maximum, stored / packet);
        if (!simulate && packets > 0L) {
            stored -= packet * packets;
            emitsEnergy = true;
            setChanged();
        }
        return packets;
    }

    @Override
    public long stored(EnergyType type) {
        return type == EnergyType.QUANTUM ? stored : 0L;
    }

    @Override
    public long capacity(EnergyType type) {
        return type == EnergyType.QUANTUM ? profile.inputCapacity() : 0L;
    }

    public long stored() {
        return stored;
    }

    public boolean emitsEnergy() {
        return emitsEnergy;
    }

    public String status() {
        return status;
    }

    public EnergyConverterProfile profile() {
        return profile;
    }

    private void setLit(
            Level level,
            BlockPos pos,
            BlockState state,
            boolean lit) {
        if (state.hasProperty(ZpmDechargerBlock.LIT)
                && state.getValue(ZpmDechargerBlock.LIT) != lit) {
            level.setBlock(
                    pos,
                    state.setValue(ZpmDechargerBlock.LIT, lit),
                    3);
        }
    }

    private Direction front() {
        BlockState state = getBlockState();
        return state.hasProperty(ZpmDechargerBlock.FACING)
                ? state.getValue(ZpmDechargerBlock.FACING)
                : null;
    }

    @Override
    protected void saveAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putLong("stored", stored);
        tag.putBoolean("emits_energy", emitsEnergy);
        tag.putString("status", status);
        if (!slot.stack.isEmpty()) {
            tag.put("Item", slot.stack.save(registries));
        }
    }

    @Override
    protected void loadAdditional(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = Math.max(0L, tag.getLong("stored"));
        emitsEnergy = tag.getBoolean("emits_energy");
        status = tag.getString("status");
        slot.stack = tag.contains("Item")
                ? ItemStack.parseOptional(registries, tag.getCompound("Item"))
                : ItemStack.EMPTY;
        if (status.isBlank()) {
            status = slot.stack.isEmpty() ? "no_zpm" : "charging";
        }
    }

    /**
     * GT6 {@code isItemValidForSlot} is {@code IL.ZPM.equal}. No CC item
     * maps to that container, so every insert is rejected.
     */
    private static final class Slot implements IItemHandler {
        private final Runnable changed;
        private ItemStack stack = ItemStack.EMPTY;

        private Slot(Runnable changed) {
            this.changed = changed;
        }

        @Override
        public int getSlots() {
            return 1;
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            return slot == 0 ? stack : ItemStack.EMPTY;
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            return stack;
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            if (slot != 0 || amount <= 0 || stack.isEmpty()) {
                return ItemStack.EMPTY;
            }
            ItemStack extracted = stack.copy();
            if (!simulate) {
                stack = ItemStack.EMPTY;
                changed.run();
            }
            return extracted;
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            return false;
        }
    }
}
