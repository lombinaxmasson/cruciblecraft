package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.RecipeMap;

import net.minecraft.resources.ResourceLocation;

/**
 * EMI-independent registration snapshot for configured processing machines.
 */
public record ProcessingEmiRegistrationPlan(
        List<MachineRegistration> machines,
        List<RecipeRegistration> recipes) {

    public ProcessingEmiRegistrationPlan {
        machines = List.copyOf(machines);
        recipes = List.copyOf(recipes);
    }

    public static ProcessingEmiRegistrationPlan create(
            List<ProcessingMachineSpec> specs) {
        Objects.requireNonNull(specs, "specs");
        List<MachineRegistration> machines = new ArrayList<>();
        List<RecipeRegistration> recipes = new ArrayList<>();
        Set<ResourceLocation> categoryIds = new HashSet<>();
        Set<ResourceLocation> mapIds = new HashSet<>();
        for (ProcessingMachineSpec spec : specs) {
            Objects.requireNonNull(spec, "spec");
            RecipeMap map = spec.requireRecipeMap();
            if (!categoryIds.add(spec.id())) {
                throw new IllegalArgumentException(
                        "Duplicate processing EMI category " + spec.id());
            }
            if (!mapIds.add(map.id())) {
                throw new IllegalArgumentException(
                        "Duplicate processing EMI recipe map " + map.id());
            }
            MachineRegistration machine =
                    new MachineRegistration(spec, spec.id(), map);
            machines.add(machine);
            for (RecipeMap.Entry entry : map.entries()) {
                recipes.add(new RecipeRegistration(
                        machine, entry.id(), entry.recipe()));
            }
        }
        return new ProcessingEmiRegistrationPlan(machines, recipes);
    }

    public record MachineRegistration(
            ProcessingMachineSpec spec,
            ResourceLocation categoryId,
            RecipeMap recipeMap) {
        public MachineRegistration {
            Objects.requireNonNull(spec, "spec");
            Objects.requireNonNull(categoryId, "categoryId");
            Objects.requireNonNull(recipeMap, "recipeMap");
            if (!spec.id().equals(categoryId)
                    || !spec.recipeMapId().equals(recipeMap.id())) {
                throw new IllegalArgumentException(
                        "Machine/category/map registration does not match its spec");
            }
        }
    }

    public record RecipeRegistration(
            MachineRegistration machine,
            ResourceLocation id,
            GTRecipe recipe) {
        public RecipeRegistration {
            Objects.requireNonNull(machine, "machine");
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(recipe, "recipe");
        }
    }
}
