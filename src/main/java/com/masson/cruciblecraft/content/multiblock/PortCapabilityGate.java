package com.masson.cruciblecraft.content.multiblock;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;

/**
 * Directional capability contract for a physical multiblock port.
 *
 * <p>A shared {@link MultiblockPortHost} still owns one inventory/tank view.
 * This class only decides which operations a particular structure cell may
 * expose through that view.</p>
 */
public final class PortCapabilityGate {
    private PortCapabilityGate() {}

    public static boolean itemInsert(PortType type) {
        return type == PortType.ITEM_FLUID
                || type == PortType.ITEM_FLUID_IN
                || type == PortType.ITEM_FLUID_ENERGY_IN
                || type == PortType.ITEM_FLUID_ENERGY;
    }

    public static boolean itemExtract(PortType type) {
        return type == PortType.ITEM_FLUID
                || type == PortType.ITEM_FLUID_OUT
                || type == PortType.ITEM_FLUID_ENERGY;
    }

    public static boolean fluidFill(PortType type) {
        return type == PortType.ITEM_FLUID
                || type == PortType.ITEM_FLUID_IN
                || type == PortType.ITEM_FLUID_ENERGY_IN
                || type == PortType.ITEM_FLUID_ENERGY
                || type == PortType.FLUID;
    }

    public static boolean fluidDrain(PortType type) {
        return type == PortType.ITEM_FLUID
                || type == PortType.ITEM_FLUID_OUT
                || type == PortType.ITEM_FLUID_ENERGY
                || type == PortType.FLUID
                || type == PortType.FLUID_OUT;
    }

    public static boolean energyInsert(PortType type) {
        return type == PortType.ENERGY_INPUT
                || type == PortType.ITEM_FLUID_ENERGY_IN
                || type == PortType.ITEM_FLUID_ENERGY;
    }

    public static boolean itemCapable(PortType type) {
        return itemInsert(type) || itemExtract(type);
    }

    public static boolean fluidCapable(PortType type) {
        return fluidFill(type) || fluidDrain(type);
    }
}
