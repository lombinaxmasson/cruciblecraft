package com.masson.cruciblecraft.client.color;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class Gt6OpeningBlockColorTest {
    @Test
    void ceramicTintMatchesGt6MtCeramic() {
        assertEquals(0xFFDC8246, Gt6OpeningBlockColor.ceramicColor());
    }
}
