package com.masson.cruciblecraft.material.prefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
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

class T10HotIngotRuleDataTest {
    private static final Path RECIPE_ROOT = Path.of(
            "src/main/resources/data/cruciblecraft/recipe/t10");

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
    void hotIngotRulesMatchTheIndependentTagAndGateSet(
            @TempDir Path config) throws Exception {
        var materials = MaterialLoader.load(config).values();
        var registered = MaterialRegistrationGate.load(materials);
        Set<String> expected = new LinkedHashSet<>();
        materials.stream()
                .filter(material -> material.gt6Metadata()
                        .map(metadata -> metadata.generationTags().contains(
                                "ITEMGENERATOR.INGOTS_HOT"))
                        .orElse(false))
                .filter(material -> registered.get(material.id())
                        .contains(MaterialPrefixes.INGOT))
                .map(material -> material.id())
                .forEach(expected::add);
        assertEquals(321, expected.size());

        Path coolingRule = RECIPE_ROOT.resolve("cooling/hot_ingot_to_ingot.json");
        assertTrue(Files.notExists(coolingRule), coolingRule.toString());

        RuleCase entry = new RuleCase(
                "smelter/ingot_to_hot_ingot",
                "smelter/ingot_to_hot_ingot.json");
        MaterialRule rule = loadRule(RECIPE_ROOT.resolve(entry.path()));
        assertEquals(
                "material.has(\"gt6:itemgenerator/hotingots\")",
                rule.conditions().getFirst());
        var plans = MaterialRuleExpansion.expandPlansWithRegisteredForms(
                ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft",
                        "t10/" + entry.id()),
                rule,
                materials,
                registered);
        assertEquals(321, plans.size());
        assertEquals(
                expected,
                plans.stream()
                        .map(MaterialRuleExpansion.Plan::materialId)
                        .collect(java.util.stream.Collectors.toSet()));
        assertTrue(plans.stream().allMatch(plan ->
                plan.itemInputs().size() == 1
                        && plan.itemOutputs().size() == 1));
    }

    private static MaterialRule loadRule(Path path) throws Exception {
        JsonObject json = JsonParser.parseString(
                Files.readString(path)).getAsJsonObject();
        assertEquals(
                "cruciblecraft:material_rule",
                json.remove("type").getAsString());
        assertTrue(!json.has("balance_policy"));
        return MaterialRule.CODEC.codec()
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();
    }

    private record RuleCase(String id, String path) {}
}
