package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

class SteelmakingTickDecisionsTest {
    @Test
    void staleActiveBatchResetsEvenWithZeroStoredAir() {
        var stale = SteelmakingTickDecisions.evaluate(
                Map.of("iron", 432, "carbon", 144, "tin", 1),
                432,
                0L);
        var validButEmpty = SteelmakingTickDecisions.evaluate(
                Map.of("iron", 432, "carbon", 144),
                432,
                0L);

        assertEquals(SteelmakingTickDecisions.Action.RESET_STALE, stale.action());
        assertEquals(SteelmakingTickDecisions.Action.WAIT_FOR_AIR, validButEmpty.action());
    }
}
