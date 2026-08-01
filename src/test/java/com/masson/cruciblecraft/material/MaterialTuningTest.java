package com.masson.cruciblecraft.material;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.masson.cruciblecraft.material.def.MaterialTuning;
import com.masson.cruciblecraft.material.def.ThermalProperties;
import com.mojang.serialization.JsonOps;
import org.junit.jupiter.api.Test;

class MaterialTuningTest {
    @Test
    void tuningPreservesGenerationSelectionMetadata() {
        MaterialDefinition base = MaterialDefinition.CODEC.parse(
                        JsonOps.INSTANCE,
                        JsonParser.parseString("""
                                {
                                  "id": "metadata",
                                  "generation_flags": ["cruciblecraft:generates_ingot"],
                                  "include_prefixes": ["tiny_dust"],
                                  "exclude_prefixes": ["dust"],
                                  "thermal": {
                                    "melting_point": 100,
                                    "boiling_point": 300,
                                    "density": 2
                                  }
                                }
                                """))
                .getOrThrow();

        MaterialDefinition tuned = new MaterialTuning(
                "metadata",
                Optional.of(2),
                Optional.empty(),
                Optional.empty(),
                Optional.empty(),
                Optional.empty()).apply(base);

        assertEquals(base.generationFlagIds(), tuned.generationFlagIds());
        assertEquals(base.includedPrefixIds(), tuned.includedPrefixIds());
        assertEquals(base.excludedPrefixIds(), tuned.excludedPrefixIds());
        assertEquals(base.generationFlags(), tuned.generationFlags());
        assertEquals(base.forms(), tuned.forms());
    }

    @Test
    void tuningCannotChangeRegistryShape() {
        MaterialDefinition base = new MaterialDefinition(
                "test",
                "test",
                Optional.empty(),
                1,
                "#112233",
                "metallic",
                List.of(MaterialPrefixes.INGOT, MaterialPrefixes.PLATE),
                Map.of(MaterialPrefixes.INGOT, "minecraft:iron_ingot"),
                new ThermalProperties(100.0, 300.0, 2.0),
                true,
                Map.of("iron", 1),
                false);
        MaterialDefinition tuned = new MaterialTuning(
                "test",
                Optional.of(2),
                Optional.of("#AABBCC"),
                Optional.of(120.0),
                Optional.empty(),
                Optional.of(2.5))
                .apply(base);

        assertEquals(base.id(), tuned.id());
        assertEquals(base.forms(), tuned.forms());
        assertEquals(base.formItems(), tuned.formItems());
        assertEquals(base.moltenFluid(), tuned.moltenFluid());
        assertEquals(base.composition(), tuned.composition());
        assertEquals(2, tuned.tier());
        assertEquals("#AABBCC", tuned.color());
        assertEquals(120.0, tuned.thermal().meltingPoint());
        assertEquals(300.0, tuned.thermal().boilingPoint());
        assertEquals(2.5, tuned.thermal().density());
    }
}
