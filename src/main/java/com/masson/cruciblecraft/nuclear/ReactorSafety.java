package com.masson.cruciblecraft.nuclear;

/** Jade-facing fail/backpressure state. Heat stays HU. */
public enum ReactorSafety {
    OK("ok"),
    RODS_DESTROYED_NO_COOLANT("rods_destroyed_no_coolant"),
    OUTPUT_FULL_STALLED("output_full_stalled");

    private final String key;

    ReactorSafety(String key) {
        this.key = key;
    }

    public String key() {
        return key;
    }

    public static ReactorSafety fromKey(String key) {
        for (ReactorSafety value : values()) {
            if (value.key.equals(key)) {
                return value;
            }
        }
        return OK;
    }
}
