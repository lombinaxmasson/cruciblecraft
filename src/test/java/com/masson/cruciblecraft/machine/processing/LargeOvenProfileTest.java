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

class LargeOvenProfileTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void controllerProfileIsSourceBackedEu() {
        TierProfile profile =
                ModMultiblockControllers.LARGE_OVEN_VARIANT.tierBand();
        assertEquals(
                "cruciblecraft:large_oven_profile",
                profile.tierBandId().toString());
        assertEquals("cruciblecraft:large_oven", profile.materialId());
        assertEquals(EnergyType.ELECTRIC, profile.energyType());
        assertEquals(512L, profile.inputMinimum());
        assertEquals(512L, profile.inputNominal());
        assertEquals(4_096L, profile.inputMaximum());
        assertEquals(4_096L, profile.energyCapacity());
        assertEquals(64, profile.parallelLimit());
        assertEquals(2_500, profile.efficiency());
        var io = ModProcessingMachines.LARGE_OVEN.sidedIo();
        assertEquals(
                net.minecraft.core.Direction.DOWN,
                io.itemsChannel().autoOutputWorld(
                        net.minecraft.core.Direction.NORTH).orElseThrow());
        assertEquals(
                net.minecraft.core.Direction.DOWN,
                io.fluidsChannel().autoOutputWorld(
                        net.minecraft.core.Direction.NORTH).orElseThrow());
        assertTrue(io.itemsChannel().autoInputWorld(
                net.minecraft.core.Direction.NORTH).isEmpty());
        assertTrue(io.fluidsChannel().autoInputWorld(
                net.minecraft.core.Direction.NORTH).isEmpty());
    }

    @Test
    void cheapPolicyAcceptsFullEutAndKeepsParallelDurationBehavior() {
        MachineKindSpec kind = ModMultiblockControllers.LARGE_OVEN_KIND;
        TierProfile profile =
                ModMultiblockControllers.LARGE_OVEN_VARIANT.tierBand();
        assertSame(ModProcessingMachines.LARGE_OVEN, kind.behavior());
        assertEquals(
                MachineKindSpec.OverclockPolicy.CHEAP,
                kind.overclockPolicy());
        assertTrue(kind.parallelDuration());

        assertTrue(MachineExecutionPlan.create(
                recipe(4_096L, 40), kind, profile, 1).isPresent());
        assertFalse(MachineExecutionPlan.create(
                recipe(4_097L, 40), kind, profile, 1).isPresent());

        MachineExecutionPlan parallel = MachineExecutionPlan.create(
                recipe(16L, 100), kind, profile, 64).orElseThrow();
        assertEquals(64, parallel.operations());
        assertEquals(800, parallel.effectiveDuration());
    }

    @Test
    void structureIsInvarWallsHollowNichromeCoils() {
        var stream = LargeOvenProfileTest.class.getResourceAsStream(
                "/data/cruciblecraft/multiblock_structures/large_oven.json");
        JsonObject document = JsonParser.parseReader(new InputStreamReader(
                java.util.Objects.requireNonNull(stream),
                StandardCharsets.UTF_8)).getAsJsonObject();
        JsonObject palette = document.getAsJsonObject("palette");
        int walls = 0;
        int coils = 0;
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
            } else if ("tag".equals(type)) {
                coils++;
                assertEquals(
                        "cruciblecraft:large_oven_coils",
                        predicate.get("tag").getAsString());
                assertEquals(
                        "oven_coils",
                        predicate.get("uniform_group").getAsString());
            } else if (PortType.ITEM_FLUID_ENERGY.serializedName().equals(
                    predicate.get("port").getAsString())) {
                walls++;
                assertEquals(
                        "cruciblecraft:invar/wall",
                        predicate.get("block").getAsString());
            }
        }
        assertEquals(17, walls);
        assertEquals(8, coils);
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
