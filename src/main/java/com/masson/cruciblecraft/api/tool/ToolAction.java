package com.masson.cruciblecraft.api.tool;

/**
 * World-click actions a tool can provide and a block can answer.
 *
 * <p>Declaration order is the dispatcher try-order. Plunger and crowbar stay
 * ahead of wrench so pipe right-clicks are not swallowed by connection
 * toggling.
 */
public enum ToolAction {
    PLUNGER,
    CROWBAR,
    PINCERS,
    SCREWDRIVER,
    WRENCH,
    MONKEY_WRENCH,
    WIRE_CUTTER,
    SOFT_HAMMER,
    CHISEL,
    HAMMER,
    PROSPECTOR,
    KNIFE,
    SHEARS,
    DRILL,
    BUILDER_WAND,
    MIXER,
    GRAFTER,
    MAGNIFYING_GLASS,
    IGNITER;

    /** GTM pipe outline/raytrace expands for wrench, crowbar, and plunger. */
    public boolean expandsPipeGrid() {
        return this == WRENCH
                || this == CROWBAR
                || this == PLUNGER
                || this == MONKEY_WRENCH;
    }
}
