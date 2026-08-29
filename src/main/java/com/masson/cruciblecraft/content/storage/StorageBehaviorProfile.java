package com.masson.cruciblecraft.content.storage;

import java.util.Locale;
import java.util.Objects;

import net.minecraft.resources.ResourceLocation;

public enum StorageBehaviorProfile {
    BOOKSHELF,
    BOTTLE_CRATE,
    DRAWER,
    LOCKER,
    LOCKER_CHARGING,
    MASS_STORAGE,
    MASS_STORAGE_LOGISTICS,
    STORAGE_INSERTER;

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static StorageBehaviorProfile parse(String value) {
        Objects.requireNonNull(value, "value");
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public boolean massStorage() {
        return this == MASS_STORAGE || this == MASS_STORAGE_LOGISTICS;
    }

    public boolean locker() {
        return this == LOCKER || this == LOCKER_CHARGING;
    }

    public ResourceLocation model() {
        String path = switch (this) {
            case BOOKSHELF -> "block/storage_bookshelf";
            case BOTTLE_CRATE -> "block/storage_bottle_crate";
            case DRAWER -> "block/storage_drawer";
            case LOCKER, LOCKER_CHARGING -> "block/storage_locker";
            case MASS_STORAGE, MASS_STORAGE_LOGISTICS -> "block/storage_mass";
            case STORAGE_INSERTER -> "block/storage_inserter";
        };
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }
}
