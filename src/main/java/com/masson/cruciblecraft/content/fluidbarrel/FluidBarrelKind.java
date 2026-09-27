package com.masson.cruciblecraft.content.fluidbarrel;

/** GT6 barrel subclasses under {@code gregtech.tileentity.tanks}. */
public enum FluidBarrelKind {
    WOOD,
    PLASTIC,
    METAL,
    LOGISTICS;

    public boolean onlySimple() {
        return this == WOOD;
    }

    public boolean canSeal() {
        return this != LOGISTICS;
    }

    public boolean keepsFilter() {
        return this == LOGISTICS;
    }

    public boolean woodenSound() {
        return this == WOOD || this == PLASTIC;
    }

    public static FluidBarrelKind parse(String name) {
        for (FluidBarrelKind kind : values()) {
            if (kind.name().equals(name)) {
                return kind;
            }
        }
        throw new IllegalArgumentException("Unknown fluid barrel kind " + name);
    }
}
