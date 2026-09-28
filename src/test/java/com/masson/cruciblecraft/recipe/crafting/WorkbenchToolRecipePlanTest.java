package com.masson.cruciblecraft.recipe.crafting;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;

class WorkbenchToolRecipePlanTest {
    private static final Path RECIPE_ROOT = Path.of(
            "src/generated/resources/data/cruciblecraft/recipe");
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
        var assemblies = WorkbenchToolRecipePlan.assemblies();
        Set<String> paths = plan.stream()
                .map(WorkbenchToolRecipePlan.Recipe::path)
                .collect(Collectors.toUnmodifiableSet());
        assertEquals(plan.size(), paths.size());
        assertTrue(paths.contains("tools/iron/wrench"));
        assertTrue(paths.contains("tools/iron/tool_head_file"));
        assertTrue(paths.contains("tools/iron/tool_head_hammer"));
        assertTrue(paths.contains("tools/iron/tool_head_pickaxe"));
        assertTrue(paths.contains("tools/iron/tool_head_shovel"));
        assertTrue(paths.contains("tools/iron/tool_head_axe"));
        assertTrue(paths.contains("tools/iron/tool_head_hoe"));
        assertTrue(paths.contains("tools/iron/tool_head_sword"));
        assertTrue(paths.contains("tools/iron/knife"));
        assertTrue(paths.contains("tools/iron/club"));
        assertTrue(paths.contains("tools/iron/crowbar"));
        assertTrue(paths.contains("tools/iron/plunger"));
        assertTrue(paths.contains("tools/iron/pincers"));
        assertFalse(paths.contains("tools/iron/file"));
        assertFalse(paths.contains("tools/iron/pickaxe"));
        assertTrue(paths.contains("tools/stone/pickaxe"));
        assertTrue(paths.contains("tools/stone/hoe"));
        assertTrue(paths.contains("tools/stone/smithing_hammer"));
        assertTrue(paths.contains("tools/stone/club"));
        assertTrue(paths.contains("tools/granite/pickaxe"));
        assertTrue(paths.contains("tools/granite/axe"));
        assertTrue(paths.contains("tools/granite/smithing_hammer"));
        assertTrue(paths.contains("tools/andesite/pickaxe"));
        assertTrue(paths.contains("tools/flint_pickaxe"));
        assertTrue(paths.contains("tools/flint_axe"));
        assertTrue(paths.contains("tools/flint_shovel"));
        assertTrue(paths.contains("tools/flint_knife"));
        assertTrue(paths.contains("tools/obsidian/knife_from_rock"));
        assertTrue(paths.contains("tools/obsidian/axe"));
        assertTrue(paths.contains("tools/bone/club"));
        assertTrue(paths.contains("tools/petrified_wood/smithing_hammer"));
        assertFalse(paths.contains("tools/flint/pickaxe"));
        assertFalse(paths.contains("tools/flint/axe"));
        assertFalse(paths.contains("tools/obsidian/hoe"));
        assertFalse(paths.contains("tools/obsidian/club"));
        assertFalse(paths.contains("tools/obsidian/smithing_hammer"));
        assertTrue(paths.contains("prefix/boule2plate_gem/redstone_alloy"));
        assertTrue(paths.contains("prefix/plate_gem2tiny/redstone_alloy"));
        assertTrue(paths.contains("prefix/boule2plate_gem/silicon"));
        assertTrue(paths.contains("prefix/plate_gem2tiny/silicon"));
        assertTrue(paths.contains("prefix/flawless2plate_gem/diamond"));
        assertTrue(paths.contains("prefix/gem2tiny_plate_gem/diamond"));
        assertTrue(paths.contains("prefix/plate_gem2tiny/diamond"));
        assertFalse(paths.contains("tools/wood/pickaxe"));
        for (var recipe : plan) {
            if (recipe.path().equals("tools/flint_knife")
                    || recipe.path().equals("tools/flint_axe")
                    || recipe.path().equals("tools/stone/axe")
                    || recipe.path().equals("tools/stone/hoe")
                    || recipe.path().equals("tools/obsidian/knife_from_rock")
                    || recipe.path().equals("tools/gold/rolling_pin")
                    || recipe.path().equals("tools/bone/club")) {
                assertTrue(recipe.mirrored(), recipe.path());
            }
            if (recipe.path().equals("tools/flint_pickaxe")
                    || recipe.path().equals("tools/flint_shovel")
                    || recipe.path().equals("tools/stone/pickaxe")
                    || recipe.path().equals("tools/stone/shovel")
                    || recipe.path().equals("tools/stone/knife")
                    || recipe.path().equals("tools/iron/club")) {
                assertFalse(recipe.mirrored(), recipe.path());
            }
        }
        assertEquals(
                plan,
                WorkbenchToolRuntimeRecipes.plannedRecipes(
                        materials,
                        registered));
        assertEquals(18, assemblies.size());
        assertTrue(assemblies.stream().anyMatch(assembly ->
                "tools/assemble/magnifying_glass".equals(assembly.path())
                        && "lens".equals(assembly.headPrefix())
                        && "cruciblecraft:material_magnifying_glass".equals(
                                assembly.resultId())));
        var variants = WorkbenchToolRecipePlan.assemblyVariants(
                materials, registered);
        assertTrue(variants.stream().anyMatch(variant ->
                "tools/assemble/pickaxe/iron".equals(variant.path())
                        && "cruciblecraft:iron/tool_head_pickaxe".equals(
                                variant.headLogicalId())
                        && "cruciblecraft:material_pickaxe".equals(
                                variant.resultId())
                        && "iron".equals(variant.material())));
        assertTrue(variants.stream().anyMatch(variant ->
                "tools/assemble/smithing_hammer/iron".equals(variant.path())));
        assertFalse(variants.stream().anyMatch(variant ->
                variant.path().contains("/wrench/")
                        || "tools/assemble/soft_hammer/iron".equals(
                                variant.path())
                        || variant.path().startsWith(
                                "tools/assemble/gem_pick/")));
        assertTrue(variants.size() > 100, variants.size() + " assembly rows");
        for (var recipe : plan) {
            Set<String> keys = new HashSet<>();
            keys.addAll(recipe.ingredients().keySet());
            keys.addAll(recipe.catalysts().keySet());
            for (String row : recipe.pattern()) {
                for (int index = 0; index < row.length(); index++) {
                    String symbol = String.valueOf(row.charAt(index));
                    if (!" ".equals(symbol)) {
                        assertTrue(
                                keys.contains(symbol),
                                recipe.path() + " missing symbol " + symbol);
                    }
                }
            }
        }

        for (var assembly : assemblies) {
            Path file = RECIPE_ROOT.resolve(assembly.path() + ".json");
            assertTrue(Files.isRegularFile(file), assembly.path());
            var actual = JsonParser.parseString(Files.readString(file));
            assertEquals(assembly.toJson(), actual, assembly.path());
        }
        for (var recipe : plan) {
            Path file = RECIPE_ROOT.resolve(recipe.path() + ".json");
            assertFalse(
                    Files.isRegularFile(file),
                    recipe.path() + " must be supplied at reload time");
        }
        for (String obsolete : OBSOLETE_PATHS) {
            assertTrue(
                    Files.notExists(RECIPE_ROOT.resolve(obsolete)),
                    obsolete);
        }
    }
}
