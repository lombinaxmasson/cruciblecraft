package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

/** Versioned selected chance-roll state, including the valid empty-roll case. */
public record ChanceOutputState(
        boolean valid,
        String recipeId,
        String recipeFingerprint,
        List<ItemStack> outputs) {
    static final int MAX_SAVED_OUTPUTS = 256;

    public ChanceOutputState {
        recipeId = recipeId == null ? "" : recipeId;
        recipeFingerprint = recipeFingerprint == null ? "" : recipeFingerprint;
        if (outputs.size() > MAX_SAVED_OUTPUTS) {
            throw new IllegalArgumentException(
                    "Chance output state has " + outputs.size()
                            + " stacks; maximum persisted count is "
                            + MAX_SAVED_OUTPUTS);
        }
        outputs = outputs.stream().map(ItemStack::copy).toList();
    }

    @Override public List<ItemStack> outputs() {
        return outputs.stream().map(ItemStack::copy).toList();
    }

    public boolean matches(String candidateRecipeId, String candidateFingerprint) {
        return valid
                && recipeId.equals(candidateRecipeId)
                && recipeFingerprint.equals(candidateFingerprint);
    }

    public void write(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean("rolled_outputs_valid", valid);
        tag.putString("selected_recipe_id", recipeId);
        tag.putString("selected_recipe_fingerprint", recipeFingerprint);
        tag.putInt("rolled_output_count", outputs.size());
        for (int index = 0; index < outputs.size(); index++) {
            tag.put("rolled_output_" + index, outputs.get(index).save(registries));
        }
    }

    public static ChanceOutputState read(
            CompoundTag tag,
            HolderLookup.Provider registries) {
        List<ItemStack> outputs = new ArrayList<>();
        int count = tag.getInt("rolled_output_count");
        boolean valid = tag.getBoolean("rolled_outputs_valid");
        if (count < 0 || count > MAX_SAVED_OUTPUTS) {
            // A corrupt count must not turn into a valid empty chance roll or
            // drive an unbounded NBT scan.
            count = 0;
            valid = false;
        }
        for (int index = 0; index < count; index++) {
            ItemStack output = ItemStack.parseOptional(
                    registries, tag.getCompound("rolled_output_" + index));
            if (!output.isEmpty()) {
                outputs.add(output);
            }
        }
        return new ChanceOutputState(
                valid,
                tag.getString("selected_recipe_id"),
                tag.getString("selected_recipe_fingerprint"),
                outputs);
    }
}
