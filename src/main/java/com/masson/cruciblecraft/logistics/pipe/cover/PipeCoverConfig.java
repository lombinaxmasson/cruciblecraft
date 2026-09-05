package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

import net.minecraft.resources.ResourceLocation;

/** Bounded per-face overrides; absent fields inherit the definition values. */
public record PipeCoverConfig(
        Optional<String> matchId,
        Optional<Integer> rate,
        Optional<Integer> pressureThreshold,
        Optional<Integer> exactCount,
        Optional<CoverDefinition.TransferMode> mode,
        Optional<Integer> selector,
        Optional<Integer> networkId,
        Optional<Integer> invert,
        int visual,
        int redstone) {
    public static final int MAX_MATCH_ID_LENGTH = 128;
    public static final PipeCoverConfig EMPTY = new PipeCoverConfig(
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            Optional.empty(),
            0,
            0);

    public PipeCoverConfig {
        matchId = matchId == null ? Optional.empty() : matchId;
        rate = rate == null ? Optional.empty() : rate;
        pressureThreshold = pressureThreshold == null
                ? Optional.empty()
                : pressureThreshold;
        exactCount = exactCount == null ? Optional.empty() : exactCount;
        mode = mode == null ? Optional.empty() : mode;
        selector = selector == null ? Optional.empty() : selector;
        networkId = networkId == null ? Optional.empty() : networkId;
        invert = invert == null ? Optional.empty() : invert;
        matchId.ifPresent(id -> {
            if (id.length() > MAX_MATCH_ID_LENGTH
                    || ResourceLocation.tryParse(id) == null) {
                throw new IllegalArgumentException(
                        "Invalid pipe cover match id " + id);
            }
        });
        rate.ifPresent(value -> bounded(
                "rate", value, 0, CoverDefinition.MAX_FLUID_RATE));
        pressureThreshold.ifPresent(value -> bounded(
                "pressure threshold",
                value,
                0,
                CoverDefinition.MAX_PRESSURE_THRESHOLD));
        exactCount.ifPresent(value -> bounded(
                "exact count", value, 0, CoverDefinition.MAX_EXACT_COUNT));
        selector.ifPresent(value -> bounded(
                "selector", value, 0, CoverDefinition.MAX_SELECTOR));
        networkId.ifPresent(value -> bounded(
                "network id", value, 0, CoverDefinition.MAX_NETWORK_ID));
        invert.ifPresent(value -> bounded("invert", value, 0, 1));
        bounded("visual", visual, 0, 10);
        bounded("redstone", redstone, 0, 15);
    }

    public Set<CoverDefinition.ConfigField> presentFields() {
        EnumSet<CoverDefinition.ConfigField> fields =
                EnumSet.noneOf(CoverDefinition.ConfigField.class);
        matchId.ifPresent(value ->
                fields.add(CoverDefinition.ConfigField.MATCH_ID));
        rate.ifPresent(value -> fields.add(CoverDefinition.ConfigField.RATE));
        pressureThreshold.ifPresent(value ->
                fields.add(CoverDefinition.ConfigField.PRESSURE_THRESHOLD));
        exactCount.ifPresent(value ->
                fields.add(CoverDefinition.ConfigField.EXACT_COUNT));
        mode.ifPresent(value -> fields.add(CoverDefinition.ConfigField.MODE));
        selector.ifPresent(value ->
                fields.add(CoverDefinition.ConfigField.SELECTOR));
        networkId.ifPresent(value ->
                fields.add(CoverDefinition.ConfigField.NETWORK_ID));
        return Set.copyOf(fields);
    }

    public PipeCoverConfig withMatchId(String id) {
        return new PipeCoverConfig(
                Optional.of(id),
                rate,
                pressureThreshold,
                exactCount,
                mode,
                selector,
                networkId,
                invert,
                visual,
                redstone);
    }

    public PipeCoverConfig withInvert(int value) {
        return new PipeCoverConfig(
                matchId,
                rate,
                pressureThreshold,
                exactCount,
                mode,
                selector,
                networkId,
                Optional.of(value),
                visual,
                redstone);
    }

    public PipeCoverConfig withDisplay(int nextVisual, int nextRedstone) {
        return new PipeCoverConfig(
                matchId,
                rate,
                pressureThreshold,
                exactCount,
                mode,
                selector,
                networkId,
                invert,
                nextVisual,
                nextRedstone);
    }

    public PipeCoverConfig with(
            CoverDefinition.ConfigField field, int value) {
        return switch (field) {
            case RATE -> new PipeCoverConfig(
                    matchId,
                    Optional.of(value),
                    pressureThreshold,
                    exactCount,
                    mode,
                    selector,
                    networkId,
                    invert,
                    visual,
                    redstone);
            case PRESSURE_THRESHOLD -> new PipeCoverConfig(
                    matchId,
                    rate,
                    Optional.of(value),
                    exactCount,
                    mode,
                    selector,
                    networkId,
                    invert,
                    visual,
                    redstone);
            case EXACT_COUNT -> new PipeCoverConfig(
                    matchId,
                    rate,
                    pressureThreshold,
                    Optional.of(value),
                    mode,
                    selector,
                    networkId,
                    invert,
                    visual,
                    redstone);
            case MODE -> {
                if (value < 0
                        || value
                                >= CoverDefinition.TransferMode.values().length) {
                    throw new IllegalArgumentException(
                            "Cover mode ordinal is outside its hard bound");
                }
                yield new PipeCoverConfig(
                        matchId,
                        rate,
                        pressureThreshold,
                        exactCount,
                        Optional.of(
                                CoverDefinition.TransferMode.values()[value]),
                        selector,
                        networkId,
                        invert,
                        visual,
                        redstone);
            }
            case SELECTOR -> new PipeCoverConfig(
                    matchId,
                    rate,
                    pressureThreshold,
                    exactCount,
                    mode,
                    Optional.of(value),
                    networkId,
                    invert,
                    visual,
                    redstone);
            case NETWORK_ID -> new PipeCoverConfig(
                    matchId,
                    rate,
                    pressureThreshold,
                    exactCount,
                    mode,
                    selector,
                    Optional.of(value),
                    invert,
                    visual,
                    redstone);
            case MATCH_ID -> throw new IllegalArgumentException(
                    "Match id is not an integer cover field");
        };
    }

    private static void bounded(
            String field, int value, int minimum, int maximum) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    "Cover " + field + " is outside ["
                            + minimum + ", " + maximum + "]");
        }
    }
}
