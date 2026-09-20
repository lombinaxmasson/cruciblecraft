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
 * GT6 backside-hole auto-output: {@code getOffset*(mFacing, 3)} is three
 * blocks behind the controller, past the far 3x3 face.
 */
public final class DistillationTowerAutoOutput {
    private DistillationTowerAutoOutput() {}

    public static void pushItems(
            ProcessingMachineBlockEntity host, boolean pulse) {
        if (host.itemAutoOutputDisabled() || !host.hasOutputItems()) {
            return;
        }
        Level level = host.getLevel();
        if (level == null || (!pulse && level.getGameTime() % 200L != 5L)) {
            return;
        }
        IItemHandler dest = holeItems(host, 0);
        if (dest == null) {
            return;
        }
        ProcessingMachineAutoIo.moveItems(
                host.internalItemView(ProcessingMachineSpec.CapabilityAccess.OUTPUT),
                dest);
    }

    public static void pushFluids(
            ProcessingMachineBlockEntity host,
            DistillationTowerFluidRouting.Kind kind) {
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
            IFluidHandler dest = holeFluids(
                    host, DistillationTowerFluidRouting.localY(kind, stored));
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
                front);
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
                front);
    }

    public static BlockPos hole(
            ProcessingMachineBlockEntity host, Direction front, int localY) {
        return host.getBlockPos()
                .relative(front.getOpposite(), 3)
                .above(localY);
    }
}
