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
            groups.add(group("bath/mte", "cruciblecraft:bath_mte_group_" + index));
        }
        groups.add(group("bath/identity", "cruciblecraft:bath_identity_fake"));
        groups.add(group("T47", "cruciblecraft:t47_bath_exact"));
        JsonArray sliced = CompactRecipeRuntimeWaveSlice.groupsThroughBathMte(groups);
        assertEquals(13, sliced.size());
        for (int index = 0; index < sliced.size(); index++) {
            assertFalse(
                    CompactRecipeRuntimeWaveSlice.afterBathMte(
                            sliced.get(index).getAsJsonObject()));
        }
        assertTrue(CompactRecipeRuntimeWaveSlice.afterBathMte(groups.get(13).getAsJsonObject()));
        assertTrue(CompactRecipeRuntimeWaveSlice.afterBathMte(groups.get(14).getAsJsonObject()));
    }

    @Test
    void composedV2T47T48T49StayOutOfAssemblerThroughBathMteSlice() {
        JsonArray groups = new JsonArray();
        for (int index = 0; index < 12; index++) {
            groups.add(group("T37", "cruciblecraft:historical_" + index));
        }
        groups.add(group("T46", "cruciblecraft:t46_bath_mte"));
        groups.add(group("T47", "cruciblecraft:t47_bath_exact"));
        groups.add(group("T48", "cruciblecraft:t48_bath_exact"));
        groups.add(group("T49", "cruciblecraft:t49_bath_exact_multi"));
        JsonArray sliced = CompactRecipeRuntimeWaveSlice.groupsThroughBathMte(groups);
        assertEquals(13, sliced.size());
        assertEquals(
                "cruciblecraft:t46_bath_mte",
                sliced.get(12).getAsJsonObject().get("publication_group").getAsString());
        assertTrue(CompactRecipeRuntimeWaveSlice.afterBathMte(groups.get(13).getAsJsonObject()));
        assertTrue(CompactRecipeRuntimeWaveSlice.afterBathMte(groups.get(14).getAsJsonObject()));
        assertTrue(CompactRecipeRuntimeWaveSlice.afterBathMte(groups.get(15).getAsJsonObject()));
    }

    @Test
    void missingWaveIdStaysInHistoricalSlice() {
        JsonArray groups = new JsonArray();
        JsonObject legacy = new JsonObject();
        legacy.addProperty("publication_group", "cruciblecraft:legacy");
        groups.add(legacy);
        groups.add(group("bath/remainder", "cruciblecraft:bath/remainder/exact"));
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
