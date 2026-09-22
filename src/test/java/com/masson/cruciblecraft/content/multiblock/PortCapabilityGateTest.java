package com.masson.cruciblecraft.content.multiblock;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;

class PortCapabilityGateTest {
    @Test
    void inputOnlyPortsCannotExtract() {
        assertTrue(PortCapabilityGate.itemInsert(
                PortType.ITEM_FLUID_ENERGY_IN));
        assertTrue(PortCapabilityGate.fluidFill(
                PortType.ITEM_FLUID_ENERGY_IN));
        assertTrue(PortCapabilityGate.energyInsert(
                PortType.ITEM_FLUID_ENERGY_IN));
        assertFalse(PortCapabilityGate.itemExtract(
                PortType.ITEM_FLUID_ENERGY_IN));
        assertFalse(PortCapabilityGate.fluidDrain(
                PortType.ITEM_FLUID_ENERGY_IN));
    }

    @Test
    void outputPortsCannotInsert() {
        assertTrue(PortCapabilityGate.itemExtract(PortType.ITEM_FLUID_OUT));
        assertTrue(PortCapabilityGate.fluidDrain(PortType.ITEM_FLUID_OUT));
        assertFalse(PortCapabilityGate.itemInsert(PortType.ITEM_FLUID_OUT));
        assertFalse(PortCapabilityGate.fluidFill(PortType.ITEM_FLUID_OUT));
        assertFalse(PortCapabilityGate.energyInsert(PortType.ITEM_FLUID_OUT));
    }

    @Test
    void fluidPortIsBidirectionalButNeverAnItemOrEnergyPort() {
        assertTrue(PortCapabilityGate.fluidFill(PortType.FLUID));
        assertTrue(PortCapabilityGate.fluidDrain(PortType.FLUID));
        assertFalse(PortCapabilityGate.itemCapable(PortType.FLUID));
        assertFalse(PortCapabilityGate.energyInsert(PortType.FLUID));
    }

    @Test
    void energyInputHasNoItemOrFluidView() {
        assertTrue(PortCapabilityGate.energyInsert(PortType.ENERGY_INPUT));
        assertFalse(PortCapabilityGate.itemCapable(PortType.ENERGY_INPUT));
        assertFalse(PortCapabilityGate.fluidCapable(PortType.ENERGY_INPUT));
    }

    @Test
    void completePortTypeMatrixKeepsBidirectionalAndOneWaySemantics() {
        assertTrue(PortCapabilityGate.itemInsert(PortType.ITEM_FLUID));
        assertTrue(PortCapabilityGate.itemExtract(PortType.ITEM_FLUID));
        assertTrue(PortCapabilityGate.fluidFill(PortType.ITEM_FLUID));
        assertTrue(PortCapabilityGate.fluidDrain(PortType.ITEM_FLUID));

        assertTrue(PortCapabilityGate.itemInsert(PortType.ITEM_FLUID_IN));
        assertTrue(PortCapabilityGate.fluidFill(PortType.ITEM_FLUID_IN));
        assertFalse(PortCapabilityGate.itemExtract(PortType.ITEM_FLUID_IN));
        assertFalse(PortCapabilityGate.fluidDrain(PortType.ITEM_FLUID_IN));

        assertTrue(PortCapabilityGate.itemInsert(PortType.ITEM_FLUID_ENERGY));
        assertTrue(PortCapabilityGate.itemExtract(PortType.ITEM_FLUID_ENERGY));
        assertTrue(PortCapabilityGate.fluidFill(PortType.ITEM_FLUID_ENERGY));
        assertTrue(PortCapabilityGate.fluidDrain(PortType.ITEM_FLUID_ENERGY));

        assertTrue(PortCapabilityGate.fluidDrain(PortType.FLUID_OUT));
        assertFalse(PortCapabilityGate.fluidFill(PortType.FLUID_OUT));
    }
}
