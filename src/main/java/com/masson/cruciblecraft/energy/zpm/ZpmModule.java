package com.masson.cruciblecraft.energy.zpm;

import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.battery.BatteryCharge;

import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code MultiTileEntityZPM} / {@code IL.ZPM}, source id 14999.
 *
 * <p>The module stores QU and refuses charging. A decharger extracts packets
 * of {@link #INPUT}. There is no crafting recipe; GT6 places it in dungeon
 * libraries.
 */
public final class ZpmModule {
    public static final long INPUT = 131_072L;
    public static final long SIZE_MIN = 1L;
    public static final long SIZE_MAX = 262_144L;
    public static final long CAPACITY = 2_000_000_000_000L;
    public static final int DISPLAY_SCALE = 15;
    public static final int SOURCE_ID = 14999;
    public static final int SOURCE_LINE = 1103;
    /** Bat-box pull when the buffer index is 0, once per second. */
    public static final int PULL_PACKETS_LOW = 40;
    /** Bat-box pull when the buffer index is 1. */
    public static final int PULL_PACKETS_HIGH = 20;

    private ZpmModule() {}

    public static boolean is(ItemStack stack) {
        return stack != null
                && !stack.isEmpty()
                && stack.getItem() instanceof ZpmModuleItem;
    }

    public static long charge(ItemStack stack) {
        return BatteryCharge.get(stack);
    }

    public static int displayedEnergy(long stored) {
        if (stored <= 0L || CAPACITY <= 0L) {
            return 0;
        }
        long scaled = stored * DISPLAY_SCALE / CAPACITY;
        if (scaled > DISPLAY_SCALE) {
            scaled = DISPLAY_SCALE;
        }
        return (int) scaled;
    }

    /**
     * GT6 {@code doEnergyExtraction}. {@code doEnergyInjection} on this class
     * always returns 0, so there is no insert.
     */
    public static long extractPackets(ItemStack stack, long size, long amount) {
        if (!is(stack) || amount < 1L || stack.getCount() != 1) {
            return 0L;
        }
        long magnitude = EnergyPackets.magnitude(size);
        if (magnitude < SIZE_MIN || magnitude > SIZE_MAX) {
            return 0L;
        }
        long energy = charge(stack);
        if (energy < magnitude) {
            return 0L;
        }
        long packets = Math.min(INPUT, amount);
        while (packets > 1L && energy < EnergyPackets.units(magnitude, packets)) {
            packets--;
        }
        long removed = EnergyPackets.units(magnitude, packets);
        if (removed <= 0L || energy < removed) {
            return 0L;
        }
        BatteryCharge.set(stack, energy - removed, CAPACITY);
        return packets;
    }
}
