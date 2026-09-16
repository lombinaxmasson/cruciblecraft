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
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ClusterMillPrepSpecTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void specIsOneInOneOutBufferedRuAndUnregistered() {
        ProcessingMachineSpec spec = ClusterMillPrepSpec.SPEC;
        assertEquals("clustermill", spec.id().getPath());
        assertEquals("clustermill", spec.recipeMapId().getPath());
        assertEquals("clustermill", spec.requireRecipeMap().id().getPath());
        assertEquals(List.of(0), spec.items().inputs());
        assertEquals(List.of(1), spec.items().outputs());
        assertTrue(spec.fluids().inputs().isEmpty());
        assertTrue(spec.fluids().outputs().isEmpty());
        assertEquals(EnergyType.KINETIC_ROTATION, spec.energy().type());
        assertEquals(ProcessingMachineSpec.EnergyMode.BUFFERED, spec.energy().mode());
        assertEquals(4_096L, spec.energy().capacity());
        assertEquals(256L, spec.energy().maxPacket());
        Direction front = Direction.NORTH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().items().resolve(front, Direction.EAST));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                spec.sidedIo().items().resolve(front, Direction.WEST));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().items().resolve(front, front));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.NONE,
                spec.sidedIo().fluids().resolve(front, Direction.WEST));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                spec.sidedIo().energy().resolve(front, front.getOpposite()));
    }

    @Test
    void validatorAcceptsPlateToFoilShapeAndRejectsWrongCounts() {
        ProcessingMachineSpec spec = ClusterMillPrepSpec.SPEC;
        GTRecipe valid = new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT)),
                List.of(1),
                List.of(new ItemStack(Items.PAPER, 4)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16L,
                0L);
        assertEquals(Optional.empty(), spec.validator().validate(valid));

        GTRecipe twoInputs = new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT), Ingredient.of(Items.GOLD_INGOT)),
                List.of(1, 1),
                List.of(new ItemStack(Items.PAPER, 4)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                16L,
                0L);
        assertEquals(
                Optional.of("clustermill_recipe_shape"),
                spec.validator().validate(twoInputs));

        GTRecipe overPacket = new GTRecipe(
                List.of(Ingredient.of(Items.IRON_INGOT)),
                List.of(1),
                List.of(new ItemStack(Items.PAPER, 4)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                257L,
                0L);
        assertEquals(
                Optional.of("clustermill_recipe_shape"),
                spec.validator().validate(overPacket));
    }
}
