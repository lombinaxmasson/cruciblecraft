package com.masson.cruciblecraft.material.prefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.heat.ItemHeat;
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
        Map<String, com.masson.cruciblecraft.material.def.MaterialDefinition>
                materialsById = materials.stream().collect(
                        java.util.stream.Collectors.toUnmodifiableMap(
                                material -> material.id(),
                                material -> material));
        var registered = MaterialRegistrationGate.load(materials);
        Set<String> expected = new LinkedHashSet<>();
        materials.stream()
                .filter(material -> material.gt6Metadata().orElseThrow()
                        .generationTags().contains("ITEMGENERATOR.INGOTS_HOT"))
                .filter(material -> registered.get(material.id())
                        .contains(MaterialPrefixes.INGOT))
                .map(material -> material.id())
                .forEach(expected::add);
        assertEquals(321, expected.size());

        for (RuleCase entry : List.of(
                new RuleCase(
                        "smelter/ingot_to_hot_ingot",
                        "smelter/ingot_to_hot_ingot.json"),
                new RuleCase(
                        "cooling/hot_ingot_to_ingot",
                        "cooling/hot_ingot_to_ingot.json"))) {
            boolean cooling = entry.id().startsWith("cooling/");
            MaterialRule rule = loadRule(
                    RECIPE_ROOT.resolve(entry.path()),
                    cooling);
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
            if (cooling) {
                assertTrue(plans.stream().allMatch(plan ->
                        plan.duration() == (int) Math.ceil(Math.max(
                                1.0,
                                materialsById.get(plan.materialId())
                                                .thermal()
                                                .meltingPoint()
                                        - ItemHeat.AMBIENT_TEMPERATURE))));
            }
        }
    }

    private static MaterialRule loadRule(
            Path path,
            boolean expectsBalancePolicy) throws Exception {
        JsonObject json = JsonParser.parseString(
                Files.readString(path)).getAsJsonObject();
        assertEquals(
                "cruciblecraft:material_rule",
                json.remove("type").getAsString());
        JsonObject policy = json.has("balance_policy")
                ? json.remove("balance_policy").getAsJsonObject()
                : null;
        if (expectsBalancePolicy) {
            assertEquals("DESIGN_POLICY", policy.get("status").getAsString());
            assertEquals("UNVERIFIED", policy.get("gt6_equivalence").getAsString());
            assertEquals("O-36", policy.get("open_item").getAsString());
            assertEquals(
                    "celsius",
                    policy.get("temperature_unit").getAsString());
            assertEquals(
                    ItemHeat.AMBIENT_TEMPERATURE,
                    policy.get("ambient_temperature").getAsFloat());
            assertEquals(
                    ItemHeat.COOLING_RATE_PER_TICK,
                    policy.get("cooling_rate_per_tick").getAsFloat());
            assertEquals(
                    json.get("duration").getAsString(),
                    policy.get("duration_formula").getAsString());
            assertTrue(!policy.get("replacement_condition")
                    .getAsString()
                    .isBlank());
        } else {
            assertTrue(policy == null);
        }
        return MaterialRule.CODEC.codec()
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();
    }

    private record RuleCase(String id, String path) {}
}
