package com.masson.cruciblecraft.content.mold;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.content.blockentity.LargeCrucibleBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;

/**
 * GT6 {@code ITileEntityMold}: a side that can accept a molten material stack
 * from a crucible or faucet.
 */
public interface MoldHost {
    @Nullable
    static MoldHost at(BlockGetter level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MoldHost host) {
            return host;
        }
        return LargeCrucibleBlockEntity.pourHostAtWall(level, pos);
    }

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
