package com.masson.cruciblecraft.logistics.hopper;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

/** One registered Hopper or Queue Hopper identity. This is not a tier row. */
public record HopperVariant(
        ResourceLocation id,
        HopperKind kind,
        ResourceLocation materialId,
        int slots) {
    public HopperVariant {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(materialId, "materialId");
        if (slots < 1 || slots > 36) {
            throw new IllegalArgumentException("Hopper slots must be 1..36");
        }
        if (kind.isQueue() && slots < 2) {
            throw new IllegalArgumentException("Queue Hopper slots must be >= 2");
        }
        if (!"cruciblecraft".equals(id.getNamespace())
                || id.getPath().equals(kind.pathSuffix())
                || !id.getPath().endsWith("_" + kind.pathSuffix())) {
            throw new IllegalArgumentException(
                    "Hopper id must be <material>_" + kind.pathSuffix()
                            + ": " + id);
        }
    }

    public String materialPath() {
        return materialId.getPath();
    }

    public int slotLimit() {
        return kind.isQueue() ? 64 : HopperTransferCore.TICK_ITEM_CAP;
    }
}
