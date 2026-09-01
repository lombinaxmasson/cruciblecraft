package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CompactLoadMetricWindowsTest {
    @Test
    void splitAxesDoNotReuseMixedAllocation() {
        assertTrue(CompactLoadMetricWindows.isSplitAxis(
                CompactLoadMetricWindows.AXIS_RELOAD_TRANSIENT_ALLOCATION));
        assertTrue(CompactLoadMetricWindows.isSplitAxis(
                CompactLoadMetricWindows.AXIS_LOOKUP_ALLOCATION));
        assertFalse(CompactLoadMetricWindows.isSplitAxis(
                CompactLoadMetricWindows.LEGACY_MIXED_ALLOCATION));
        assertTrue(CompactLoadMetricWindows.isLegacyMixedAllocation(
                CompactLoadMetricWindows.LEGACY_MIXED_ALLOCATION));
    }

    @Test
    void unmeasuredZeroFillFailsClosedWhileMeasuredLookupZeroPasses() {
        assertTrue(CompactLoadMetricWindows.unmeasuredZeroFillFailsClosed(false, 0L));
        assertFalse(CompactLoadMetricWindows.unmeasuredZeroFillFailsClosed(true, 0L));
        assertTrue(CompactLoadMetricWindows.lookupZeroEventsIsMeasuredPass(
                true, true, 0L));
        assertFalse(CompactLoadMetricWindows.lookupZeroEventsIsMeasuredPass(
                false, true, 0L));
    }
}
