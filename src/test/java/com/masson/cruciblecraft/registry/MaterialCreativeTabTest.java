package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

class MaterialCreativeTabTest {
    @Test
    void allBuiltinPrefixesHaveOneSemanticTabAndUnknownPrefixesUseMisc() {
        var prefixes = MaterialPrefixCatalog.values();

        EnumMap<MaterialCreativeTab, Set<MaterialPrefix>> grouped =
                new EnumMap<>(MaterialCreativeTab.class);
        for (MaterialCreativeTab tab : MaterialCreativeTab.values()) {
            grouped.put(tab, new LinkedHashSet<>());
        }
        prefixes.forEach(prefix ->
                grouped.get(MaterialCreativeTab.forPrefix(prefix)).add(prefix));

        assertEquals(prefixes.size(), grouped.values().stream().mapToInt(Set::size).sum());
        assertEquals(
                Set.copyOf(prefixes),
                grouped.values().stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.toUnmodifiableSet()));
        assertEquals(1, grouped.get(MaterialCreativeTab.ORES).size());
        assertEquals(2, grouped.get(MaterialCreativeTab.RAW_ORES).size());
        assertTrue(prefixes.stream().anyMatch(MaterialCreativeTab::isToolHeadPrefix));
        assertTrue(grouped.get(MaterialCreativeTab.MISC).stream()
                .anyMatch(MaterialCreativeTab::isToolHeadPrefix));
        assertEquals(
                MaterialCreativeTab.MISC,
                MaterialCreativeTab.forPrefix(new MaterialPrefix("example:unknown")));
        assertEquals(
                MaterialCreativeTab.CABLES,
                MaterialCreativeTab.forPrefix(MaterialPrefixes.WIRE));
        assertEquals(
                MaterialCreativeTab.CABLES,
                MaterialCreativeTab.forPrefix(MaterialPrefixes.DOUBLE_WIRE));
        assertEquals(
                MaterialCreativeTab.CABLES,
                MaterialCreativeTab.forPrefix(MaterialPrefixes.CABLE));
        assertEquals(
                MaterialCreativeTab.WIRES,
                MaterialCreativeTab.forPrefix(MaterialPrefixes.FINE_WIRE));
    }

    @Test
    void currentGateAssignsEveryMaterialItemOnceWithinTabLimits(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        assertEquals(324L, registered.values().stream()
                .filter(forms -> forms.contains(MaterialPrefixes.DOUBLE_INGOT))
                .count());
        assertEquals(324L, registered.values().stream()
                .filter(forms -> forms.contains(MaterialPrefixes.TRIPLE_INGOT))
                .count());
        assertEquals(321L, registered.values().stream()
                .filter(forms -> forms.contains(MaterialPrefixes.INGOT_HOT))
                .count());
        Map<MaterialCreativeTab, List<String>> plan =
                MaterialCreativeTab.planEntryIds(materials, registered);

        LinkedHashSet<String> expectedOreItems = new LinkedHashSet<>();
        LinkedHashSet<String> expectedItems = new LinkedHashSet<>();
        for (var material : materials) {
            List<MaterialPrefix> forms = registered.get(material.id());
            if (forms.contains(MaterialPrefixes.ORE)) {
                expectedOreItems.add("cruciblecraft:" + material.id() + "_ore");
                expectedOreItems.add("cruciblecraft:deepslate_" + material.id() + "_ore");
            }
        }
        expectedItems.addAll(expectedOreItems);
        for (var material : materials) {
            for (MaterialPrefix prefix : registered.get(material.id())) {
                if (prefix.equals(MaterialPrefixes.ORE)
                        || MaterialCreativeTab.isToolHeadPrefix(prefix)) {
                    continue;
                }
                expectedItems.add(material.formItems().getOrDefault(
                        prefix,
                        "cruciblecraft:" + material.registryName(prefix)));
            }
        }

        List<String> actual = plan.values().stream().flatMap(List::stream).toList();
        assertEquals(expectedItems, new LinkedHashSet<>(actual));
        assertEquals(expectedItems.size(), actual.size(), "form_items must be de-duplicated");
        assertEquals(expectedOreItems, Set.copyOf(plan.get(MaterialCreativeTab.ORES)));
        plan.entrySet().stream()
                .filter(entry -> entry.getKey() != MaterialCreativeTab.ORES)
                .forEach(entry -> assertTrue(
                        java.util.Collections.disjoint(expectedOreItems, entry.getValue()),
                        entry.getKey().name()));

        Map<MaterialCreativeTab, Integer> counts = plan.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue().size()));
        assertEquals(
                Map.ofEntries(
                        Map.entry(MaterialCreativeTab.ORES, 294),
                        Map.entry(MaterialCreativeTab.RAW_ORES, 1_150),
                        Map.entry(MaterialCreativeTab.ORE_PROCESSING, 2_755),
                        Map.entry(MaterialCreativeTab.DUSTS, 3_590),
                        Map.entry(MaterialCreativeTab.METALS_GEMS, 4_724),
                        Map.entry(MaterialCreativeTab.PLATES, 3_785),
                        Map.entry(MaterialCreativeTab.PARTS, 4_237),
                        Map.entry(MaterialCreativeTab.MECHANICAL_PARTS, 2_152),
                        Map.entry(MaterialCreativeTab.WIRES, 168),
                        Map.entry(MaterialCreativeTab.CABLES, 625),
                        Map.entry(MaterialCreativeTab.PIPES, 406),
                        Map.entry(MaterialCreativeTab.MISC, 859)),
                counts);
        assertEquals(
                0,
                plan.get(MaterialCreativeTab.MISC).stream()
                        .filter(id -> id.contains("tool_head"))
                        .count());
        assertFalse(MaterialCreativeTab.toolHeadEntryIds(
                materials, registered, Map.of()).isEmpty());
        counts.forEach((tab, count) ->
                assertTrue(count < 6_000, tab + " has " + count + " entries"));
    }

    @Test
    void toolHeadFormsLeaveMaterialTabsAndAreCollectedForTools(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        Map<MaterialCreativeTab, List<String>> plan =
                MaterialCreativeTab.planEntryIds(materials, registered);
        assertEquals(
                0,
                plan.values().stream()
                        .flatMap(List::stream)
                        .filter(id -> id.contains("tool_head"))
                        .count());
        List<String> toolHeads = MaterialCreativeTab.toolHeadEntryIds(
                materials, registered, Map.of());
        assertFalse(toolHeads.isEmpty());
        assertTrue(toolHeads.stream().allMatch(id -> id.contains("tool_head")));
    }

    @Test
    void preferencesUseFirstOwnerAcrossFormsAndDoNotReplaceOreBlocks() {
        MaterialDefinition material = new MaterialDefinition(
                "shared",
                "shared",
                Optional.empty(),
                0,
                "#808080",
                "metallic",
                List.of(
                        MaterialPrefixes.ORE,
                        MaterialPrefixes.DUST,
                        MaterialPrefixes.INGOT),
                Map.of(MaterialPrefixes.DUST, "minecraft:clay"),
                new ThermalProperties(1.0),
                false,
                Map.of(),
                false);
        Map<String, String> preferences = Map.of(
                "shared/" + MaterialPrefixes.ORE.serializedId(), "minecraft:diamond",
                "shared/" + MaterialPrefixes.DUST.serializedId(), "minecraft:stick",
                "shared/" + MaterialPrefixes.INGOT.serializedId(), "minecraft:stick");

        Map<MaterialCreativeTab, List<String>> plan =
                MaterialCreativeTab.planEntryIds(
                        List.of(material),
                        Map.of("shared", material.forms()),
                        preferences);

        assertEquals(
                List.of(
                        "cruciblecraft:shared_ore",
                        "cruciblecraft:deepslate_shared_ore"),
                plan.get(MaterialCreativeTab.ORES));
        assertEquals(
                List.of("minecraft:stick"),
                plan.get(MaterialCreativeTab.DUSTS),
                "the first form owning a unified item determines its tab");
        assertEquals(List.of(), plan.get(MaterialCreativeTab.METALS_GEMS));
        assertEquals(
                3,
                plan.values().stream().mapToInt(List::size).sum(),
                "the shared preferred item must only appear once");
    }

    @Test
    void materialEntryPlanIsBuiltOncePerRuntimeRevision() {
        ModCreativeTabs.MaterialEntryPlanCache cache =
                new ModCreativeTabs.MaterialEntryPlanCache();
        AtomicInteger builds = new AtomicInteger();
        java.util.function.Supplier<Map<MaterialCreativeTab, List<String>>> planner = () -> {
            builds.incrementAndGet();
            return Map.of(MaterialCreativeTab.MISC, List.of("test:item"));
        };

        Map<MaterialCreativeTab, List<String>> first = cache.get(4L, planner);
        for (MaterialCreativeTab ignored : MaterialCreativeTab.values()) {
            assertSame(first, cache.get(4L, planner));
        }
        assertEquals(1, builds.get(), "all material tabs must share one plan for a revision");

        Map<MaterialCreativeTab, List<String>> reloaded = cache.get(5L, planner);
        assertEquals(2, builds.get());
        assertSame(reloaded, cache.get(5L, planner));
    }
}
