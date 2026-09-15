package com.masson.cruciblecraft.content.mold;

import java.util.Locale;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;

public enum MoldShape {
    INGOT(MaterialPrefixes.INGOT, 0b0_01110_01110_01110_01110_01110),
    PLATE(MaterialPrefixes.PLATE, 0b0_11111_11111_11111_11111_11111),
    ROD(MaterialPrefixes.ROD, 0b0_00000_00000_11111_00000_00000),
    BOLT(MaterialPrefixes.BOLT, 0b0_00000_00000_00100_00100_00000);

    private final MaterialPrefix form;
    private final int mask;

    MoldShape(MaterialPrefix form, int mask) {
        this.form = form;
        this.mask = mask;
    }

    public MaterialPrefix form() {
        return form;
    }

    public int mask() {
        return mask;
    }

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static MoldShape parse(String value) {
        return valueOf(value.toUpperCase(Locale.ROOT));
    }

    public static Optional<MoldShape> fromMask(int mask) {
        for (MoldShape shape : values()) {
            if (shape.mask == mask) {
                return Optional.of(shape);
            }
        }
        return Optional.empty();
    }
}
