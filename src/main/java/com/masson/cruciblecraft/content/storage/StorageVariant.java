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
        String acquisitionProfile) {
    public StorageVariant {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(family, "family");
        Objects.requireNonNull(behavior, "behavior");
        Objects.requireNonNull(english, "english");
        Objects.requireNonNull(chinese, "chinese");
        Objects.requireNonNull(acquisitionProfile, "acquisitionProfile");
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
}
