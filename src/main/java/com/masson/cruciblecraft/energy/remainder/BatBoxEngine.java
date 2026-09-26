package com.masson.cruciblecraft.energy.remainder;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.energy.EnergyPackets;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * GT6 {@code TileEntityBase10EnergyBatBox} buffer, item exchange, and
 * network intake. Packet sizes above {@code input * 2} overcharge.
 */
public final class BatBoxEngine {
    public record Intake(long consumed, boolean overcharge) {
        static final Intake NONE = new Intake(0L, false);
    }

    private final RemainderDevice device;
    private final ItemStackHandler items;
    private long buffer;
    private int mode;
    private int batteryCount = -1;
    private int chargeableCount = -1;
    private long receivable;
    private boolean inventoryDirty = true;

    public BatBoxEngine(RemainderDevice device, ItemStackHandler items) {
        this.device = device;
        this.items = items;
    }

    public RemainderDevice device() {
        return device;
    }

    public long buffer() {
        return buffer;
    }

    public long output() {
        return device.output();
    }

    public long input() {
        return device.input();
    }

    public int mode() {
        return mode;
    }

    public long networkCapacity() {
        return device.input() * 320L * device.slots();
    }

    public void markDirty() {
        inventoryDirty = true;
        batteryCount = -1;
        chargeableCount = -1;
    }

    public void restore(long stored, int storedMode) {
        buffer = Math.max(0L, stored);
        mode = Math.max(0, Math.min(15, storedMode));
        recount();
    }

    public int beginTick(long gameTime, boolean stopped) {
        if (gameTime % 20L == 1L) {
            exchange();
        } else if (inventoryDirty || batteryCount < 0) {
            recount();
        }
        int emit = 0;
        if (buffer >= device.output() && !stopped) {
            emit = BatBoxMath.emitCount(mode, batteryCount);
        }
        receivable = (long) chargeableCount * device.input() * 2L;
        return emit;
    }

    public void exchange() {
        int packets = BatBoxMath.packets(
                BatBoxMath.band(buffer, device.input(), device.slots()));
        EnergyType energy = device.energy();
        if (packets > 0) {
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                long taken = BatteryItemEnergy.extract(
                        stack, energy, device.output(), packets);
                buffer += device.output() * taken;
            }
        } else if (packets < 0) {
            long inject = -packets;
            for (int slot = 0; slot < items.getSlots(); slot++) {
                ItemStack stack = items.getStackInSlot(slot);
                long added = BatteryItemEnergy.inject(
                        stack, energy, device.input(), inject);
                buffer -= device.input() * added;
            }
        }
        recount();
    }

    public void consumeEmitted(long packets) {
        if (packets > 0L) {
            buffer -= device.output() * packets;
        }
    }

    public Intake insert(long size, long amount, boolean simulate) {
        if (amount <= 0L || receivable <= 0L) {
            return Intake.NONE;
        }
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude <= 0L) {
            return Intake.NONE;
        }
        if (magnitude > device.input() * 2L) {
            return new Intake(amount, !simulate);
        }
        long cap = networkCapacity();
        if (buffer >= cap) {
            return Intake.NONE;
        }
        long room = cap - buffer;
        long offered = Math.min(room, magnitude * amount);
        long consumed = Math.min(
                amount, offered / magnitude + (offered % magnitude != 0L ? 1L : 0L));
        while (consumed > 1L && (consumed - 1L) * magnitude > receivable) {
            consumed--;
        }
        if (consumed <= 0L) {
            return Intake.NONE;
        }
        if (!simulate) {
            receivable -= consumed * magnitude;
            buffer += consumed * magnitude;
        }
        return new Intake(consumed, false);
    }

    private void recount() {
        batteryCount = 0;
        chargeableCount = 0;
        EnergyType energy = device.energy();
        for (int slot = 0; slot < items.getSlots(); slot++) {
            ItemStack stack = items.getStackInSlot(slot);
            if (BatteryItemEnergy.canInject(stack, energy, device.input())) {
                chargeableCount++;
            }
            if (BatteryItemEnergy.canExtract(stack, energy, device.output())) {
                batteryCount++;
            }
        }
        inventoryDirty = false;
    }
}
