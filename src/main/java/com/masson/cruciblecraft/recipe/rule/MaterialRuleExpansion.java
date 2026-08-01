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
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.recipe.gt.GTRecipe;
import com.masson.cruciblecraft.registry.ModFluids;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.fluids.FluidStack;

/** One generic projection path for every declarative and legacy-adapted rule. */
public final class MaterialRuleExpansion {
    private static final int MAX_ITEM_BATCH = 64;
    private MaterialRuleExpansion() {}

    public static List<Expanded> expand(
            ResourceLocation ruleId,
            MaterialRule rule) {
        return expand(
                ruleId,
                rule,
                MaterialCatalog.values(),
                DEFAULT_RESOLVER,
                MaterialCatalog.prefixIndex());
    }

    public static List<Expanded> expand(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            ResourceResolver resolver) {
        return expand(
                ruleId,
                rule,
                availableMaterials,
                resolver,
                buildPrefixIndex(availableMaterials));
    }

    public static List<Expanded> expand(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials) {
        return expand(ruleId, rule, availableMaterials, DEFAULT_RESOLVER);
    }

    public static List<Expanded> expand(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            Map<String, String> candidatePreferences) {
        return expand(
                ruleId,
                rule,
                availableMaterials,
                candidateResolver(candidatePreferences));
    }

    private static List<Expanded> expand(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            ResourceResolver resolver,
            Map<MaterialPrefix, List<MaterialDefinition>> prefixIndex) {
        if (rule.target().isEmpty()) {
            return List.of();
        }
        Compiled compiled = compile(ruleId, rule);
        Map<String, MaterialDefinition> materialsById = index(availableMaterials);
        List<MaterialDefinition> candidates =
                candidateMaterials(
                        rule, compiled.requiredPrefixes(), availableMaterials, prefixIndex);
        List<Expanded> result = new ArrayList<>();
        for (MaterialDefinition material : candidates) {
            EvaluationContext context = new EvaluationContext(
                    material,
                    indexedPrefixes(material, prefixIndex),
                    compiled.inputUnit(),
                    compiled.outputUnit(),
                    compiled.singlePrefixUnit());
            if (compiled.conditions().stream().anyMatch(condition ->
                    !condition.evaluateBoolean(context))) {
                continue;
            }
            Optional<GTRecipe> projected =
                    project(ruleId, rule, compiled, material, context, materialsById, resolver);
            projected.ifPresent(recipe -> result.add(new Expanded(
                    expandedId(ruleId, material.id()),
                    rule.target().orElseThrow(),
                    recipe,
                    rule.material().isPresent())));
        }
        return List.copyOf(result);
    }

    /**
     * Registry-independent expansion plan used for validation and parity tests.
     * Runtime projection resolves these refs to tags/items/fluids afterwards.
     */
    public static List<Plan> expandPlans(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials) {
        return expandPlans(
                ruleId,
                rule,
                availableMaterials,
                buildPrefixIndex(availableMaterials));
    }

    /** Registry-independent plan expansion using the committed registration gate. */
    public static List<Plan> expandRegisteredPlans(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials) {
        return expandPlansWithForms(
                ruleId,
                rule,
                availableMaterials,
                MaterialRegistrationGate.load(availableMaterials));
    }

    public static List<Plan> expandPlansWithForms(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            Map<String, List<MaterialPrefix>> formsByMaterial) {
        return expandPlans(
                ruleId,
                rule,
                availableMaterials,
                buildPrefixIndex(availableMaterials, formsByMaterial));
    }

    private static List<Plan> expandPlans(
            ResourceLocation ruleId,
            MaterialRule rule,
            java.util.Collection<MaterialDefinition> availableMaterials,
            Map<MaterialPrefix, List<MaterialDefinition>> prefixIndex) {
        if (rule.target().isEmpty()) {
            return List.of();
        }
        Compiled compiled = compile(ruleId, rule);
        Map<String, MaterialDefinition> materialsById = index(availableMaterials);
        List<Plan> plans = new ArrayList<>();
        for (MaterialDefinition material
                : candidateMaterials(
                        rule, compiled.requiredPrefixes(), availableMaterials, prefixIndex)) {
            EvaluationContext context = new EvaluationContext(
                    material,
                    indexedPrefixes(material, prefixIndex),
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
            for (int index = 0; index < compiled.itemInputs().size(); index++) {
                CompiledItem resource = compiled.itemInputs().get(index);
                SelectedMaterial selection = selectMaterial(
                        material, resource.materialSelector(), materialsById).orElse(null);
                if (selection == null || resource.prefix().isPresent()
                        && !hasIndexedPrefix(
                                selection.material(), resource.prefix().get(), prefixIndex)) {
                    itemInputs.clear();
                    break;
                }
                Optional<Integer> count = evaluateResourceCount(
                        override.itemInputCounts().get(Integer.toString(index)),
                        resource.amount(),
                        context.forResource(resource.prefix(), selection.targetUnits()),
                        ruleId,
                        resource.candidateSpecificAmount());
                if (count.isEmpty()) {
                    itemInputs.clear();
                    break;
                }
                itemInputs.add(new PlannedResource(
                        ref(resource.prefix(), resource.fixed(), selection.material().id()),
                        count.get(),
                        GTRecipe.GUARANTEED_CHANCE));
            }
            if (itemInputs.size() != compiled.itemInputs().size()) continue;
            List<PlannedResource> itemOutputs = new ArrayList<>();
            for (int index = 0; index < compiled.itemOutputs().size(); index++) {
                CompiledItem resource = compiled.itemOutputs().get(index);
                SelectedMaterial selection = selectMaterial(
                        material, resource.materialSelector(), materialsById).orElse(null);
                if (selection == null || resource.prefix().isPresent()
                        && !hasIndexedPrefix(
                                selection.material(), resource.prefix().get(), prefixIndex)) {
                    if (resource.optional()) continue;
                    itemOutputs.clear();
                    break;
                }
                EvaluationContext resourceContext =
                        context.forResource(resource.prefix(), selection.targetUnits());
                Optional<Integer> count = evaluateResourceCount(
                        override.itemOutputCounts().get(Integer.toString(index)),
                        resource.amount(), resourceContext, ruleId, resource.candidateSpecificAmount());
                if (count.isEmpty()) {
                    if (resource.optional()) continue;
                    itemOutputs.clear();
                    break;
                }
                itemOutputs.add(new PlannedResource(
                        ref(resource.prefix(), resource.fixed(), selection.material().id()),
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
                    ref(resource.prefix(), resource.fixed()),
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
            String materialId) {
        return new ResourceRef(
                prefix.map(MaterialPrefix::serializedId),
                fixed,
                materialId);
    }

    private static ResourceRef ref(
            Optional<MaterialPrefix> prefix,
            Optional<ResourceLocation> fixed) {
        return ref(prefix, fixed, "");
    }

    private static Optional<GTRecipe> project(
            ResourceLocation ruleId,
            MaterialRule rule,
            Compiled compiled,
            MaterialDefinition material,
            EvaluationContext context,
            Map<String, MaterialDefinition> materialsById,
            ResourceResolver resolver) {
        MaterialRule.MaterialOverride override =
                rule.materialOverrides().getOrDefault(
                        material.id(), MaterialRule.MaterialOverride.empty());
        List<Ingredient> itemInputs = new ArrayList<>();
        List<Integer> inputCounts = new ArrayList<>();
        for (int index = 0; index < compiled.itemInputs().size(); index++) {
            CompiledItem resource = compiled.itemInputs().get(index);
            SelectedMaterial selection = selectMaterial(
                    material, resource.materialSelector(), materialsById).orElse(null);
            if (selection == null || resource.prefix().isPresent()
                    && !selection.material().forms().contains(resource.prefix().get())) {
                return Optional.empty();
            }
            Optional<Ingredient> ingredient = resolver.itemInput(
                    selection.material(), resource.prefix(), resource.fixed());
            if (ingredient.isEmpty()) return Optional.empty();
            Optional<Integer> count = evaluateResourceCount(
                    override.itemInputCounts().get(Integer.toString(index)),
                    resource.amount(),
                    context.forResource(resource.prefix(), selection.targetUnits()),
                    ruleId,
                    resource.candidateSpecificAmount());
            if (count.isEmpty()) return Optional.empty();
            itemInputs.add(ingredient.get());
            inputCounts.add(count.get());
        }

        List<ItemStack> itemOutputs = new ArrayList<>();
        List<Integer> chances = new ArrayList<>();
        for (int index = 0; index < compiled.itemOutputs().size(); index++) {
            CompiledItem resource = compiled.itemOutputs().get(index);
            SelectedMaterial selection = selectMaterial(
                    material, resource.materialSelector(), materialsById).orElse(null);
            if (selection == null || resource.prefix().isPresent()
                    && !selection.material().forms().contains(resource.prefix().get())) {
                if (resource.optional()) continue;
                return Optional.empty();
            }
            Optional<Item> item = resolver.itemOutput(
                    selection.material(), resource.prefix(), resource.fixed());
            if (item.isEmpty()) {
                if (resource.optional()) continue;
                return Optional.empty();
            }
            EvaluationContext resourceContext =
                    context.forResource(resource.prefix(), selection.targetUnits());
            Optional<Integer> count = evaluateResourceCount(
                    override.itemOutputCounts().get(Integer.toString(index)),
                    resource.amount(), resourceContext, ruleId, resource.candidateSpecificAmount());
            if (count.isEmpty()) {
                if (resource.optional()) continue;
                return Optional.empty();
            }
            int chance = evaluateInt(
                    override.outputChances().get(Integer.toString(index)),
                    resource.chance(),
                    resourceContext,
                    ruleId);
            itemOutputs.add(new ItemStack(item.get(), count.get()));
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
                itemOutputs,
                fluidInputs,
                fluidOutputs,
                chances,
                duration,
                eut,
                special,
                rule.canBeBuffered()));
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
                    material, resource.prefix(), resource.fixed());
            if (fluid.isEmpty()) return null;
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
            return required.stream().allMatch(prefix ->
                    prefixIndex.getOrDefault(prefix, List.of()).contains(selected))
                    ? List.of(selected)
                    : List.of();
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
                RuleExpression.numeric(resource.count(), diagnostic),
                RuleExpression.numeric(resource.chance(), diagnostic),
                resource.materialSelector().orElse("self"),
                resource.optional());
    }

    private static CompiledFluid compile(
            MaterialRule.FluidResource resource,
            String diagnostic) {
        return new CompiledFluid(
                resource.prefix().map(MaterialPrefixCatalog::require),
                resource.fluid(),
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
            boolean selectorSpecific) {
        try {
            int count = evaluateInt(override, base, context, ruleId);
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
            String materialId) {
        public ResourceRef(Optional<String> prefix, Optional<ResourceLocation> fixed) {
            this(prefix, fixed, "");
        }
    }

    public record PlannedResource(ResourceRef resource, int amount, int chance) {}

    public record Plan(
            ResourceLocation id,
            ResourceLocation target,
            String materialId,
            List<PlannedResource> itemInputs,
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
                Optional<ResourceLocation> fixed);
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
                    Optional<ResourceLocation> fixed) {
                if (prefix.isEmpty()) {
                    return BuiltInRegistries.FLUID.getOptional(fixed.orElseThrow());
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

    private static final ResourceResolver DEFAULT_RESOLVER = new ResourceResolver() {
        @Override
        public Optional<Ingredient> itemInput(
                MaterialDefinition material,
                Optional<MaterialPrefix> prefix,
                Optional<ResourceLocation> fixed) {
            return prefix.isPresent()
                    ? MaterialLookup.ingredient(material.id(), prefix.get())
                    : BuiltInRegistries.ITEM.getOptional(fixed.orElseThrow()).map(Ingredient::of);
        }

        @Override
        public Optional<Item> itemOutput(
                MaterialDefinition material,
                Optional<MaterialPrefix> prefix,
                Optional<ResourceLocation> fixed) {
            return prefix.isPresent()
                    ? MaterialLookup.item(material.id(), prefix.get())
                    : BuiltInRegistries.ITEM.getOptional(fixed.orElseThrow());
        }

        @Override
        public Optional<Fluid> fluid(
                MaterialDefinition material,
                Optional<MaterialPrefix> prefix,
                Optional<ResourceLocation> fixed) {
            if (prefix.isEmpty()) {
                return BuiltInRegistries.FLUID.getOptional(fixed.orElseThrow());
            }
            if (!material.forms().contains(prefix.get()) || !material.moltenFluid()) {
                return Optional.empty();
            }
            return ModFluids.molten(material.id())
                    .map(registration -> registration.source().get());
        }
    };

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
            RuleExpression amount,
            RuleExpression chance,
            String materialSelector,
            boolean optional) {
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
            RuleExpression amount) {}

    private record EvaluationContext(
            MaterialDefinition material,
            Set<MaterialPrefix> availablePrefixes,
            Double inputUnit,
            Double outputUnit,
            Double prefixUnit,
            Double targetUnits,
            Double resourcePrefixUnits) implements RuleExpression.Context {
        EvaluationContext(
                MaterialDefinition material,
                Set<MaterialPrefix> availablePrefixes,
                Double inputUnit,
                Double outputUnit,
                Double prefixUnit) {
            this(
                    material,
                    availablePrefixes,
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
                    availablePrefixes,
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
                default -> throw new IllegalArgumentException("unknown lookup " + function);
            };
            if (value <= 0L || value > 9_007_199_254_740_991L) {
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
        public boolean hasPrefix(String prefix) {
            return availablePrefixes.contains(MaterialPrefixCatalog.require(prefix));
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
