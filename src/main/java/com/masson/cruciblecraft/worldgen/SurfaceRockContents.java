package com.masson.cruciblecraft.worldgen;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * GT6 {@code WorldgenRocks} NBT_VALUE: empty stone rock, flint, meteoric
 * {@code rockGt} / {@code oreRaw}.
 */
public enum SurfaceRockContents implements StringRepresentable {
    EMPTY("empty"),
    FLINT("flint"),
    METEORIC_ROCK("meteoric_rock"),
    METEORIC_RAW("meteoric_raw");

    public static final EnumProperty<SurfaceRockContents> PROPERTY =
            EnumProperty.create("contents", SurfaceRockContents.class);

    private final String serialized;

    SurfaceRockContents(String serialized) {
        this.serialized = serialized;
    }

    @Override
    public String getSerializedName() {
        return serialized;
    }
}
