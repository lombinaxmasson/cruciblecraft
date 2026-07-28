package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialForm;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

class MaterialColorsTest {
    @Test
    void acceptsAndParsesOnlyStrictSixDigitRgb() {
        assertEquals(0x12ABEF, MaterialColors.parse("#12ABEF"));
        assertThrows(IllegalArgumentException.class, () -> MaterialColors.parse("#FFF"));
        assertThrows(IllegalArgumentException.class, () -> MaterialColors.parse("12ABEF"));
        assertThrows(IllegalArgumentException.class, () -> MaterialColors.parse("#12ABEG"));
    }

    @Test
    void definitionConstructionAndCodecRejectInvalidColors() {
        assertThrows(IllegalArgumentException.class, () -> definition("#FFF"));
        assertThrows(IllegalArgumentException.class, () -> definition("FFFFFF"));

        var decoded = MaterialDefinition.CODEC.parse(
                JsonOps.INSTANCE,
                JsonParser.parseString("""
                        {
                          "id": "bad_color",
                          "color": "#FFF",
                          "thermal": { "melting_point": 100 }
                        }
                        """));
        assertTrue(decoded.error().isPresent());
    }

    private static MaterialDefinition definition(String color) {
        return new MaterialDefinition(
                "test",
                "test",
                Optional.empty(),
                0,
                color,
                "metallic",
                List.of(MaterialForm.DUST),
                Map.of(),
                new ThermalProperties(100),
                false,
                Map.of(),
                false);
    }
}
