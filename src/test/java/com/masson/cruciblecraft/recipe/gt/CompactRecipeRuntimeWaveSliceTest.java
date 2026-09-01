package com.masson.cruciblecraft.recipe.gt;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompactRecipeRuntimeWaveSliceTest {
    @Test
    void extraComposedGroupKeepsAssemblerThroughBathMteSliceAt13() {
        JsonArray groups = new JsonArray();
        for (int index = 0; index < 13; index++) {
            groups.add(group("T46", "cruciblecraft:t46_group_" + index));
        }
        groups.add(group("T48", "cruciblecraft:t48_fake"));
        JsonArray sliced = CompactRecipeRuntimeWaveSlice.groupsThroughBathMte(groups);
        assertEquals(13, sliced.size());
        for (int index = 0; index < sliced.size(); index++) {
            assertFalse(
                    CompactRecipeRuntimeWaveSlice.afterBathMte(
                            sliced.get(index).getAsJsonObject()));
        }
        assertTrue(CompactRecipeRuntimeWaveSlice.afterBathMte(groups.get(13).getAsJsonObject()));
    }

    @Test
    void missingWaveIdStaysInHistoricalSlice() {
        JsonArray groups = new JsonArray();
        JsonObject legacy = new JsonObject();
        legacy.addProperty("publication_group", "cruciblecraft:legacy");
        groups.add(legacy);
        groups.add(group("T47", "cruciblecraft:bath/remainder/exact"));
        JsonArray sliced = CompactRecipeRuntimeWaveSlice.groupsThroughBathMte(groups);
        assertEquals(1, sliced.size());
        assertEquals(
                "cruciblecraft:legacy",
                sliced.get(0).getAsJsonObject().get("publication_group").getAsString());
    }

    private static JsonObject group(String waveId, String publicationGroup) {
        JsonObject group = new JsonObject();
        group.addProperty("wave_id", waveId);
        group.addProperty("publication_group", publicationGroup);
        return group;
    }
}
