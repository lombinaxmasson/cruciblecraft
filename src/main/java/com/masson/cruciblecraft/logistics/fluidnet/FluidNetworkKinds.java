package com.masson.cruciblecraft.logistics.fluidnet;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/** Kind and direction mapping for the fluid logistics cover slice. */
public final class FluidNetworkKinds {
    public static final ResourceLocation STORAGE = ResourceLocation.fromNamespaceAndPath(
            "cruciblecraft", "logistics_fluid_storage");
    public static final ResourceLocation IMPORT = ResourceLocation.fromNamespaceAndPath(
            "cruciblecraft", "logistics_fluid_import");
    public static final ResourceLocation EXPORT = ResourceLocation.fromNamespaceAndPath(
            "cruciblecraft", "logistics_fluid_export");
    public static final ResourceLocation STORAGE_BEHAVIOR =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "logistics_fluid_storage");
    public static final ResourceLocation TRANSFER_BEHAVIOR =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "logistics_fluid_transfer");

    public enum TransferDirection {
        IMPORT,
        EXPORT
    }

    private FluidNetworkKinds() {}

    public static boolean isLogistics(ResourceLocation id) {
        return id != null && (
                STORAGE.equals(id) || IMPORT.equals(id) || EXPORT.equals(id));
    }

    public static boolean isStorage(ResourceLocation id) {
        return STORAGE.equals(id);
    }

    public static Optional<TransferDirection> direction(ResourceLocation id) {
        if (IMPORT.equals(id)) {
            return Optional.of(TransferDirection.IMPORT);
        }
        if (EXPORT.equals(id)) {
            return Optional.of(TransferDirection.EXPORT);
        }
        return Optional.empty();
    }

    public static boolean isJoined(int networkId) {
        return networkId >= 1
                && networkId <= com.masson.cruciblecraft.logistics.pipe.cover
                        .CoverDefinition.MAX_NETWORK_ID;
    }

    public static int networkId(
            com.masson.cruciblecraft.logistics.pipe.cover.PipeCover cover) {
        return cover.config().networkId().orElse(0);
    }
}
