package com.masson.cruciblecraft.recipe.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.PrefixMaterialItem;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialComponentPolicies;
import com.masson.cruciblecraft.material.def.GT6MaterialMetadata;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.logistics.pipe.PipeCatalog;
import com.masson.cruciblecraft.recipe.gt.ComponentIngredientIndex;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.recipe.gt.ItemInputAction;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.component.DataComponentPredicate;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.common.crafting.DataComponentIngredient;
import net.neoforged.neoforge.fluids.FluidStack;

/** One generic projection path for every declarative and legacy-adapted rule. */
public final class MaterialRuleExpansion {
    private static final int MAX_ITEM_BATCH = 64;
    private MaterialRuleExpansion() {}

    /** One explicit factual/registered form snapshot shared by a full expansion pass. */
    public record FormIndexes(
            Map<MaterialPrefix, List<MaterialDefinition>> factual,
            Map<MaterialPrefix, List<MaterialDefinition>> registered) {
        public FormIndexes {
            factual = Map.copyOf(factual);
            registered = Map.copyOf(registered);
        }

        public static FormIndexes factualOnly(
                Collection<MaterialDefinition> materials) {
            Map<MaterialPrefix, List<MaterialDefinition>> factual =
                    buildPrefixIndex(materials);
            return new FormIndexes(factual, factual);
        }

        public static FormIndexes withRegisteredForms(
                Collection<MaterialDefinition> materials,
                Map<String, List<MaterialPrefix>> registeredForms) {
            return new FormIndexes(
                    buildPrefixIndex(materials),
                    buildPrefixIndex(materials, registeredForms));
        }
    }

    public static List<Expanded> expand(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            ResourceResolver resolver,
            FormIndexes formIndexes) {
        return expandInternal(
                ruleId, rule, availableMaterials, resolver, formIndexes);
    }

    public static List<Expanded> expand(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            Map<String, String> candidatePreferences,
            FormIndexes formIndexes) {
        return expandInternal(
                ruleId,
                rule,
                availableMaterials,
                candidateResolver(candidatePreferences),
                formIndexes);
    }

    /**
     * Materializes one exact sparse relation. Production family providers use
     * this path so long-tail lookup does not expand the other 2,781 relations.
     */
    public static Expanded expandSparseRelation(
            MaterialRule owner,
            MaterialRule.SparseRelation relation,
            Collection<MaterialDefinition> availableMaterials,
            Map<String, String> candidatePreferences,
            FormIndexes formIndexes) {
        return expandSparseRelation(
                owner,
                relation,
                availableMaterials,
                candidateResolver(candidatePreferences),
                formIndexes);
    }

    public static Expanded expandSparseRelation(
            MaterialRule owner,
            MaterialRule.SparseRelation relation,
            Collection<MaterialDefinition> availableMaterials,
            ResourceResolver resolver,
            FormIndexes formIndexes) {
        if (owner.sparse().isEmpty()
                || owner.sparse().orElseThrow().relations().stream()
                        .noneMatch(candidate -> candidate == relation
                                || candidate.equals(relation))) {
            throw new IllegalArgumentException(
                    "Sparse relation is not owned by the supplied material rule");
        }
        List<Expanded> selected = expandInternal(
                sparseParentId(relation),
                sparseRelationRule(
                        owner, owner.sparse().orElseThrow(), relation),
                availableMaterials,
                resolver,
                formIndexes);
        if (selected.size() != 1
                || !selected.getFirst().id().equals(relation.stableId())) {
            throw new IllegalArgumentException(
                    "Sparse relation did not materialize exactly its stable id "
                            + relation.stableId());
        }
        return selected.getFirst();
    }

    private static List<Expanded> expandInternal(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            ResourceResolver resolver,
            FormIndexes formIndexes) {
        if (rule.target().isEmpty()) {
            return List.of();
        }
        if (rule.sparse().isPresent()) {
            return expandSparse(
                    rule,
                    availableMaterials,
                    resolver,
                    formIndexes);
        }
        Compiled compiled = compile(ruleId, rule);
        Map<String, MaterialDefinition> materialsById = index(availableMaterials);
        Set<String> knownMaterialTags = knownMaterialTags(availableMaterials);
        List<MaterialDefinition> candidates =
                candidateMaterials(
                        ruleId,
                        rule,
                        compiled.requiredPrefixes(),
                        availableMaterials,
                        formIndexes.registered());
        List<Expanded> result = new ArrayList<>();
        for (MaterialDefinition material : candidates) {
            EvaluationContext context = new EvaluationContext(
                    material,
                    materialsById,
                    knownMaterialTags,
                    indexedPrefixes(material, formIndexes.factual()),
                    indexedPrefixes(material, formIndexes.registered()),
                    formIndexes.registered(),
                    compiled.inputUnit(),
                    compiled.outputUnit(),
                    compiled.singlePrefixUnit());
            if (compiled.conditions().stream().anyMatch(condition ->
                    !condition.evaluateBoolean(context))) {
                continue;
            }
            Optional<GTRecipe> projected =
                    project(
                            ruleId,
                            rule,
                            compiled,
                            material,
                            context,
                            materialsById,
                            resolver,
                            formIndexes.registered());
            projected.ifPresent(recipe -> result.add(new Expanded(
                    expandedId(ruleId, material.id()),
                    rule.target().orElseThrow(),
                    recipe,
                    rule.material().isPresent())));
        }
        return List.copyOf(result);
    }

    private static List<Expanded> expandSparse(
            MaterialRule rule,
            Collection<MaterialDefinition> availableMaterials,
            ResourceResolver resolver,
            FormIndexes formIndexes) {
        MaterialRule.SparseTable table = rule.sparse().orElseThrow();
        List<Expanded> result = new ArrayList<>();
        for (MaterialRule.SparseRelation relation : table.relations()) {
            List<Expanded> selected = expandInternal(
                    sparseParentId(relation),
                    sparseRelationRule(rule, table, relation),
                    availableMaterials,
                    resolver,
                    formIndexes);
            if (selected.size() != 1
                    || !selected.getFirst().id().equals(relation.stableId())) {
                throw new IllegalArgumentException(
                        "Sparse relation did not materialize exactly its stable id "
                                + relation.stableId());
            }
            result.add(selected.getFirst());
        }
        return List.copyOf(result);
    }

    /** Test-only plan expansion that deliberately treats every factual form as registered. */
    public static List<Plan> expandFactualPlans(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials) {
        return expandPlans(
                ruleId,
                rule,
                availableMaterials,
                FormIndexes.factualOnly(availableMaterials));
    }

    /** Plan expansion with an explicit player-obtainable form snapshot. */
    public static List<Plan> expandPlansWithRegisteredForms(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            Map<String, List<MaterialPrefix>> formsByMaterial) {
        return expandPlans(
                ruleId,
                rule,
                availableMaterials,
                FormIndexes.withRegisteredForms(
                        availableMaterials, formsByMaterial));
    }

    private static List<Plan> expandPlans(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            FormIndexes formIndexes) {
        if (rule.target().isEmpty()) {
            return List.of();
        }
        if (rule.sparse().isPresent()) {
            return expandSparsePlans(
                    rule,
                    availableMaterials,
                    formIndexes);
        }
        Compiled compiled = compile(ruleId, rule);
        Map<String, MaterialDefinition> materialsById = index(availableMaterials);
        Set<String> knownMaterialTags = knownMaterialTags(availableMaterials);
        List<Plan> plans = new ArrayList<>();
        for (MaterialDefinition material
                : candidateMaterials(
                        ruleId,
                        rule,
                        compiled.requiredPrefixes(),
                        availableMaterials,
                        formIndexes.registered())) {
            EvaluationContext context = new EvaluationContext(
                    material,
                    materialsById,
                    knownMaterialTags,
                    indexedPrefixes(material, formIndexes.factual()),
                    indexedPrefixes(material, formIndexes.registered()),
                    formIndexes.registered(),
                    compiled.inputUnit(),
                    compiled.outputUnit(),
                    compiled.singlePrefixUnit());
            if (compiled.conditions().stream().anyMatch(condition ->
                    !condition.evaluateBoolean(context))) {
                continue;
            }
            MaterialRule.MaterialOverride override =
                    rule.materialOverrides().getOrDefault(
                            material.id(), MaterialRule.MaterialOverride.empty());
            List<PlannedResource> itemInputs = new ArrayList<>();
            List<ItemInputAction> itemInputActions = new ArrayList<>();
            for (int index = 0; index < compiled.itemInputs().size(); index++) {
                CompiledItem resource = compiled.itemInputs().get(index);
                SelectedMaterial selection = selectMaterial(
                        material, resource.materialSelector(), materialsById).orElse(null);
                if (selection == null || resource.prefix().isPresent()
                        && !hasIndexedPrefix(
                                selection.material(),
                                resource.prefix().get(),
                                formIndexes.registered())) {
                    throw unresolvedRequiredResource(
                            ruleId, material, "item input", index, resource);
                }
                Optional<Integer> count = evaluateResourceCount(
                        override.itemInputCounts().get(Integer.toString(index)),
                        resource.amount(),
                        context.forResource(resource.prefix(), selection.targetUnits()),
                        ruleId,
                        resource.candidateSpecificAmount(),
                        resource.fixed().isPresent()
                                || resource.tag().isPresent());
                if (count.isEmpty()) {
                    itemInputs.clear();
                    break;
                }
                itemInputs.add(new PlannedResource(
                        ref(
                                resource.prefix(),
                                resource.fixed(),
                                resource.tag(),
                                selection.material().id(),
                                resolveStringComponents(
                                        resource.stringComponents(), material)),
                        count.get(),
                        GTRecipe.GUARANTEED_CHANCE));
                itemInputActions.add(inputAction(resource, count.get()));
            }
            if (itemInputs.size() != compiled.itemInputs().size()) continue;
            List<PlannedResource> itemOutputs = new ArrayList<>();
            for (int index = 0; index < compiled.itemOutputs().size(); index++) {
                CompiledItem resource = compiled.itemOutputs().get(index);
                SelectedMaterial selection = selectMaterial(
                        material, resource.materialSelector(), materialsById).orElse(null);
                if (selection == null || resource.prefix().isPresent()
                        && !hasIndexedPrefix(
                                selection.material(),
                                resource.prefix().get(),
                                formIndexes.registered())) {
                    if (resource.optional()) continue;
                    throw unresolvedRequiredResource(
                            ruleId, material, "item output", index, resource);
                }
                EvaluationContext resourceContext =
                        context.forResource(resource.prefix(), selection.targetUnits());
                Optional<Integer> count = evaluateResourceCount(
                        override.itemOutputCounts().get(Integer.toString(index)),
                        resource.amount(), resourceContext, ruleId,
                        resource.candidateSpecificAmount(), false);
                if (count.isEmpty()) {
                    if (resource.optional()) continue;
                    itemOutputs.clear();
                    break;
                }
                itemOutputs.add(new PlannedResource(
                        ref(
                                resource.prefix(),
                                resource.fixed(),
                                selection.material().id(),
                                resolveStringComponents(
                                        resource.stringComponents(), material)),
                        count.get(),
                        evaluateInt(
                                override.outputChances().get(Integer.toString(index)),
                                resource.chance(), resourceContext, ruleId)));
            }
            if (itemOutputs.isEmpty() && !compiled.itemOutputs().isEmpty()) continue;
            List<PlannedResource> fluidInputs = plannedFluids(
                    compiled.fluidInputs(), override, context, ruleId, "input");
            List<PlannedResource> fluidOutputs = plannedFluids(
                    compiled.fluidOutputs(), override, context, ruleId, "output");
            plans.add(new Plan(
                    expandedId(ruleId, material.id()),
                    rule.target().orElseThrow(),
                    material.id(),
                    itemInputs,
                    itemInputActions,
                    itemOutputs,
                    fluidInputs,
                    fluidOutputs,
                    evaluateInt(override.duration().orElse(null), compiled.duration(), context, ruleId),
                    evaluateLong(override.eut().orElse(null), compiled.eut(), context, ruleId),
                    evaluateLong(
                            override.specialValue().orElse(null),
                            compiled.specialValue(), context, ruleId),
                    rule.canBeBuffered(),
                    rule.material().isPresent()));
        }
        return List.copyOf(plans);
    }

    private static List<Plan> expandSparsePlans(
            MaterialRule rule,
            Collection<MaterialDefinition> availableMaterials,
            FormIndexes formIndexes) {
        MaterialRule.SparseTable table = rule.sparse().orElseThrow();
        List<Plan> result = new ArrayList<>();
        for (MaterialRule.SparseRelation relation : table.relations()) {
            List<Plan> selected = expandPlans(
                    sparseParentId(relation),
                    sparseRelationRule(rule, table, relation),
                    availableMaterials,
                    formIndexes);
            if (selected.size() != 1
                    || !selected.getFirst().id().equals(relation.stableId())) {
                throw new IllegalArgumentException(
                        "Sparse relation did not plan exactly its stable id "
                                + relation.stableId());
            }
            result.add(selected.getFirst());
        }
        return List.copyOf(result);
    }

    private static ResourceLocation sparseParentId(
            MaterialRule.SparseRelation relation) {
        String path = relation.stableId().getPath();
        int separator = path.lastIndexOf('/');
        if (separator <= 0) {
            throw new IllegalArgumentException(
                    "Sparse stable id has no material suffix: "
                            + relation.stableId());
        }
        return ResourceLocation.fromNamespaceAndPath(
                relation.stableId().getNamespace(),
                path.substring(0, separator));
    }

    private static MaterialRule sparseRelationRule(
            MaterialRule owner,
            MaterialRule.SparseTable table,
            MaterialRule.SparseRelation relation) {
        return new MaterialRule(
                owner.target(),
                List.of(
                        new MaterialRule.ItemResource(
                                Optional.of("cruciblecraft:"
                                        + relation.input().prefix()),
                                Optional.empty(),
                                Integer.toString(relation.input().count()),
                                Integer.toString(GTRecipe.GUARANTEED_CHANCE)),
                        new MaterialRule.ItemResource(
                                Optional.empty(),
                                Optional.of(table.shapeItem()),
                                "0",
                                Integer.toString(GTRecipe.GUARANTEED_CHANCE))),
                List.of(new MaterialRule.ItemResource(
                        Optional.of("cruciblecraft:"
                                + relation.output().prefix()),
                        Optional.empty(),
                        Integer.toString(relation.output().count()),
                        Integer.toString(GTRecipe.GUARANTEED_CHANCE))),
                List.of(),
                List.of(),
                Integer.toString(relation.duration()),
                Long.toString(relation.eut()),
                "0",
                owner.canBeBuffered(),
                Optional.of(relation.material()),
                Map.of(),
                List.of(),
                Optional.empty(),
                List.of());
    }

    private static List<PlannedResource> plannedFluids(
            List<CompiledFluid> resources,
            MaterialRule.MaterialOverride override,
            EvaluationContext context,
            ResourceLocation ruleId,
            String side) {
        List<PlannedResource> result = new ArrayList<>();
        for (int index = 0; index < resources.size(); index++) {
            CompiledFluid resource = resources.get(index);
            result.add(new PlannedResource(
                    refFluid(
                            resource.prefix(),
                            resource.fixed(),
                            resource.materialFluid()),
                    evaluateInt(
                            override.fluidAmounts().get(side + ":" + index),
                            resource.amount(), context, ruleId),
                    GTRecipe.GUARANTEED_CHANCE));
        }
        return List.copyOf(result);
    }

    private static ResourceRef ref(
            Optional<MaterialPrefix> prefix,
            Optional<ResourceLocation> fixed,
            Optional<ResourceLocation> tag,
            String materialId,
            Map<ResourceLocation, String> stringComponents) {
        if (tag.isPresent()) {
            return new ResourceRef(
                    Optional.of("tag:" + tag.orElseThrow()),
                    Optional.empty(),
                    materialId,
                    stringComponents);
        }
        return new ResourceRef(
                prefix.map(MaterialPrefix::serializedId),
                fixed,
                materialId,
                stringComponents);
    }

    private static ResourceRef ref(
            Optional<MaterialPrefix> prefix,
            Optional<ResourceLocation> fixed,
            String materialId,
            Map<ResourceLocation, String> stringComponents) {
        return ref(
                prefix,
                fixed,
                Optional.empty(),
                materialId,
                stringComponents);
    }

    private static ResourceRef ref(
            Optional<MaterialPrefix> prefix,
            Optional<ResourceLocation> fixed,
            String materialId) {
        return ref(prefix, fixed, materialId, Map.of());
    }

    private static ResourceRef ref(
            Optional<MaterialPrefix> prefix,
            Optional<ResourceLocation> fixed) {
        return ref(prefix, fixed, "");
    }

    private static ResourceRef refFluid(
            Optional<MaterialPrefix> prefix,
            Optional<ResourceLocation> fixed,
            Optional<String> materialFluid) {
        return materialFluid
                .map(selector -> new ResourceRef(
                        Optional.of("material_fluid:" + selector),
                        Optional.empty()))
                .orElseGet(() -> ref(prefix, fixed));
    }

    private static Optional<GTRecipe> project(
            ResourceLocation ruleId,
            MaterialRule rule,
            Compiled compiled,
            MaterialDefinition material,
            EvaluationContext context,
            Map<String, MaterialDefinition> materialsById,
            ResourceResolver resolver,
            Map<MaterialPrefix, List<MaterialDefinition>> registeredPrefixIndex) {
        MaterialRule.MaterialOverride override =
                rule.materialOverrides().getOrDefault(
                        material.id(), MaterialRule.MaterialOverride.empty());
        List<Ingredient> itemInputs = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();
        List<ItemInputAction> itemInputActions = new ArrayList<>();
        for (int index = 0; index < compiled.itemInputs().size(); index++) {
            CompiledItem resource = compiled.itemInputs().get(index);
            SelectedMaterial selection = selectMaterial(
                    material, resource.materialSelector(), materialsById).orElse(null);
            if (selection == null || resource.prefix().isPresent()
                    && !hasIndexedPrefix(
                            selection.material(),
                            resource.prefix().get(),
                            registeredPrefixIndex)) {
                throw unresolvedRequiredResource(
                        ruleId, material, "item input", index, resource);
            }
            Optional<Ingredient> ingredient = resource.tag()
                    .map(tag -> Ingredient.of(TagKey.create(
                            Registries.ITEM, tag)))
                    .or(() -> resolver.itemInput(
                            selection.material(),
                            resource.prefix(),
                            resource.fixed()));
            if (ingredient.isEmpty()) {
                throw unresolvedRequiredResource(
                        ruleId, material, "item input", index, resource);
            }
            Optional<Integer> count = evaluateResourceCount(
                    override.itemInputCounts().get(Integer.toString(index)),
                    resource.amount(),
                    context.forResource(resource.prefix(), selection.targetUnits()),
                    ruleId,
                    resource.candidateSpecificAmount(),
                    resource.fixed().isPresent()
                            || resource.tag().isPresent());
            if (count.isEmpty()) {
                return Optional.empty();
            }
            itemInputs.add(withStringComponents(
                    ingredient.get(), resource, material));
            inputCounts.add(count.get());
            itemInputActions.add(inputAction(resource, count.get()));
        }

        List<ItemStack> itemOutputs = new ArrayList<>();
        List<Integer> chances = new ArrayList<>();
        for (int index = 0; index < compiled.itemOutputs().size(); index++) {
            CompiledItem resource = compiled.itemOutputs().get(index);
            SelectedMaterial selection = selectMaterial(
                    material, resource.materialSelector(), materialsById).orElse(null);
            if (selection == null || resource.prefix().isPresent()
                    && !hasIndexedPrefix(
                            selection.material(),
                            resource.prefix().get(),
                            registeredPrefixIndex)) {
                if (resource.optional()) continue;
                throw unresolvedRequiredResource(
                        ruleId, material, "item output", index, resource);
            }
            Optional<Item> item = resolver.itemOutput(
                    selection.material(), resource.prefix(), resource.fixed());
            if (item.isEmpty()) {
                if (resource.optional()) continue;
                throw unresolvedRequiredResource(
                        ruleId, material, "item output", index, resource);
            }
            EvaluationContext resourceContext =
                    context.forResource(resource.prefix(), selection.targetUnits());
            Optional<Integer> count = evaluateResourceCount(
                    override.itemOutputCounts().get(Integer.toString(index)),
                    resource.amount(), resourceContext, ruleId,
                    resource.candidateSpecificAmount(), false);
            if (count.isEmpty()) {
                if (resource.optional()) continue;
                return Optional.empty();
            }
            int chance = evaluateInt(
                    override.outputChances().get(Integer.toString(index)),
                    resource.chance(),
                    resourceContext,
                    ruleId);
            ItemStack stack;
            if (resource.prefix().isPresent()
                    && item.get() instanceof PrefixMaterialItem) {
                stack = MaterialLookup.tryStack(
                                selection.material(),
                                resource.prefix().get(),
                                count.get(),
                                MaterialCatalog.isBootstrapped()
                                        ? MaterialCatalog.runtimePreferences()
                                        : Map.of())
                        .orElseGet(() -> new ItemStack(item.get(), count.get()));
            } else {
                stack = new ItemStack(item.get(), count.get());
            }
            applyStringComponents(
                    stack,
                    resolveStringComponents(
                            resource.stringComponents(), material),
                    ruleId);
            itemOutputs.add(stack);
            chances.add(chance);
        }

        List<FluidStack> fluidInputs = resolveFluids(
                ruleId, material, compiled.fluidInputs(), override, context, "input", resolver);
        if (fluidInputs == null) return Optional.empty();
        List<FluidStack> fluidOutputs = resolveFluids(
                ruleId, material, compiled.fluidOutputs(), override, context, "output", resolver);
        if (fluidOutputs == null) return Optional.empty();

        int duration = evaluateInt(
                override.duration().orElse(null), compiled.duration(), context, ruleId);
        long eut = evaluateLong(
                override.eut().orElse(null), compiled.eut(), context, ruleId);
        long special = evaluateLong(
                override.specialValue().orElse(null), compiled.specialValue(), context, ruleId);
        return Optional.of(new GTRecipe(
                itemInputs,
                inputCounts,
                itemInputActions,
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                chances,
                duration,
                eut,
                special,
                rule.canBeBuffered(),
                Optional.empty()));
    }

    private static ItemInputAction inputAction(
            CompiledItem resource,
            int count) {
        return resource.inputAction().orElseGet(() ->
                count == 0 ? ItemInputAction.PRESERVE : ItemInputAction.CONSUME);
    }

    private static Ingredient withStringComponents(
            Ingredient ingredient,
            CompiledItem resource,
            MaterialDefinition material) {
        if (resource.stringComponents().isEmpty()) {
            return ingredient;
        }
        Item item = BuiltInRegistries.ITEM.getOptional(
                        resource.fixed().orElseThrow())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Unknown fixed component item "
                                + resource.fixed().orElseThrow()));
        DataComponentPredicate.Builder predicate = DataComponentPredicate.builder();
        resolveStringComponents(resource.stringComponents(), material)
                .forEach((componentId, value) -> predicate.expect(
                        ComponentIngredientIndex.stringComponentType(componentId)
                                .orElseThrow(() -> new IllegalArgumentException(
                                        "Unsupported indexed string component "
                                                + componentId)),
                        value));
        return DataComponentIngredient.of(false, predicate.build(), item);
    }

    private static void applyStringComponents(
            ItemStack stack,
            Map<ResourceLocation, String> components,
            ResourceLocation ruleId) {
        ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(stack.getItem());
        components.forEach((componentId, value) -> {
            if (!MaterialComponentPolicies.isValid(
                    itemId.toString(),
                    componentId.toString(),
                    value,
                    MaterialCatalog::contains)) {
                throw new IllegalArgumentException(
                        "material rule " + ruleId
                                + ": output " + itemId
                                + " rejects " + componentId
                                + "=" + value);
            }
            stack.set(
                ComponentIngredientIndex.stringComponentType(componentId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "Unsupported indexed string component "
                                        + componentId)),
                value);
        });
    }

    private static Map<ResourceLocation, String> resolveStringComponents(
            Map<ResourceLocation, String> components,
            MaterialDefinition material) {
        LinkedHashMap<ResourceLocation, String> resolved = new LinkedHashMap<>();
        components.forEach((componentId, value) -> resolved.put(
                componentId,
                value.equals("$material") ? material.id() : value));
        return Map.copyOf(resolved);
    }

    private static List<FluidStack> resolveFluids(
            ResourceLocation ruleId,
            MaterialDefinition material,
            List<CompiledFluid> resources,
            MaterialRule.MaterialOverride override,
            EvaluationContext context,
            String side,
            ResourceResolver resolver) {
        List<FluidStack> result = new ArrayList<>();
        for (int index = 0; index < resources.size(); index++) {
            CompiledFluid resource = resources.get(index);
            Optional<Fluid> fluid = resolver.fluid(
                    material,
                    resource.prefix(),
                    resource.fixed(),
                    resource.materialFluid());
            if (fluid.isEmpty()) {
                throw new IllegalArgumentException(
                        "material rule " + ruleId + ": required fluid " + side
                                + " " + index + " did not resolve for material "
                                + material.id());
            }
            String key = side + ":" + index;
            int amount = evaluateInt(
                    override.fluidAmounts().get(key),
                    resource.amount(),
                    context,
                    ruleId);
            result.add(new FluidStack(fluid.get(), amount));
        }
        return result;
    }

    private static List<MaterialDefinition> candidateMaterials(
            ResourceLocation ruleId,
            MaterialRule rule,
            Set<MaterialPrefix> required,
            java.util.Collection<MaterialDefinition> availableMaterials,
            Map<MaterialPrefix, List<MaterialDefinition>> prefixIndex) {
        if (rule.material().isPresent()) {
            MaterialDefinition selected = availableMaterials.stream()
                    .filter(material -> material.id().equals(rule.material().get()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Unknown material " + rule.material().get()));
            List<String> missing = required.stream()
                    .filter(prefix -> !prefixIndex
                            .getOrDefault(prefix, List.of())
                            .contains(selected))
                    .map(MaterialPrefix::serializedId)
                    .sorted()
                    .toList();
            if (!missing.isEmpty()) {
                throw new IllegalArgumentException(
                        "material rule " + ruleId + ": explicit material "
                                + selected.id() + " lacks required registered forms "
                                + missing);
            }
            return List.of(selected);
        }
        List<MaterialDefinition> smallestIndexed = required.stream()
                .map(prefix -> prefixIndex.getOrDefault(prefix, List.of()))
                .min(Comparator.comparingInt(List::size))
                .orElse(null);
        Collection<MaterialDefinition> smallest =
                smallestIndexed == null ? availableMaterials : smallestIndexed;
        return smallest.stream()
                .filter(material -> required.stream().allMatch(prefix ->
                        prefixIndex.getOrDefault(prefix, List.of()).contains(material)))
                .sorted(Comparator.comparing(MaterialDefinition::id))
                .toList();
    }

    private static boolean hasIndexedPrefix(
            MaterialDefinition material,
            MaterialPrefix prefix,
            Map<MaterialPrefix, List<MaterialDefinition>> prefixIndex) {
        return prefixIndex.getOrDefault(prefix, List.of()).contains(material);
    }

    private static Set<MaterialPrefix> indexedPrefixes(
            MaterialDefinition material,
            Map<MaterialPrefix, List<MaterialDefinition>> prefixIndex) {
        return prefixIndex.entrySet().stream()
                .filter(entry -> entry.getValue().contains(material))
                .map(Map.Entry::getKey)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static Map<MaterialPrefix, List<MaterialDefinition>> buildPrefixIndex(
            Collection<MaterialDefinition> materials) {
        LinkedHashMap<String, List<MaterialPrefix>> factual = new LinkedHashMap<>();
        materials.forEach(material ->
                factual.put(material.id(), material.forms()));
        return buildPrefixIndex(materials, factual);
    }

    private static Map<MaterialPrefix, List<MaterialDefinition>> buildPrefixIndex(
            Collection<MaterialDefinition> materials,
            Map<String, List<MaterialPrefix>> formsByMaterial) {
        LinkedHashMap<MaterialPrefix, List<MaterialDefinition>> mutable = new LinkedHashMap<>();
        for (MaterialDefinition material : materials) {
            for (MaterialPrefix prefix : formsByMaterial.get(material.id())) {
                mutable.computeIfAbsent(prefix, ignored -> new ArrayList<>()).add(material);
            }
        }
        LinkedHashMap<MaterialPrefix, List<MaterialDefinition>> result = new LinkedHashMap<>();
        mutable.forEach((prefix, values) -> result.put(prefix, List.copyOf(values)));
        return Map.copyOf(result);
    }

    private static Map<String, MaterialDefinition> index(
            java.util.Collection<MaterialDefinition> materials) {
        LinkedHashMap<String, MaterialDefinition> result = new LinkedHashMap<>();
        materials.forEach(material -> result.put(material.id(), material));
        return Map.copyOf(result);
    }

    private static Set<String> knownMaterialTags(
            Collection<MaterialDefinition> materials) {
        return materials.stream()
                .flatMap(material -> material.gt6Metadata().stream())
                .flatMap(metadata -> metadata.materialTags().stream())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
    }

    private static IllegalArgumentException unresolvedRequiredResource(
            ResourceLocation ruleId,
            MaterialDefinition material,
            String side,
            int index,
            CompiledItem resource) {
        String reference = resource.prefix()
                .map(MaterialPrefix::serializedId)
                .or(() -> resource.fixed().map(ResourceLocation::toString))
                .or(() -> resource.tag().map(tag -> "#" + tag))
                .orElse("<unknown>");
        return new IllegalArgumentException(
                "material rule " + ruleId + ": required " + side + " "
                        + index + " (" + reference
                        + ") did not resolve for material " + material.id());
    }

    private static Optional<SelectedMaterial> selectMaterial(
            MaterialDefinition source,
            String selector,
            Map<String, MaterialDefinition> materialsById) {
        if (selector.equals("self")) {
            return Optional.of(new SelectedMaterial(source, null));
        }
        if (selector.startsWith("material:")) {
            String selectedId = selector.substring("material:".length());
            return Optional.ofNullable(materialsById.get(selectedId))
                    .map(material -> new SelectedMaterial(material, null));
        }
        if (source.gt6Metadata().isEmpty()) {
            return Optional.empty();
        }
        String selectedId;
        if (selector.startsWith("processing_target:")) {
            String key = selector.substring("processing_target:".length());
            var target = source.gt6Metadata().get().processingTargets().get(key);
            if (target == null) return Optional.empty();
            selectedId = target.material();
            return Optional.ofNullable(materialsById.get(selectedId))
                    .map(material -> new SelectedMaterial(
                            material, target.ccUnits().map(Long::doubleValue).orElse(null)));
        } else {
            int index = Integer.parseInt(selector.substring("byproduct:".length()));
            var byproducts = source.gt6Metadata().get().byproducts();
            if (index >= byproducts.size()) return Optional.empty();
            selectedId = byproducts.get(index).material();
        }
        return Optional.ofNullable(materialsById.get(selectedId))
                .map(material -> new SelectedMaterial(material, null));
    }

    private static Compiled compile(ResourceLocation ruleId, MaterialRule rule) {
        String diagnostic = ruleId.toString();
        List<CompiledItem> inputs = rule.itemInputs().stream()
                .map(resource -> compile(resource, diagnostic)).toList();
        List<CompiledItem> outputs = rule.itemOutputs().stream()
                .map(resource -> compile(resource, diagnostic)).toList();
        List<CompiledFluid> fluidInputs = rule.fluidInputs().stream()
                .map(resource -> compile(resource, diagnostic)).toList();
        List<CompiledFluid> fluidOutputs = rule.fluidOutputs().stream()
                .map(resource -> compile(resource, diagnostic)).toList();
        Set<MaterialPrefix> required = new LinkedHashSet<>();
        inputs.stream().filter(CompiledItem::selectsSelf)
                .forEach(resource -> resource.prefix().ifPresent(required::add));
        outputs.stream().filter(CompiledItem::selectsSelf)
                .forEach(resource -> resource.prefix().ifPresent(required::add));
        fluidInputs.forEach(resource -> resource.prefix().ifPresent(required::add));
        fluidOutputs.forEach(resource -> resource.prefix().ifPresent(required::add));
        return new Compiled(
                inputs,
                outputs,
                fluidInputs,
                fluidOutputs,
                RuleExpression.numeric(rule.duration(), diagnostic),
                RuleExpression.numeric(rule.eut(), diagnostic),
                RuleExpression.numeric(rule.specialValue(), diagnostic),
                rule.conditions().stream()
                        .map(expression -> RuleExpression.bool(expression, diagnostic))
                        .toList(),
                Set.copyOf(required),
                uniqueUnit(java.util.stream.Stream.concat(
                        inputs.stream().flatMap(resource -> resource.prefix().stream()),
                        fluidInputs.stream().flatMap(resource -> resource.prefix().stream()))),
                uniqueUnit(java.util.stream.Stream.concat(
                        outputs.stream().flatMap(resource -> resource.prefix().stream()),
                        fluidOutputs.stream().flatMap(resource -> resource.prefix().stream()))),
                uniqueUnit(java.util.stream.Stream.of(
                                inputs.stream().flatMap(resource -> resource.prefix().stream()),
                                outputs.stream().flatMap(resource -> resource.prefix().stream()),
                                fluidInputs.stream().flatMap(resource -> resource.prefix().stream()),
                                fluidOutputs.stream().flatMap(resource -> resource.prefix().stream()))
                        .flatMap(stream -> stream)));
    }

    private static CompiledItem compile(
            MaterialRule.ItemResource resource,
            String diagnostic) {
        return new CompiledItem(
                resource.prefix().map(MaterialPrefixCatalog::require),
                resource.item(),
                resource.tag(),
                RuleExpression.numeric(resource.count(), diagnostic),
                RuleExpression.numeric(resource.chance(), diagnostic),
                resource.materialSelector().orElse("self"),
                resource.optional(),
                resource.stringComponents(),
                resource.inputAction());
    }

    private static CompiledFluid compile(
            MaterialRule.FluidResource resource,
            String diagnostic) {
        return new CompiledFluid(
                resource.prefix().map(MaterialPrefixCatalog::require),
                resource.fluid(),
                resource.materialFluid(),
                RuleExpression.numeric(resource.amount(), diagnostic));
    }

    private static Double uniqueUnit(java.util.stream.Stream<MaterialPrefix> prefixes) {
        Set<Integer> units = prefixes
                .map(MaterialPrefix::units)
                .collect(java.util.stream.Collectors.toSet());
        return units.size() == 1 ? units.iterator().next().doubleValue() : null;
    }

    private static int evaluateInt(
            String override,
            RuleExpression base,
            EvaluationContext context,
            ResourceLocation ruleId) {
        return override == null
                ? base.evaluateInt(context)
                : RuleExpression.numeric(override, ruleId.toString()).evaluateInt(context);
    }

    private static Optional<Integer> evaluateResourceCount(
            String override,
            RuleExpression base,
            EvaluationContext context,
            ResourceLocation ruleId,
            boolean selectorSpecific,
            boolean allowPresenceOnly) {
        try {
            int count = evaluateInt(override, base, context, ruleId);
            if (count == 0 && allowPresenceOnly) {
                return Optional.of(0);
            }
            if (count <= 0) {
                if (selectorSpecific) return Optional.empty();
                throw new IllegalArgumentException(
                        "material rule " + ruleId + ": resource count must be positive");
            }
            if (count > MAX_ITEM_BATCH) {
                if (selectorSpecific) return Optional.empty();
                throw new IllegalArgumentException(
                        "material rule " + ruleId
                                + ": item batch count exceeds one-slot capacity "
                                + MAX_ITEM_BATCH);
            }
            return Optional.of(count);
        } catch (IllegalArgumentException exception) {
            if (selectorSpecific
                    && (exception.getMessage().contains("result is not an exact long")
                    || exception.getMessage().contains("requires an unambiguous value")
                    || exception.getMessage().contains("requires an exact target amount")
                    || exception.getMessage().contains("target_units(")
                            && exception.getMessage().contains(
                                    "requires a positive exact safe integer"))) {
                return Optional.empty();
            }
            throw exception;
        }
    }

    private static long evaluateLong(
            String override,
            RuleExpression base,
            EvaluationContext context,
            ResourceLocation ruleId) {
        return override == null
                ? base.evaluateLong(context)
                : RuleExpression.numeric(override, ruleId.toString()).evaluateLong(context);
    }

    private static ResourceLocation expandedId(ResourceLocation ruleId, String materialId) {
        return ResourceLocation.fromNamespaceAndPath(
                ruleId.getNamespace(),
                ruleId.getPath() + "/" + materialId);
    }

    public record Expanded(
            ResourceLocation id,
            ResourceLocation target,
            GTRecipe recipe,
            boolean materialSpecific) {}

    public record ResourceRef(
            Optional<String> prefix,
            Optional<ResourceLocation> fixed,
            String materialId,
            Map<ResourceLocation, String> stringComponents) {
        public ResourceRef {
            stringComponents = Map.copyOf(stringComponents);
        }

        public ResourceRef(
                Optional<String> prefix,
                Optional<ResourceLocation> fixed,
                String materialId) {
            this(prefix, fixed, materialId, Map.of());
        }

        public ResourceRef(Optional<String> prefix, Optional<ResourceLocation> fixed) {
            this(prefix, fixed, "", Map.of());
        }

        @Override
        public String toString() {
            String legacy = "ResourceRef[prefix=" + prefix
                    + ", fixed=" + fixed
                    + ", materialId=" + materialId;
            return stringComponents.isEmpty()
                    ? legacy + "]"
                    : legacy + ", stringComponents=" + stringComponents + "]";
        }
    }

    public record PlannedResource(ResourceRef resource, int amount, int chance) {}

    public record Plan(
            ResourceLocation id,
            ResourceLocation target,
            String materialId,
            List<PlannedResource> itemInputs,
            List<ItemInputAction> itemInputActions,
            List<PlannedResource> itemOutputs,
            List<PlannedResource> fluidInputs,
            List<PlannedResource> fluidOutputs,
            int duration,
            long eut,
            long specialValue,
            boolean canBeBuffered,
            boolean materialSpecific) {
        public Plan {
            itemInputs = List.copyOf(itemInputs);
            itemInputActions = List.copyOf(itemInputActions);
            itemOutputs = List.copyOf(itemOutputs);
            fluidInputs = List.copyOf(fluidInputs);
            fluidOutputs = List.copyOf(fluidOutputs);
        }
    }

    public interface ResourceResolver {
        Optional<Ingredient> itemInput(
                MaterialDefinition material,
                Optional<MaterialPrefix> prefix,
                Optional<ResourceLocation> fixed);

        Optional<Item> itemOutput(
                MaterialDefinition material,
                Optional<MaterialPrefix> prefix,
                Optional<ResourceLocation> fixed);

        Optional<Fluid> fluid(
                MaterialDefinition material,
                Optional<MaterialPrefix> prefix,
                Optional<ResourceLocation> fixed,
                Optional<String> materialFluid);
    }

    public static ResourceResolver candidateResolver(
            Map<String, String> candidatePreferences) {
        return candidateResolver(
                candidatePreferences,
                (material, prefix, preferences) ->
                        MaterialLookup.item(material, prefix, preferences));
    }

    static ResourceResolver candidateResolver(
            Map<String, String> candidatePreferences,
            CandidateItemResolver itemResolver) {
        Map<String, String> preferences = Map.copyOf(candidatePreferences);
        return new ResourceResolver() {
            @Override
            public Optional<Ingredient> itemInput(
                    MaterialDefinition material,
                    Optional<MaterialPrefix> prefix,
                    Optional<ResourceLocation> fixed) {
                return prefix.isPresent()
                        ? MaterialLookup.ingredient(material, prefix.get())
                        : BuiltInRegistries.ITEM.getOptional(
                                fixed.orElseThrow()).map(Ingredient::of);
            }

            @Override
            public Optional<Item> itemOutput(
                    MaterialDefinition material,
                    Optional<MaterialPrefix> prefix,
                    Optional<ResourceLocation> fixed) {
                return prefix.isPresent()
                        ? itemResolver.resolve(material, prefix.get(), preferences)
                        : BuiltInRegistries.ITEM.getOptional(fixed.orElseThrow());
            }

            @Override
            public Optional<Fluid> fluid(
                    MaterialDefinition material,
                    Optional<MaterialPrefix> prefix,
                    Optional<ResourceLocation> fixed,
                    Optional<String> materialFluid) {
                if (materialFluid.isPresent()) {
                    return switch (materialFluid.orElseThrow()) {
                        case "chemical" -> ModFluids.chemical(material.id())
                                .map(registration -> registration.source().get());
                        case "molten" -> ModFluids.molten(material.id())
                                .map(registration -> registration.source().get());
                        default -> throw new IllegalStateException(
                                "Compiled unsupported material fluid selector: "
                                        + materialFluid.orElseThrow());
                    };
                }
                if (fixed.isPresent()) {
                    return BuiltInRegistries.FLUID.getOptional(fixed.orElseThrow());
                }
                if (prefix.isEmpty()) {
                    throw new IllegalStateException(
                            "Compiled fluid resource has no selector");
                }
                if (!material.forms().contains(prefix.get()) || !material.moltenFluid()) {
                    return Optional.empty();
                }
                return ModFluids.molten(material.id())
                        .map(registration -> registration.source().get());
            }
        };
    }

    @FunctionalInterface
    interface CandidateItemResolver {
        Optional<Item> resolve(
                MaterialDefinition material,
                MaterialPrefix prefix,
                Map<String, String> preferences);
    }

    private record Compiled(
            List<CompiledItem> itemInputs,
            List<CompiledItem> itemOutputs,
            List<CompiledFluid> fluidInputs,
            List<CompiledFluid> fluidOutputs,
            RuleExpression duration,
            RuleExpression eut,
            RuleExpression specialValue,
            List<RuleExpression> conditions,
            Set<MaterialPrefix> requiredPrefixes,
            Double inputUnit,
            Double outputUnit,
            Double singlePrefixUnit) {}

    private record CompiledItem(
            Optional<MaterialPrefix> prefix,
            Optional<ResourceLocation> fixed,
            Optional<ResourceLocation> tag,
            RuleExpression amount,
            RuleExpression chance,
            String materialSelector,
            boolean optional,
            Map<ResourceLocation, String> stringComponents,
            Optional<ItemInputAction> inputAction) {
        boolean selectsSelf() {
            return materialSelector.equals("self");
        }
        boolean selectsTarget() {
            return materialSelector.startsWith("processing_target:");
        }
        boolean candidateSpecificAmount() {
            return selectsTarget() || amount.usesLookup();
        }
    }

    private record CompiledFluid(
            Optional<MaterialPrefix> prefix,
            Optional<ResourceLocation> fixed,
            Optional<String> materialFluid,
            RuleExpression amount) {}

    private record EvaluationContext(
            MaterialDefinition material,
            Map<String, MaterialDefinition> knownMaterials,
            Set<String> knownMaterialTags,
            Set<MaterialPrefix> factualForms,
            Set<MaterialPrefix> registeredForms,
            Map<MaterialPrefix, List<MaterialDefinition>> registeredPrefixIndex,
            Double inputUnit,
            Double outputUnit,
            Double prefixUnit,
            Double targetUnits,
            Double resourcePrefixUnits) implements RuleExpression.Context {
        EvaluationContext(
                MaterialDefinition material,
                Map<String, MaterialDefinition> knownMaterials,
                Set<String> knownMaterialTags,
                Set<MaterialPrefix> factualForms,
                Set<MaterialPrefix> registeredForms,
                Map<MaterialPrefix, List<MaterialDefinition>> registeredPrefixIndex,
                Double inputUnit,
                Double outputUnit,
                Double prefixUnit) {
            this(
                    material,
                    knownMaterials,
                    knownMaterialTags,
                    factualForms,
                    registeredForms,
                    registeredPrefixIndex,
                    inputUnit,
                    outputUnit,
                    prefixUnit,
                    null,
                    null);
        }

        EvaluationContext forResource(
                Optional<MaterialPrefix> resourcePrefix,
                Double selectedTargetUnits) {
            return new EvaluationContext(
                    material,
                    knownMaterials,
                    knownMaterialTags,
                    factualForms,
                    registeredForms,
                    registeredPrefixIndex,
                    inputUnit,
                    outputUnit,
                    prefixUnit,
                    selectedTargetUnits,
                    resourcePrefix.map(prefix -> (double) prefix.units()).orElse(null));
        }
        @Override
        public double number(String name) {
            return switch (name) {
                case "material.tier" -> material.tier();
                case "material.mass" ->
                        material.thermal().density() * MaterialPrefixes.INGOT.units();
                case "material.thermal.melting_point" -> material.thermal().meltingPoint();
                case "material.thermal.boiling_point" -> material.thermal().boilingPoint();
                case "material.thermal.density" -> material.thermal().density();
                case "material.explosion_damage" -> material.gt6Metadata()
                        .map(GT6MaterialMetadata::explosionDamage)
                        .orElse(0.0D);
                case "material.heat_damage" -> material.gt6Metadata()
                        .map(GT6MaterialMetadata::heatDamage)
                        .orElse(0.0D);
                case "material.tool.types" -> material.gt6Metadata()
                        .map(metadata -> (double) metadata.tool().types())
                        .orElse(0.0D);
                case "material.tool.quality" -> material.gt6Metadata()
                        .map(metadata -> (double) metadata.tool().quality())
                        .orElse(0.0D);
                case "input.units" -> requireUnit(inputUnit, name);
                case "output.units" -> requireUnit(outputUnit, name);
                case "prefix.units" -> requireUnit(prefixUnit, name);
                case "target.units" -> requireUnit(targetUnits, name);
                case "resource.prefix.units" -> requireUnit(resourcePrefixUnits, name);
                default -> throw new IllegalArgumentException("unknown numeric variable " + name);
            };
        }

        @Override
        public double lookup(String function, String key) {
            long value = switch (function) {
                case "target_units" -> material.gt6Metadata()
                        .map(metadata -> metadata.processingTargets().get(key))
                        .flatMap(target -> target == null
                                ? Optional.<Long>empty()
                                : target.ccUnits())
                        .orElseThrow(() -> new IllegalArgumentException(
                                "target_units(" + key + ") requires an exact target amount"));
                case "prefix_units" -> MaterialPrefixCatalog.require(key).units();
                case "fluid_pipe_recipe" -> PipeCatalog.recipeEnabled(
                        material, PipeCatalog.Kind.FLUID, key) ? 1L : 0L;
                case "item_pipe_recipe" -> PipeCatalog.recipeEnabled(
                        material, PipeCatalog.Kind.ITEM, key) ? 1L : 0L;
                default -> throw new IllegalArgumentException("unknown lookup " + function);
            };
            if ((function.equals("target_units")
                            || function.equals("prefix_units"))
                    && (value <= 0L || value > 9_007_199_254_740_991L)) {
                throw new IllegalArgumentException(
                        function + "(" + key + ") requires a positive exact safe integer");
            }
            return value;
        }

        @Override
        public boolean materialHas(String flag) {
            String namespaced = flag.indexOf(':') >= 0 ? flag : "cruciblecraft:" + flag;
            return material.generationFlagIds().stream().anyMatch(value ->
                    value.equals(flag) || value.equals(namespaced));
        }

        @Override
        public boolean materialTag(String tag) {
            if (!knownMaterialTags.contains(tag)) {
                throw new IllegalArgumentException(
                        "Unknown material.tag value: " + tag);
            }
            return material.gt6Metadata()
                    .map(metadata -> metadata.materialTags().contains(tag))
                    .orElse(false);
        }

        @Override
        public boolean materialIs(String materialId) {
            MaterialRule.requireMaterialId(materialId, "material.is argument");
            if (!knownMaterials.containsKey(materialId)) {
                throw new IllegalArgumentException(
                        "Unknown material.is material: " + materialId);
            }
            return material.id().equals(materialId);
        }

        @Override
        public boolean hasForm(String prefix) {
            return factualForms.contains(MaterialPrefixCatalog.require(prefix));
        }

        @Override
        public boolean hasRegistered(String prefix) {
            return registeredForms.contains(MaterialPrefixCatalog.require(prefix));
        }

        @Override
        public boolean hasRegisteredFor(String selector, String prefix) {
            MaterialPrefix required = MaterialPrefixCatalog.require(prefix);
            return selectMaterial(material, selector, knownMaterials)
                    .map(selected -> hasIndexedPrefix(
                            selected.material(), required, registeredPrefixIndex))
                    .orElse(false);
        }

        private static double requireUnit(Double value, String name) {
            if (value == null) {
                throw new IllegalArgumentException(name + " is ambiguous for this rule");
            }
            return value;
        }
    }

    private record SelectedMaterial(MaterialDefinition material, Double targetUnits) {}
}
