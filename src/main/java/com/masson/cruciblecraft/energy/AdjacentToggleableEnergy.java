package com.masson.cruciblecraft.energy;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.blockentity.ElectricMotorBlockEntity;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.registry.ModCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * GT6 {@code ITileEntityAdjacentOnOff}: turn an adjacent emitter on or off
 * when it can emit {@code type} into {@code face}.
 */
public final class AdjacentToggleableEnergy {
    private AdjacentToggleableEnergy() {}

    public static void setOnOff(
            Level level,
            BlockPos position,
            Direction face,
            EnergyType type,
            boolean on) {
        if (level == null || level.isClientSide || !level.hasChunkAt(position)) {
            return;
        }
        IEnergyHandler handler = level.getCapability(
                ModCapabilities.ENERGY, position, face);
        if (handler == null || !handler.handles(type, face)) {
            return;
        }
        BlockEntity blockEntity = level.getBlockEntity(position);
        if (blockEntity instanceof MachineCoverHost host
                && host.switchableOnOff()) {
            host.setStateOnOff(on);
            return;
        }
        if (blockEntity instanceof ElectricMotorBlockEntity motor) {
            motor.setAdjacentOnOff(on);
        }
    }
}
