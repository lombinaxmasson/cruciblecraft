package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy.Decision;
import com.masson.cruciblecraft.content.multiblock.PluginQuarantinePolicy.Resolution;

class PluginQuarantinePolicyTest {
    @Test
    void blankFirstSaveAdoptsTheCurrentId() {
        Decision decision = PluginQuarantinePolicy.resolve(
                "", "cruciblecraft:processing_host");
        assertEquals(Resolution.ACCEPTED, decision.resolution());
        assertEquals(
                "cruciblecraft:processing_host",
                decision.persistedPluginId());
        assertFalse(decision.quarantineReason().isPresent());
    }

    @Test
    void equalIdsAreAccepted() {
        Decision decision = PluginQuarantinePolicy.resolve(
                "cruciblecraft:shared_port_supply",
                "cruciblecraft:shared_port_supply");
        assertEquals(Resolution.ACCEPTED, decision.resolution());
        assertEquals(
                "cruciblecraft:shared_port_supply",
                decision.persistedPluginId());
        assertFalse(decision.quarantineReason().isPresent());
    }

    @Test
    void mismatchStaysQuarantinedWithTheSavedIdAndAReason() {
        Decision decision = PluginQuarantinePolicy.resolve(
                "cruciblecraft:not_a_plugin",
                "cruciblecraft:processing_host");
        assertEquals(Resolution.QUARANTINED, decision.resolution());
        assertEquals(
                "cruciblecraft:not_a_plugin",
                decision.persistedPluginId());
        assertTrue(decision.quarantineReason().isPresent());
        assertTrue(
                decision.quarantineReason().orElseThrow()
                        .contains("does not match"));
    }

    @Test
    void onlyQuarantinedDecisionsCarryAReason() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Decision(
                        Resolution.ACCEPTED,
                        "cruciblecraft:processing_host",
                        java.util.Optional.of("unexpected")));
        assertThrows(
                IllegalArgumentException.class,
                () -> new Decision(
                        Resolution.QUARANTINED,
                        "cruciblecraft:processing_host",
                        java.util.Optional.empty()));
    }
}
