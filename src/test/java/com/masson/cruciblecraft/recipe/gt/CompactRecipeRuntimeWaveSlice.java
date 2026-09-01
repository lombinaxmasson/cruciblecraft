package com.masson.cruciblecraft.recipe.gt;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

/**
 * Wave-local composed v2 slice. Historical bath-mte integrated load must keep the
 * assembler-through-bath-mte 13-group set even after later cards append groups.
 */
public final class CompactRecipeRuntimeWaveSlice {
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
        String waveId = group.get("wave_id").getAsString();
        if (waveId.length() < 2 || waveId.charAt(0) != 'T') {
            return false;
        }
        try {
            return Integer.parseInt(waveId.substring(1)) > 46;
        } catch (NumberFormatException ignored) {
            return false;
        }
    }
}
