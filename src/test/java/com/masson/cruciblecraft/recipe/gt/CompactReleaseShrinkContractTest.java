package com.masson.cruciblecraft.recipe.gt;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The shipped jar drops matrix evidence hashes, the row evidence index, and
 * relation evidence_hashes (gradle/scripts/release-resources.gradle). Runtime
 * decoding must not see a difference.
 */
class CompactReleaseShrinkContractTest {
    private static final String DEV_MATRIX = """
            {
              "shared": {
                "duration": 3913,
                "eut": 96,
                "source_kind": "SOURCE_DERIVED",
                "selected_source_recipe": "gt.recipe.extruder#bulk_3408"
              },
              "dicts": {
                "fluids": [{"fluid_inputs": [], "fluid_outputs": []}],
                "hashes": [
                  "bcb831ed259609bd26263bc797b4b387c8a6dcd44fbd74ce24b868f526add6eb",
                  "1afec6aee861a72b603aca36841111465eec93d9622762b8ebe956223551f6a3"
                ],
                "item_inputs": [[]],
                "item_outputs": [[]]
              },
              "rows": [
                [0, 0, 0, "cruciblecraft:gt6/447d39da135360b1", 0, 0],
                [0, 0, 0, "cruciblecraft:gt6/550fa3f501ffd8ec", 1, 3]
              ]
            }
            """;
    private static final String SHIPPED_MATRIX = """
            {"shared":{"duration":3913,"eut":96,"source_kind":"SOURCE_DERIVED",\
            "selected_source_recipe":"gt.recipe.extruder#bulk_3408"},\
            "dicts":{"fluids":[{"fluid_inputs":[],"fluid_outputs":[]}],\
            "item_inputs":[[]],"item_outputs":[[]]},\
            "rows":[[0,0,0,"cruciblecraft:gt6/447d39da135360b1",0],\
            [0,0,0,"cruciblecraft:gt6/550fa3f501ffd8ec",3]]}
            """;
    private static final String DEV_PROVENANCE = """
            {"source_kind": "SOURCE_DERIVED",
             "selected_source_recipe": "gt.recipe.mixer#12",
             "evidence_hashes": ["9057f92b871ca302cb980f0e0152e6128847c5e41f303a90867c7e0312c6895c"]}
            """;
    private static final String SHIPPED_PROVENANCE = """
            {"source_kind":"SOURCE_DERIVED","selected_source_recipe":"gt.recipe.mixer#12"}
            """;

    @BeforeAll
    static void bootstrapMinecraft() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void shippedMatrixDecodesLikeDevelopmentMatrix() {
        CompactGTRecipeFamilyDefinition.AuthoredMatrixV1 dev =
                decode(CompactGTRecipeFamilyDefinition.AuthoredMatrixV1.CODEC, DEV_MATRIX);
        CompactGTRecipeFamilyDefinition.AuthoredMatrixV1 shipped =
                decode(CompactGTRecipeFamilyDefinition.AuthoredMatrixV1.CODEC, SHIPPED_MATRIX);
        assertEquals(dev, shipped);
        assertEquals(3, shipped.rows().get(1).shadowOrder());
    }

    @Test
    void shippedProvenanceDecodesLikeDevelopmentProvenance() {
        assertEquals(
                decode(GTRecipeProvenance.CODEC, DEV_PROVENANCE),
                decode(GTRecipeProvenance.CODEC, SHIPPED_PROVENANCE));
    }

    private static <T> T decode(Codec<T> codec, String json) {
        JsonElement element = JsonParser.parseString(json);
        return codec.parse(JsonOps.INSTANCE, element).getOrThrow();
    }
}
