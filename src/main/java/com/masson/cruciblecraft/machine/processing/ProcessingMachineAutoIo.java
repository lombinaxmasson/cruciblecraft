package com.masson.cruciblecraft.machine.processing;

import java.util.Objects;

import com.masson.cruciblecraft.logistics.hopper.HopperTransferCore;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;

/** GT6 {@code ST.moveAll} / {@code FL.move} for processing-machine auto I/O. */
public final class ProcessingMachineAutoIo {
    private ProcessingMachineAutoIo() {}

    public static ProcessingMachineSpec.CapabilityAccess overlayFluidAccess(
            ProcessingMachineSpec.CapabilityAccess access,
            Direction side,
            Direction autoOutput,
            boolean autoOutputDisabled) {
        Objects.requireNonNull(access, "access");
        if (autoOutputDisabled
                || side == null
                || autoOutput == null
                || side != autoOutput) {
            return access;
        }
        return switch (access) {
            case INPUT -> ProcessingMachineSpec.CapabilityAccess.NONE;
            case BOTH -> ProcessingMachineSpec.CapabilityAccess.OUTPUT;
            case OUTPUT, NONE -> access;
        };
    }

    public static int moveItems(IItemHandler from, IItemHandler to) {
        if (from == null || to == null) {
            return 0;
        }
        return HopperTransferCore.push(from, to, 0, false, false);
    }

    public static int moveFluids(IFluidHandler from, IFluidHandler to) {
        if (from == null || to == null) {
            return 0;
        }
        int moved = 0;
        for (int tank = 0; tank < from.getTanks(); tank++) {
            FluidStack stored = from.getFluidInTank(tank);
            if (stored.isEmpty()) {
                continue;
            }
            FluidStack simulated = from.drain(stored, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty()) {
                continue;
            }
            int accepted = to.fill(simulated, IFluidHandler.FluidAction.SIMULATE);
            if (accepted <= 0) {
                continue;
            }
            FluidStack drained = from.drain(
                    simulated.copyWithAmount(accepted),
                    IFluidHandler.FluidAction.EXECUTE);
            if (drained.isEmpty()) {
                continue;
            }
            int filled = to.fill(drained, IFluidHandler.FluidAction.EXECUTE);
            if (filled < drained.getAmount()) {
                from.fill(
                        drained.copyWithAmount(drained.getAmount() - filled),
                        IFluidHandler.FluidAction.EXECUTE);
            }
            moved += filled;
        }
        return moved;
    }

    public static IItemHandler neighborItems(Level level, BlockPos pos, Direction machineSide) {
        if (level == null || pos == null || machineSide == null) {
            return null;
        }
        return level.getCapability(
                Capabilities.ItemHandler.BLOCK,
                pos.relative(machineSide),
                machineSide.getOpposite());
    }

    public static IFluidHandler neighborFluids(Level level, BlockPos pos, Direction machineSide) {
        if (level == null || pos == null || machineSide == null) {
            return null;
        }
        return level.getCapability(
                Capabilities.FluidHandler.BLOCK,
                pos.relative(machineSide),
                machineSide.getOpposite());
    }
}
