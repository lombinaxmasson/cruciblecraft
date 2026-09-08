package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.material.def.MaterialLoader;

class HydrocarbonFluidGateTest {
    @Test
    void pinnedCrudeOilIdentityJoinsExistingChemicalFluidGates(
            @TempDir Path configDirectory) {
        var entries = ChemicalFluidRegistrationGate.load(
                MaterialLoader.load(configDirectory).values());
        assertTrue(!entries.isEmpty());
        var byId = entries.stream().collect(Collectors.toMap(
                ChemicalFluidRegistrationGate.Entry::id,
                Function.identity()));
        var crudeOil = byId.get("crude_oil");
        assertEquals("crude_oil", crudeOil.materialId());
        assertEquals(
                ChemicalFluidRegistrationGate.State.LIQUID,
                crudeOil.state());
        assertFalse(crudeOil.worldPlaceable());
        assertEquals(
                "gt6_dump/gt6_recipe_dump/maps/"
                        + "gt.recipe.distillery.json#recipes[872]",
                crudeOil.source().path());
        assertEquals(
                "3703e40308c8c030763fd6297dea8b210d2a77b1",
                crudeOil.source().revision());
        assertTrue(crudeOil.source().reason().contains(
                "permanent DESIGN_POLICY"));
        var naturalGas = byId.get("natural_gas");
        assertEquals("natural_gas", naturalGas.materialId());
        assertEquals(
                ChemicalFluidRegistrationGate.State.GAS,
                naturalGas.state());
        assertFalse(naturalGas.worldPlaceable());
        assertEquals(
                "gt6_dump/gt6_recipe_dump/maps/"
                        + "gt.recipe.generifier.json#recipes[553]",
                naturalGas.source().path());
    }
}
