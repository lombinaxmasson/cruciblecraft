package com.masson.cruciblecraft.compat.jade.observation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
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
        assertEquals(
                List.of(new CrucibleObservation.MetalAmount("copper", 144)),
                observation.metals().value());
        assertEquals("1", JadeDisplayUnits.formatIngotAmount(144));
        assertEquals("0.111", JadeDisplayUnits.formatIngotAmount(16));
        assertEquals("0.5", JadeDisplayUnits.formatIngotAmount(72));
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
    void reactorJadeIsHuAndMissingDataIsUnavailable() {
        ReactorCoreObservation missing =
                ReactorCoreObservation.fromServerData(new CompoundTag());
        assertFalse(missing.heatHu().available());
        assertFalse(missing.neutrons().available());
        assertFalse(missing.safety().available());
        assertFalse(missing.hasKelvinField());

        ReactorCoreObservation snapshot = ReactorCoreObservation.fromSnapshot(
                40L,
                8L,
                12,
                "cruciblecraft:distilled_water",
                1000,
                "cruciblecraft:steam",
                160,
                true,
                false,
                com.masson.cruciblecraft.nuclear.ReactorSafety.OK);
        assertEquals(40L, snapshot.heatHu().value());
        assertEquals(8L, snapshot.lastHeatHu().value());
        assertEquals("ok", snapshot.safety().value());
    }

    @Test
    void batteryJadeUsesStoredNotDisplayedEnergy() {
        BatteryObservation missing =
                BatteryObservation.fromServerData(new CompoundTag());
        assertFalse(missing.stored().available());
        BatteryObservation snapshot = BatteryObservation.fromSnapshot(
                "EU", 64L, 256L, 8L, 32L, 16L);
        assertEquals("EU", snapshot.energyType().value());
        assertEquals(64L, snapshot.stored().value());
        assertEquals(16L, snapshot.inputSize().value());
    }

    @Test
    void converterJadeLeavesMissingBuffersUnavailable() {
        ConverterObservation missing =
                ConverterObservation.fromServerData(new CompoundTag());
        assertFalse(missing.bufferStored().available());
        assertFalse(missing.activity().available());
        assertFalse(missing.accepts().available());
    }

    @Test
    void fillPercentClampsAndRounds() {
        assertEquals(0, JadeDisplayUnits.fillPercent(0.0f));
        assertEquals(100, JadeDisplayUnits.fillPercent(1.0f));
        assertEquals(33, JadeDisplayUnits.fillPercent(0.333f));
    }
}
