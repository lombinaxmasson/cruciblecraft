package com.masson.cruciblecraft.compat.jade.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.material.GT6ImportUnits;

import net.minecraft.nbt.CompoundTag;

class JadeObservationTest {
    @Test
    void crucibleConvertsCelsiusAtTheDisplayBoundaryOnly() {
        CrucibleObservation observation = CrucibleObservation.fromSnapshot(
                1_084.62f,
                40L,
                2_000.0f,
                0.5f,
                "molten",
                true,
                Map.of("copper", 144),
                144,
                576,
                false);
        assertEquals(
                GT6ImportUnits.celsiusToKelvin(1_084.62f),
                observation.temperatureKelvin().value(),
                1.0e-6);
        assertEquals(
                GT6ImportUnits.celsiusToKelvin(2_000.0f),
                observation.meltdownKelvin().value(),
                1.0e-6);
        assertEquals(50, observation.fillPercent().value());
        assertEquals("unavailable", observation.cacheDisplay());
        assertEquals("copper: 144 u", observation.contents().value());
    }

    @Test
    void missingCrucibleServerDataIsUnavailableNotZero() {
        CrucibleObservation observation =
                CrucibleObservation.fromServerData(new CompoundTag());
        assertFalse(observation.temperatureKelvin().available());
        assertFalse(observation.bufferedHeatHu().available());
        assertFalse(observation.meltdownKelvin().available());
        assertFalse(observation.fillPercent().available());
        assertEquals("unavailable", observation.cacheDisplay());
        assertEquals("unavailable", observation.renderKey());
    }

    @Test
    void transformerModeAndSidesComeFromReversedFlagNotFacingGuess() {
        TransformerObservation stepDown = TransformerObservation.fromSnapshot(
                "cruciblecraft:electric_transformer_lv_mv",
                "lv",
                "mv",
                false,
                32L,
                256L,
                true,
                Map.of(
                        "up", TransformerObservation.Side.input(128L),
                        "north", TransformerObservation.Side.output(32L, 4L)));
        assertEquals("step_down", stepDown.modeKey());
        assertTrue(stepDown.sides().get("up").input());
        assertEquals(128L, stepDown.sides().get("up").voltage());
        assertEquals(32L, stepDown.sides().get("north").voltage());
        assertEquals(4L, stepDown.sides().get("north").packetMultiplier());

        TransformerObservation stepUp = TransformerObservation.fromSnapshot(
                "cruciblecraft:electric_transformer_lv_mv",
                "lv",
                "mv",
                true,
                0L,
                256L,
                false,
                Map.of(
                        "up", TransformerObservation.Side.output(128L, 1L),
                        "north", TransformerObservation.Side.input(32L)));
        assertEquals("step_up", stepUp.modeKey());
        assertTrue(stepUp.modeKnown());
        assertFalse(stepUp.sides().get("up").input());
        assertEquals(128L, stepUp.sides().get("up").voltage());
        assertTrue(stepUp.sides().get("down").available() == false);
    }

    @Test
    void missingTransformerServerDataLeavesDynamicFieldsUnavailable() {
        CompoundTag empty = new CompoundTag();
        // Profile strings are static catalog data filled by fromBlockAndServerData
        // when the block is present; without tags the numeric fields stay empty.
        TransformerObservation observation = TransformerObservation.fromSnapshot(
                "cruciblecraft:electric_transformer_lv_mv",
                "lv",
                "mv",
                false,
                0L,
                0L,
                false,
                Map.of());
        CrucibleObservation missingHu = CrucibleObservation.fromServerData(empty);
        assertFalse(missingHu.bufferedHeatHu().available());
        assertTrue(observation.sides().get("east").available() == false);
    }

    @Test
    void fillPercentClampsAndRounds() {
        assertEquals(0, JadeDisplayUnits.fillPercent(0.0f));
        assertEquals(100, JadeDisplayUnits.fillPercent(1.0f));
        assertEquals(33, JadeDisplayUnits.fillPercent(0.333f));
    }
}
