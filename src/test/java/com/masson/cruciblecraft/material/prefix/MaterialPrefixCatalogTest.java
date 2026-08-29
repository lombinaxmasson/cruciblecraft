package com.masson.cruciblecraft.material.prefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.masson.cruciblecraft.api.material.MaterialPrefix;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.material.MaterialFingerprint;
import com.masson.cruciblecraft.material.MissingMaterialStackRewriter;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class MaterialPrefixCatalogTest {
    @Test
    void rawOreUsesNeoForgeSharedMaterialTag() {
        var rawOre = MaterialPrefixCatalog.definition(MaterialPrefixes.RAW_ORE);
        assertEquals("c", rawOre.tagNamespace());
        assertEquals("raw_materials", rawOre.tagDirectory());
    }

    @Test
    void earlyAccessFailsInsteadOfFreezingImplicitly() {
        MaterialPrefixTestFixture.reset();
        try {
            assertThrows(IllegalStateException.class, MaterialPrefixCatalog::values);
            assertThrows(IllegalStateException.class, MaterialPrefixes.INGOT::units);
            assertTrue(MaterialPrefix.CODEC.parse(
                            JsonOps.INSTANCE,
                            new JsonPrimitive("ingot"))
                    .error()
                    .isPresent());
            assertFalse(MaterialPrefixCatalog.isBootstrapped());
        } finally {
            MaterialPrefixTestFixture.bootstrapBuiltins();
        }
    }

    @Test
    void bootstrapsActualConfigDirectoryAndRejectsDuplicates(@TempDir Path directory)
            throws Exception {
        Files.writeString(directory.resolve("wire.json"), """
                {
                  "id": "example:wire",
                  "serialized_path": "addon_wire",
                  "units": 9,
                  "generation_flag": "example:wire",
                  "tag_directory": "wires",
                  "model_texture": "minecraft:item/string",
                  "aliases": ["legacy/wire"],
                  "capabilities": []
                }
                """);
        MaterialPrefixTestFixture.reset();
        try {
            MaterialPrefixCatalog.bootstrap(directory);
            assertEquals(9, MaterialPrefixCatalog.require("example:wire").units());
            assertTrue(Files.exists(directory.resolve("_legacy_prefixes.json")));
        } finally {
            MaterialPrefixTestFixture.reset();
            MaterialPrefixTestFixture.bootstrapBuiltins();
        }

        Path duplicateDirectory = Files.createDirectory(directory.resolve("duplicate"));
        Files.writeString(duplicateDirectory.resolve("duplicate.json"), """
                {
                  "id": "cruciblecraft:ingot",
                  "serialized_path": "other_ingot",
                  "units": 144,
                  "generation_flag": "cruciblecraft:ingot",
                  "tag_directory": "ingots",
                  "model_texture": "minecraft:item/iron_ingot"
                }
                """);
        MaterialPrefixTestFixture.reset();
        try {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> MaterialPrefixCatalog.bootstrap(duplicateDirectory));
            assertFalse(MaterialPrefixCatalog.isBootstrapped());
        } finally {
            MaterialPrefixTestFixture.bootstrapBuiltins();
        }
    }

    @Test
    void persistsRemovedAddonSuffixesAndSupportsHierarchicalAliases(
            @TempDir Path directory) {
        MaterialPrefixDefinition wire = definition(
                new MaterialPrefix("example:wire"),
                "addon_wire",
                List.of("legacy/wire"));
        MaterialPrefixTestFixture.reset();
        try {
            assertTrue(MaterialPrefixCatalog.addStartupPrefix(wire));
            MaterialPrefixCatalog.bootstrap(directory);
            assertTrue(MaterialPrefixCatalog.legacySuffixesLongestFirst().stream()
                    .anyMatch(suffix -> suffix.value().equals("legacy/wire")));

            MaterialPrefixTestFixture.reset();
            MaterialPrefixCatalog.bootstrap(directory);
            assertFalse(MaterialPrefixCatalog.find("example:wire").isPresent());

            var unknown = MissingMaterialStackRewriter.plan(
                    "cruciblecraft:blue_steel_legacy/wire",
                    Map.of(),
                    ignored -> false);
            assertEquals(MissingMaterialStackRewriter.Kind.UNKNOWN, unknown.kind());
            assertEquals("blue_steel", unknown.materialId());
            assertEquals("addon_wire", unknown.form());

            var protectedItem = MissingMaterialStackRewriter.plan(
                    "cruciblecraft:blue_steel_legacy/wire",
                    Map.of(),
                    "cruciblecraft:blue_steel_legacy/wire"::equals);
            assertEquals(MissingMaterialStackRewriter.Kind.UNCHANGED, protectedItem.kind());
        } finally {
            MaterialPrefixTestFixture.reset();
            MaterialPrefixTestFixture.bootstrapBuiltins();
        }
    }

    @Test
    void preservesRetiredTinyDustAliasWhenCanonicalPathIsReused(
            @TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("_legacy_prefixes.json"), """
                {
                  "suffixes": {
                    "tiny_dust": "small_dust"
                  }
                }
                """);
        MaterialPrefixTestFixture.reset();
        try {
            MaterialPrefixCatalog.bootstrap(directory);
            assertEquals(
                    MaterialPrefixes.TINY_DUST,
                    MaterialPrefixCatalog.require("tiny_dust"));
            assertTrue(MaterialPrefixCatalog.legacySuffixesLongestFirst().stream()
                    .anyMatch(suffix -> suffix.value().equals("tiny_dust")
                            && suffix.canonicalPath().equals("small_dust")));

            var migrated = MissingMaterialStackRewriter.plan(
                    "cruciblecraft:blue_steel_tiny_dust",
                    Map.of(
                            "blue_steel/small_dust",
                            "cruciblecraft:blue_steel/small_dust"),
                    ignored -> false);
            assertEquals(MissingMaterialStackRewriter.Kind.CANONICAL, migrated.kind());
            assertEquals("small_dust", migrated.form());
        } finally {
            MaterialPrefixTestFixture.reset();
            MaterialPrefixTestFixture.bootstrapBuiltins();
        }
    }

    @Test
    void loadsBuiltinIdsUnitsTagsAndModelHints() {
        assertEquals(
                List.of(
                        MaterialPrefixes.BLOCK,
                        MaterialPrefixes.ORE,
                        MaterialPrefixes.RAW_ORE,
                        MaterialPrefixes.CRUSHED_ORE,
                        MaterialPrefixes.TINY_CRUSHED_ORE,
                        MaterialPrefixes.WASHED_CRUSHED_ORE,
                        MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                        MaterialPrefixCatalog.require(
                                "tiny_centrifuged_crushed_ore"),
                        MaterialPrefixes.PURIFIED_DUST,
                        MaterialPrefixes.INGOT,
                        MaterialPrefixes.DUST,
                        MaterialPrefixes.PLATE,
                        MaterialPrefixCatalog.require("plate_gem"),
                        MaterialPrefixes.ROD,
                        MaterialPrefixes.SMALL_DUST),
                List.copyOf(MaterialPrefixCatalog.values()).subList(0, 15));
        assertEquals(58, MaterialPrefixCatalog.values().size());
        assertEquals(16, MaterialPrefixes.TINY_CRUSHED_ORE.units());
        assertEquals(144, MaterialPrefixes.WASHED_CRUSHED_ORE.units());
        assertEquals(144, MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE.units());
        assertEquals(144, MaterialPrefixes.PURIFIED_DUST.units());
        assertEquals(
                List.of(
                        1296, 144, 144, 144, 16, 144, 144, 22,
                        144, 144, 144, 144, 144, 72, 36),
                List.copyOf(MaterialPrefixCatalog.values()).subList(0, 15).stream()
                        .map(MaterialPrefix::units).toList());
        assertEquals("small_dusts", MaterialPrefixes.SMALL_DUST.tagDirectory());
        assertEquals(144, MaterialPrefixes.GEM.units());
        assertEquals(16, MaterialPrefixes.TINY_DUST.units());
        assertEquals(2, MaterialPrefixes.DUST_DIV72.units());
        assertEquals("c", MaterialPrefixes.GEM.tagNamespace());
        assertEquals("storage_blocks", MaterialPrefixes.BLOCK.tagDirectory());
        assertEquals("c", MaterialPrefixes.BLOCK.tagNamespace());
        assertEquals("c", MaterialPrefixes.SMALL_DUST.tagNamespace());
        assertEquals(MaterialPrefixes.RAW_ORE, MaterialPrefixCatalog.require("oreraw"));
        assertEquals(MaterialPrefixes.CRUSHED_ORE, MaterialPrefixCatalog.require("crushed"));
        assertEquals(MaterialPrefixes.SMALL_DUST, MaterialPrefixCatalog.require("dustsmall"));
        assertEquals(MaterialPrefixes.TINY_DUST, MaterialPrefixCatalog.require("dusttiny"));
        assertEquals(MaterialPrefixes.DUST_DIV72, MaterialPrefixCatalog.require("dustDiv72"));
        assertEquals(MaterialPrefixes.DOUBLE_INGOT, MaterialPrefixCatalog.require("ingotdouble"));
        assertEquals(MaterialPrefixes.TRIPLE_INGOT, MaterialPrefixCatalog.require("ingottriple"));
        assertEquals(MaterialPrefixes.INGOT_HOT, MaterialPrefixCatalog.require("ingothot"));
        assertEquals(288, MaterialPrefixes.DOUBLE_INGOT.units());
        assertEquals(432, MaterialPrefixes.TRIPLE_INGOT.units());
        assertEquals(144, MaterialPrefixes.INGOT_HOT.units());
        assertEquals(
                3.0,
                MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT_HOT).heatDamage());
        assertEquals(
                0.0,
                MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT).heatDamage());
        assertEquals(
                MaterialPrefixes.TINY_CRUSHED_ORE,
                MaterialPrefixCatalog.require("crushedtiny"));
        assertEquals(
                Set.of(
                        MaterialPrefixes.BLOCK,
                        MaterialPrefixes.ORE,
                        MaterialPrefixes.RAW_ORE,
                        MaterialPrefixes.INGOT,
                        MaterialPrefixes.DUST,
                        MaterialPrefixes.PLATE,
                        MaterialPrefixCatalog.require("plate_gem"),
                        MaterialPrefixes.ROD,
                        MaterialPrefixes.NUGGET,
                        MaterialPrefixes.GEM,
                        MaterialPrefixes.WIRE,
                        MaterialPrefixes.SMALL_DUST,
                        MaterialPrefixes.TINY_DUST,
                        MaterialPrefixes.DUST_DIV72,
                        MaterialPrefixes.CRUSHED_ORE,
                        MaterialPrefixes.FOIL,
                        MaterialPrefixes.GEAR,
                        MaterialPrefixes.FINE_WIRE,
                        MaterialPrefixes.CABLE,
                        MaterialPrefixes.DOUBLE_CABLE,
                        MaterialPrefixes.QUADRUPLE_CABLE,
                        MaterialPrefixes.OCTUPLE_CABLE,
                        MaterialPrefixes.DODECUPLE_CABLE,
                        MaterialPrefixes.TINY_FLUID_PIPE,
                        MaterialPrefixes.SMALL_FLUID_PIPE,
                        MaterialPrefixes.FLUID_PIPE,
                        MaterialPrefixes.LARGE_FLUID_PIPE,
                        MaterialPrefixes.HUGE_FLUID_PIPE,
                        MaterialPrefixes.ITEM_PIPE,
                        MaterialPrefixCatalog.require("large_item_pipe"),
                        MaterialPrefixCatalog.require("huge_item_pipe"),
                        MaterialPrefixes.DOUBLE_WIRE,
                        MaterialPrefixes.QUADRUPLE_WIRE,
                        MaterialPrefixes.OCTUPLE_WIRE,
                        MaterialPrefixes.DODECUPLE_WIRE,
                        MaterialPrefixes.HEXADECUPLE_WIRE,
                        MaterialPrefixCatalog.require("rock")),
                MaterialPrefixCatalog.values().stream()
                        .filter(prefix -> prefix.tagNamespace().equals("c"))
                        .collect(java.util.stream.Collectors.toUnmodifiableSet()));
        assertEquals(
                "cruciblecraft:item/material/ingot",
                MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT).modelTexture());
    }

    @Test
    void rejectsIdPathAndAliasConflicts() {
        MaterialPrefix first = new MaterialPrefix("example:first");
        MaterialPrefix second = new MaterialPrefix("example:second");
        assertThrows(IllegalArgumentException.class, () ->
                MaterialPrefixCatalog.validateDefinitions(List.of(
                        definition(first, "first", List.of("shared")),
                        definition(second, "second", List.of("shared")))));
        assertThrows(IllegalArgumentException.class, () ->
                MaterialPrefixCatalog.validateDefinitions(List.of(
                        definition(first, "same", List.of()),
                        definition(second, "same", List.of()))));
    }

    @Test
    void resolvesGenerationFlagsThenIncludesAndExcludesAndWritesNewSchema() {
        JsonObject json = JsonParser.parseString("""
                {
                  "id": "testium",
                  "generation_flags": [
                    "cruciblecraft:generates_dust",
                    "cruciblecraft:generates_ingot"
                  ],
                  "include_prefixes": ["tiny_dust"],
                  "exclude_prefixes": ["dust", "small_dust"],
                  "thermal": {
                    "melting_point": 1000,
                    "boiling_point": 2000,
                    "density": 5
                  }
                }
                """).getAsJsonObject();
        MaterialDefinition material = MaterialDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow();

        assertEquals(
                List.of(MaterialPrefixes.INGOT, MaterialPrefixes.TINY_DUST),
                material.forms());
        assertTrue(material.generationFlags().cardinality() == 2);
        assertEquals(List.of("tiny_dust"), material.includedPrefixIds());
        assertEquals(List.of("dust", "small_dust"), material.excludedPrefixIds());
        assertEquals(
                List.of(
                        "cruciblecraft:generates_dust",
                        "cruciblecraft:generates_ingot"),
                material.generationFlagIds());

        JsonObject encoded = MaterialDefinition.CODEC.encodeStart(JsonOps.INSTANCE, material)
                .getOrThrow()
                .getAsJsonObject();
        assertTrue(encoded.has("generation_flags"));
        assertTrue(encoded.has("include_prefixes"));
        assertEquals(
                "tiny_dust",
                encoded.getAsJsonArray("include_prefixes").get(0).getAsString());
        assertEquals(
                "dust",
                encoded.getAsJsonArray("exclude_prefixes").get(0).getAsString());
        assertFalse(encoded.has("forms"));
    }

    @Test
    void oreImplicationsCloseBeforeExplicitExcludes() {
        JsonObject json = JsonParser.parseString("""
                {
                  "id": "implied_ore",
                  "include_prefixes": ["ore"],
                  "exclude_prefixes": ["washed_crushed_ore"],
                  "thermal": {
                    "melting_point": 1000,
                    "boiling_point": 2000,
                    "density": 5
                  }
                }
                """).getAsJsonObject();
        MaterialDefinition material = MaterialDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow();

        assertEquals(
                List.of(
                        MaterialPrefixes.ORE,
                        MaterialPrefixes.CRUSHED_ORE,
                        MaterialPrefixes.CENTRIFUGED_CRUSHED_ORE,
                        MaterialPrefixes.PURIFIED_DUST),
                material.forms());
        assertFalse(material.forms().contains(MaterialPrefixes.RAW_ORE));
        assertFalse(material.forms().contains(MaterialPrefixes.DUST));
        assertFalse(material.forms().contains(MaterialPrefixes.INGOT));
        assertEquals(
                List.of(
                        "crushed_ore",
                        "washed_crushed_ore",
                        "centrifuged_crushed_ore",
                        "purified_dust"),
                MaterialPrefixCatalog.definition(MaterialPrefixes.ORE).impliedPrefixes());

        JsonObject encodedPrefix = MaterialPrefixDefinition.CODEC.encodeStart(
                        JsonOps.INSTANCE,
                        MaterialPrefixCatalog.definition(MaterialPrefixes.ORE))
                .getOrThrow()
                .getAsJsonObject();
        assertEquals(
                4,
                encodedPrefix.getAsJsonArray("implied_prefixes").size());
    }

    @Test
    void addonImplicationsAreTransitiveAndExplicitExcludesWin(
            @TempDir Path directory) throws Exception {
        Files.writeString(directory.resolve("a.json"), """
                {
                  "id": "example:a",
                  "serialized_path": "example_a",
                  "units": 1,
                  "generation_flag": "example:a",
                  "tag_directory": "example_as",
                  "model_texture": "minecraft:item/stone",
                  "implied_prefixes": ["example:b"]
                }
                """);
        Files.writeString(directory.resolve("b.json"), """
                {
                  "id": "example:b",
                  "serialized_path": "example_b",
                  "units": 1,
                  "generation_flag": "example:b",
                  "tag_directory": "example_bs",
                  "model_texture": "minecraft:item/stone",
                  "implied_prefixes": ["dust"]
                }
                """);
        MaterialPrefixTestFixture.reset();
        try {
            MaterialPrefixCatalog.bootstrap(directory);
            JsonObject json = JsonParser.parseString("""
                    {
                      "id": "transitive",
                      "include_prefixes": ["example:a"],
                      "exclude_prefixes": ["dust"],
                      "thermal": {
                        "melting_point": 1000,
                        "boiling_point": 2000,
                        "density": 5
                      }
                    }
                    """).getAsJsonObject();
            MaterialDefinition material = MaterialDefinition.CODEC.parse(
                            JsonOps.INSTANCE, json)
                    .getOrThrow();
            assertEquals(
                    List.of(
                            new MaterialPrefix("example:a"),
                            new MaterialPrefix("example:b")),
                    material.forms());
        } finally {
            MaterialPrefixTestFixture.reset();
            MaterialPrefixTestFixture.bootstrapBuiltins();
        }
    }

    @Test
    void rejectsUnknownAndCyclicPrefixImplications() {
        MaterialPrefix first = new MaterialPrefix("example:first");
        MaterialPrefix second = new MaterialPrefix("example:second");
        assertThrows(IllegalArgumentException.class, () ->
                MaterialPrefixCatalog.validateDefinitions(List.of(
                        definition(first, "first", List.of(), List.of("missing")))));
        IllegalArgumentException cycle = assertThrows(
                IllegalArgumentException.class,
                () -> MaterialPrefixCatalog.validateDefinitions(List.of(
                        definition(first, "first", List.of(), List.of("second")),
                        definition(second, "second", List.of(), List.of("first")))));
        assertTrue(cycle.getMessage().contains("cycle"));
    }

    @Test
    void explicitlyAllowsFormlessMetadataMaterials() {
        JsonObject json = JsonParser.parseString("""
                {
                  "id": "oxygen",
                  "include_prefixes": [],
                  "metadata_only": true,
                  "thermal": {
                    "melting_point": -218.79,
                    "boiling_point": -182.95,
                    "density": 0.001429
                  }
                }
                """).getAsJsonObject();
        MaterialDefinition material = MaterialDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow();

        assertTrue(material.forms().isEmpty());
        assertTrue(material.formItems().isEmpty());
        assertTrue(material.metadataOnly());
        JsonObject encoded = MaterialDefinition.CODEC.encodeStart(JsonOps.INSTANCE, material)
                .getOrThrow().getAsJsonObject();
        assertTrue(encoded.get("metadata_only").getAsBoolean());
    }

    @Test
    void rejectsMetadataOnlyMaterialsWithItemStructure() {
        JsonObject json = JsonParser.parseString("""
                {
                  "id": "invalid_metadata",
                  "include_prefixes": ["dust"],
                  "metadata_only": true,
                  "thermal": {
                    "melting_point": 0,
                    "boiling_point": 100,
                    "density": 1
                  }
                }
                """).getAsJsonObject();
        assertThrows(IllegalArgumentException.class, () ->
                MaterialDefinition.CODEC.parse(JsonOps.INSTANCE, json).getOrThrow());
    }

    @Test
    void acceptsLegacyFormsAndAliases() {
        assertEquals(
                MaterialPrefixes.ROD,
                MaterialPrefix.CODEC.parse(JsonOps.INSTANCE, new JsonPrimitive("stick"))
                        .getOrThrow());
        JsonObject json = JsonParser.parseString("""
                {
                  "id": "legacy",
                  "forms": ["ingot", "stick", "tiny_dust"],
                  "form_items": {"stick": "minecraft:stick"},
                  "thermal": {
                    "melting_point": 1000,
                    "boiling_point": 2000,
                    "density": 5
                  }
                }
                """).getAsJsonObject();
        MaterialDefinition material = MaterialDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow();

        assertEquals(
                List.of(MaterialPrefixes.INGOT, MaterialPrefixes.ROD, MaterialPrefixes.TINY_DUST),
                material.forms());
        assertEquals("minecraft:stick", material.formItems().get(MaterialPrefixes.ROD));
    }

    @Test
    void materialCodecKeepsLegacyDefaultsAndRoundTripsGt6Metadata() {
        JsonObject json = JsonParser.parseString("""
                {
                  "id": "imported",
                  "forms": ["dust"],
                  "thermal": {
                    "melting_point": 1083.85,
                    "boiling_point": 2561.85,
                    "density": 8.96
                  },
                  "gt6_metadata": {
                    "source_id": 290,
                    "source_name": "Copper",
                    "aliases": ["AnyCopper"],
                    "state": "solid",
                    "formula": "Cu",
                    "source_thermal": {
                      "melting_point_kelvin": 1357,
                      "melting_point_celsius": 1083.85,
                      "boiling_point_kelvin": 2835,
                      "boiling_point_celsius": 2561.85,
                      "plasma_point_kelvin": 283500,
                      "plasma_point_celsius": 283226.85,
                      "density": 8.96
                    },
                    "tool": {
                      "durability": 64,
                      "speed": 4,
                      "quality": 0,
                      "types": 2
                    },
                    "processing_targets": {
                      "smelting": {
                        "material": "Copper",
                        "source_material_id": 290,
                        "source_material_name": "Copper",
                        "numerator_u": 648648000,
                        "cc_units": 144
                      }
                    },
                    "material_tags": ["ATOMIC.ELEMENT"],
                    "generation_tags": ["ITEMGENERATOR.DUSTS"]
                  }
                }
                """).getAsJsonObject();

        MaterialDefinition material = MaterialDefinition.CODEC.parse(JsonOps.INSTANCE, json)
                .getOrThrow();
        assertEquals(0, material.tier());
        assertFalse(material.moltenFluid());
        assertEquals(290, material.gt6Metadata().orElseThrow().sourceId());
        assertEquals(
                283500.0,
                material.gt6Metadata().orElseThrow().sourceThermal().plasmaPointKelvin());
        assertEquals(
                144L,
                material.gt6Metadata().orElseThrow()
                        .processingTargets().get("smelting").ccUnits().orElseThrow());

        MaterialDefinition roundTripped = MaterialDefinition.CODEC.parse(
                        JsonOps.INSTANCE,
                        MaterialDefinition.CODEC.encodeStart(JsonOps.INSTANCE, material)
                                .getOrThrow())
                .getOrThrow();
        assertEquals("Copper", roundTripped.gt6Metadata().orElseThrow().sourceName());
        assertEquals(
                283226.85,
                roundTripped.gt6Metadata().orElseThrow()
                        .sourceThermal().plasmaPointCelsius());
    }

    @Test
    void prefixTableChangesStructuralFingerprint() {
        List<MaterialPrefixDefinition> changed =
                new ArrayList<>(MaterialPrefixCatalog.definitions());
        int ingotIndex = changed.indexOf(
                MaterialPrefixCatalog.definition(MaterialPrefixes.INGOT));
        MaterialPrefixDefinition ingot = changed.get(ingotIndex);
        changed.set(ingotIndex, new MaterialPrefixDefinition(
                ingot.prefix(),
                ingot.serializedPath(),
                ingot.units() + 1,
                ingot.generationFlag(),
                ingot.tagDirectory(),
                ingot.tagNamespace(),
                ingot.modelTemplate(),
                ingot.modelTexture(),
                ingot.aliases(),
                ingot.capabilities()));

        assertNotEquals(
                MaterialFingerprint.structure(List.of()),
                MaterialFingerprint.structure(List.of(), changed));
    }

    private static MaterialPrefixDefinition definition(
            MaterialPrefix prefix,
            String path,
            List<String> aliases) {
        return definition(prefix, path, aliases, List.of());
    }

    private static MaterialPrefixDefinition definition(
            MaterialPrefix prefix,
            String path,
            List<String> aliases,
            List<String> impliedPrefixes) {
        return new MaterialPrefixDefinition(
                prefix,
                path,
                1,
                "example:flag_" + path,
                path + "s",
                "example",
                "minecraft:item/generated",
                "minecraft:item/stone",
                aliases,
                impliedPrefixes,
                Set.of("example:test"));
    }
}
