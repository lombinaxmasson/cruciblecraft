package com.masson.cruciblecraft.logistics.itemnet;

import java.util.Optional;

import net.minecraft.resources.ResourceLocation;

/** Kind and direction mapping for the two item logistics cover kinds. */
public final class ItemNetworkKinds {
    public static final ResourceLocation STORAGE = ResourceLocation.fromNamespaceAndPath(
            "cruciblecraft", "logistics_item_storage");
    public static final ResourceLocation IMPORT = ResourceLocation.fromNamespaceAndPath(
            "cruciblecraft", "logistics_item_import");
    public static final ResourceLocation EXPORT = ResourceLocation.fromNamespaceAndPath(
            "cruciblecraft", "logistics_item_export");
    public static final ResourceLocation STORAGE_BEHAVIOR =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "logistics_item_storage");
    public static final ResourceLocation TRANSFER_BEHAVIOR =
            ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft", "logistics_item_transfer");

    public enum TransferDirection {
        IMPORT,
        EXPORT
    }

    private ItemNetworkKinds() {}

    public static boolean isLogistics(ResourceLocation id) {
        return id != null && (
                STORAGE.equals(id) || IMPORT.equals(id) || EXPORT.equals(id));
    }

    public static boolean isStorage(ResourceLocation id) {
        return STORAGE.equals(id);
    }

    public static boolean isForbiddenFrozenKind(ResourceLocation id) {
        if (id == null || !"cruciblecraft".equals(id.getNamespace())) {
            return false;
        }
        String path = id.getPath();
        if (!path.startsWith("logistics_")) {
            return false;
        }
        return !com.masson.cruciblecraft.logistics.core.LogisticsDumpKinds
                .KNOWN_LOGISTICS_PATHS.contains(path);
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
