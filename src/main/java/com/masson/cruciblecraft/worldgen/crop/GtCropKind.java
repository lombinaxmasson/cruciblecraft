package com.masson.cruciblecraft.worldgen.crop;

/**
 * GT6 {@code WorldgenGlowtus} / {@code WorldgenBushes}.
 */
public enum GtCropKind {
    GLOWTUS("glowtus", 16, 2),
    BUSH("bush", 1, 4);

    private final String id;
    private final int amount;
    private final int probability;

    GtCropKind(String id, int amount, int probability) {
        this.id = id;
        this.amount = amount;
        this.probability = probability;
    }

    public String id() {
        return id;
    }

    public int amount() {
        return amount;
    }

    public int probability() {
        return probability;
    }

    public static GtCropKind fromId(String id) {
        for (GtCropKind kind : values()) {
            if (kind.id.equals(id)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown GT crop kind " + id);
    }
}
