package com.masson.cruciblecraft.content.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class ToolBreakScrapTest {
    @Test
    void scrapBoundMatchesGt6Formula() {
        // GT6: amount < U/4 → no scrap; else 1+RNG.nextInt(1+(int)(4*amount/U)).
        assertEquals(0, ToolBreakScrap.scrapRandomBound(0));
        assertEquals(5, ToolBreakScrap.scrapRandomBound(1));
        assertEquals(17, ToolBreakScrap.scrapRandomBound(4));
    }
}
