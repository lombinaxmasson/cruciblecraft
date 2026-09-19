package com.masson.cruciblecraft.energy.drive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.Test;

class RotationEngineConversionTest {
    private static RotationEngineCatalog.Profile brass() {
        return RotationEngineCatalog.require(
                ResourceLocation.parse("cruciblecraft:brass/rotation_engine"));
    }

    @Test
    void brassFullBufferEmitsSixteenTimesTwoAndWastes() {
        RotationEngineCatalog.Profile brass = brass();
        assertEquals(32L, brass.inputRec());
        assertEquals(16L, brass.outputRec());
        assertEquals(64L, brass.capacity());
        RotationEngineConversion.Tick tick = RotationEngineConversion.tick(
                64L, brass, 0);
        assertTrue(tick.canEmit());
        assertFalse(tick.overloaded());
        assertEquals(32L, tick.outputSize());
        assertEquals(0L, tick.storedAfterWaste());
    }

    @Test
    void halfBufferStillMeetsMinimumKu() {
        RotationEngineCatalog.Profile brass = brass();
        RotationEngineConversion.Tick tick = RotationEngineConversion.tick(
                16L, brass, 0);
        assertTrue(tick.canEmit());
        assertEquals(8L, tick.outputSize());
        assertEquals(0L, tick.storedAfterWaste());
    }

    @Test
    void belowOutputMinDoesNotEmitButStillWastes() {
        RotationEngineCatalog.Profile brass = brass();
        RotationEngineConversion.Tick tick = RotationEngineConversion.tick(
                8L, brass, 0);
        assertFalse(tick.canEmit());
        assertEquals(0L, tick.outputSize());
        assertEquals(0L, tick.storedAfterWaste());
    }

    @Test
    void overshootingOutputMaxOverloads() {
        RotationEngineCatalog.Profile brass = brass();
        RotationEngineConversion.Tick tick = RotationEngineConversion.tick(
                96L, brass, 0);
        assertTrue(tick.overloaded());
        assertFalse(tick.canEmit());
        assertEquals(0L, tick.storedAfterWaste());
    }

    @Test
    void unitsMatchesGt6InputToOutputRatio() {
        assertEquals(
                32L,
                RotationEngineConversion.units(64L, 32L, 16L, false));
        assertEquals(
                64L,
                RotationEngineConversion.units(64L, 16L, 16L, true));
    }
}
