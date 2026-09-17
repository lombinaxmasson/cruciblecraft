package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class FiniteFluidQuantaTest {
    @Test
    void bind4ClampsLikeUtCode() {
        assertEquals(0, FiniteFluidQuanta.bind4(-3));
        assertEquals(0, FiniteFluidQuanta.bind4(0));
        assertEquals(8, FiniteFluidQuanta.bind4(8));
        assertEquals(15, FiniteFluidQuanta.bind4(15));
        assertEquals(15, FiniteFluidQuanta.bind4(23));
    }

    @Test
    void quantaIsMetaPlusOne() {
        assertEquals(1, FiniteFluidQuanta.quanta(0));
        assertEquals(8, FiniteFluidQuanta.quanta(FiniteFluidQuanta.DRIP_META));
        assertEquals(16, FiniteFluidQuanta.quanta(FiniteFluidQuanta.FULL_META));
    }

    @Test
    void springDripAddsEightAndClamps() {
        assertEquals(8, FiniteFluidQuanta.bind4(0 + 8));
        assertEquals(15, FiniteFluidQuanta.bind4(7 + 8));
        assertEquals(15, FiniteFluidQuanta.bind4(15 + 8));
    }
}
