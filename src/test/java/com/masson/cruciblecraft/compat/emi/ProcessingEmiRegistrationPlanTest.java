package com.masson.cruciblecraft.compat.emi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.SharedConstants;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.fml.loading.LoadingModList;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class ProcessingEmiRegistrationPlanTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void configuredMachinesHaveOneCategoryMapAndWorkstationEach() {
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);

        assertEquals(
                ModProcessingMachines.CONFIGURED_MACHINES.size(),
                plan.machines().size());
        assertEquals(
                ModProcessingMachines.CONFIGURED_MACHINES.size(),
                plan.machines().stream()
                .map(ProcessingEmiRegistrationPlan.MachineRegistration::categoryId)
                .distinct()
                .count());
        assertEquals(
                ModProcessingMachines.CONFIGURED_MACHINES.stream()
                        .map(spec -> spec.requireRecipeMap().id())
                        .distinct()
                        .count(),
                plan.machines().stream()
                .map(machine -> machine.recipeMap().id())
                .distinct()
                .count());
        plan.machines().forEach(machine -> {
            assertEquals(machine.spec().id(), machine.categoryId());
            assertEquals(machine.spec().recipeMapId(), machine.recipeMap().id());
        });
        assertFalse(plan.machines().stream().anyMatch(machine ->
                machine.recipeMap().id().getPath().equals("crusher")));
    }

    @Test
    void snapshotsEveryLiveMapEntryExactlyOnce() {
        ProcessingEmiRegistrationPlan plan = ProcessingEmiRegistrationPlan.create(
                ModProcessingMachines.CONFIGURED_MACHINES);
        List<String> expected = plan.machines().stream()
                .flatMap(machine -> machine.recipeMap().entries().stream()
                        .map(entry -> key(machine.categoryId(), entry.id())))
                .toList();
        List<String> actual = plan.recipes().stream()
                .map(recipe -> key(recipe.machine().categoryId(), recipe.id()))
                .toList();

        assertEquals(expected, actual);
        assertEquals(
                ModProcessingMachines.CONFIGURED_MACHINES.stream()
                        .mapToInt(spec -> spec.requireRecipeMap().entries().size())
                        .sum(),
                actual.size());
        assertEquals(plan.machines().size(), plan.census().configuredMachines());
        assertEquals(actual.size(), plan.census().liveEntries());
        assertEquals(actual.size(), plan.census().registeredEntries());
        assertTrue(plan.census().exactMatch());
        assertEquals(List.of(), plan.census().missingLiveIds());
        assertEquals(List.of(), plan.census().extraRegisteredIds());
    }

    @Test
    void registrationSnapshotKeepsEntriesFromDistinctMapsSeparate() {
        RecipeMap firstMap = new RecipeMap(id("emi_test_first_map"));
        RecipeMap secondMap = new RecipeMap(id("emi_test_second_map"));
        firstMap.replaceRecipes(List.of(
                new RecipeMap.Entry(id("emi_test/first"), recipe(Items.IRON_INGOT)),
                new RecipeMap.Entry(id("emi_test/second"), recipe(Items.GOLD_INGOT))));
        secondMap.replaceRecipes(List.of(
                new RecipeMap.Entry(id("emi_test/third"), recipe(Items.COPPER_INGOT))));
        ProcessingMachineSpec first = withMap(
                ModProcessingMachines.SLUICE, id("emi_test_first"), firstMap);
        ProcessingMachineSpec second = withMap(
                ModProcessingMachines.SHREDDER, id("emi_test_second"), secondMap);

        ProcessingEmiRegistrationPlan plan =
                ProcessingEmiRegistrationPlan.create(List.of(first, second));

        assertEquals(2, plan.machines().size());
        assertEquals(
                List.of(
                        "cruciblecraft:emi_test_first|cruciblecraft:emi_test/first",
                        "cruciblecraft:emi_test_first|cruciblecraft:emi_test/second",
                        "cruciblecraft:emi_test_second|cruciblecraft:emi_test/third"),
                plan.recipes().stream()
                        .map(recipe -> key(
                                recipe.machine().categoryId(), recipe.id()))
                        .toList());
    }

    private static ProcessingMachineSpec withMap(
            ProcessingMachineSpec source,
            ResourceLocation machineId,
            RecipeMap map) {
        return new ProcessingMachineSpec(
                machineId,
                map.id(),
                () -> map,
                source.items(),
                source.fluids(),
                source.energy(),
                source.sidedIo(),
                source.validator(),
                source.buffering(),
                source.ui());
    }

    private static GTRecipe recipe(net.minecraft.world.item.Item output) {
        return new GTRecipe(
                List.of(Ingredient.of(Items.COBBLESTONE)),
                List.of(1),
                List.of(new ItemStack(output)),
                List.of(),
                List.of(),
                List.of(GTRecipe.GUARANTEED_CHANCE),
                20,
                8L,
                0L);
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("cruciblecraft", path);
    }

    private static String key(
            ResourceLocation category,
            ResourceLocation recipe) {
        return category + "|" + recipe;
    }
}
