package com.masson.cruciblecraft.energy.remainder;

import com.masson.cruciblecraft.api.energy.EnergyType;

/**
 * GT6 {@code MultiTileEntityMagicFieldAbsorber} rows that exist in this
 * pack. Twilight Forest trophies are not registered, so KU/HU/LU/CU trophy
 * rows stay absent.
 */
public record MagicFieldOffer(EnergyType type, long magnitude) {
    public static final MagicFieldOffer DRAGON_EGG =
            new MagicFieldOffer(EnergyType.QUANTUM, 64L);
    public static final MagicFieldOffer SKULL =
            new MagicFieldOffer(EnergyType.TIME, 1L);

    public enum Source {
        NONE,
        DRAGON_EGG,
        SKULL
    }

    public static MagicFieldOffer of(Source source) {
        return switch (source) {
            case DRAGON_EGG -> DRAGON_EGG;
            case SKULL -> SKULL;
            case NONE -> null;
        };
    }

    public long packetSize() {
        return type.sizeIrrelevant() ? 1L : magnitude;
    }

    public long packetAmount() {
        return type.sizeIrrelevant() ? magnitude : 1L;
    }
}
