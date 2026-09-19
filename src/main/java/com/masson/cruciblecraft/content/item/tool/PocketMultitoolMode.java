package com.masson.cruciblecraft.content.item.tool;

import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

/**
 * GT6 pocket-multitool metadata cycle:
 * closed → knife → saw → file → screwdriver → wire cutter → scissors →
 * chisel → closed.
 */
public enum PocketMultitoolMode {
    CLOSED(null, "closed"),
    KNIFE(ToolKind.KNIFE, "knife"),
    SAW(ToolKind.SAW, "saw"),
    FILE(ToolKind.FILE, "file"),
    SCREWDRIVER(ToolKind.SCREWDRIVER, "screwdriver"),
    WIRE_CUTTER(ToolKind.WIRE_CUTTER, "wire_cutter"),
    SCISSORS(ToolKind.SCISSORS, "scissors"),
    CHISEL(ToolKind.CHISEL, "chisel");

    private final ToolKind kind;
    private final String serializedName;

    PocketMultitoolMode(ToolKind kind, String serializedName) {
        this.kind = kind;
        this.serializedName = serializedName;
    }

    public ToolKind kind() {
        return kind;
    }

    public String serializedName() {
        return serializedName;
    }

    public PocketMultitoolMode next() {
        PocketMultitoolMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }

    public static PocketMultitoolMode fromOrdinal(int ordinal) {
        PocketMultitoolMode[] values = values();
        if (ordinal < 0 || ordinal >= values.length) {
            return CLOSED;
        }
        return values[ordinal];
    }
}
