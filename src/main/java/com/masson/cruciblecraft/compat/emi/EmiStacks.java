package com.masson.cruciblecraft.compat.emi;

import dev.emi.emi.api.stack.EmiStack;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * EMI round-trips non-empty component patches through SNBT. Empty patches
 * still encode as {@code {}}, which {@code StringNbtReader} then rejects as
 * "Error parsing NBT in deserialized stack".
 */
final class EmiStacks {
    private EmiStacks() {}

    static EmiStack ofItem(ItemStack stack) {
        if (stack.isEmpty()) {
            return EmiStack.EMPTY;
        }
        if (stack.getComponentsPatch().isEmpty()) {
            return EmiStack.of(stack.getItem(), stack.getCount());
        }
        return EmiStack.of(stack);
    }

    static EmiStack ofFluid(FluidStack stack) {
        if (stack.isEmpty()) {
            return EmiStack.EMPTY;
        }
        if (stack.getComponentsPatch().isEmpty()) {
            return EmiStack.of(stack.getFluid(), stack.getAmount());
        }
        return EmiStack.of(
                stack.getFluid(),
                stack.getComponentsPatch(),
                stack.getAmount());
    }
}
