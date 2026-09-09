package com.masson.cruciblecraft.machine.processing.prep;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class PrepMachineSpecsTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void slicerIsTwoInTwoOutBufferedEuAndUnregistered() {
        ProcessingMachineSpec spec = SlicerPrepSpec.SPEC;
        assertEquals("slicer", spec.id().getPath());
        assertEquals(List.of(0, 1), spec.items().inputs());
        assertEquals(List.of(2, 3), spec.items().outputs());
        assertEquals(EnergyType.ELECTRIC, spec.energy().type());
        assertEquals(PrepMachineCommon.EU_MAX_PACKET, spec.energy().maxPacket());
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().items().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                spec.sidedIo().items().resolve(front, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().energy().resolve(front, front.getOpposite()));
        GTRecipe valid = itemRecipe(2, 2, 16L);
        assertEquals(Optional.empty(), spec.validator().validate(valid));
    }

    @Test
    void loomKeepsSixItemInputsAndSharesTheDumpWithElectricHost() {
        ProcessingMachineSpec kinetic = LoomPrepSpec.SPEC;
        ProcessingMachineSpec electric = ElectricLoomPrepSpec.SPEC;
        assertEquals("loom", kinetic.id().getPath());
        assertEquals("electricloom", electric.id().getPath());
        assertEquals("loom", electric.recipeMapId().getPath());
        assertEquals(EnergyType.KINETIC_ROTATION, kinetic.energy().type());
        assertEquals(EnergyType.ELECTRIC, electric.energy().type());
        assertEquals(5_000, ElectricLoomPrepSpec.EFFICIENCY_PERMILLE);
        assertEquals(List.of(0, 1, 2, 3, 4, 5), kinetic.items().inputs());
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                kinetic.sidedIo().items().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                kinetic.sidedIo().items().resolve(front, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                kinetic.sidedIo().energy().resolve(front, Direction.WEST));
        assertEquals(Optional.empty(), kinetic.validator().validate(itemRecipe(1, 1, 16L)));
    }

    @Test
    void pressureWasherTakesFluidOnTopAndBottom() {
        ProcessingMachineSpec spec = PressureWasherPrepSpec.SPEC;
        assertEquals("pressurewasher", spec.id().getPath());
        assertEquals(1, spec.fluids().inputs().size());
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().fluids().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().items().resolve(front, Direction.UP));
        GTRecipe valid = new GTRecipe(
                List.of(Ingredient.of(Items.OAK_LOG)),
                List.of(1),
                List.of(new ItemStack(Items.STRIPPED_OAK_LOG)),
                List.of(new FluidStack(Fluids.WATER, 1_000)),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16L,
                0L);
        assertEquals(Optional.empty(), spec.validator().validate(valid));
    }

    @Test
    void injectorAndPrinterAndNanofabKeepEuPanelTanks() {
        assertEquals(2, InjectorPrepSpec.SPEC.fluids().inputs().size());
        assertEquals(1, InjectorPrepSpec.SPEC.fluids().outputs().size());
        assertEquals(6, PrinterPrepSpec.SPEC.fluids().inputs().size());
        assertTrue(PrinterPrepSpec.SPEC.fluids().outputs().isEmpty());
        assertEquals(1, NanofabPrepSpec.SPEC.fluids().inputs().size());
        assertEquals(1, NanofabPrepSpec.SPEC.fluids().outputs().size());
        assertEquals(EnergyType.ELECTRIC, InjectorPrepSpec.SPEC.energy().type());
        assertEquals(EnergyType.ELECTRIC, PrinterPrepSpec.SPEC.energy().type());
        assertEquals(EnergyType.ELECTRIC, NanofabPrepSpec.SPEC.energy().type());
    }

    @Test
    void laminatorAndMelterUseAdjacentHeatAndKeepMelterParallel() {
        assertEquals(EnergyType.HEAT, LaminatorPrepSpec.SPEC.energy().type());
        assertEquals(
                ProcessingMachineSpec.EnergyMode.ADJACENT,
                LaminatorPrepSpec.SPEC.energy().mode());
        assertEquals(EnergyType.HEAT, MelterPrepSpec.SPEC.energy().type());
        assertEquals(1_000, MelterPrepSpec.PARALLEL);
        assertTrue(MelterPrepSpec.PARALLEL_DURATION);
        assertTrue(MelterPrepSpec.CHEAP_OVERCLOCKING);
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                MelterPrepSpec.SPEC.sidedIo().energy().resolve(front, Direction.DOWN));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                MelterPrepSpec.SPEC.sidedIo().items().resolve(front, Direction.UP));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                MelterPrepSpec.SPEC.sidedIo().items().resolve(
                        front, front.getCounterClockWise()));
    }

    private static GTRecipe itemRecipe(int inputs, int outputs, long eut) {
        return new GTRecipe(
                java.util.Collections.nCopies(inputs, Ingredient.of(Items.IRON_INGOT)),
                java.util.Collections.nCopies(inputs, 1),
                java.util.stream.IntStream.range(0, outputs)
                        .mapToObj(index -> new ItemStack(Items.PAPER))
                        .toList(),
                List.of(),
                List.of(),
                java.util.Collections.nCopies(outputs, GTRecipe.GUARANTEED_CHANCE),
                20,
                eut,
                0L);
    }
}
