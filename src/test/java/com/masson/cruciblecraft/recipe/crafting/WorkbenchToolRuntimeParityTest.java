package com.masson.cruciblecraft.recipe.crafting;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.google.gson.JsonElement;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;

import net.minecraft.resources.ResourceLocation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks the runtime recipe projection to the former generated workbench-tool
 * set after those files are removed from the resource tree.
 */
class WorkbenchToolRuntimeParityTest {
    private static final Path RECIPE_ROOT = Path.of(
            "src/generated/resources/data/cruciblecraft/recipe");

    @Test
    void plannedRuntimeJsonMatchesEveryGeneratedWorkbenchTool(
            @TempDir Path configDirectory) throws Exception {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        var planned = WorkbenchToolRuntimeRecipes.plannedRecipes(
                materials,
                registered);
        Map<ResourceLocation, JsonElement> defaults = new LinkedHashMap<>();
        WorkbenchToolRuntimeRecipes.addDefaults(
                defaults,
                materials,
                registered);
        Set<String> expected = planned.stream()
                .map(WorkbenchToolRecipePlan.Recipe::path)
                .map(path -> path + ".json")
                .collect(Collectors.toUnmodifiableSet());
        try (Stream<Path> files = Files.walk(RECIPE_ROOT)) {
            Set<String> generated = files.filter(Files::isRegularFile)
                    .filter(path -> path.toString().endsWith(".json"))
                    .map(RECIPE_ROOT::relativize)
                    .map(Path::toString)
                    .map(path -> path.replace('\\', '/'))
                    .filter(path -> path.startsWith("tools/")
                            || path.startsWith("prefix/"))
                    .filter(path -> !path.startsWith("tools/assemble/"))
                    .filter(path -> !path.startsWith("tools/pattern/"))
                    .collect(Collectors.toUnmodifiableSet());
            assertTrue(
                    generated.stream().noneMatch(expected::contains),
                    "runtime workbench paths must not remain generated");
        }
        assertEquals(expected.size(), defaults.size());
        for (WorkbenchToolRecipePlan.Recipe recipe : planned) {
            ResourceLocation id = ResourceLocation.fromNamespaceAndPath(
                    "cruciblecraft",
                    recipe.path());
            assertEquals(recipe.toJson(), defaults.get(id), recipe.path());
        }
    }
}
