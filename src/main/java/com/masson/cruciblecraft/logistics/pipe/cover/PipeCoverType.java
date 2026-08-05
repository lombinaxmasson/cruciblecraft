package com.masson.cruciblecraft.logistics.pipe.cover;

/** The deliberately small T8 cover surface. */
public enum PipeCoverType {
    FILTER("filter"),
    ONE_WAY_VALVE("one_way_valve"),
    OUTPUT_PUMP("output_pump");

    private final String id;

    PipeCoverType(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public static PipeCoverType decode(String id) {
        for (PipeCoverType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown pipe cover type " + id);
    }
}
