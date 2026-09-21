package com.masson.cruciblecraft.material.prefix;

import java.nio.file.Files;
import java.nio.file.Path;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PipeRuleDataTest {
    private static final Path RULE_ROOT = Path.of(
            "src/main/resources/data/cruciblecraft/recipe/pipe/extruder");
    private static final Map<String, Domain> RULES = Map.of(
            "plate_to_tiny_fluid_pipe", new Domain("tiny_fluid_pipe", true),
            "plate_to_small_fluid_pipe", new Domain("small_fluid_pipe", true),
            "plates_to_fluid_pipe", new Domain("fluid_pipe", true),
            "plates_to_large_fluid_pipe", new Domain("large_fluid_pipe", true),
            "plates_to_huge_fluid_pipe", new Domain("huge_fluid_pipe", true),
            "plates_to_item_pipe", new Domain("item_pipe", false),
            "plates_to_large_item_pipe", new Domain("large_item_pipe", false),
            "plates_to_huge_item_pipe", new Domain("huge_item_pipe", false));

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
    void genericRulesPublishOnlySourceRecipeEnabledPipes(
            @TempDir Path config) throws Exception {
        var materials = MaterialLoader.load(config).values();
        var registered = MaterialRegistrationGate.load(materials);
        Set<String> signatures = new LinkedHashSet<>();
        int plans = 0;

        for (var entry : RULES.entrySet()) {
            MaterialRule rule = loadRule(
                    RULE_ROOT.resolve(entry.getKey() + ".json"));
            Domain domain = entry.getValue();
            String recipeFact = (domain.fluid()
                    ? "fluid_pipe_recipe("
                    : "item_pipe_recipe(")
                    + domain.outputForm() + ")";
            assertEquals(
                    List.of(
                            "material.tag(\"PROCESSING.EXTRUDABLE\")",
                            "has_registered(plate) && has_registered("
                                    + domain.outputForm() + ") && "
                                    + recipeFact + " == 1"),
                    rule.conditions());
            var expanded = MaterialRuleExpansion
                    .expandPlansWithRegisteredForms(
                            ResourceLocation.fromNamespaceAndPath(
                                    "cruciblecraft",
                                    "pipe/extruder/" + entry.getKey()),
                            rule,
                            materials,
                            registered);
            assertEquals(domain.fluid() ? 35 : 21, expanded.size());
            for (var plan : expanded) {
                assertTrue(signatures.add(
                        entry.getKey() + "/" + plan.materialId()));
                if (domain.fluid()) {
                    assertFalse(Set.of(
                                    "carbon",
                                    "plastic",
                                    "rubber",
                                    "wood",
                                    "wood_treated")
                            .contains(plan.materialId()));
                }
            }
            plans += expanded.size();
        }

        assertEquals(238, plans);
        assertEquals(238, signatures.size());
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

    private record Domain(String outputForm, boolean fluid) {}
}
