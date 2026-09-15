package com.masson.cruciblecraft.content.mold;

import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * GT6 {@code ITileEntityMold}: a side that can accept a molten material stack
 * from a crucible or faucet.
 */
public interface MoldHost {
    boolean isMoldInputSide(Direction side);

    float moldMaxTemperatureCelsius();

    int moldRequiredMaterialUnits();

    /**
     * @return units subtracted from the offered stack; 0 if the mold refused
     */
    int fillMold(String materialId, int availableUnits, float temperature, Direction side);

    default ItemStack takeOutput(Player player, boolean causeDamage) {
        return ItemStack.EMPTY;
    }
}
