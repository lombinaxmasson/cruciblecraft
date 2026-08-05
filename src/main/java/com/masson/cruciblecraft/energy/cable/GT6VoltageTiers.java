package com.masson.cruciblecraft.energy.cable;

import com.masson.cruciblecraft.energy.EnergyPackets;

/** Pinned CS.V[] and UT.Code.tierMax semantics from GT6. */
public final class GT6VoltageTiers {
    public static final long[] VOLTAGES = {
        8L,
        32L,
        128L,
        512L,
        2_048L,
        8_192L,
        32_768L,
        131_072L,
        524_288L,
        2_097_152L,
        8_388_608L,
        33_554_432L,
        134_217_728L,
        536_870_912L,
        2_147_483_648L,
        8_589_934_592L
    };

    private GT6VoltageTiers() {}

    public static int tierMax(long size) {
        long magnitude = EnergyPackets.magnitude(size);
        for (int tier = 0; tier < VOLTAGES.length; tier++) {
            if (magnitude <= VOLTAGES[tier]) {
                return tier;
            }
        }
        return VOLTAGES.length;
    }

    public static float contactDamage(long wattage) {
        return tierMax(wattage) * 4.0F;
    }
}
