package com.masson.cruciblecraft.logistics.fluidnet;

/** DESIGN_POLICY load caps for the fluid cover network. */
public final class FluidNetworkLimits {
    public static final int MAX_VISITED_PIPES = 4_096;
    public static final int MAX_ENDPOINTS_PER_COMPONENT = 256;
    public static final int DEFAULT_RATE = 1_000;

    private FluidNetworkLimits() {}
}
