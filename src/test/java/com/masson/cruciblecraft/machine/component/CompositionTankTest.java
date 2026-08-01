package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class CompositionTankTest {
    @Test
    void mutationsAreAtomicAtTheCompositionBoundary() {
        CompositionTank tank = new CompositionTank();
        tank.addAll(Map.of("copper", 12, "tin", 4));
        assertEquals(12, tank.units("copper"));
        assertTrue(tank.containsAtLeast(Map.of("copper", 8, "tin", 4)));

        assertFalse(tank.removeAll(Map.of("copper", 13)));
        assertEquals(12, tank.units("copper"));
        assertTrue(tank.removeAll(Map.of("copper", 8, "tin", 4)));
        assertEquals(4, tank.units("copper"));
        assertEquals(0, tank.units("tin"));
    }

    @Test
    void replacementDropsNonPositiveEntries() {
        CompositionTank tank = new CompositionTank();
        tank.replace(Map.of("iron", 144, "carbon", 0));
        assertEquals(144, tank.units("iron"));
        assertEquals(0, tank.units("carbon"));
    }
}
