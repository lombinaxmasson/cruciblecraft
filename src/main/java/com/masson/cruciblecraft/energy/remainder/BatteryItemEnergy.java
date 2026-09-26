package com.masson.cruciblecraft.energy.remainder;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.battery.BatteryBlockItem;
import com.masson.cruciblecraft.energy.battery.BatteryCharge;
import com.masson.cruciblecraft.energy.battery.EnergyBatteryProfile;

import net.minecraft.world.item.ItemStack;

/** GT6 {@code IItemEnergy} on a placed-battery item stack. */
public final class BatteryItemEnergy {
    private BatteryItemEnergy() {}

    public static boolean matches(ItemStack stack, EnergyType type) {
        EnergyBatteryProfile profile = profile(stack);
        return profile != null && profile.energyType() == type;
    }

    public static boolean canInject(ItemStack stack, EnergyType type, long size) {
        EnergyBatteryProfile profile = profile(stack);
        if (!fits(profile, type, size)) {
            return false;
        }
        return BatteryCharge.get(stack) + EnergyPackets.magnitude(size)
                <= profile.capacity();
    }

    public static boolean canExtract(ItemStack stack, EnergyType type, long size) {
        EnergyBatteryProfile profile = profile(stack);
        if (!fits(profile, type, size)) {
            return false;
        }
        return BatteryCharge.get(stack) >= EnergyPackets.magnitude(size);
    }

    public static long inject(
            ItemStack stack, EnergyType type, long size, long packets) {
        EnergyBatteryProfile profile = profile(stack);
        if (packets <= 0L || !fits(profile, type, size)) {
            return 0L;
        }
        long magnitude = EnergyPackets.magnitude(size);
        long stored = BatteryCharge.get(stack);
        long room = profile.capacity() - stored;
        long moved = Math.min(packets, room / magnitude);
        if (moved <= 0L) {
            return 0L;
        }
        BatteryCharge.set(stack, stored + moved * magnitude, profile.capacity());
        return moved;
    }

    public static long extract(
            ItemStack stack, EnergyType type, long size, long packets) {
        EnergyBatteryProfile profile = profile(stack);
        if (packets <= 0L || !fits(profile, type, size)) {
            return 0L;
        }
        long magnitude = EnergyPackets.magnitude(size);
        long stored = BatteryCharge.get(stack);
        long moved = Math.min(packets, stored / magnitude);
        if (moved <= 0L) {
            return 0L;
        }
        BatteryCharge.set(stack, stored - moved * magnitude, profile.capacity());
        return moved;
    }

    private static boolean fits(
            EnergyBatteryProfile profile, EnergyType type, long size) {
        if (profile == null || profile.energyType() != type || size == 0L) {
            return false;
        }
        long magnitude = EnergyPackets.magnitude(size);
        return magnitude >= profile.sizeMin() && magnitude <= profile.sizeMax();
    }

    private static EnergyBatteryProfile profile(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.getItem() instanceof BatteryBlockItem battery) {
            return battery.profile();
        }
        return null;
    }
}
