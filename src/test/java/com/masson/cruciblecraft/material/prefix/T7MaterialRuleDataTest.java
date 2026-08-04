package com.masson.cruciblecraft.material.prefix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.MaterialRegistrationGate;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.recipe.rule.MaterialRule;
import com.masson.cruciblecraft.recipe.rule.MaterialRuleExpansion;
import com.mojang.serialization.JsonOps;

import net.minecraft.resources.ResourceLocation;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class T7MaterialRuleDataTest {
    private static final Path MATERIAL_ROOT = Path.of(
            "src/main/resources/data/cruciblecraft/materials");
    private static final Path GATE = Path.of(
            "src/main/resources/data/cruciblecraft/material_registration_gate.json");
    private static final Map<String, Path> RULE_PATHS = Map.of(
            "ingot_to_dust", Path.of(
                    "src/main/resources/data/cruciblecraft/recipe/mortar/"
                            + "ingot_to_dust.json"),
            "gem_to_dust", Path.of(
                    "src/main/resources/data/cruciblecraft/recipe/mortar/"
                            + "gem_to_dust.json"));

    @BeforeEach
    void bootstrapPrefixes() {
        MaterialPrefixTestFixture.reset();
        MaterialPrefixTestFixture.bootstrapBuiltins();
    }

    @AfterEach
    void restorePrefixes() {
        MaterialPrefixTestFixture.reset();
        MaterialPrefixTestFixture.bootstrapBuiltins();
    }

    @Test
    void authoredRulesMatchIndependentlyDerivedMaterialSets(
            @TempDir Path config) throws Exception {
        var materials = MaterialLoader.load(config).values();
        var registered = MaterialRegistrationGate.load(materials);
        Map<String, Set<String>> expected = independentlyDerivedSets();
        Map<String, Set<String>> actual = new LinkedHashMap<>();
        Set<String> signatures = new LinkedHashSet<>();

        for (var entry : RULE_PATHS.entrySet()) {
            MaterialRule rule = loadRule(entry.getValue());
            assertEquals(
                    List.of(
                            "material.tag(\"PROCESSING.MORTAR_GRINDABLE\")",
                            "has_registered(" + inputForm(entry.getKey())
                                    + ") && has_registered(dust)"),
                    rule.conditions());
            List<MaterialRuleExpansion.Plan> plans =
                    MaterialRuleExpansion.expandPlansWithRegisteredForms(
                            ResourceLocation.fromNamespaceAndPath(
                                    "cruciblecraft",
                                    "mortar/" + entry.getKey()),
                            rule,
                            materials,
                            registered);
            Set<String> materialIds = plans.stream()
                    .map(MaterialRuleExpansion.Plan::materialId)
                    .collect(java.util.stream.Collectors.toCollection(
                            LinkedHashSet::new));
            assertEquals(plans.size(), materialIds.size());
            actual.put(entry.getKey(), materialIds);
            plans.forEach(plan -> assertTrue(signatures.add(
                    entry.getKey() + "/" + plan.materialId())));
        }

        assertEquals(expected, actual);
        assertEquals(126, actual.get("ingot_to_dust").size());
        assertEquals(94, actual.get("gem_to_dust").size());
        assertEquals(220, signatures.size());
        assertTrue(actual.get("ingot_to_dust").contains("iron"));
        assertTrue(actual.get("gem_to_dust").contains("amber"));
    }

    private static MaterialRule loadRule(Path path) throws Exception {
        JsonObject json = JsonParser.parseString(
                Files.readString(path)).getAsJsonObject();
        assertEquals(
                "cruciblecraft:material_rule",
                json.remove("type").getAsString());
        return MaterialRule.CODEC.codec()
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();
    }

    private static Map<String, Set<String>> independentlyDerivedSets()
            throws Exception {
        JsonObject gate = JsonParser.parseString(
                Files.readString(GATE)).getAsJsonObject()
                .getAsJsonObject("materials");
        var index = JsonParser.parseString(Files.readString(
                MATERIAL_ROOT.resolve("index.json"))).getAsJsonArray();
        Map<String, Set<String>> result = new LinkedHashMap<>();
        result.put("ingot_to_dust", new LinkedHashSet<>());
        result.put("gem_to_dust", new LinkedHashSet<>());
        for (var value : index) {
            JsonObject material = JsonParser.parseString(Files.readString(
                    MATERIAL_ROOT.resolve(value.getAsString()))).getAsJsonObject();
            String materialId = material.get("id").getAsString();
            Set<String> tags = strings(
                    material.getAsJsonObject("gt6_metadata")
                            .getAsJsonArray("material_tags"));
            Set<String> forms = strings(gate.getAsJsonArray(materialId));
            if (!tags.contains("PROCESSING.MORTAR_GRINDABLE")
                    || !forms.contains("dust")) {
                continue;
            }
            if (forms.contains("ingot")) {
                result.get("ingot_to_dust").add(materialId);
            }
            if (forms.contains("gem")) {
                result.get("gem_to_dust").add(materialId);
            }
        }
        return result;
    }

    private static Set<String> strings(
            com.google.gson.JsonArray values) {
        return values.asList().stream()
                .map(value -> value.getAsString())
                .collect(java.util.stream.Collectors.toCollection(
                        LinkedHashSet::new));
    }

    private static String inputForm(String ruleId) {
        return ruleId.substring(0, ruleId.indexOf('_'));
    }
}
