package com.masson.cruciblecraft.material.def;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

class ThermalPropertiesTest {
    @Test
    void codecNormalizesUnknownGt6ThermalValues() {
        ThermalProperties thermal = ThermalProperties.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString("""
                                {
                                  "melting_point": 1000,
                                  "boiling_point": 900,
                                  "density": 0
                                }
                                """))
                .getOrThrow();

        assertEquals(1000.0, thermal.meltingPoint());
        assertEquals(2000.0, thermal.boilingPoint());
        assertEquals(1.0, thermal.density());
    }

    @Test
    void directRuntimeConstructionRemainsStrict() {
        assertDoesNotThrow(
                () -> new ThermalProperties(1000.0, 2000.0, 1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ThermalProperties(Double.NaN, 2000.0, 1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ThermalProperties(
                        1000.0, Double.POSITIVE_INFINITY, 1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ThermalProperties(1000.0, 900.0, 1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ThermalProperties(1000.0, 1000.0, 1.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ThermalProperties(1000.0, 2000.0, 0.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ThermalProperties(1000.0, 2000.0, -1.0));
    }
}
