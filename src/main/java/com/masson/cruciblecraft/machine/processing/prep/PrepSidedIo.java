package com.masson.cruciblecraft.machine.processing.prep;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;

import net.minecraft.core.Direction;

final class PrepSidedIo {
    private PrepSidedIo() {}

    static ProcessingMachineSpec.SideRule none() {
        return (front, side) -> ProcessingMachineSpec.CapabilityAccess.NONE;
    }

    static ProcessingMachineSpec.SideRule backEnergy() {
        return (front, side) -> side != null && side == front.getOpposite()
                ? ProcessingMachineSpec.CapabilityAccess.INPUT
                : ProcessingMachineSpec.CapabilityAccess.NONE;
    }

    static ProcessingMachineSpec.SideRule leftRightEnergy() {
        return (front, side) -> {
            if (side == null || !side.getAxis().isHorizontal()) {
                return ProcessingMachineSpec.CapabilityAccess.NONE;
            }
            if (side == front || side == front.getOpposite()) {
                return ProcessingMachineSpec.CapabilityAccess.NONE;
            }
            return ProcessingMachineSpec.CapabilityAccess.INPUT;
        };
    }

    static ProcessingMachineSpec.SideRule leftInRightOut() {
        return (front, side) -> {
            if (side == null) {
                return ProcessingMachineSpec.CapabilityAccess.NONE;
            }
            if (side == front.getCounterClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.INPUT;
            }
            if (side == front.getClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.OUTPUT;
            }
            return ProcessingMachineSpec.CapabilityAccess.NONE;
        };
    }

    static ProcessingMachineSpec.SideRule topInBottomOut() {
        return (front, side) -> {
            if (side == Direction.UP) {
                return ProcessingMachineSpec.CapabilityAccess.INPUT;
            }
            if (side == Direction.DOWN) {
                return ProcessingMachineSpec.CapabilityAccess.OUTPUT;
            }
            return ProcessingMachineSpec.CapabilityAccess.NONE;
        };
    }

    static ProcessingMachineSpec.SideRule leftUpInRightDownOut() {
        return (front, side) -> {
            if (side == null) {
                return ProcessingMachineSpec.CapabilityAccess.NONE;
            }
            if (side == Direction.UP || side == front.getCounterClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.INPUT;
            }
            if (side == Direction.DOWN || side == front.getClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.OUTPUT;
            }
            return ProcessingMachineSpec.CapabilityAccess.NONE;
        };
    }

    static ProcessingMachineSpec.SideRule leftUpIn() {
        return (front, side) -> {
            if (side == null) {
                return ProcessingMachineSpec.CapabilityAccess.NONE;
            }
            if (side == Direction.UP || side == front.getCounterClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.INPUT;
            }
            return ProcessingMachineSpec.CapabilityAccess.NONE;
        };
    }

    static ProcessingMachineSpec.SideRule leftOrUpInRightOut() {
        return (front, side) -> {
            if (side == null) {
                return ProcessingMachineSpec.CapabilityAccess.NONE;
            }
            if (side == Direction.UP || side == front.getCounterClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.INPUT;
            }
            if (side == front.getClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.OUTPUT;
            }
            return ProcessingMachineSpec.CapabilityAccess.NONE;
        };
    }

    static ProcessingMachineSpec.SideRule upInLeftOut() {
        return (front, side) -> {
            if (side == Direction.UP) {
                return ProcessingMachineSpec.CapabilityAccess.INPUT;
            }
            if (side != null && side == front.getCounterClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.OUTPUT;
            }
            return ProcessingMachineSpec.CapabilityAccess.NONE;
        };
    }

    static ProcessingMachineSpec.SideRule upInRightOut() {
        return (front, side) -> {
            if (side == Direction.UP) {
                return ProcessingMachineSpec.CapabilityAccess.INPUT;
            }
            if (side != null && side == front.getClockWise()) {
                return ProcessingMachineSpec.CapabilityAccess.OUTPUT;
            }
            return ProcessingMachineSpec.CapabilityAccess.NONE;
        };
    }

    static ProcessingMachineSpec.SideRule upDownIn() {
        return (front, side) -> side == Direction.UP || side == Direction.DOWN
                ? ProcessingMachineSpec.CapabilityAccess.INPUT
                : ProcessingMachineSpec.CapabilityAccess.NONE;
    }
}
