package com.masson.cruciblecraft.material;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GT6ImportUnitsTest {
    @Test
    void knownCopperIronAndTinTemperaturesConvertBetweenCelsiusAndKelvin() {
        assertEquals(1_357.77, GT6ImportUnits.celsiusToKelvin(1_084.62), 1.0e-9);
        assertEquals(1_811.15, GT6ImportUnits.celsiusToKelvin(1_538.0), 1.0e-9);
        assertEquals(505.08, GT6ImportUnits.celsiusToKelvin(231.93), 1.0e-9);
        assertEquals(1_084.62, GT6ImportUnits.kelvinToCelsius(1_357.77), 1.0e-9);
        assertEquals(1_358, GT6ImportUnits.celsiusToRoundedKelvin(1_084.62));
        assertEquals(1_811, GT6ImportUnits.celsiusToRoundedKelvin(1_538.0));
        assertEquals(505, GT6ImportUnits.celsiusToRoundedKelvin(231.93));
    }

    @Test
    void exactQuantityConversionsRoundTrip() {
        assertEquals(144, GT6ImportUnits.gt6UToCcMaterialUnits(648_648_000));
        assertEquals(648_648_000, GT6ImportUnits.ccMaterialUnitsToGt6U(144));
        assertEquals(0, GT6ImportUnits.gt6UToCcMaterialUnits(0));
    }

    @Test
    void quantityConversionsRejectFractionsNegativesAndOverflow() {
        assertThrows(
                ArithmeticException.class,
                () -> GT6ImportUnits.gt6UToCcMaterialUnits(648_648_001));
        assertThrows(
                IllegalArgumentException.class,
                () -> GT6ImportUnits.gt6UToCcMaterialUnits(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> GT6ImportUnits.ccMaterialUnitsToGt6U(-1));
        assertThrows(
                ArithmeticException.class,
                () -> GT6ImportUnits.ccMaterialUnitsToGt6U(
                        Long.MAX_VALUE / GT6ImportUnits.GT6_U_PER_CC_UNIT + 1));
    }

    @Test
    void temperatureConversionsRejectNonFiniteAndImpossibleAbsoluteValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> GT6ImportUnits.celsiusToKelvin(Double.NaN));
        assertThrows(
                IllegalArgumentException.class,
                () -> GT6ImportUnits.kelvinToCelsius(Double.POSITIVE_INFINITY));
        assertThrows(
                IllegalArgumentException.class,
                () -> GT6ImportUnits.kelvinToCelsius(-1));
        assertThrows(
                IllegalArgumentException.class,
                () -> GT6ImportUnits.celsiusToKelvin(-274));
    }
}
