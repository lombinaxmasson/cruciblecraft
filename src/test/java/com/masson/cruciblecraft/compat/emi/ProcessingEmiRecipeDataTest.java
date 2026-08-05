package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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

class ProcessingEmiRecipeDataTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void partitionsConsumedPreservedAndWornInputs() {
        ProcessingEmiRecipeData data = ProcessingEmiRecipeData.from(
                ModProcessingMachines.ASSEMBLER,
                sampleRecipe());

        assertEquals(1, data.consumedInputs().size());
        assertEquals(2L, data.consumedInputs().getFirst().displayAmount());
        assertEquals(ItemInputAction.Kind.CONSUME,
                data.consumedInputs().getFirst().action().kind());

        assertEquals(2, data.catalysts().size());
        assertEquals(ItemInputAction.Kind.PRESERVE,
                data.catalysts().get(0).action().kind());
        assertEquals(1L, data.catalysts().get(0).displayAmount());
        assertEquals("Preserved", data.catalysts().get(0).actionText());
        assertEquals(ItemInputAction.Kind.WEAR,
                data.catalysts().get(1).action().kind());
        assertEquals(3, data.catalysts().get(1).action().damage());
        assertEquals("Tool wear: 3 durability",
                data.catalysts().get(1).actionText());
    }

    @Test
    void preservesChanceFluidDurationAndEnergyData() {
        ProcessingEmiRecipeData data = ProcessingEmiRecipeData.from(
                ModProcessingMachines.ASSEMBLER,
                sampleRecipe());

        assertEquals(2_500, data.itemOutputs().getFirst().chance());
        assertEquals(0.25F, data.itemOutputs().getFirst().chanceFraction());
        assertEquals("Chance: 25.00%", data.itemOutputs().getFirst().chanceText());
        assertEquals(750, data.fluidInputs().getFirst().stack().getAmount());
        assertEquals(Fluids.WATER,
                data.fluidInputs().getFirst().stack().getFluid());
        assertEquals(125, data.fluidOutputs().getFirst().stack().getAmount());
        assertEquals(Fluids.LAVA,
                data.fluidOutputs().getFirst().stack().getFluid());
        assertEquals("Time: 40 ticks (2.00 s)", data.durationText());
        assertEquals("Power: 24 KU/t", data.powerText());

        assertEquals("EU/t", ProcessingEmiRecipeData.from(
                ModProcessingMachines.ELECTROLYZER, sampleRecipe()).energyUnit());
        assertEquals("HEAT/t", ProcessingEmiRecipeData.from(
                ModProcessingMachines.SMELTER, sampleRecipe()).energyUnit());
    }

    private static GTRecipe sampleRecipe() {
        return new GTRecipe(
                List.of(
                        Ingredient.of(Items.IRON_INGOT),
                        Ingredient.of(Items.PAPER),
                        Ingredient.of(Items.IRON_PICKAXE)),
                List.of(2, 0, 0),
                List.of(
                        ItemInputAction.CONSUME,
                        ItemInputAction.PRESERVE,
                        ItemInputAction.wear(3)),
                List.of(new ItemStack(Items.DIAMOND)),
                List.of(new FluidStack(Fluids.WATER, 750)),
                List.of(new FluidStack(Fluids.LAVA, 125)),
                List.of(2_500),
                40,
                24L,
                0L,
                true,
                Optional.empty());
    }
}
