package com.masson.cruciblecraft.heat;

import java.util.Optional;

import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.GTRecipeQuery;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModRecipeMaps;

import net.minecraft.world.item.ItemStack;

/** Executes passive, data-defined material cooling transformations. */
public final class MaterialItemCooling {
    private MaterialItemCooling() {}

    public static Optional<ItemStack> coolIfReady(
            ItemStack stack,
            long gameTime) {
        if (stack.isEmpty() || !ItemHeat.isCooled(stack, gameTime)) {
            return Optional.empty();
        }
        return ModRecipeMaps.COOLING.find(
                        GTRecipeQuery.items(stack))
                .flatMap(recipe -> apply(stack, recipe));
    }

    static Optional<ItemStack> apply(ItemStack input, GTRecipe recipe) {
        if (recipe.itemInputs().size() != 1
                || recipe.itemInputCounts().getFirst() != 1
                || recipe.itemOutputs().size() != 1
                || !recipe.fluidInputs().isEmpty()
                || !recipe.fluidOutputs().isEmpty()
                || recipe.outputChances().getFirst()
                        != GTRecipe.GUARANTEED_CHANCE) {
            return Optional.empty();
        }
        ItemStack output = recipe.itemOutputs().getFirst();
        output.applyComponents(input.getComponentsPatch());
        output.remove(ModComponents.HEAT.get());
        output.setCount(Math.multiplyExact(
                input.getCount(),
                output.getCount()));
        return output.getCount() <= output.getMaxStackSize()
                ? Optional.of(output)
                : Optional.empty();
    }
}
