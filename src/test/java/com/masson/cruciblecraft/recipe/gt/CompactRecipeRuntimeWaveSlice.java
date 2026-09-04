package com.masson.cruciblecraft.recipe.gt;

import java.util.Set;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Wave-local composed v2 slice. Historical bath-mte integrated load must keep the
 * assembler-through-bath-mte group set even after later cards append groups.
 */
public final class CompactRecipeRuntimeWaveSlice {
    private static final Set<String> AFTER_BATH_MTE = Set.of(
            "bath/remainder",
            "bath/identity",
            "bath/tiny-purified",
            "bath/tiny_purified");

    private CompactRecipeRuntimeWaveSlice() {}

    public static JsonArray groupsThroughBathMte(JsonArray allGroups) {
        JsonArray selected = new JsonArray();
        for (int index = 0; index < allGroups.size(); index++) {
            JsonObject group = allGroups.get(index).getAsJsonObject();
            if (afterBathMte(group)) {
                continue;
            }
            selected.add(group);
        }
        return selected;
    }

    public static boolean afterBathMte(JsonObject group) {
        if (!group.has("wave_id") || group.get("wave_id").isJsonNull()) {
            return false;
        }
        return AFTER_BATH_MTE.contains(group.get("wave_id").getAsString());
    }
}
