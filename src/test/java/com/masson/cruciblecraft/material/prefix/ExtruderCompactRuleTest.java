package com.masson.cruciblecraft.material.prefix;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExtruderCompactRuleTest {
    private static final Path COMPACT_ROOT = Path.of(
            "src/test/resources/extruder_compact_rule_fixture/"
                    + "recipe/extruder/compact");
    private static final Path EXPECTED =
            Path.of("tools/extruder_expected.json");
    private static final Path READINESS =
            Path.of("tools/extruder_readiness.json");

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
    void expectedFindMatchAndRuntimeExpansionAreBidirectional(
            @TempDir Path config) throws Exception {
        Map<String, ExpectedRow> expected = loadExpected();
        List<LoadedTable> tables = loadCompactTables();
        var materials = MaterialLoader.load(config).values();
        var registered = MaterialRegistrationGate.load(materials);
        Map<String, MaterialRuleExpansion.Plan> runtime = new LinkedHashMap<>();
        Set<String> relationIds = new LinkedHashSet<>();

        assertEquals(20, tables.size());
        for (LoadedTable loaded : tables) {
            MaterialRule.SparseTable sparse = loaded.rule().sparse().orElseThrow();
            assertEquals(sparse.relations().size(), sparse.relations().stream()
                    .map(MaterialRule.SparseRelation::material)
                    .distinct()
                    .count());
            for (MaterialRule.SparseRelation relation : sparse.relations()) {
                ExpectedRow row = expected.get(relation.stableId().toString());
                assertTrue(relationIds.add(relation.stableId().toString()));
                assertEquals(row.material(), relation.material());
                assertEquals(row.inputPrefix(), relation.input().prefix());
                assertEquals(row.inputCount(), relation.input().count());
                assertEquals(row.outputPrefix(), relation.output().prefix());
                assertEquals(row.outputCount(), relation.output().count());
                assertEquals(row.duration(), relation.duration());
                assertEquals(row.eut(), relation.eut());
                assertEquals(row.fallback(), relation.fallback());
                assertEquals(row.forgingTarget(), relation.forgingTarget().orElse(null));
                assertEquals(row.plateGem(), relation.plateGem());
                assertEquals(row.heatMode(), relation.heatMode());
                assertEquals(row.shadowOrder(), relation.shadowOrder());
                assertEquals(
                        relation,
                        sparse.findMatch(
                                row.material(),
                                row.inputPrefix(),
                                row.inputCount()).orElseThrow());
            }
            assertTrue(sparse.findMatch(
                    "not_a_material", "ingot", 1).isEmpty());
            List<MaterialRuleExpansion.Plan> plans =
                    MaterialRuleExpansion.expandPlansWithRegisteredForms(
                            loaded.id(),
                            loaded.rule(),
                            materials,
                            registered);
            for (MaterialRuleExpansion.Plan plan : plans) {
                assertEquals(null, runtime.put(plan.id().toString(), plan));
            }
        }

        assertEquals(expected.keySet(), relationIds);
        assertEquals(expected.keySet(), runtime.keySet());
        assertEquals(2782, runtime.size());
        for (ExpectedRow row : expected.values()) {
            MaterialRuleExpansion.Plan plan = runtime.get(row.stableId());
            assertEquals(row.material(), plan.materialId());
            assertEquals(row.inputPrefix(), prefix(plan.itemInputs().getFirst()));
            assertEquals(row.inputCount(), plan.itemInputs().getFirst().amount());
            assertEquals(row.outputPrefix(), prefix(plan.itemOutputs().getFirst()));
            assertEquals(row.outputCount(), plan.itemOutputs().getFirst().amount());
            assertEquals(row.duration(), plan.duration());
            assertEquals(row.eut(), plan.eut());
            assertTrue(plan.materialSpecific());
        }

        JsonObject readiness = JsonParser.parseString(
                Files.readString(READINESS)).getAsJsonObject();
        JsonObject scope = readiness.getAsJsonObject("scope");
        assertEquals(2782, scope.get("runtime_publication").getAsInt());
        assertEquals(
                257,
                scope.get("pipe_publication_separate_owner").getAsInt());
    }

    @Test
    void sparseCodecRejectsDuplicateIdAmbiguousMaterialAndFallbackDrift()
            throws Exception {
        JsonObject original = JsonParser.parseString(Files.readString(
                COMPACT_ROOT.resolve("normal_plate.json"))).getAsJsonObject();
        original.remove("type");

        JsonObject duplicate = original.deepCopy();
        var duplicateRelations = duplicate.getAsJsonObject("sparse")
                .getAsJsonArray("relations");
        duplicateRelations.add(duplicateRelations.get(0).deepCopy());
        assertThrows(RuntimeException.class, () -> decode(duplicate));

        JsonObject ambiguous = original.deepCopy();
        var ambiguousRelations = ambiguous.getAsJsonObject("sparse")
                .getAsJsonArray("relations");
        JsonObject second = ambiguousRelations.get(1).getAsJsonObject();
        second.addProperty(
                "material",
                ambiguousRelations.get(0).getAsJsonObject()
                        .get("material").getAsString());
        assertThrows(RuntimeException.class, () -> decode(ambiguous));

        JsonObject fallback = original.deepCopy();
        fallback.getAsJsonObject("sparse")
                .getAsJsonArray("relations")
                .get(0).getAsJsonObject()
                .addProperty("fallback", "dust");
        assertThrows(RuntimeException.class, () -> decode(fallback));
    }

    @Test
    void sparseExpansionFailsClosedWhenExactRegisteredFormDisappears(
            @TempDir Path config) throws Exception {
        LoadedTable loaded = loadCompactTables().getFirst();
        MaterialRule.SparseRelation relation =
                loaded.rule().sparse().orElseThrow().relations().getFirst();
        var materials = MaterialLoader.load(config).values();
        Map<String, List<MaterialPrefix>> registered = new LinkedHashMap<>(
                MaterialRegistrationGate.load(materials));
        List<MaterialPrefix> reduced = new ArrayList<>(
                registered.get(relation.material()));
        reduced.remove(MaterialPrefixCatalog.require(relation.input().prefix()));
        registered.put(relation.material(), List.copyOf(reduced));

        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialRuleExpansion.expandPlansWithRegisteredForms(
                        loaded.id(), loaded.rule(), materials, registered));
        assertTrue(error.getMessage().contains(relation.material()));
        assertTrue(error.getMessage().contains("registered forms"));
    }

    private static Map<String, ExpectedRow> loadExpected() throws Exception {
        var rows = JsonParser.parseString(Files.readString(EXPECTED))
                .getAsJsonObject().getAsJsonArray("relations");
        Map<String, ExpectedRow> result = new LinkedHashMap<>();
        rows.forEach(value -> {
            JsonObject row = value.getAsJsonObject();
            JsonObject input = row.getAsJsonObject("input");
            JsonObject output = row.getAsJsonObject("output");
            ExpectedRow expected = new ExpectedRow(
                    row.get("stable_id").getAsString(),
                    row.get("material").getAsString(),
                    input.get("prefix").getAsString(),
                    input.get("count").getAsInt(),
                    output.get("prefix").getAsString(),
                    output.get("count").getAsInt(),
                    row.get("duration").getAsInt(),
                    row.get("eut").getAsLong(),
                    row.get("fallback").getAsString(),
                    row.get("forging_target").isJsonNull()
                            ? null
                            : row.get("forging_target").getAsString(),
                    row.get("plateGem").getAsBoolean(),
                    row.get("heat_mode").getAsString(),
                    row.get("shadow_order").getAsInt());
            assertEquals(null, result.put(expected.stableId(), expected));
        });
        return Map.copyOf(result);
    }

    private static List<LoadedTable> loadCompactTables() throws Exception {
        try (var paths = Files.list(COMPACT_ROOT)) {
            return paths.filter(path -> path.toString().endsWith(".json"))
                    .sorted(Comparator.comparing(Path::toString))
                    .map(path -> {
                        try {
                            JsonObject json = JsonParser.parseString(
                                    Files.readString(path)).getAsJsonObject();
                            assertEquals(
                                    "cruciblecraft:material_rule",
                                    json.remove("type").getAsString());
                            String name = path.getFileName().toString();
                            String recipePath = "extruder/compact/"
                                    + name.substring(0, name.length() - 5);
                            return new LoadedTable(
                                    ResourceLocation.fromNamespaceAndPath(
                                            "cruciblecraft", recipePath),
                                    decode(json));
                        } catch (Exception exception) {
                            throw new IllegalStateException(exception);
                        }
                    })
                    .toList();
        }
    }

    private static MaterialRule decode(JsonObject json) {
        return MaterialRule.CODEC.codec()
                .parse(JsonOps.INSTANCE, json)
                .getOrThrow();
    }

    private static String prefix(
            MaterialRuleExpansion.PlannedResource resource) {
        String value = resource.resource().prefix().orElseThrow();
        int separator = value.indexOf(':');
        return separator < 0 ? value : value.substring(separator + 1);
    }

    private record LoadedTable(ResourceLocation id, MaterialRule rule) {}

    private record ExpectedRow(
            String stableId,
            String material,
            String inputPrefix,
            int inputCount,
            String outputPrefix,
            int outputCount,
            int duration,
            long eut,
            String fallback,
            String forgingTarget,
            boolean plateGem,
            String heatMode,
            int shadowOrder) {}
}
