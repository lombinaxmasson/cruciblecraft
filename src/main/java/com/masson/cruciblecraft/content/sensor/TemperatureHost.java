package com.masson.cruciblecraft.content.sensor;

import net.minecraft.core.Direction;

/**
 * Side-aware temperature contract used by the standalone thermometer.
 *
 * <p>GT6 exposes the same information through
 * {@code ITileEntityTemperature}; NeoForge does not provide a generic
 * temperature capability.
 */
public interface TemperatureHost {
    float temperatureCelsius(Direction side);

    float temperatureMaxCelsius(Direction side);
}
