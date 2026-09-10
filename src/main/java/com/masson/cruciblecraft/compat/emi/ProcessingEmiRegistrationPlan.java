package com.masson.cruciblecraft.compat.emi;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
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
        List<RecipeRegistration> recipes,
        RegistrationCensus census) {

    public ProcessingEmiRegistrationPlan {
        machines = List.copyOf(machines);
        recipes = List.copyOf(recipes);
        Objects.requireNonNull(census, "census");
    }

    public ProcessingEmiRegistrationPlan(
            List<MachineRegistration> machines,
            List<RecipeRegistration> recipes) {
        this(machines, recipes, censusOf(machines, recipes));
    }

    public static ProcessingEmiRegistrationPlan create(
            List<ProcessingMachineSpec> specs) {
        Objects.requireNonNull(specs, "specs");
        List<MachineRegistration> machines = new ArrayList<>();
        List<RecipeRegistration> recipes = new ArrayList<>();
        Set<ResourceLocation> categoryIds = new HashSet<>();
        Set<String> categoryRecipeIds = new HashSet<>();
        for (ProcessingMachineSpec spec : specs) {
            Objects.requireNonNull(spec, "spec");
            RecipeMap map = spec.requireRecipeMap();
            if (!categoryIds.add(spec.id())) {
                throw new IllegalArgumentException(
                        "Duplicate processing EMI category " + spec.id());
            }
            MachineRegistration machine =
                    new MachineRegistration(spec, spec.id(), map);
            machines.add(machine);
            for (RecipeMap.Entry entry : map.entries()) {
                String key = spec.id() + "|" + entry.id();
                if (!categoryRecipeIds.add(key)) {
                    throw new IllegalArgumentException(
                            "Duplicate processing EMI recipe "
                                    + spec.id() + " " + entry.id());
                }
                recipes.add(new RecipeRegistration(
                        machine, entry.id(), entry.recipe()));
            }
        }
        RegistrationCensus census = censusOf(machines, recipes);
        if (!census.exactMatch()) {
            throw new IllegalArgumentException(
                    "Processing EMI census mismatch configured="
                            + census.configuredMachines()
                            + " live=" + census.liveEntries()
                            + " registered=" + census.registeredEntries()
                            + " missing=" + census.missingLiveIds()
                            + " extra=" + census.extraRegisteredIds());
        }
        return new ProcessingEmiRegistrationPlan(machines, recipes, census);
    }

    public static RegistrationCensus censusOf(
            List<MachineRegistration> machines,
            List<RecipeRegistration> recipes) {
        LinkedHashSet<String> liveKeys = new LinkedHashSet<>();
        for (MachineRegistration machine : machines) {
            for (RecipeMap.Entry entry : machine.recipeMap().entries()) {
                liveKeys.add(machine.categoryId() + "|" + entry.id());
            }
        }
        LinkedHashSet<String> registeredKeys = new LinkedHashSet<>();
        for (RecipeRegistration recipe : recipes) {
            registeredKeys.add(
                    recipe.machine().categoryId() + "|" + recipe.id());
        }
        List<String> missing = liveKeys.stream()
                .filter(id -> !registeredKeys.contains(id))
                .toList();
        List<String> extra = registeredKeys.stream()
                .filter(id -> !liveKeys.contains(id))
                .toList();
        return new RegistrationCensus(
                machines.size(),
                liveKeys.size(),
                registeredKeys.size(),
                missing,
                extra);
    }

    public record RegistrationCensus(
            int configuredMachines,
            int liveEntries,
            int registeredEntries,
            List<String> missingLiveIds,
            List<String> extraRegisteredIds) {
        public RegistrationCensus {
            missingLiveIds = List.copyOf(missingLiveIds);
            extraRegisteredIds = List.copyOf(extraRegisteredIds);
        }

        public boolean exactMatch() {
            return missingLiveIds.isEmpty()
                    && extraRegisteredIds.isEmpty()
                    && liveEntries == registeredEntries;
        }
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
