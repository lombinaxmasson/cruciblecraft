package com.masson.cruciblecraft.energy.rotation;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.energy.EnergyEmitter;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * RU-only transfer boundary.
 *
 * <p>The conservation commit is shared with the generic emitter, while axle
 * and gearbox blocks remain solely responsible for rotational topology. KU
 * and EU callers cannot enter this path.
 */
public final class RotationalEnergyTransfer {
    public static long emit(
            Level level,
            BlockPos position,
            IEnergyHandler source,
            Direction output) {
        return EnergyEmitter.emit(
                level,
                position,
                source,
                EnergyType.KINETIC_ROTATION,
                output);
    }

    private RotationalEnergyTransfer() {}
}
