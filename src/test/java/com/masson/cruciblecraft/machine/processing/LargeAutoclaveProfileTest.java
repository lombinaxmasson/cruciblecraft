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

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeAutoclaveProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerProfileIsSourceBackedTu() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_AUTOCLAVE_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_autoclave_profile",
                profile.tierBandId().toString());
        assertEquals(
                "cruciblecraft:large_autoclave",
                profile.materialId());
        assertEquals(EnergyType.TIME, profile.energyType());
        assertEquals(1L, profile.inputMinimum());
        assertEquals(1L, profile.inputNominal());
        assertEquals(16L, profile.inputMaximum());
        assertEquals(16L, profile.energyCapacity());
        assertEquals(16, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
        ProcessingMachineSpec runtime =
                ModMultiblockControllers.LARGE_AUTOCLAVE_VARIANT.runtimeSpec();
        assertEquals(EnergyType.TIME, runtime.energy().type());
        assertEquals(
                ProcessingMachineSpec.EnergyMode.BUFFERED,
                runtime.energy().mode());
        assertFalse(runtime.sidedIo().itemsChannel().hasAutoInput());
        assertFalse(runtime.sidedIo().fluidsChannel().hasAutoInput());
    }

    @Test
    void standardPolicyKeepsParallelWithoutDurationStretch() {
        MachineKindSpec kind = ModMultiblockControllers.LARGE_AUTOCLAVE_KIND;
        TierProfile profile =
                ModMultiblockControllers.LARGE_AUTOCLAVE_VARIANT.tierBand();
        assertSame(ModProcessingMachines.AUTOCLAVE, kind.behavior());
        assertEquals(
                MachineKindSpec.OverclockPolicy.STANDARD,
                kind.overclockPolicy());
        assertFalse(kind.parallelDuration());

        assertTrue(MachineExecutionPlan.create(
                recipe(16L, 40), kind, profile, 1).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe(17L, 40), kind, profile, 1).isPresent());

        MachineExecutionPlan parallel = MachineExecutionPlan.create(
                recipe(1L, 100), kind, profile, 16).orElseThrow();
        assertEquals(16, parallel.operations());
        assertEquals(100, parallel.effectiveDuration());
    }

    @Test
    void structureIsHollowCubeOfItemFluidEnergyWalls() {
        var stream = LargeAutoclaveProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/"
                        + "large_autoclave.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject palette = document.getAsJsonObject("palette");
        int walls = 0;
        int air = 0;
        int controllers = 0;
        for (var row : document.getAsJsonArray("structure")) {
            JsonObject predicate = palette.getAsJsonObject(
                    row.getAsJsonObject().get("predicate").getAsString());
            String type = predicate.get("type").getAsString();
            if ("controller".equals(type)) {
                controllers++;
            } else if ("air".equals(type)) {
                air++;
            } else if (PortType.ITEM_FLUID_ENERGY.serializedName().equals(
                    predicate.get("port").getAsString())) {
                walls++;
            }
        }
        assertEquals(25, walls);
        assertEquals(1, air);
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
