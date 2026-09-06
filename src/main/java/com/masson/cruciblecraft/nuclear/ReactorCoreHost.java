package com.masson.cruciblecraft.nuclear;

import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/** Mutable GT6 reactor-core host used by rod physics. */
public interface ReactorCoreHost {
    ReactorCoolant coolant();

    int oldNeutrons(int slot);

    void addNeutrons(int slot, long amount);

    void addHeat(long amount);

    void replaceRod(int slot, ItemStack replacement);

    default FluidStack coolantStack() {
        return FluidStack.EMPTY;
    }
}
