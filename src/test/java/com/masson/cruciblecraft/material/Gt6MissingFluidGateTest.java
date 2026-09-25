package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

class Gt6MissingFluidGateTest {
    @Test
    void namedFluidsKeepGt6PhysicalProperties() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/cruciblecraft/gt6_named_fluid_gate.json")))
                .getAsJsonObject();
        assertEquals(4, root.getAsJsonArray("fluids").size());
        Map<String, int[]> expected = Map.of(
                "charged_matter", new int[] {1, -5000, 1000, 15},
                "fiery_blood", new int[] {1500, 1000, 1000, 10},
                "fiery_tears", new int[] {1500, 1000, 1000, 10},
                "blueberry_juice", new int[] {300, 1000, 1000, 0});
        for (var element : root.getAsJsonArray("fluids")) {
            JsonObject fluid = element.getAsJsonObject();
            int[] numbers = expected.get(fluid.get("id").getAsString());
            assertEquals(numbers[0], fluid.get("temperature_kelvin").getAsInt());
            assertEquals(numbers[1], fluid.get("density").getAsInt());
            assertEquals(numbers[2], fluid.get("viscosity").getAsInt());
            assertEquals(numbers[3], fluid.get("light_level").getAsInt());
        }
    }

    @Test
    void iceAndPetrotheumStayOnTheirMaterials() throws Exception {
        JsonObject root = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/cruciblecraft/gt6_material_fluid_gate.json")))
                .getAsJsonObject();
        assertEquals(2, root.getAsJsonArray("fluids").size());
        JsonObject plastic = JsonParser.parseString(Files.readString(Path.of(
                "src/main/resources/data/cruciblecraft/materials/plastic.json")))
                .getAsJsonObject();
        assertTrue(plastic.get("molten_fluid").getAsBoolean());
    }
}
