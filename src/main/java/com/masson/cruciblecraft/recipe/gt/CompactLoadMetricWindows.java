package com.masson.cruciblecraft.recipe.gt;

/**
 * v3 load-metric windows. Reload transient allocation and lookup allocation
 * must not share a sampling interval.
 */
public final class CompactLoadMetricWindows {
    public static final String RELOAD_TRANSIENT = "reload";
    public static final String LOOKUP = "lookup";
    public static final String RETAINED_AFTER_GC = "retained_after_controlled_gc";

    public static final String AXIS_RELOAD_TRANSIENT_ALLOCATION =
            "reload_transient_allocation_bytes";
    public static final String AXIS_LOOKUP_ALLOCATION =
            "lookup_allocation_bytes_per_operation";
    public static final String AXIS_RETAINED = "retained_memory_bytes";
    public static final String LEGACY_MIXED_ALLOCATION = "allocation_bytes";

    private CompactLoadMetricWindows() {}

    public static boolean isSplitAxis(String axis) {
        return AXIS_RELOAD_TRANSIENT_ALLOCATION.equals(axis)
                || AXIS_LOOKUP_ALLOCATION.equals(axis);
    }

    public static boolean isLegacyMixedAllocation(String axis) {
        return LEGACY_MIXED_ALLOCATION.equals(axis);
    }

    public static boolean unmeasuredZeroFillFailsClosed(boolean measured, long value) {
        return !measured && value == 0L;
    }

    public static boolean lookupZeroEventsIsMeasuredPass(
            boolean measured, boolean recordingPresent, long eventCount) {
        return measured && recordingPresent && eventCount == 0L;
    }
}
