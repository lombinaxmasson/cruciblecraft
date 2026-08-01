package com.masson.cruciblecraft.recipe.gt;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec;
import com.masson.cruciblecraft.recipe.rule.LegacyMaterialRuleAdapter;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRecipe;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleRuntimeMetadata;
import com.masson.cruciblecraft.registry.ModRecipeMaps;
import com.masson.cruciblecraft.registry.ModRecipes;
import com.masson.cruciblecraft.registry.ModProcessingMachines;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;

/** Builds immutable concrete RecipeMap and material metadata snapshots. */
public final class GTRecipeMapLoader {
    private GTRecipeMapLoader() {}

    public static void reload(RecipeManager manager) {
        long started = System.nanoTime();
        Map<ResourceLocation, RecipeMap> knownMaps = new HashMap<>();
        Map<RecipeMap, List<ResolvedRecipe>> resolved = new HashMap<>();
        for (RecipeMap map : ModRecipeMaps.ALL) {
            knownMaps.put(map.id(), map);
            resolved.put(map, new ArrayList<>());
        }

        for (RecipeHolder<GTRecipeEntry> holder
                : manager.getAllRecipesFor(ModRecipes.GT_RECIPE_TYPE.get())) {
            GTRecipeEntry entry = holder.value();
            RecipeMap map = knownMaps.get(entry.map());
            if (map == null) {
                throw recipeValidationError(
                        holder.id(),
                        "Unknown recipe map " + entry.map());
            }
            validateTarget(holder.id(), map, entry.recipe());
            resolved.get(map).add(new ResolvedRecipe(holder.id(), entry.recipe(), false));
        }

        List<RecipeHolder<MaterialRuleRecipe>> declarative = manager
                .getAllRecipesFor(ModRecipes.MATERIAL_RULE_TYPE.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .toList();
        MaterialRuleRuntimeMetadata.Publication metadata = buildRuntimeMetadata(declarative);
        if (!metadata.diagnostics().isEmpty()) {
            throw new IllegalArgumentException(
                    "Invalid material-rule metadata: " + String.join("; ", metadata.diagnostics()));
        }
        MaterialCatalog.RuntimePreview materialPreview = MaterialCatalog.previewRuntime(
                metadata.tunings(), metadata.preferences());

        List<RuleSource> rules = new ArrayList<>();
        declarative.stream()
                .filter(holder -> holder.value().rule().target().isPresent())
                .forEach(holder -> rules.add(
                        new RuleSource(holder.id(), holder.value().rule())));
        manager.getAllRecipesFor(ModRecipes.CRUSHER_TYPE.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .filter(holder ->
                        !LegacyMaterialRuleAdapter.replacedByConcreteOreChain(
                                holder.value()))
                .forEach(holder -> rules.add(new RuleSource(
                        holder.id(),
                        LegacyMaterialRuleAdapter.fromCrusher(holder.value()))));
        manager.getAllRecipesFor(ModRecipes.ANVIL_TYPE.get()).stream()
                .sorted(Comparator.comparing(holder -> holder.id().toString()))
                .forEach(holder -> rules.add(new RuleSource(
                        holder.id(),
                        LegacyMaterialRuleAdapter.fromAnvil(holder.value()))));

        rules.stream()
                .sorted(Comparator.comparing(source -> source.id().toString()))
                .forEach(source -> expand(
                        source,
                        knownMaps,
                        resolved,
                        materialPreview.definitions().values(),
                        materialPreview.unificationPreferences()));

        LinkedHashMap<RecipeMap, List<RecipeMap.Entry>> candidates = new LinkedHashMap<>();
        for (RecipeMap map : ModRecipeMaps.ALL) {
            List<RecipeMap.Entry> ordered = resolved.get(map).stream()
                    .sorted((left, right) -> RecipeExpansionRules.comparePriority(
                            left.materialSpecific(),
                            left.id().toString(),
                            right.materialSpecific(),
                            right.id().toString()))
                    .map(recipe -> new RecipeMap.Entry(recipe.id(), recipe.recipe()))
                    .toList();
            validateNoShadows(map, ordered);
            candidates.put(map, ordered);
        }
        validateRequiredMaps(candidates);
        int t3Recipes = 0;
        for (var spec : ModProcessingMachines.T3_MACHINES) {
            int count = candidates.get(spec.requireRecipeMap()).size();
            t3Recipes += count;
        }
        if (t3Recipes > ModProcessingMachines.T3_EXPANSION_BUDGET) {
            throw new IllegalStateException(
                    "T3 expansion " + t3Recipes + " exceeds budget "
                            + ModProcessingMachines.T3_EXPANSION_BUDGET);
        }

        List<RecipeMap.Prepared> prepared = candidates.entrySet().stream()
                .map(entry -> entry.getKey().prepareRecipes(entry.getValue()))
                .toList();
        long epoch = GTRecipeRuntimeEpoch.publish(materialPreview, prepared);
        for (int index = 0; index < ModRecipeMaps.ALL.size(); index++) {
            RecipeMap map = ModRecipeMaps.ALL.get(index);
            RecipeMap.Prepared snapshot = prepared.get(index);
            CrucibleCraft.LOGGER.info(
                    "RecipeMap {} - {} recipes, {} unindexed",
                    map.id(), snapshot.entries().size(), snapshot.unindexedRecipeCount());
        }
        CrucibleCraft.LOGGER.info(
                "Published recipe epoch {} with {} playable T3 recipes across {} maps "
                        + "in {} ms (budget {})",
                epoch,
                t3Recipes,
                ModProcessingMachines.T3_MACHINES.size(),
                (System.nanoTime() - started) / 1_000_000L,
                ModProcessingMachines.T3_EXPANSION_BUDGET);
    }

    private static void expand(
            RuleSource source,
            Map<ResourceLocation, RecipeMap> knownMaps,
            Map<RecipeMap, List<ResolvedRecipe>> output,
            java.util.Collection<com.masson.cruciblecraft.material.def.MaterialDefinition>
                    effectiveMaterials,
            Map<String, String> candidatePreferences) {
        ResourceLocation target = source.rule().target().orElseThrow();
        RecipeMap map = knownMaps.get(target);
        if (map == null) {
            throw recipeValidationError(
                    source.id(),
                    "Unknown recipe map " + target);
        }
        for (MaterialRuleExpansion.Expanded expanded
                : MaterialRuleExpansion.expand(
                        source.id(), source.rule(), effectiveMaterials, candidatePreferences)) {
            validateTarget(expanded.id(), map, expanded.recipe());
            output.get(map).add(new ResolvedRecipe(
                    expanded.id(),
                    expanded.recipe(),
                    expanded.materialSpecific()));
        }
    }

    private static void validateTarget(
            ResourceLocation recipeId,
            RecipeMap map,
            GTRecipe recipe) {
        boolean anvil = map == ModRecipeMaps.ANVIL
                || map == ModRecipeMaps.ANVIL_BEND_SMALL
                || map == ModRecipeMaps.ANVIL_BEND_BIG;
        if (anvil && !AnvilRecipeExecutionRules.supportsOutputs(
                recipe.itemOutputs().size(),
                recipe.outputChances().isEmpty()
                        ? -1
                        : recipe.outputChances().getFirst())) {
            throw recipeValidationError(
                    recipeId,
                    "Unsupported anvil output shape for map " + map.id());
        }
        var machine = ModProcessingMachines.forRecipeMap(map.id());
        if (machine.isPresent()) {
            var invalid = machine.get().validator().validate(recipe);
            if (invalid.isPresent()) {
                throw recipeValidationError(
                        recipeId,
                        "Machine " + machine.get().id() + " rejected recipe for map "
                                + map.id() + " (" + invalid.get() + ")");
            }
        }
    }

    static void validateRequiredMaps(
            Map<RecipeMap, List<RecipeMap.Entry>> candidates) {
        java.util.LinkedHashSet<RecipeMap> required = new java.util.LinkedHashSet<>();
        required.add(ModRecipeMaps.COKE_OVEN);
        required.add(ModRecipeMaps.CRUSHER);
        required.add(ModRecipeMaps.ANVIL);
        ModProcessingMachines.CONFIGURED_MACHINES.stream()
                .map(ProcessingMachineSpec::requireRecipeMap)
                .forEach(required::add);
        for (RecipeMap map : required) {
            List<RecipeMap.Entry> entries = candidates.get(map);
            if (entries == null) {
                throw new IllegalArgumentException("Missing required map candidate " + map.id());
            }
            if (entries.isEmpty()) {
                throw new IllegalArgumentException(
                        "Required playable map " + map.id() + " loaded zero recipes");
            }
        }
    }

    static void validateNoShadows(RecipeMap map, List<RecipeMap.Entry> entries) {
        Map<String, ResourceLocation> signatures = new HashMap<>();
        for (RecipeMap.Entry entry : entries) {
            String signature = inputSignature(entry.recipe());
            ResourceLocation previous = signatures.putIfAbsent(signature, entry.id());
            if (previous != null) {
                throw new IllegalArgumentException(
                        "Shadowed input signature in map " + map.id() + ": "
                                + describeLogicalResource(previous) + " conflicts with "
                                + describeLogicalResource(entry.id())
                                + "; signature=" + signature
                                + ". Change the recipe input or specificity to remove "
                                + "the conflict.");
            }
        }
    }

    static String logicalRecipeResourcePath(ResourceLocation recipeId) {
        return "data/" + recipeId.getNamespace() + "/recipe/"
                + recipeId.getPath() + ".json";
    }

    private static IllegalArgumentException recipeValidationError(
            ResourceLocation recipeId,
            String message) {
        return new IllegalArgumentException(
                message + "; " + describeLogicalResource(recipeId));
    }

    private static String describeLogicalResource(ResourceLocation recipeId) {
        return "recipe " + recipeId + " (logical resource path "
                + logicalRecipeResourcePath(recipeId) + ")";
    }

    private static String inputSignature(GTRecipe recipe) {
        List<String> items = new ArrayList<>();
        for (int index = 0; index < recipe.itemInputs().size(); index++) {
            var ingredient = recipe.itemInputs().get(index);
            String alternatives = java.util.Arrays.stream(ingredient.getItems())
                    .filter(stack -> !stack.isEmpty())
                    .map(GTRecipeMapLoader::stackIdentity)
                    .sorted()
                    .collect(java.util.stream.Collectors.joining("|"));
            items.add(recipe.itemInputCounts().get(index)
                    + "@" + ingredient.getClass().getName()
                    + "@" + java.util.Arrays.toString(ingredient.getValues())
                    + "@" + alternatives
                    + (ingredient.isCustom()
                            ? "@" + ingredient.getCustomIngredient()
                            : ""));
        }
        items.sort(String::compareTo);
        List<String> fluids = recipe.fluidInputs().stream()
                .map(stack -> stack.getAmount()
                        + "@" + BuiltInRegistries.FLUID.getKey(stack.getFluid())
                        + "@" + stack.getComponentsPatch())
                .sorted()
                .toList();
        return String.join(",", items) + "||" + String.join(",", fluids);
    }

    private static String stackIdentity(ItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getItem())
                + "@" + stack.getComponentsPatch();
    }

    private static MaterialRuleRuntimeMetadata.Publication buildRuntimeMetadata(
            List<RecipeHolder<MaterialRuleRecipe>> holders) {
        return MaterialRuleRuntimeMetadata.build(
                        holders.stream()
                                .map(holder -> new MaterialRuleRuntimeMetadata.Source(
                                        holder.id(), holder.value().rule()))
                                .toList(),
                        MaterialCatalog.startupValues(),
                        MaterialLookup::isValidPreference);
    }

    private record RuleSource(ResourceLocation id, MaterialRule rule) {}

    private record ResolvedRecipe(
            ResourceLocation id,
            GTRecipe recipe,
            boolean materialSpecific) {}
}
