package com.masson.cruciblecraft.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PerTickEnergyBudgetTest {
    @Test
    void sharesOneBudgetAndKeepsSimulationSideEffectFree() {
        PerTickEnergyBudget budget = new PerTickEnergyBudget();

        assertEquals(16L, budget.claim(10L, 16L, 16L, true));
        assertEquals(16L, budget.claim(10L, 16L, 16L, true));
        assertEquals(10L, budget.claim(10L, 16L, 10L, false));
        assertEquals(6L, budget.claim(10L, 16L, 16L, false));
        assertEquals(0L, budget.claim(10L, 16L, 16L, false));
        assertEquals(16L, budget.claim(11L, 16L, 16L, false));
    }

    @Test
    void minimumLongIsAValidFirstTick() {
        PerTickEnergyBudget budget = new PerTickEnergyBudget();

        assertEquals(4L, budget.claim(Long.MIN_VALUE, 4L, 4L, false));
        assertEquals(0L, budget.claim(Long.MIN_VALUE, 4L, 4L, false));
    }
}
