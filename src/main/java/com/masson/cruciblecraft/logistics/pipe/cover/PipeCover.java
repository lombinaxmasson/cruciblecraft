package com.masson.cruciblecraft.logistics.pipe.cover;

import java.util.Objects;
import java.util.Optional;

/**
 * One immutable face attachment.
 *
 * <p>The optional match id is an item or fluid registry id for FILTER. T8
 * intentionally has no fuzzy/tag filter mode yet.
 */
public record PipeCover(
        PipeCoverType type,
        Optional<String> matchId) {
    public PipeCover {
        Objects.requireNonNull(type, "type");
        matchId = matchId == null ? Optional.empty() : matchId;
        if (type != PipeCoverType.FILTER && matchId.isPresent()) {
            throw new IllegalArgumentException(
                    "Only filter covers may carry a match id");
        }
        matchId.ifPresent(id -> {
            if (!id.matches("[a-z0-9_.-]+:[a-z0-9_./-]+")) {
                throw new IllegalArgumentException(
                        "Invalid pipe cover match id " + id);
            }
        });
    }

    public static PipeCover filter(String matchId) {
        return new PipeCover(
                PipeCoverType.FILTER, Optional.of(matchId));
    }

    public static PipeCover valve() {
        return new PipeCover(
                PipeCoverType.ONE_WAY_VALVE, Optional.empty());
    }

    public static PipeCover pump() {
        return new PipeCover(
                PipeCoverType.OUTPUT_PUMP, Optional.empty());
    }
}
