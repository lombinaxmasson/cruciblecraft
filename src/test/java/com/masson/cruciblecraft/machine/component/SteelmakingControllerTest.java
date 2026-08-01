package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.recipe.SteelmakingProcess;
import org.junit.jupiter.api.Test;

class SteelmakingControllerTest {
    @Test
    void restoreSanitizesPersistedProcessState() {
        SteelmakingController controller = new SteelmakingController();
        controller.restore(
                Long.MAX_VALUE,
                -1,
                Integer.MAX_VALUE);

        assertTrue(controller.storedAir() > 0L);
        assertEquals(0, controller.batchIronUnits());
        assertEquals(
                SteelmakingProcess.REACTION_INTERVAL_TICKS - 1,
                controller.reactionTicks());
        assertTrue(controller.blocksFluidTransfer());
    }

    @Test
    void resetClearsEverySteelmakingGate() {
        SteelmakingController controller = new SteelmakingController();
        controller.restore(5L, 0, 3);
        assertTrue(controller.reset());
        assertEquals(0L, controller.storedAir());
        assertEquals(0, controller.reactionTicks());
        assertFalse(controller.active());
        assertFalse(controller.blocksFluidTransfer());
        assertFalse(controller.reset());
    }
}
