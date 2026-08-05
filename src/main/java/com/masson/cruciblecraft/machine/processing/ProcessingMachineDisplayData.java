package com.masson.cruciblecraft.machine.processing;

import net.minecraft.network.chat.Component;

/** Pure status projection used by configured menu synchronization. */
public final class ProcessingMachineDisplayData {
    public static final String UNSUPPORTED_VERSION = "unsupported_version";
    public static final String INVENTORY_LAYOUT_QUARANTINED =
            "inventory_layout_quarantined";
    public static final String MATERIAL_QUARANTINED = "material_quarantined";
    public static final String UNKNOWN = "unknown";

    private ProcessingMachineDisplayData() {}

    public static int statusIndex(ProcessingMachineSpec spec, String status) {
        String visible = normalizeStatus(status);
        int index = spec.ui().statuses().indexOf(visible);
        if (index >= 0) {
            return index;
        }
        int unknown = spec.ui().statuses().indexOf(UNKNOWN);
        if (unknown < 0) {
            throw new IllegalStateException(
                    "Processing status vocabulary lacks " + UNKNOWN + ": "
                            + spec.id());
        }
        return unknown;
    }

    public static String normalizeStatus(String status) {
        if (status == null || status.isEmpty()) {
            return "running";
        }
        if (status.startsWith("unsupported_version_")) {
            return UNSUPPORTED_VERSION;
        }
        return status;
    }

    public static Component statusComponent(String status, int argument) {
        String visible = normalizeStatus(status);
        String key = "screen.cruciblecraft.processing.status." + visible;
        return UNSUPPORTED_VERSION.equals(visible)
                || INVENTORY_LAYOUT_QUARANTINED.equals(visible)
                ? Component.translatable(key, argument)
                : Component.translatable(key);
    }
}
