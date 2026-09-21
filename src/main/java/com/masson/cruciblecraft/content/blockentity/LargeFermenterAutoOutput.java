package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineAutoIo;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * GT6 {@code getOffset*(OPOS[mFacing], 5)}: fluids at controller Y, items at
 * Y+1, one block past the far 5x5 face.
 */
public final class LargeFermenterAutoOutput {
    private LargeFermenterAutoOutput() {}

    public static void pushItems(
            ProcessingMachineBlockEntity host, boolean pulse) {
        if (host.itemAutoOutputDisabled() || !host.hasOutputItems()) {
            return;
        }
        Level level = host.getLevel();
        if (level == null || (!pulse && level.getGameTime() % 200L != 5L)) {
            return;
        }
        IItemHandler dest = holeItems(host, 1);
        if (dest == null) {
            return;
        }
        ProcessingMachineAutoIo.moveItems(
                host.internalItemView(ProcessingMachineSpec.CapabilityAccess.OUTPUT),
                dest);
    }

    public static void pushFluids(ProcessingMachineBlockEntity host) {
        Level level = host.getLevel();
        if (level == null) {
            return;
        }
        for (ProcessingMachineSpec.TankSpec tankSpec
                : host.spec().fluids().outputs()) {
            FluidTank tank = host.tanks().get(tankSpec.index());
            FluidStack stored = tank.getFluid();
            if (stored.isEmpty()) {
                continue;
            }
            IFluidHandler dest = holeFluids(host, 0);
            if (dest == null) {
                continue;
            }
            FluidStack simulated = tank.drain(
                    stored, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) {
                continue;
            }
            int accepted = dest.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            FluidStack drained = tank.drain(
                    simulated.copyWithAmount(accepted),
                    IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }
            int filled = dest.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            if (filled < drained.getAmount()) {
                tank.fill(
                        drained.copyWithAmount(drained.getAmount() - filled),
                        IFluidHandler.FluidAction.EXECUTE);
            }
        }
    }

    public static IItemHandler holeItems(
            ProcessingMachineBlockEntity host, int localY) {
        Level level = host.getLevel();
        if (level == null) {
            return null;
        }
        Direction front = host.machineFront();
        return level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                hole(host, front, localY),
                destinationSide(front));
    }

    public static IFluidHandler holeFluids(
            ProcessingMachineBlockEntity host, int localY) {
        Level level = host.getLevel();
        if (level == null) {
            return null;
        }
        Direction front = host.machineFront();
        return level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                hole(host, front, localY),
                destinationSide(front));
    }

    /**
     * GT6 {@code WD.te(..., OPOS[mFacing], ...)}: the side on the target is
     * the offset direction, the face pointing away from the controller.
     */
    public static Direction destinationSide(Direction front) {
        return front.getOpposite();
    }

    public static BlockPos hole(
            ProcessingMachineBlockEntity host, Direction front, int localY) {
        return host.getBlockPos()
                .relative(front.getOpposite(), 5)
                .above(localY);
    }
}
