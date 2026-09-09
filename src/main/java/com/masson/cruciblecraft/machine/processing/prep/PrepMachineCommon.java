package com.masson.cruciblecraft.machine.processing.prep;

import java.util.List;

final class PrepMachineCommon {
    static final List<String> STATUSES = List.of(
            "idle",
            "running",
            "invalid_recipe",
            "recipe_power_exceeded",
            "overcharged",
            "output_blocked",
            "underpowered",
            "unsupported_version",
            "inventory_layout_quarantined",
            "material_quarantined",
            "unknown");
    static final long RU_CAPACITY = 4_096L;
    static final long RU_MAX_PACKET = 256L;
    static final long EU_CAPACITY = 65_536L;
    static final long EU_MAX_PACKET = 8_192L;
    static final long HU_MAX_PACKET = 1_024L;
    static final int PANEL_TANK = 64_000;
    static final int MELTER_TANK_IN = 4_000;
    static final int MELTER_TANK_OUT = 8_000;

    private PrepMachineCommon() {}
}
