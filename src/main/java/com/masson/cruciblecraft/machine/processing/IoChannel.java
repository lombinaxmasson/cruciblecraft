package com.masson.cruciblecraft.machine.processing;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import net.minecraft.core.Direction;

/**
 * One GT6 I/O channel (items, fluids, or energy). World-face access uses the
 * registered bitmask; {@code null} is GT6 {@code SIDE_ANY} after {@code SBIT_A}.
 */
public record IoChannel(
        int inputMask,
        int outputMask,
        Optional<MachineRelativeFace> autoInput,
        Optional<MachineRelativeFace> autoOutput)
        implements ProcessingMachineSpec.SideRule {

    public static final IoChannel NONE = new IoChannel(0, 0, Optional.empty(), Optional.empty());

    public IoChannel {
        if ((inputMask & ~MachineRelativeFace.ANY_MASK) != 0
                || (outputMask & ~MachineRelativeFace.ANY_MASK) != 0) {
            throw new IllegalArgumentException("I/O mask is out of range");
        }
        autoInput = Objects.requireNonNull(autoInput, "autoInput");
        autoOutput = Objects.requireNonNull(autoOutput, "autoOutput");
    }

    public static IoChannel items(int in, int autoIn, int out, int autoOut) {
        return new IoChannel(in, out, MachineRelativeFace.fromIndex(autoIn), MachineRelativeFace.fromIndex(autoOut));
    }

    public static IoChannel fluids(int in, int autoIn, int out, int autoOut) {
        return items(in, autoIn, out, autoOut);
    }

    public static IoChannel energy(int mask) {
        return new IoChannel(mask, 0, Optional.empty(), Optional.empty());
    }

    @Override
    public ProcessingMachineSpec.CapabilityAccess resolve(Direction front, Direction side) {
        boolean in = connected(inputMask, front, side);
        boolean out = connected(outputMask, front, side);
        if (in && out) {
            return ProcessingMachineSpec.CapabilityAccess.BOTH;
        }
        if (in) {
            return ProcessingMachineSpec.CapabilityAccess.INPUT;
        }
        if (out) {
            return ProcessingMachineSpec.CapabilityAccess.OUTPUT;
        }
        return ProcessingMachineSpec.CapabilityAccess.NONE;
    }

    public boolean hasAutoInput() {
        return autoInput.isPresent();
    }

    public boolean hasAutoOutput() {
        return autoOutput.isPresent();
    }

    public Optional<Direction> autoInputWorld(Direction front) {
        return autoInput.map(face -> face.toWorld(front));
    }

    public Optional<Direction> autoOutputWorld(Direction front) {
        return autoOutput.map(face -> face.toWorld(front));
    }

    public Optional<Direction> firstInputWorld(Direction front) {
        return firstWorld(front, inputMask);
    }

    public Optional<Direction> firstOutputWorld(Direction front) {
        return firstWorld(front, outputMask);
    }

    public boolean anySideInput() {
        return (effective(inputMask) & MachineRelativeFace.ANY_MASK)
                == MachineRelativeFace.ANY_MASK;
    }

    public boolean anySideOutput() {
        return (effective(outputMask) & MachineRelativeFace.ANY_MASK)
                == MachineRelativeFace.ANY_MASK;
    }

    public List<MachineRelativeFace> listedInputs() {
        return listed(inputMask);
    }

    public List<MachineRelativeFace> listedOutputs() {
        return listed(outputMask);
    }

    public List<MachineRelativeFace> listedEnergy() {
        return listed(inputMask);
    }

    public boolean tooltipUsesAnyForm(boolean input) {
        return (input ? inputMask : outputMask) == MachineRelativeFace.ANY_MASK;
    }

    public boolean energyTooltipUsesAnyForm() {
        return tooltipUsesAnyForm(true);
    }

    private static boolean connected(int mask, Direction front, Direction side) {
        if (mask == 0) {
            return false;
        }
        if (side == null) {
            return (effective(mask) & MachineRelativeFace.ANY_SIDE_BIT) != 0;
        }
        if (front == null || !front.getAxis().isHorizontal()) {
            return false;
        }
        return MachineRelativeFace.connected(mask, MachineRelativeFace.fromWorld(front, side));
    }

    private static int effective(int mask) {
        return mask == 0 ? 0 : mask | MachineRelativeFace.ANY_SIDE_BIT;
    }

    private static List<MachineRelativeFace> listed(int mask) {
        List<MachineRelativeFace> faces = new ArrayList<>();
        for (MachineRelativeFace face : MachineRelativeFace.values()) {
            if (MachineRelativeFace.connected(mask, face)) {
                faces.add(face);
            }
        }
        return List.copyOf(faces);
    }

    private static Optional<Direction> firstWorld(Direction front, int mask) {
        for (MachineRelativeFace face : MachineRelativeFace.values()) {
            if (MachineRelativeFace.connected(mask, face)) {
                return Optional.of(face.toWorld(front));
            }
        }
        return Optional.empty();
    }
}
