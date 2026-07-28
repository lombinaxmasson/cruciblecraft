package com.masson.cruciblecraft.content.mold;

import java.util.Locale;

import com.masson.cruciblecraft.api.material.MaterialForm;

public enum MoldShape {
    INGOT(MaterialForm.INGOT, 0b0_01110_01110_01110_01110_01110),
    PLATE(MaterialForm.PLATE, 0b0_11111_11111_11111_11111_11111),
    ROD(MaterialForm.ROD, 0b0_00000_00000_11111_00000_00000),
    BOLT(MaterialForm.BOLT, 0b0_00000_00000_00100_00100_00000);

    private final MaterialForm form;
    private final int mask;

    MoldShape(MaterialForm form, int mask) {
        this.form = form;
        this.mask = mask;
    }

    public MaterialForm form() {
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
}
