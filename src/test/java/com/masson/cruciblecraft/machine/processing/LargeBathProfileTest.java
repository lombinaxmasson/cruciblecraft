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
import net.minecraft.core.Direction;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class LargeBathProfileTest {
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
                ModMultiblockControllers.LARGE_BATH_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_bath_profile",
                profile.tierBandId().toString());
        assertEquals("cruciblecraft:large_bath", profile.materialId());
        assertEquals(EnergyType.TIME, profile.energyType());
        assertEquals(1L, profile.inputMinimum());
        assertEquals(1L, profile.inputNominal());
        assertEquals(16L, profile.inputMaximum());
        assertEquals(16L, profile.energyCapacity());
        assertEquals(64, profile.parallelLimit());
        assertEquals(10_000, profile.efficiency());
    }

    @Test
    void standardPolicyWithoutCheapOverclockOrParallelDuration() {
        MachineKindSpec kind = ModMultiblockControllers.LARGE_BATH_KIND;
        assertSame(ModProcessingMachines.BATH, kind.behavior());
        assertEquals(
                MachineKindSpec.OverclockPolicy.STANDARD,
                kind.overclockPolicy());
        assertFalse(kind.parallelDuration());
    }

    @Test
    void controllerAcceptsAnyFaceAndAutoOutputsDown() {
        ProcessingMachineSpec spec =
                ModMultiblockControllers.LARGE_BATH_VARIANT.runtimeSpec();
        Direction front = Direction.NORTH;
        ProcessingMachineIoAssertions.assertMatchesProfile(spec);
        assertFalse(spec.sidedIo().itemsChannel().hasAutoInput());
        assertFalse(spec.sidedIo().fluidsChannel().hasAutoInput());
        assertEquals(
                Direction.DOWN,
                ProcessingMachineIoFaces.itemOutput(spec, front));
        assertEquals(
                Direction.DOWN,
                ProcessingMachineIoFaces.fluidOutput(spec, front));
        for (Direction side : Direction.values()) {
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.BOTH,
                    spec.sidedIo().items().resolve(front, side),
                    side.getName());
            assertEquals(
                    ProcessingMachineSpec.CapabilityAccess.INPUT,
                    spec.sidedIo().energy().resolve(front, side),
                    side.getName());
        }
        ProcessingMachineSpec single = ModProcessingMachines.BATH;
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.INPUT,
                single.sidedIo().items().resolve(front, Direction.EAST));
        assertEquals(
                ProcessingMachineSpec.CapabilityAccess.OUTPUT,
                single.sidedIo().items().resolve(front, Direction.WEST));
    }

    @Test
    void structureIsFiveByFiveByTwoItemFluidWalls() {
        var stream = LargeBathProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/large_bath.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                stream, StandardCharsets.UTF_8)).getAsJsonObject();
        var structure = document.getAsJsonArray("structure");
        assertEquals(50, structure.size());
        JsonObject palette = document.getAsJsonObject("palette");
        int itemFluid = 0;
        int controllers = 0;
        for (var element : structure) {
            String key = element.getAsJsonObject().get("predicate").getAsString();
            JsonObject predicate = palette.getAsJsonObject(key);
            if ("controller".equals(predicate.get("type").getAsString())) {
                controllers++;
            } else {
                assertEquals(
                        PortType.ITEM_FLUID.serializedName(),
                        predicate.get("port").getAsString());
                itemFluid++;
            }
        }
        assertEquals(49, itemFluid);
        assertEquals(1, controllers);
    }

    @Test
    void tuRecipesParallelWithoutStretchingDuration() {
        MachineKindSpec kind = ModMultiblockControllers.LARGE_BATH_KIND;
        TierProfile profile =
                ModMultiblockControllers.LARGE_BATH_VARIANT.tierBand();
        MachineExecutionPlan timed = MachineExecutionPlan.create(
                recipe(0L, 16), kind, profile, 64).orElseThrow();
        assertEquals(64, timed.operations());
        assertEquals(16, timed.effectiveDuration());
        assertEquals(16L, timed.totalWork());
        assertEquals(0L, timed.nominalPower());
        assertTrue(MachineExecutionPlan.create(
                recipe(16L, 40), kind, profile, 1).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe(17L, 40), kind, profile, 1).isPresent());
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
