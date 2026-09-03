package com.masson.cruciblecraft.logistics.itemnet;

/** DESIGN_POLICY load caps for the item cover network. Independent of pipe traversal. */
public final class ItemNetworkLimits {
    public static final int MAX_VISITED_PIPES = 4_096;
    public static final int MAX_ENDPOINTS_PER_COMPONENT = 256;
    public static final int DEFAULT_RATE = 8;

    private ItemNetworkLimits() {}
}
