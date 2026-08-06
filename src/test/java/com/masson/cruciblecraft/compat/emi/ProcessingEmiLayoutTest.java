package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.neoforge.fluids.FluidStack;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingEmiLayoutTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void everyConfiguredLayoutIsBoundedAndNonOverlapping() {
        assertEquals(24, ModProcessingMachines.CONFIGURED_MACHINES.size());
        for (ProcessingMachineSpec spec
                : ModProcessingMachines.CONFIGURED_MACHINES) {
            ProcessingEmiLayout layout =
                    ProcessingEmiLayout.create(spec, fullDisplayData(spec));
            List<ProcessingEmiLayout.Rect> bounds = layout.visibleBounds();
            for (ProcessingEmiLayout.Rect rect : bounds) {
                assertTrue(rect.x() >= 0 && rect.y() >= 0, spec.id().toString());
                assertTrue(rect.right() <= layout.width(), spec.id().toString());
                assertTrue(rect.bottom() <= layout.height(), spec.id().toString());
            }
            for (int left = 0; left < bounds.size(); left++) {
                for (int right = left + 1; right < bounds.size(); right++) {
                    assertFalse(
                            bounds.get(left).overlaps(bounds.get(right)),
                            spec.id() + ": " + bounds.get(left) + " / "
                                    + bounds.get(right));
                }
            }
            assertTrue(layout.durationTextY() >= bounds.stream()
                    .mapToInt(ProcessingEmiLayout.Rect::bottom)
                    .max()
                    .orElse(0));
            assertTrue(layout.powerTextY() > layout.durationTextY());
            assertTrue(layout.powerTextY() < layout.height());
        }
    }

    @Test
    void layoutRetainsSpecCoordinatesAndToolRoles() {
        for (ProcessingMachineSpec spec
                : ModProcessingMachines.CONFIGURED_MACHINES) {
            ProcessingEmiLayout layout =
                    ProcessingEmiLayout.create(spec, fullDisplayData(spec));
            assertEquals(spec.ui().progress().x(), layout.progress().x());
            assertEquals(spec.ui().progress().y(), layout.progress().y());
            for (ProcessingEmiLayout.ItemSlot slot : layout.itemSlots()) {
                ProcessingMachineSpec.SlotPosition source =
                        spec.ui().machineSlots().get(slot.machineSlot());
                assertEquals(source.x(), slot.bounds().x(), spec.id().toString());
                assertEquals(source.y(), slot.bounds().y(), spec.id().toString());
                if (slot.kind() == ProcessingEmiLayout.ItemKind.CATALYST) {
                    assertEquals(
                            ProcessingMachineSpec.SlotRole.TOOL,
                            spec.items().role(slot.machineSlot()),
                            spec.id().toString());
                }
            }
            for (ProcessingEmiLayout.FluidTank tank : layout.fluidTanks()) {
                ProcessingMachineSpec.TankPosition source = spec.ui().tanks().stream()
                        .filter(candidate -> candidate.tank() == tank.tank())
                        .findFirst()
                        .orElseThrow();
                assertTrue(tank.bounds().x() >= source.x(), spec.id().toString());
                assertEquals(source.y(), tank.bounds().y(), spec.id().toString());
                assertEquals(source.width(), tank.bounds().width());
                assertEquals(source.height(), tank.bounds().height());
            }
        }
    }

    private static ProcessingEmiRecipeData fullDisplayData(
            ProcessingMachineSpec spec) {
        List<ProcessingEmiRecipeData.ItemInput> consumed = new ArrayList<>();
        List<ProcessingEmiRecipeData.ItemInput> catalysts = new ArrayList<>();
        int recipeIndex = 0;
        for (int slot : spec.items().inputs()) {
            ProcessingMachineSpec.SlotRole role = spec.items().role(slot);
            ProcessingEmiRecipeData.ItemInput input =
                    new ProcessingEmiRecipeData.ItemInput(
                            recipeIndex++,
                            Ingredient.of(role == ProcessingMachineSpec.SlotRole.TOOL
                                    ? Items.IRON_PICKAXE
                                    : Items.COBBLESTONE),
                            1L,
                            role == ProcessingMachineSpec.SlotRole.TOOL
                                    ? ItemInputAction.PRESERVE
                                    : ItemInputAction.CONSUME);
            (role == ProcessingMachineSpec.SlotRole.TOOL
                    ? catalysts
                    : consumed).add(input);
        }
        List<ProcessingEmiRecipeData.ItemOutput> itemOutputs =
                new ArrayList<>();
        for (int index = 0; index < spec.items().outputs().size(); index++) {
            itemOutputs.add(new ProcessingEmiRecipeData.ItemOutput(
                    index,
                    new ItemStack(Items.STONE),
                    GTRecipe.GUARANTEED_CHANCE));
        }
        List<ProcessingEmiRecipeData.FluidResource> fluidInputs =
                new ArrayList<>();
        for (int index = 0; index < spec.fluids().inputs().size(); index++) {
            fluidInputs.add(new ProcessingEmiRecipeData.FluidResource(
                    index, new FluidStack(Fluids.WATER, 1)));
        }
        List<ProcessingEmiRecipeData.FluidResource> fluidOutputs =
                new ArrayList<>();
        for (int index = 0; index < spec.fluids().outputs().size(); index++) {
            fluidOutputs.add(new ProcessingEmiRecipeData.FluidResource(
                    index, new FluidStack(Fluids.LAVA, 1)));
        }
        return new ProcessingEmiRecipeData(
                consumed,
                catalysts,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                20,
                1L,
                spec.energy().type());
    }
}
