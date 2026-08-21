package com.masson.cruciblecraft.logistics.hopper;

import net.minecraft.resources.ResourceLocation;

/** Hopper family kinds. Dust Funnel is a separate block, not a third kind. */
public enum HopperKind {
    HOPPER("hopper"),
    QUEUE_HOPPER("queue_hopper");

    private final String pathSuffix;

    HopperKind(String pathSuffix) {
        this.pathSuffix = pathSuffix;
    }

    public String pathSuffix() {
        return pathSuffix;
    }

    public ResourceLocation kindId() {
        return ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", pathSuffix);
    }

    public boolean isQueue() {
        return this == QUEUE_HOPPER;
    }
}
