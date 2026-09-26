package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.ZpmDechargerBlock;
import com.masson.cruciblecraft.energy.EnergyEmitter;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.converter.EnergyConverterHost;
import com.masson.cruciblecraft.energy.converter.EnergyConverterProfile;
import com.masson.cruciblecraft.energy.zpm.ZpmModule;
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
 * <p>The single slot accepts only the zero-point module ({@code IL.ZPM}).
 * Once a second the buffer pulls QU out of that module, then the front emits
 * one packet of the profile output ({@code EU} or {@code QU}). The module
 * cannot be charged, so external QU is rejected. Packet sizes stay at GT6
 * {@code V[7]}.
 */
public final class ZpmDechargerBlockEntity extends BlockEntity
        implements IEnergyHandler {
    private final EnergyConverterProfile profile;
    private final long packet;
    private final EnergyType outputType;
    private final Slot slot = new Slot(this::setChanged);
    private long stored;
    private int tickPhase;
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
        outputType = energyType(profile.outputPacket().identity());
        if (packet != profile.outputPacket().size() || packet <= 0L) {
            throw new IllegalStateException(
                    "ZPM decharger packet sizes drifted");
        }
    }

    private static EnergyType energyType(String identity) {
        return switch (identity) {
            case "EU" -> EnergyType.ELECTRIC;
            case "QU" -> EnergyType.QUANTUM;
            default -> throw new IllegalStateException(
                    "ZPM decharger emits " + identity);
        };
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            ZpmDechargerBlockEntity decharger) {
        decharger.pullFromModule();
        decharger.emit(level, pos, state);
    }

    /**
     * GT6 bat-box item callback, once per second. Index 0 pulls 40 packets
     * and index 1 pulls 20. The module refuses injection, so indexes 6 and 7
     * add nothing.
     */
    private void pullFromModule() {
        tickPhase++;
        if (tickPhase % 20 != 1 || !ZpmModule.is(slot.stack)) {
            return;
        }
        long band = stored / (packet * 40L);
        int index = (int) Math.max(0L, Math.min(7L, band));
        int packets = switch (index) {
            case 0 -> ZpmModule.PULL_PACKETS_LOW;
            case 1 -> ZpmModule.PULL_PACKETS_HIGH;
            default -> 0;
        };
        if (packets == 0) {
            return;
        }
        long taken = ZpmModule.extractPackets(slot.stack, packet, packets);
        if (taken > 0L) {
            stored += packet * taken;
            setChanged();
        }
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
                level, pos, outputType, packet, 1L, front);
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
        if (side == front) {
            return type == outputType;
        }
        return type == EnergyType.QUANTUM;
    }

    @Override
    public long insert(
            EnergyType type,
            long size,
            long amount,
            Direction side,
            boolean simulate) {
        // The only legal item refuses charging, so receivable power stays 0.
        return 0L;
    }

    @Override
    public long outputSize(EnergyType type, Direction side) {
        Direction front = front();
        return type == outputType
                        && front != null
                        && side == front
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
        tag.putInt("tick_phase", tickPhase);
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
        tickPhase = Math.max(0, tag.getInt("tick_phase"));
        emitsEnergy = tag.getBoolean("emits_energy");
        status = tag.getString("status");
        slot.stack = tag.contains("Item")
                ? ItemStack.parseOptional(registries, tag.getCompound("Item"))
                : ItemStack.EMPTY;
        if (status.isBlank()) {
            status = slot.stack.isEmpty() ? "no_zpm" : "charging";
        }
    }

    /** GT6 {@code isItemValidForSlot} is {@code IL.ZPM.equal}. */
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
            if (slot != 0
                    || stack.isEmpty()
                    || !this.stack.isEmpty()
                    || !ZpmModule.is(stack)) {
                return stack;
            }
            if (simulate) {
                return stack.getCount() == 1
                        ? ItemStack.EMPTY
                        : stack.copyWithCount(stack.getCount() - 1);
            }
            this.stack = stack.split(1);
            changed.run();
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
            return slot == 0 && ZpmModule.is(stack);
        }
    }
}
