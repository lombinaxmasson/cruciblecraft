package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.content.blockentity.CokeOvenBlockEntity;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class CokeOvenProcessTest {
    @Test
    void activeProcessingConsumesOneHuPerTick() {
        assertEquals(1.0, CokeOvenBlockEntity.PROCESS_HEAT_PER_TICK);
    }

    @Test
    void fullItemOrFluidOutputPausesTheProcess() {
        assertTrue(CokeOvenProcess.hasItemCapacity(63, 64, 1, true));
        assertFalse(CokeOvenProcess.hasItemCapacity(64, 64, 1, true));
        assertFalse(CokeOvenProcess.hasItemCapacity(1, 64, 1, false));
        assertTrue(CokeOvenProcess.hasFluidCapacity(31_500, 32_000, 500));
        assertFalse(CokeOvenProcess.hasFluidCapacity(31_501, 32_000, 500));
    }

    @Test
    void progressStopsAtTheRecipeDuration() {
        assertEquals(1, CokeOvenProcess.advance(0, 3_600));
        assertEquals(3_600, CokeOvenProcess.advance(3_599, 3_600));
        assertEquals(3_600, CokeOvenProcess.advance(3_600, 3_600));
    }

    @Test
    void defaultCoalRecipeUsesGtScaledOutputs() throws IOException {
        Path recipePath = Path.of(
                "src/main/resources/data/cruciblecraft/recipe/coke_oven/coal.json");
        var recipe = JsonParser.parseString(Files.readString(recipePath)).getAsJsonObject();
        assertEquals(3_600, recipe.get("duration").getAsInt());
        assertEquals(
                "cruciblecraft:coal_coke",
                recipe.getAsJsonObject("output").get("id").getAsString());
        assertEquals(
                "cruciblecraft:creosote",
                recipe.getAsJsonObject("fluid_output").get("id").getAsString());
        assertEquals(
                500,
                recipe.getAsJsonObject("fluid_output").get("amount").getAsInt());
    }
}
