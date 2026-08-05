package com.masson.cruciblecraft.recipe.rule;

import java.util.Collection;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialDefinition;

import net.minecraft.resources.ResourceLocation;

/** Production-path reachability report over the exact rules loaded into recipe maps. */
public final class T2ChainReachability {
    private T2ChainReachability() {}

    public static Report analyze(Collection<MaterialDefinition> materials) {
        Map<String, List<com.masson.cruciblecraft.api.material.MaterialPrefix>>
                registeredForms = MaterialRegistrationGate.load(materials);
        LinkedHashMap<ResourceLocation, Integer> recipes = new LinkedHashMap<>();
        LinkedHashSet<String> reachable = new LinkedHashSet<>();
        LinkedHashMap<String, List<ResourceLocation>> paths = new LinkedHashMap<>();
        LinkedHashMap<String, String> unreachable = new LinkedHashMap<>();
        List<MaterialRuleExpansion.Plan> allPlans = new ArrayList<>();
        int byproductOutputs = 0;
        allPlans.addAll(MaterialRuleExpansion.expandPlansWithRegisteredForms(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "crusher/raw_ore_to_crushed_ore"),
                T2ChainRules.CRUSHER_RAW_TO_CRUSHED,
                materials,
                registeredForms));
        for (T2ChainRules.Definition definition : T2ChainRules.ALL) {
            var plans = MaterialRuleExpansion.expandPlansWithRegisteredForms(
                    ResourceLocation.fromNamespaceAndPath(
                            "cruciblecraft", definition.path()),
                    definition.rule(),
                    materials,
                    registeredForms);
            recipes.put(definition.rule().target().orElseThrow(), plans.size());
            allPlans.addAll(plans);
            byproductOutputs += plans.stream().mapToInt(plan ->
                    Math.max(0, plan.itemOutputs().size() - 1)).sum();
        }
        Map<String, MaterialDefinition> byId = materials.stream().collect(
                java.util.stream.Collectors.toMap(MaterialDefinition::id, material -> material));
        for (MaterialDefinition material : materials.stream()
                .filter(value -> registeredForms.get(value.id())
                        .contains(MaterialPrefixes.RAW_ORE))
                .sorted(java.util.Comparator.comparing(MaterialDefinition::id))
                .toList()) {
            Map<Node, List<ResourceLocation>> available = concreteClosure(material, allPlans);
            var target = material.gt6Metadata().map(metadata ->
                    metadata.processingTargets().get("smelting")).orElse(null);
            if (target == null || target.ccUnits().isEmpty() || target.ccUnits().get() <= 0L) {
                unreachable.put(material.id(), "missing positive exact smelting target amount");
                continue;
            }
            MaterialDefinition targetMaterial = byId.get(target.material());
            if (targetMaterial == null
                    || !registeredForms.get(targetMaterial.id())
                            .contains(MaterialPrefixes.INGOT)) {
                unreachable.put(material.id(), "smelting target has no registered ingot form");
                continue;
            }
            Node goal = new Node(target.material(), MaterialPrefixes.INGOT.serializedId());
            List<ResourceLocation> path = available.get(goal);
            if (path != null) {
                reachable.add(material.id());
                paths.put(material.id(), path);
            } else if (available.containsKey(
                    new Node(material.id(), MaterialPrefixes.DUST.serializedId()))) {
                unreachable.put(material.id(),
                        "no representable expanded smelting batch within one-slot capacity");
            } else {
                unreachable.put(material.id(), "no concrete expanded raw-ore-to-dust path");
            }
        }
        return new Report(recipes, reachable, unreachable, paths, byproductOutputs);
    }

    private static Map<Node, List<ResourceLocation>> concreteClosure(
            MaterialDefinition source,
            List<MaterialRuleExpansion.Plan> plans) {
        LinkedHashMap<Node, List<ResourceLocation>> available = new LinkedHashMap<>();
        available.put(new Node(
                source.id(), MaterialPrefixes.RAW_ORE.serializedId()), List.of());
        boolean changed;
        do {
            changed = false;
            for (MaterialRuleExpansion.Plan plan : plans) {
                List<Node> inputs = plan.itemInputs().stream()
                        .map(T2ChainReachability::node)
                        .toList();
                if (inputs.isEmpty() || !inputs.stream().allMatch(available::containsKey)) {
                    continue;
                }
                List<ResourceLocation> parentPath = inputs.stream()
                        .map(available::get)
                        .max(java.util.Comparator.comparingInt(List::size))
                        .orElseThrow();
                List<ResourceLocation> nextPath = new ArrayList<>(parentPath);
                nextPath.add(plan.id());
                for (MaterialRuleExpansion.PlannedResource output : plan.itemOutputs()) {
                    if (output.chance() != 10_000) continue;
                    Node outputNode = node(output);
                    if (available.putIfAbsent(outputNode, List.copyOf(nextPath)) == null) {
                        changed = true;
                    }
                }
            }
        } while (changed);
        return available;
    }

    private static Node node(MaterialRuleExpansion.PlannedResource resource) {
        return new Node(
                resource.resource().materialId(),
                resource.resource().prefix().orElseThrow());
    }

    private record Node(String materialId, String prefixId) {}

    public record Report(
            Map<ResourceLocation, Integer> recipesPerMap,
            Set<String> reachableMaterials,
            Map<String, String> unreachableOres,
            Map<String, List<ResourceLocation>> pathsByMaterial,
            int byproductOutputs) {
        public Report {
            recipesPerMap = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(recipesPerMap));
            reachableMaterials = java.util.Collections.unmodifiableSet(
                    new LinkedHashSet<>(reachableMaterials));
            unreachableOres = java.util.Collections.unmodifiableMap(
                    new LinkedHashMap<>(unreachableOres));
            LinkedHashMap<String, List<ResourceLocation>> copiedPaths = new LinkedHashMap<>();
            pathsByMaterial.forEach((material, path) ->
                    copiedPaths.put(material, List.copyOf(path)));
            pathsByMaterial = java.util.Collections.unmodifiableMap(copiedPaths);
        }
    }
}
