package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.IntUnaryOperator;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import net.minecraft.world.item.ItemStack;

/** Shared stack-level recipe output chance policy. */
public final class ChanceOutputs {
    private ChanceOutputs() {}

    public static List<ItemStack> roll(
            List<ItemStack> outputs,
            List<Integer> chances,
            IntUnaryOperator randomBelow) {
        Objects.requireNonNull(outputs, "outputs");
        Objects.requireNonNull(chances, "chances");
        if (outputs.size() != chances.size()) {
            throw new IllegalArgumentException("Every item output requires one chance");
        }
        List<ItemStack> rolled = new ArrayList<>();
        for (int index = 0; index < outputs.size(); index++) {
            ItemStack output = roll(outputs.get(index), chances.get(index), randomBelow);
            if (!output.isEmpty()) {
                rolled.add(output);
            }
        }
        return List.copyOf(rolled);
    }

    public static ItemStack roll(
            ItemStack output,
            int chance,
            IntUnaryOperator randomBelow) {
        Objects.requireNonNull(output, "output");
        Objects.requireNonNull(randomBelow, "randomBelow");
        if (chance < 0 || chance > GTRecipe.GUARANTEED_CHANCE) {
            throw new IllegalArgumentException("Output chance is outside 0..10000");
        }
        if (chance == 0) {
            return ItemStack.EMPTY;
        }
        if (chance == GTRecipe.GUARANTEED_CHANCE) {
            return output.copy();
        }
        int value = randomBelow.applyAsInt(GTRecipe.GUARANTEED_CHANCE);
        if (value < 0 || value >= GTRecipe.GUARANTEED_CHANCE) {
            throw new IllegalArgumentException("RNG returned an out-of-range value");
        }
        return value < chance ? output.copy() : ItemStack.EMPTY;
    }
}
