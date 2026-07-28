package com.masson.cruciblecraft.heat;

/**
 * Pure thermal helpers with no Minecraft dependencies.
 * Temperature is plain Celsius; each tick just walks toward a target at a fixed rate.
 */
public final class ThermalSim {
    private ThermalSim() {}

    /** 一个 tick 的更新：朝 target 走 ratePerTick，到了就停。GT6/TFC 都是这个。 */
    public static float step(float temp, float target, float ratePerTick) {
        if (ratePerTick <= 0.0f) return temp;
        if (temp < target) return Math.min(temp + ratePerTick, target);
        if (temp > target) return Math.max(temp - ratePerTick, target);
        return target;
    }

    /** 补偿离线的 tick,直接乘,不需要 exp,比之前还便宜。 */
    public static float stepTicks(float temp, float target, float ratePerTick, long ticks) {
        if (ticks <= 0 || ratePerTick <= 0.0f) return temp;
        float delta = ratePerTick * ticks;
        return temp < target ? Math.min(temp + delta, target)
             : temp > target ? Math.max(temp - delta, target)
             : target;
    }
}
