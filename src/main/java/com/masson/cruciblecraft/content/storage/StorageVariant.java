package com.masson.cruciblecraft.content.storage;

import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

public record StorageVariant(
        ResourceLocation id,
        String family,
        StorageBehaviorProfile behavior,
        int slots,
        int capacity,
        boolean sourceVisible,
        boolean charging,
        boolean logistics,
        boolean countsTowardStorage624,
        String english,
        String chinese,
        Integer plankIndex,
        String acquisitionProfile,
        String modelProfile,
        String representativeMaterial,
        String expansionKey) {
    public StorageVariant {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(behavior, "behavior");
        Objects.requireNonNull(english, "english");
        Objects.requireNonNull(chinese, "chinese");
        Objects.requireNonNull(acquisitionProfile, "acquisitionProfile");
        if (modelProfile == null || modelProfile.isBlank()) {
            modelProfile = family;
        }
        if (representativeMaterial == null || representativeMaterial.isBlank()) {
            representativeMaterial = "steel";
        }
        if (expansionKey == null) {
            expansionKey = "";
        }
        if (slots < 0) {
            throw new IllegalArgumentException("slots");
        }
        if (capacity < 0) {
            throw new IllegalArgumentException("capacity");
        }
    }

    public String path() {
        return id.getPath();
    }

    public ResourceLocation model() {
        String path = switch (modelProfile) {
            case "bookshelf_metal" -> "block/storage_bookshelf_metal";
            case "bookshelf_wood" -> "block/storage_bookshelf_wood";
            case "bottle_crate", "bottle_crate_wood" -> "block/storage_bottle_crate_wood";
            case "bottle_crate_metal" -> "block/storage_bottle_crate_metal";
            case "drawer" -> "block/storage_drawer";
            case "locker" -> "block/storage_locker";
            case "charging_locker" -> "block/storage_charging_locker";
            case "mass_storage_barrel" -> "block/storage_mass_barrel";
            case "mass_storage_box" -> "block/storage_mass_box";
            case "mass_storage_standard" -> "block/storage_mass";
            case "mass_storage_logistics" -> "block/storage_mass_logistics";
            case "storage_inserter", "inserter" -> "block/storage_inserter";
            default -> behavior.model().getPath();
        };
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    public String tintMaterial() {
        return switch (expansionKey) {
            case "6983" -> "skyroot";
            case "6984" -> "silverwood";
            case "6985" -> "greatwood";
            case "6986" -> "shimmerwood";
            case "6987" -> "dreamwood";
            case "6988" -> "livingwood";
            case "6989" -> "weedwood";
            case "6997" -> "ironwood";
            case "6999" -> "wood_treated";
            case "6990", "6991", "6992", "6998" -> "wood";
            case "6993", "6994", "6995", "6996" -> "plastic";
            case "4000", "6000", "7100", "7300", "8600" -> "lead";
            default -> plankIndex() != null ? "wood" : representativeMaterial;
        };
    }
}
