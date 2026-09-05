package com.masson.cruciblecraft.logistics.genericnet;

/** DESIGN_POLICY load caps for the generic cover network. */
public final class GenericNetworkLimits {
    public static final int MAX_VISITED_PIPES = 4_096;
    public static final int MAX_ENDPOINTS_PER_COMPONENT = 256;
    public static final int DEFAULT_ITEM_RATE = 8;
    public static final int DEFAULT_FLUID_RATE = 1_000;

    private GenericNetworkLimits() {}
}
