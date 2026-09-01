package com.masson.cruciblecraft.datagen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.registry.ModFuelGenerators;
import com.masson.cruciblecraft.registry.ModProcessingMachines;
import com.mojang.serialization.JsonOps;

import net.minecraft.SharedConstants;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.RegistryOps;
import net.minecraft.server.Bootstrap;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class HydrocarbonResourceTest {
    private static final Path ROOT = Path.of(
            "src/hydrocarbon_recipe_generated/resources/data/"
                    + "cruciblecraft/recipe");
    private static RegistryAccess registries;

    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        registries = new RegistryAccess.ImmutableRegistryAccess(
                BuiltInRegistries.REGISTRY.stream().toList());
    }

    @Test
    void allFourRecipesCarryPinnedSourceProvenance() throws Exception {
        assertRecipe(
                "hydrocarbon/distillery/crude_oil_to_fuel_and_lubricant.json",
                "cruciblecraft:distillery",
                "gt.recipe.distillery.json#recipes[872]");
        assertRecipe(
                "hydrocarbon/generifier/natural_gas_to_methane.json",
                "cruciblecraft:generifier",
                "gt.recipe.generifier.json#recipes[553]");
        assertRecipe(
                "hydrocarbon/fuels_engine/fuel_oil.json",
                "cruciblecraft:fuels_engine",
                "gt.recipe.fuels.engine.json#recipes[14]");
        assertRecipe(
                "hydrocarbon/fuels_gas/methane.json",
                "cruciblecraft:fuels_gas",
                "gt.recipe.fuels.burn.json#recipes[20]");
    }

    @Test
    void processingAndFuelValidatorsKeepSignedEnergyDomainsSeparate()
            throws Exception {
        GTRecipe distillery = decode(
                "hydrocarbon/distillery/crude_oil_to_fuel_and_lubricant.json");
        GTRecipe generifier = decode(
                "hydrocarbon/generifier/natural_gas_to_methane.json");
        GTRecipe engine = decode(
                "hydrocarbon/fuels_engine/fuel_oil.json");
        GTRecipe gas = decode(
                "hydrocarbon/fuels_gas/methane.json");

        assertTrue(ModProcessingMachines.DISTILLERY.validator()
                .validate(distillery).isEmpty());
        assertTrue(ModProcessingMachines.GENERIFIER.validator()
                .validate(generifier).isEmpty());
        assertTrue(ModFuelGenerators.FUEL_ENGINE
                .validate(engine).isEmpty());
        assertTrue(ModFuelGenerators.BURNING_GAS_GENERATOR
                .validate(gas).isEmpty());
        GTRecipe oneExhaust = copyWithFluidOutputs(
                gas, List.of(gas.fluidOutputs().getFirst()));
        assertTrue(ModFuelGenerators.BURNING_GAS_GENERATOR
                .validate(oneExhaust).isEmpty());
        GTRecipe unsupportedExhaust = copyWithFluidOutputs(
                gas,
                List.of(
                        gas.fluidOutputs().getFirst(),
                        gas.fluidOutputs().get(1),
                        gas.fluidOutputs().getFirst()));
        assertEquals(
                "host_output_shape_unsupported",
                ModFuelGenerators.BURNING_GAS_GENERATOR
                        .validate(unsupportedExhaust).orElseThrow());
        assertTrue(ModProcessingMachines.DISTILLERY.validator()
                .validate(engine).isPresent());
        assertEquals(-64L, engine.eut());
        assertEquals(-64L, gas.eut());
        assertEquals(24, gas.duration());
        assertEquals(1_152L,
                ModFuelGenerators.BURNING_GAS_GENERATOR
                        .generatedEnergy(gas));
        assertEquals(
                1_152L,
                java.util.stream.IntStream.range(0, gas.duration())
                        .mapToLong(tick ->
                                ModFuelGenerators.BURNING_GAS_GENERATOR
                                        .generatedEnergyAtTick(gas, tick))
                        .sum());
        assertEquals(
                net.minecraft.core.Direction.UP,
                ModFuelGenerators.BURNING_GAS_GENERATOR
                        .energyOutputSide(
                                net.minecraft.core.Direction.EAST));
        assertEquals(
                com.masson.cruciblecraft.api.energy.EnergyType.HEAT,
                ModFuelGenerators.BURNING_GAS_GENERATOR
                        .outputEnergyType());

        var indexedMap = new com.masson.cruciblecraft.recipe.gt.RecipeMap(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "test_fuel_index"));
        ResourceLocation recipeId = ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "test_fuel");
        indexedMap.replaceRecipes(List.of(
                new com.masson.cruciblecraft.recipe.gt.RecipeMap.Entry(
                        recipeId, gas)));
        assertTrue(indexedMap.entry(recipeId).isPresent());
        assertTrue(indexedMap.hasFluidCandidate(
                gas.fluidInputs().getFirst().getFluid()));
    }

    private static void assertRecipe(
            String relative, String map, String sourceSuffix)
            throws Exception {
        JsonObject recipe = JsonParser.parseString(
                Files.readString(ROOT.resolve(relative))).getAsJsonObject();
        assertEquals("cruciblecraft:gt_recipe",
                recipe.get("type").getAsString());
        assertEquals(map, recipe.get("map").getAsString());
        assertTrue(recipe.getAsJsonObject("provenance")
                .get("selected_source_recipe").getAsString()
                .endsWith(sourceSuffix));
        assertEquals(2, recipe.getAsJsonObject("provenance")
                .getAsJsonArray("evidence_hashes").size());
    }

    private static GTRecipe decode(String relative) throws Exception {
        JsonObject source = JsonParser.parseString(
                Files.readString(ROOT.resolve(relative))).getAsJsonObject();
        source.remove("type");
        source.remove("map");
        source.getAsJsonArray("fluid_inputs").forEach(value ->
                value.getAsJsonObject().addProperty("id", "minecraft:water"));
        source.getAsJsonArray("fluid_outputs").forEach(value ->
                value.getAsJsonObject().addProperty("id", "minecraft:water"));
        return GTRecipe.CODEC.parse(
                RegistryOps.create(JsonOps.INSTANCE, registries),
                source).getOrThrow();
    }

    private static GTRecipe copyWithFluidOutputs(
            GTRecipe source, List<FluidStack> outputs) {
        return new GTRecipe(
                source.itemInputs(),
                source.itemInputCounts(),
                source.itemInputActions(),
                source.itemOutputs(),
                source.fluidInputs(),
                outputs,
                source.outputChances(),
                source.duration(),
                source.eut(),
                source.specialValue(),
                source.canBeBuffered(),
                source.provenance());
    }
}
