package com.masson.cruciblecraft.material.prefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
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

class T10MultiIngotRuleDataTest {
    private static final Path RULE_ROOT = Path.of(
            "src/main/resources/data/cruciblecraft/recipe/t10/anvil");

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
    void multiIngotRulesMatchTheIndependentTagAndGateSet(
            @TempDir Path config) throws Exception {
        var materials = MaterialLoader.load(config).values();
        var registered = MaterialRegistrationGate.load(materials);
        Set<String> expected = new LinkedHashSet<>();
        materials.stream()
                .filter(material -> material.gt6Metadata()
                        .map(metadata -> metadata.generationTags().contains(
                                "ITEMGENERATOR.MULTIINGOTS"))
                        .orElse(false))
                .filter(material -> registered.get(material.id())
                        .contains(MaterialPrefixes.INGOT))
                .map(material -> material.id())
                .forEach(expected::add);
        assertEquals(323, expected.size());

        MaterialRule doubleRule = loadRule(
                RULE_ROOT.resolve("ingot_to_double_ingot.json"));
        MaterialRule tripleRule = loadRule(
                RULE_ROOT.resolve("ingot_to_triple_ingot.json"));
        assertEquals(List.of(
                "material.has(\"gt6:itemgenerator/multiingots\")",
                "has_registered(ingot) && has_registered(double_ingot)"),
                doubleRule.conditions());
        assertEquals("8", doubleRule.specialValue());
        assertEquals("12", tripleRule.specialValue());

        Set<String> signatures = new LinkedHashSet<>();
        for (var entry : List.of(
                new RuleCase("ingot_to_double_ingot", doubleRule),
                new RuleCase("ingot_to_triple_ingot", tripleRule))) {
            var plans = MaterialRuleExpansion.expandPlansWithRegisteredForms(
                    ResourceLocation.fromNamespaceAndPath(
                            "cruciblecraft",
                            "t10/anvil/" + entry.id()),
                    entry.rule(),
                    materials,
                    registered);
            assertEquals(323, plans.size());
            assertEquals(
                    expected,
                    plans.stream()
                            .map(MaterialRuleExpansion.Plan::materialId)
                            .collect(java.util.stream.Collectors.toSet()));
            plans.forEach(plan -> assertTrue(signatures.add(
                    entry.id() + "/" + plan.materialId())));
        }
        assertEquals(646, signatures.size());
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

    private record RuleCase(String id, MaterialRule rule) {}
}
