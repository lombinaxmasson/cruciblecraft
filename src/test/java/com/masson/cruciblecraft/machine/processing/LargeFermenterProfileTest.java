package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.core.Direction;
import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeFermenterProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerProfileIsSourceBackedHu() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_FERMENTER_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_fermenter_profile",
                profile.tierBandId().toString());
        assertEquals(
                "cruciblecraft:large_fermenter",
                profile.materialId());
        assertEquals(EnergyType.HEAT, profile.energyType());
        assertEquals(1L, profile.inputMinimum());
        assertEquals(512L, profile.inputNominal());
        assertEquals(4_096L, profile.inputMaximum());
        assertEquals(4_096L, profile.energyCapacity());
        assertEquals(256, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
        assertEquals(
                Direction.SOUTH,
                com.masson.cruciblecraft.content.blockentity
                        .LargeFermenterAutoOutput.destinationSide(Direction.NORTH));
    }

    @Test
    void cheapPolicyAcceptsFullHeatAndKeepsParallelDurationBehavior() {
        MachineKindSpec kind =
                ModMultiblockControllers.LARGE_FERMENTER_KIND;
        TierProfile profile =
                ModMultiblockControllers.LARGE_FERMENTER_VARIANT.tierBand();
        assertSame(ModProcessingMachines.FERMENTER, kind.behavior());
        assertEquals(
                MachineKindSpec.OverclockPolicy.CHEAP,
                kind.overclockPolicy());
        assertTrue(kind.parallelDuration());

        assertTrue(MachineExecutionPlan.create(
                recipe(4_096L, 40), kind, profile, 1).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe(4_097L, 40), kind, profile, 1).isPresent());

        MachineExecutionPlan parallel = MachineExecutionPlan.create(
                recipe(16L, 100), kind, profile, 256).orElseThrow();
        assertEquals(256, parallel.operations());
        assertEquals(800, parallel.effectiveDuration());
    }

    @Test
    void structureSplitsBottomHeatAndWallItemFluid() {
        var stream = LargeFermenterProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/"
                        + "large_fermenter.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject palette = document.getAsJsonObject("palette");
        int walls = 0;
        int energy = 0;
        int controllers = 0;
        for (var row : document.getAsJsonArray("structure")) {
            JsonObject predicate = palette.getAsJsonObject(
                    row.getAsJsonObject().get("predicate").getAsString());
            if ("controller".equals(predicate.get("type").getAsString())) {
                controllers++;
            } else if (PortType.ITEM_FLUID.serializedName().equals(
                    predicate.get("port").getAsString())) {
                walls++;
            } else if (PortType.ENERGY_INPUT.serializedName().equals(
                    predicate.get("port").getAsString())) {
                energy++;
            }
        }
        assertEquals(49, walls);
        assertEquals(25, energy);
        assertEquals(1, controllers);
    }

    private static GTRecipe recipe(long eut, int duration) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.COBBLESTONE)),
                List.of(1),
                List.of(ItemInputAction.CONSUME),
                List.of(new ItemStack(Items.GRAVEL)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                duration,
                eut,
                0L,
                true,
                Optional.empty());
    }
}
