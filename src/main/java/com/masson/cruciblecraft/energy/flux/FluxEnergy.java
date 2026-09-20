package com.masson.cruciblecraft.energy.flux;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** GT6 RF emit through NeoForge FE. RF amount is 1:1 with FE. */
public final class FluxEnergy {
    private FluxEnergy() {}

    public static int pushToSide(
            Level level,
            BlockPos position,
            Direction sideOutOf,
            int fe) {
        if (level == null
                || level.isClientSide
                || position == null
                || sideOutOf == null
                || fe <= 0) {
            return 0;
        }
        BlockPos target = position.relative(sideOutOf);
        if (!level.hasChunkAt(target)) {
            return 0;
        }
        IEnergyStorage storage = level.getCapability(
                Capabilities.EnergyStorage.BLOCK,
                target,
                sideOutOf.getOpposite());
        if (storage == null || !storage.canReceive()) {
            return 0;
        }
        return storage.receiveEnergy(fe, false);
    }
}
