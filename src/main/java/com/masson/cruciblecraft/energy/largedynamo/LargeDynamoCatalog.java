package com.masson.cruciblecraft.energy.largedynamo;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import com.masson.cruciblecraft.content.multiblock.CoilHosts;

import net.minecraft.resources.ResourceLocation;

/**
 * GT6 LargeDynamo 17221–17224: RU in, EU out, {@code NBT_WASTE_ENERGY},
 * capacitor {@code NBT_INPUT * 2}.
 */
public final class LargeDynamoCatalog {
    private static final List<Profile> PROFILES = List.of(
            profile(
                    "stainless_steel/dynamo_main_housing",
                    17221,
                    CoilHosts.DENSE_STAINLESS,
                    4096L,
                    3072L),
            profile(
                    "titanium/dynamo_main_housing",
                    17222,
                    CoilHosts.DENSE_TITANIUM,
                    8192L,
                    6144L),
            profile(
                    "tungstensteel/dynamo_main_housing",
                    17223,
                    CoilHosts.DENSE_TUNGSTENSTEEL,
                    16384L,
                    12288L),
            profile(
                    "adamantium/dynamo_main_housing",
                    17224,
                    CoilHosts.DENSE_ADAMANTIUM,
                    131072L,
                    98304L));
    private static final Map<ResourceLocation, Profile> BY_ID = Map.of(
            PROFILES.get(0).id(), PROFILES.get(0),
            PROFILES.get(1).id(), PROFILES.get(1),
            PROFILES.get(2).id(), PROFILES.get(2),
            PROFILES.get(3).id(), PROFILES.get(3));

    private LargeDynamoCatalog() {}

    public static List<Profile> profiles() {
        return PROFILES;
    }

    public static Optional<Profile> find(ResourceLocation id) {
        return Optional.ofNullable(BY_ID.get(id));
    }

    public static Profile require(ResourceLocation id) {
        Profile profile = BY_ID.get(id);
        if (profile == null) {
            throw new IllegalArgumentException("Unknown large dynamo " + id);
        }
        return profile;
    }

    public record Profile(
            ResourceLocation id,
            int sourceId,
            ResourceLocation wallId,
            long inputRec,
            long outputRec) {
        public Profile {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(wallId, "wallId");
            if (sourceId <= 0 || inputRec <= 0L || outputRec <= 0L) {
                throw new IllegalArgumentException("Large dynamo rates " + id);
            }
        }

        public long inputMax() {
            return Math.multiplyExact(inputRec, 2L);
        }

        public long outputMin() {
            return outputRec / 2L;
        }

        public long outputMax() {
            return Math.multiplyExact(outputRec, 2L);
        }

        public long capacity() {
            return inputMax();
        }
    }

    private static Profile profile(
            String path,
            int sourceId,
            ResourceLocation wallId,
            long inputRec,
            long outputRec) {
        return new Profile(
                CoilHosts.id(path),
                sourceId,
                wallId,
                inputRec,
                outputRec);
    }
}
