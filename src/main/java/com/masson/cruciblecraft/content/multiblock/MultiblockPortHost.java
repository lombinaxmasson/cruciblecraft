package com.masson.cruciblecraft.content.multiblock;

import java.util.List;

import com.masson.cruciblecraft.api.energy.IEnergyHandler;

import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Capability host bridged by multiblock ports. Processing-machine
 * controllers implement it by delegating to their spec; conversion and
 * storage controllers (large boiler, tank) implement it directly.
 * Supply is one item/fluid view per shared host regardless of how many
 * physical port blocks bridge it.
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
}
