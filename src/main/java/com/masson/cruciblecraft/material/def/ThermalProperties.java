package com.masson.cruciblecraft.material.def;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * Runtime thermal metadata. Melting and boiling points are always degrees
 * Celsius; Kelvin conversion is restricted to external boundaries.
 */
public record ThermalProperties(double meltingPoint, double boilingPoint, double density) {
    public ThermalProperties {
        if (!Double.isFinite(meltingPoint)) {
            throw new IllegalArgumentException("Melting point must be finite");
        }
        if (!Double.isFinite(boilingPoint) || boilingPoint <= meltingPoint) {
            throw new IllegalArgumentException("Boiling point must be finite and exceed melting point");
        }
        if (!Double.isFinite(density) || density <= 0.0) {
            throw new IllegalArgumentException("Density must be finite and positive");
        }
    }

    public ThermalProperties(double meltingPoint) {
        this(meltingPoint, meltingPoint * 2.0, 1.0);
    }

    public static final Codec<ThermalProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("melting_point").forGetter(ThermalProperties::meltingPoint),
            Codec.DOUBLE.optionalFieldOf("boiling_point", -1.0).forGetter(ThermalProperties::boilingPoint),
            Codec.DOUBLE.optionalFieldOf("density", 1.0).forGetter(ThermalProperties::density)
    ).apply(instance, ThermalProperties::create));

    private static ThermalProperties create(double meltingPoint, double boilingPoint, double density) {
        double defaultBoilingPoint = Math.max(meltingPoint + 1.0, meltingPoint * 2.0);
        return new ThermalProperties(
                meltingPoint,
                boilingPoint == -1.0
                                || (Double.isFinite(boilingPoint)
                                        && boilingPoint <= meltingPoint)
                        ? defaultBoilingPoint
                        : boilingPoint,
                Double.isFinite(density) && density <= 0.0 ? 1.0 : density);
    }
}
