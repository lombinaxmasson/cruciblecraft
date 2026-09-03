package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Objects;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/** Immutable, bounded value row interpreted by one registered cover behavior. */
public record CoverDefinition(
        ResourceLocation id,
        ResourceLocation behaviorId,
        Medium medium,
        Values values,
        Set<ConfigField> configurable) {
    public static final int MAX_ITEM_RATE = 64;
    public static final int MAX_FLUID_RATE = 8_000;
    public static final int MAX_PRESSURE_THRESHOLD = 1_000_000;
    public static final int MAX_EXACT_COUNT = 64;
    public static final int MAX_SELECTOR = 5;
    public static final int MAX_NETWORK_ID = 16;

    public CoverDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(behaviorId, "behaviorId");
        Objects.requireNonNull(medium, "medium");
        Objects.requireNonNull(values, "values");
        configurable = Set.copyOf(configurable);
        int rateLimit = medium == Medium.FLUID
                ? MAX_FLUID_RATE
                : MAX_ITEM_RATE;
        if (values.rate() < 0
                || values.rate() > rateLimit
                || values.pressureThreshold() < 0
                || values.pressureThreshold() > MAX_PRESSURE_THRESHOLD
                || values.exactCount() < 0
                || values.exactCount() > MAX_EXACT_COUNT
                || values.selector() < 0
                || values.selector() > MAX_SELECTOR) {
            throw new IllegalArgumentException(
                    id + ": cover value exceeds a hard runtime bound");
        }
        if (medium == Medium.BOTH && values.rate() > MAX_ITEM_RATE) {
            throw new IllegalArgumentException(
                    id + ": dual-medium rate exceeds the item bound");
        }
    }

    public Values resolve(PipeCoverConfig overrides) {
        Objects.requireNonNull(overrides, "overrides");
        for (ConfigField field : overrides.presentFields()) {
            if (!configurable.contains(field)) {
                throw new IllegalArgumentException(
                        id + ": field is not configurable: " + field);
            }
        }
        Values resolved = new Values(
                overrides.rate().orElse(values.rate()),
                overrides.pressureThreshold().orElse(
                        values.pressureThreshold()),
                overrides.exactCount().orElse(values.exactCount()),
                overrides.mode().orElse(values.mode()),
                overrides.selector().orElse(values.selector()));
        // Re-run all hard limits after applying persisted or network values.
        return new CoverDefinition(
                id,
                behaviorId,
                medium,
                resolved,
                configurable).values();
    }

    public enum Medium {
        ITEM,
        FLUID,
        BOTH;

        public boolean supports(Medium requested) {
            return this == BOTH || this == requested;
        }
    }

    public enum TransferMode {
        UP_TO,
        EXACT
    }

    public enum ConfigField {
        MATCH_ID,
        RATE,
        PRESSURE_THRESHOLD,
        EXACT_COUNT,
        MODE,
        SELECTOR,
        NETWORK_ID
    }

    public record Values(
            int rate,
            int pressureThreshold,
            int exactCount,
            TransferMode mode,
            int selector) {
        public Values {
            Objects.requireNonNull(mode, "mode");
        }
    }
}
