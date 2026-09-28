package com.masson.cruciblecraft.recipe.crafting;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.JsonElement;
import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;

/**
 * Shared materialization boundary for GT6 workbench tool recipes.
 *
 * <p>The recipe plan remains the source of truth. Datagen uses
 * {@link #materialize(WorkbenchToolRecipePlan.Recipe)} to write the historical
 * JSON representation, while the reload mixin adds that same representation
 * to the recipe input map when the release does not carry the per-material
 * files.
 */
public final class WorkbenchToolRuntimeRecipes {
    private WorkbenchToolRuntimeRecipes() {}

    /**
     * Plans all workbench recipes from the committed startup material gate.
     */
    public static List<WorkbenchToolRecipePlan.Recipe> plannedRecipes() {
        Collection<MaterialDefinition> materials = MaterialCatalog.startupValues();
        return plannedRecipes(
                materials,
                MaterialRegistrationGate.load(materials));
    }

    /**
     * Test/datagen overload that keeps the material snapshot explicit.
     */
    public static List<WorkbenchToolRecipePlan.Recipe> plannedRecipes(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        return WorkbenchToolRecipePlan.plan(materials, registeredForms);
    }

    /**
     * Adds default JSON recipes without replacing a datapack recipe with the
     * same id. RecipeManager parses the inserted JSON through the normal
     * conditional recipe codec immediately afterwards.
     */
    public static void addDefaults(Map<ResourceLocation, JsonElement> recipes) {
        Collection<MaterialDefinition> materials = MaterialCatalog.startupValues();
        addDefaults(
                recipes,
                materials,
                MaterialRegistrationGate.load(materials));
    }

    /**
     * Explicit-snapshot overload used by datagen parity tests.
     */
    public static void addDefaults(
            Map<ResourceLocation, JsonElement> recipes,
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> registeredForms) {
        for (WorkbenchToolRecipePlan.Recipe planned
                : plannedRecipes(materials, registeredForms)) {
            recipes.putIfAbsent(
                    ResourceLocation.fromNamespaceAndPath(
                            CrucibleCraft.MODID,
                            planned.path()),
                    planned.toJson());
        }
    }

    /**
     * Materializes one planned recipe through the same codec used by reload.
     * Keeping this here prevents datagen and runtime from carrying separate
     * ingredient/result conversion logic.
     */
    public static ShapedCatalystRecipe materialize(
            WorkbenchToolRecipePlan.Recipe planned) {
        return ShapedCatalystRecipe.CODEC.codec()
                .parse(JsonOps.INSTANCE, planned.toJson())
                .getOrThrow(message -> new IllegalStateException(
                        "Failed to materialize workbench recipe "
                                + planned.path() + ": " + message));
    }

    /**
     * Encoded form used by parity tests and diagnostics.
     */
    public static Map<ResourceLocation, JsonElement> defaultJsonRecipes() {
        LinkedHashMap<ResourceLocation, JsonElement> result = new LinkedHashMap<>();
        addDefaults(result);
        return Map.copyOf(result);
    }
}
