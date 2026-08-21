package com.masson.cruciblecraft.registry;

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
        assertEquals(57, prefixes.size());

        EnumMap<MaterialCreativeTab, Set<MaterialPrefix>> grouped =
                new EnumMap<>(MaterialCreativeTab.class);
        for (MaterialCreativeTab tab : MaterialCreativeTab.values()) {
            grouped.put(tab, new LinkedHashSet<>());
        }
        prefixes.forEach(prefix ->
                grouped.get(MaterialCreativeTab.forPrefix(prefix)).add(prefix));

        assertEquals(57, grouped.values().stream().mapToInt(Set::size).sum());
        assertEquals(
                Set.copyOf(prefixes),
                grouped.values().stream()
                        .flatMap(Set::stream)
                        .collect(Collectors.toUnmodifiableSet()));
        assertEquals(1, grouped.get(MaterialCreativeTab.ORES).size());
        assertEquals(8, grouped.get(MaterialCreativeTab.ORE_PROCESSING).size());
        assertEquals(3, grouped.get(MaterialCreativeTab.DUSTS).size());
        assertEquals(7, grouped.get(MaterialCreativeTab.METALS_GEMS).size());
        assertEquals(8, grouped.get(MaterialCreativeTab.PLATES).size());
        assertEquals(5, grouped.get(MaterialCreativeTab.PARTS).size());
        assertEquals(5, grouped.get(MaterialCreativeTab.MECHANICAL_PARTS).size());
        assertEquals(6, grouped.get(MaterialCreativeTab.WIRES).size());
        assertEquals(6, grouped.get(MaterialCreativeTab.CABLES).size());
        assertEquals(8, grouped.get(MaterialCreativeTab.PIPES).size());
        assertEquals(0, grouped.get(MaterialCreativeTab.MISC).size());
        assertEquals(
                MaterialCreativeTab.MISC,
                MaterialCreativeTab.forPrefix(new MaterialPrefix("example:unknown")));
    }

    @Test
    void currentGateAssignsEveryMaterialItemOnceWithinTabLimits(
            @TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        var registered = MaterialRegistrationGate.load(materials);
        assertEquals(323L, registered.values().stream()
                .filter(forms -> forms.contains(MaterialPrefixes.DOUBLE_INGOT))
                .count());
        assertEquals(323L, registered.values().stream()
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
                if (prefix.equals(MaterialPrefixes.ORE)) {
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
                        Map.entry(MaterialCreativeTab.ORES, 274),
                        Map.entry(MaterialCreativeTab.ORE_PROCESSING, 3_498),
                        Map.entry(MaterialCreativeTab.DUSTS, 2_682),
                        Map.entry(MaterialCreativeTab.METALS_GEMS, 2_260),
                        Map.entry(MaterialCreativeTab.PLATES, 2_333),
                        Map.entry(MaterialCreativeTab.PARTS, 3_248),
                        Map.entry(MaterialCreativeTab.MECHANICAL_PARTS, 1_890),
                        Map.entry(MaterialCreativeTab.WIRES, 255),
                        Map.entry(MaterialCreativeTab.CABLES, 151),
                        Map.entry(MaterialCreativeTab.PIPES, 282),
                        Map.entry(MaterialCreativeTab.MISC, 0)),
                counts);
        counts.forEach((tab, count) ->
                assertTrue(count < 4_000, tab + " has " + count + " entries"));
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
        assertEquals(1, builds.get(), "eleven tabs must share one plan for a revision");

        Map<MaterialCreativeTab, List<String>> reloaded = cache.get(5L, planner);
        assertEquals(2, builds.get());
        assertSame(reloaded, cache.get(5L, planner));
    }
}
