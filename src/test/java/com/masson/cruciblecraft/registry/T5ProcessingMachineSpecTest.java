package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.menu.ConfiguredProcessingMachineMenu;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;

import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeAll;

class T5ProcessingMachineSpecTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void configuredMenusSynchronizeOnlyStatusAndStatusArgument() {
        assertEquals(2, ConfiguredProcessingMachineMenu.DATA_COUNT);
        assertEquals(2, ConfiguredProcessingMachineMenu.dataCount());
    }

    @Test
    void t5SetReusesSourceMapsAndAddsExactlySixDedicatedMachines() {
        assertEquals(
                List.of(
                        "bath",
                        "centrifuge",
                        "smelter",
                        "assembler",
                        "electrolyzer",
                        "mixer",
                        "distillery",
                        "autoclave",
                        "drying",
                        "compressor"),
                ModProcessingMachines.T5_MACHINES.stream()
                        .map(spec -> spec.id().getPath())
                        .toList());
        assertEquals(
                List.of(
                        "electrolyzer",
                        "mixer",
                        "distillery",
                        "autoclave",
                        "drying",
                        "compressor"),
                ModProcessingMachines.T5_DEDICATED_MACHINES.stream()
                        .map(spec -> spec.id().getPath())
                        .toList());
        assertEquals(
                List.of(
                        "electrolyzer",
                        "mixer",
                        "distillery",
                        "autoclave",
                        "drying",
                        "compressor"),
                ModProcessingMachines.T5_DEDICATED_MACHINES.stream()
                        .map(spec -> spec.requireRecipeMap().id().getPath())
                        .toList());
        assertFalse(ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .anyMatch(spec -> spec.id().getPath().equals("chemical_reactor")));
        assertEquals(
                ModProcessingMachines.CONFIGURED_MACHINES.size(),
                new HashSet<>(ModProcessingMachines.CONFIGURED_MACHINES).size());
    }

    @Test
    void dedicatedSpecsUseTheGenericConfiguredMenuRegistry() {
        for (ProcessingMachineSpec spec : ModProcessingMachines.T5_DEDICATED_MACHINES) {
            assertEquals(
                    spec.id().getPath(),
                    ModMenus.forMachine(spec).getId().getPath());
        }
        assertEquals(
                ModProcessingMachines.CONFIGURED_MACHINES.size(),
                ModMenus.processingMenuCount());
    }

    @Test
    void dedicatedSpecsExposeExactLayoutsAndSidedElectricIo() {
        assertLayout(ModProcessingMachines.ELECTROLYZER, 2, 6, 2, 3, 16_000, 16_000);
        assertLayout(ModProcessingMachines.MIXER, 4, 1, 3, 2, 32_000, 32_000);
        assertLayout(ModProcessingMachines.DISTILLERY, 2, 2, 2, 3, 8_000, 8_000);
        assertLayout(ModProcessingMachines.AUTOCLAVE, 2, 3, 1, 1, 2_500_000, 16_000);
        assertLayout(ModProcessingMachines.DRYING, 1, 1, 0, 1, 0, 32_000);
        assertLayout(ModProcessingMachines.COMPRESSOR, 1, 1, 0, 0, 0, 0);

        for (ProcessingMachineSpec spec : ModProcessingMachines.T5_DEDICATED_MACHINES) {
            assertEquals(spec.fluids().tankCount(), spec.ui().tanks().size());
            int tanks = spec.fluids().tankCount();
            assertEquals(
                    ConfiguredProcessingMachineMenu.DATA_COUNT,
                    ConfiguredProcessingMachineMenu.dataCount());
            assertEquals(EnergyType.ELECTRIC, spec.energy().type());
            assertEquals(
                    ProcessingMachineSpec.EnergyMode.BUFFERED,
                    spec.energy().mode());
            assertEquals(1_024L, spec.energy().maxPacket());
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                    spec.sidedIo().items().resolve(Direction.NORTH, Direction.NORTH));
            assertEquals(
                    spec.fluids().inputs().isEmpty()
                            ? ProcessingMachineSpec.CapabilityAccess.NONE
                            : ProcessingMachineSpec.CapabilityAccess.INPUT,
                    spec.sidedIo().fluids().resolve(Direction.NORTH, Direction.WEST));
            assertEquals(
                    spec.fluids().outputs().isEmpty()
                            ? ProcessingMachineSpec.CapabilityAccess.NONE
                            : ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                    spec.sidedIo().fluids().resolve(Direction.NORTH, Direction.NORTH));
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.INPUT,
                    spec.sidedIo().energy().resolve(Direction.NORTH, Direction.SOUTH));
            assertTrue(spec.validator().validate(validRecipe(spec)).isEmpty());
        }
    }

    @Test
    void configuredMachineSlotsAndTanksNeverOverlapOnScreen() {
        for (ProcessingMachineSpec spec : ModProcessingMachines.CONFIGURED_MACHINES) {
            for (ProcessingMachineSpec.SlotPosition slot : spec.ui().machineSlots()) {
                for (ProcessingMachineSpec.TankPosition tank : spec.ui().tanks()) {
                    boolean overlaps = slot.x() < tank.x() + tank.width()
                            && slot.x() + 18 > tank.x()
                            && slot.y() < tank.y() + tank.height()
                            && slot.y() + 18 > tank.y();
                    assertFalse(
                            overlaps,
                            spec.id() + " slot " + slot + " overlaps tank " + tank);
                }
            }
        }
    }

    @Test
    void reusedSourceMapsExposeT5OutputCapacityWithoutChangingEnergyType() {
        assertLayout(ModProcessingMachines.BATH, 1, 4, 1, 1, 4_000, 8_000);
        assertLayout(ModProcessingMachines.CENTRIFUGE, 1, 6, 1, 2, 4_000, 8_000);
        assertLayout(ModProcessingMachines.SMELTER, 1, 4, 0, 1, 0, 8_000);
        assertEquals(EnergyType.KINETIC, ModProcessingMachines.BATH.energy().type());
        assertEquals(EnergyType.KINETIC, ModProcessingMachines.CENTRIFUGE.energy().type());
        assertEquals(EnergyType.HEAT, ModProcessingMachines.SMELTER.energy().type());
        for (ProcessingMachineSpec spec : List.of(
                ModProcessingMachines.BATH,
                ModProcessingMachines.CENTRIFUGE,
                ModProcessingMachines.SMELTER)) {
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                    spec.sidedIo().fluids().resolve(
                            Direction.NORTH,
                            Direction.NORTH));
        }
    }

    @Test
    void reusedSourceValidatorsAcceptTheirExactT5LayoutsAndCapacities() {
        for (ProcessingMachineSpec spec : List.of(
                ModProcessingMachines.BATH,
                ModProcessingMachines.CENTRIFUGE,
                ModProcessingMachines.SMELTER)) {
            assertTrue(
                    spec.validator().validate(maxLayoutRecipe(spec)).isEmpty(),
                    spec.id().toString());
        }
    }

    @Test
    void dedicatedValidatorRejectsCatalystsAndOvervoltage() {
        GTRecipe catalyst = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(0),
                List.of(ItemInputAction.PRESERVE),
                List.of(new ItemStack(Items.IRON_INGOT)),
                List.of(new FluidStack(Fluids.WATER, 1000)),
                List.of(new FluidStack(Fluids.LAVA, 1000)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                24L,
                0L,
                true,
                Optional.empty());
        assertEquals(
                Optional.of("t5_recipe_input_action"),
                ModProcessingMachines.ELECTROLYZER.validator().validate(catalyst));

        GTRecipe overvoltage = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_INGOT)),
                List.of(),
                List.of(new FluidStack(Fluids.WATER, 1000)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                1_025L,
                0L);
        assertEquals(
                Optional.of("t5_recipe_energy"),
                ModProcessingMachines.MIXER.validator().validate(overvoltage));
    }

    @Test
    void electrolyzerAcceptsCurrentSourceProjectionEnvelope() {
        GTRecipe projected = new GTRecipe(
                List.of(Ingredient.of(Items.COAL)),
                List.of(1),
                List.of(
                        new ItemStack(Items.IRON_NUGGET),
                        new ItemStack(Items.GOLD_NUGGET)),
                List.of(),
                List.of(new FluidStack(Fluids.WATER, 1_000)),
                List.of(
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE),
                16,
                854L,
                0L);

        assertTrue(ModProcessingMachines.ELECTROLYZER
                .validator()
                .validate(projected)
                .isEmpty());
    }

    @Test
    void autoclaveAcceptsPinnedLargeFluidRoute() {
        GTRecipe pinned = new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE), Ingredient.of(Items.COAL)),
                List.of(1, 1),
                List.of(
                        new ItemStack(Items.IRON_NUGGET),
                        new ItemStack(Items.GOLD_NUGGET),
                        new ItemStack(Items.COPPER_INGOT)),
                List.of(new FluidStack(Fluids.WATER, 2_500_000)),
                List.of(new FluidStack(Fluids.LAVA, 16_000)),
                List.of(
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE,
                        GTRecipe.GUARANTEED_CHANCE),
                20,
                1_024L,
                0L);

        assertTrue(ModProcessingMachines.AUTOCLAVE
                .validator()
                .validate(pinned)
                .isEmpty());
    }

    private static void assertLayout(
            ProcessingMachineSpec spec,
            int itemInputs,
            int itemOutputs,
            int fluidInputs,
            int fluidOutputs,
            int fluidInputCapacity,
            int fluidOutputCapacity) {
        assertEquals(itemInputs, spec.items().inputs().size(), spec.id().toString());
        assertEquals(itemOutputs, spec.items().outputs().size(), spec.id().toString());
        assertEquals(fluidInputs, spec.fluids().inputs().size(), spec.id().toString());
        assertEquals(fluidOutputs, spec.fluids().outputs().size(), spec.id().toString());
        spec.fluids().inputs().forEach(tank ->
                assertEquals(fluidInputCapacity, tank.capacity(), spec.id().toString()));
        spec.fluids().outputs().forEach(tank ->
                assertEquals(fluidOutputCapacity, tank.capacity(), spec.id().toString()));
    }

    private static GTRecipe validRecipe(ProcessingMachineSpec spec) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.REDSTONE)),
                List.of(1),
                List.of(new ItemStack(Items.IRON_NUGGET)),
                spec.fluids().inputs().isEmpty()
                        ? List.of()
                        : List.of(new FluidStack(Fluids.WATER, 1_000)),
                spec.fluids().outputs().isEmpty()
                        ? List.of()
                        : List.of(new FluidStack(Fluids.LAVA, 500)),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                24L,
                0L);
    }

    private static GTRecipe maxLayoutRecipe(ProcessingMachineSpec spec) {
        return new GTRecipe(
                java.util.stream.IntStream.range(0, spec.items().inputs().size())
                        .mapToObj(index -> Ingredient.of(Items.REDSTONE))
                        .toList(),
                java.util.Collections.nCopies(spec.items().inputs().size(), 64),
                java.util.stream.IntStream.range(0, spec.items().outputs().size())
                        .mapToObj(index -> new ItemStack(Items.IRON_NUGGET))
                        .toList(),
                spec.fluids().inputs().stream()
                        .map(tank -> new FluidStack(Fluids.WATER, tank.capacity()))
                        .toList(),
                spec.fluids().outputs().stream()
                        .map(tank -> new FluidStack(Fluids.LAVA, tank.capacity()))
                        .toList(),
                java.util.Collections.nCopies(
                        spec.items().outputs().size(),
                        GTRecipe.GUARANTEED_CHANCE),
                20,
                spec.energy().maxPacket(),
                0L);
    }
}
