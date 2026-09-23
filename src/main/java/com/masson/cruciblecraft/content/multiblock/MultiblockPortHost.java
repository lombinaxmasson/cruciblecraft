package com.masson.cruciblecraft.content.multiblock;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;

import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Capability host bridged by multiblock ports. Processing-machine
 * controllers implement it by delegating to their spec; conversion and
 * storage controllers (large boiler, tank) implement it directly.
 * Each physical port owns a persistent {@link PortStore}. The controller
 * remains the transaction owner and may aggregate those stores for processing,
 * but a port capability never aliases another port's inventory or tanks.
 */
public interface MultiblockPortHost extends IEnergyHandler {
    ItemStackHandler inventory();

    List<FluidTank> tanks();

    /** Slots the ports may insert into (inputs only). */
    List<Integer> itemInputSlots();

    /** Slots the ports may extract from (outputs only). */
    List<Integer> itemOutputSlots();

    /** Tank indices the ports may fill (inputs only). */
    List<Integer> fluidInputTanks();

    /** Tank indices the ports may drain (outputs only). */
    List<Integer> fluidOutputTanks();

    BlockState blockState();

    /**
     * Energy inserted by a bound ENERGY_INPUT part. Controllers whose hull
     * faces do not accept energy still take packets from 18101.
     */
    default long insertFromMultiblockPort(
            EnergyType type,
            long size,
            long amount,
            boolean simulate) {
        for (Direction side : Direction.values()) {
            if (handles(type, side)) {
                return insert(type, size, amount, side, simulate);
            }
        }
        return 0L;
    }
}
