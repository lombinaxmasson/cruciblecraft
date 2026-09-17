package com.masson.cruciblecraft.content.block;

import net.minecraft.util.StringRepresentable;

/** Stone / deepslate / netherrack host for PrefixBlock-style ores. */
public enum OreStoneHost implements StringRepresentable {
    STONE("stone"),
    DEEPSLATE("deepslate"),
    NETHERRACK("netherrack");

    private final String serialized;

    OreStoneHost(String serialized) {
        this.serialized = serialized;
    }

    @Override
    public String getSerializedName() {
        return serialized;
    }
}
