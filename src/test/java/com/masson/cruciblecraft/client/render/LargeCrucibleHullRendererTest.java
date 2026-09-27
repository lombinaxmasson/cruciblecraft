package com.masson.cruciblecraft.client.render;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

class LargeCrucibleHullRendererTest {
    @Test
    void sideProfileStandsUprightAcrossTheShell() {
        assertEquals(0.0f, LargeCrucibleHullRenderer.textureV(Direction.SOUTH, 0.0f, 3.0f, 1.0f), 1.0e-4f);
        assertEquals(1.0f, LargeCrucibleHullRenderer.textureV(Direction.SOUTH, 0.0f, 0.0f, 1.0f), 1.0e-4f);
        assertEquals(0.0f, LargeCrucibleHullRenderer.textureU(Direction.SOUTH, -1.0f, 1.0f, 2.0f), 1.0e-4f);
        assertEquals(1.0f, LargeCrucibleHullRenderer.textureU(Direction.SOUTH, 2.0f, 1.0f, 2.0f), 1.0e-4f);
    }

    @Test
    void halfBlockRimUsesOnlyItsBandOfTheProfile() {
        assertEquals(0.0f, LargeCrucibleHullRenderer.textureU(Direction.UP, -1.0f, 3.0f, 0.0f), 1.0e-4f);
        assertEquals(
                1.0f / 6.0f,
                LargeCrucibleHullRenderer.textureU(Direction.UP, -0.5f, 3.0f, 0.0f),
                1.0e-4f);
        assertEquals(0.0f, LargeCrucibleHullRenderer.textureV(Direction.UP, -0.75f, 3.0f, -1.0f), 1.0e-4f);
        assertEquals(1.0f, LargeCrucibleHullRenderer.textureV(Direction.UP, -0.75f, 3.0f, 2.0f), 1.0e-4f);
    }

    @Test
    void eastAndWestFacesSampleDepthNotTheConstantAxis() {
        assertEquals(0.0f, LargeCrucibleHullRenderer.textureU(Direction.EAST, 2.0f, 1.0f, -1.0f), 1.0e-4f);
        assertEquals(1.0f, LargeCrucibleHullRenderer.textureU(Direction.EAST, 2.0f, 1.0f, 2.0f), 1.0e-4f);
        assertEquals(1.0f, LargeCrucibleHullRenderer.textureU(Direction.WEST, -1.0f, 1.0f, -1.0f), 1.0e-4f);
        assertEquals(0.0f, LargeCrucibleHullRenderer.textureU(Direction.WEST, -1.0f, 1.0f, 2.0f), 1.0e-4f);
    }
}
