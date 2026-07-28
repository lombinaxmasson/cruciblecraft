package com.masson.cruciblecraft.recipe;

import java.util.Locale;

import com.mojang.serialization.Codec;

public enum AnvilMode {
    ANVIL,
    BEND_SMALL,
    BEND_BIG;

    public static final Codec<AnvilMode> CODEC =
            Codec.STRING.xmap(AnvilMode::parse, AnvilMode::serializedName);

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static AnvilMode parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }
}
