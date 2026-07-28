package com.masson.cruciblecraft.api.material;

import java.util.Locale;

import com.mojang.serialization.Codec;

public enum MaterialForm {
    BLOCK(1_296),
    /** A mineable ore chunk with the same one-ingot value as vanilla raw metals. */
    RAW_ORE(144),
    /** A processed ore chunk preserving the raw ore's one-ingot material value. */
    CRUSHED_ORE(144),
    INGOT(144),
    DUST(144),
    PLATE(144),
    ROD(72),
    SMALL_DUST(36),
    BOLT(18),
    NUGGET(16);

    private final int units;
    public static final Codec<MaterialForm> CODEC =
            Codec.STRING.xmap(MaterialForm::parse, MaterialForm::serializedName);

    MaterialForm(int units) {
        this.units = units;
    }

    public int units() {
        return units;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MaterialForm parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
