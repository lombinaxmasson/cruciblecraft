package com.masson.cruciblecraft.material.def;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

public record ThermalProperties(double meltingPoint, double boilingPoint, double density) {

    public ThermalProperties(double meltingPoint) {
        this(meltingPoint, meltingPoint * 2.0, 1.0);
    }

    public static final Codec<ThermalProperties> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.DOUBLE.fieldOf("melting_point").forGetter(ThermalProperties::meltingPoint),
            Codec.DOUBLE.optionalFieldOf("boiling_point", -1.0).forGetter(ThermalProperties::boilingPoint),
            Codec.DOUBLE.optionalFieldOf("density", 1.0).forGetter(ThermalProperties::density)
    ).apply(instance, ThermalProperties::create));

    private static ThermalProperties create(double meltingPoint, double boilingPoint, double density) {
        return new ThermalProperties(
                meltingPoint,
                boilingPoint < 0.0 ? meltingPoint * 2.0 : boilingPoint,
                density);
    }
}
