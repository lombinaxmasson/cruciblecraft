package com.masson.cruciblecraft.recipe.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;

class WorkbenchToolRecipePlanTest {
    private static final Path RECIPE_ROOT = Path.of(
            "src/generated/resources/data/cruciblecraft/recipe");
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    private static final List<String> OBSOLETE_PATHS = List.of(
            "smithing_hammer.json",
            "bronze_smithing_hammer.json",
            "steel_smithing_hammer.json",
            "stone_smithing_hammer.json",
            "tools/iron_file.json",
            "tools/iron_wrench.json",
            "tools/iron_screwdriver.json",
            "tools/iron_saw.json",
            "tools/iron_chisel.json",
            "tools/iron_wire_cutter.json",
            "tools/iron_monkey_wrench.json",
            "tools/bronze_file.json",
            "tools/bronze_wrench.json",
            "tools/bronze_screwdriver.json",
            "tools/bronze_saw.json",
            "tools/bronze_chisel.json",
            "tools/bronze_wire_cutter.json",
            "tools/bronze_monkey_wrench.json",
            "tools/steel_file.json",
            "tools/steel_wrench.json",
            "tools/steel_screwdriver.json",
            "tools/steel_saw.json",
            "tools/steel_chisel.json",
            "tools/steel_wire_cutter.json",
            "tools/steel_monkey_wrench.json",
            "tools/stone_pickaxe.json",
            "tools/stone_axe.json",
            "tools/stone_hoe.json",
            "tools/stone_shovel.json");

    @Test
    void catalogPlanCoversStoneFamilyAndWorkshopTools(
            @TempDir Path configDirectory) throws Exception {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var plan = WorkbenchToolRecipePlan.plan(materials, registered);
        Set<String> paths = plan.stream()
                .map(WorkbenchToolRecipePlan.Recipe::path)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(plan.size(), paths.size());
        assertTrue(paths.contains("tools/iron/wrench"));
        assertTrue(paths.contains("tools/iron/file"));
        assertTrue(paths.contains("tools/iron/smithing_hammer"));
        assertTrue(paths.contains("tools/stone/pickaxe"));
        assertTrue(paths.contains("tools/stone/hoe"));
        assertTrue(paths.contains("tools/stone/smithing_hammer"));
        assertTrue(paths.contains("tools/granite/pickaxe"));
        assertTrue(paths.contains("tools/granite/axe"));
        assertTrue(paths.contains("tools/granite/smithing_hammer"));
        assertTrue(paths.contains("tools/andesite/pickaxe"));
        assertTrue(paths.contains("tools/flint_pickaxe"));
        assertTrue(paths.contains("tools/flint/pickaxe"));
        assertFalse(paths.contains("tools/wood/pickaxe"));

        if ("true".equalsIgnoreCase(System.getenv("WRITE_WORKBENCH_TOOLS"))) {
            for (var recipe : plan) {
                Path file = RECIPE_ROOT.resolve(recipe.path() + ".json");
                Files.createDirectories(file.getParent());
                Files.writeString(
                        file,
                        GSON.toJson(recipe.toJson()) + "\n",
                        StandardCharsets.UTF_8);
            }
            for (String obsolete : OBSOLETE_PATHS) {
                Files.deleteIfExists(RECIPE_ROOT.resolve(obsolete));
            }
        }

        for (var recipe : plan) {
            Path file = RECIPE_ROOT.resolve(recipe.path() + ".json");
            assertTrue(Files.isRegularFile(file), recipe.path());
            var actual = JsonParser.parseString(Files.readString(file));
            assertEquals(recipe.toJson(), actual, recipe.path());
        }
        for (String obsolete : OBSOLETE_PATHS) {
            assertTrue(
                    Files.notExists(RECIPE_ROOT.resolve(obsolete)),
                    obsolete);
        }
    }
}
