package com.masson.cruciblecraft.worldgen;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.EnumProperty;

/**
 * GT6 {@code MultiTileEntityRock} copied terrain: stone, sandstone, cobble.
 */
public enum SurfaceRockAppearance implements StringRepresentable {
    STONE("stone"),
    SANDSTONE("sandstone"),
    COBBLE("cobble");

    public static final EnumProperty<SurfaceRockAppearance> PROPERTY =
            EnumProperty.create("appearance", SurfaceRockAppearance.class);

    private final String serialized;

    SurfaceRockAppearance(String serialized) {
        this.serialized = serialized;
    }

    @Override
    public String getSerializedName() {
        return serialized;
    }
}
