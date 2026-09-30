package com.masson.cruciblecraft.gametest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.junit.jupiter.api.Test;

/**
 * The live loom map already has a sibling family. The pilot GameTest may
 * lock only that family's policy, so the sibling does not fail it.
 */
class PublicationFamilyIsolationTest {
    @Test
    void loomPilotTestIgnoresTheChemicalMiscSibling() throws Exception {
        JsonObject pilot = policy("loom.json");
        JsonObject sibling = policy("loom_chemical_misc.json");
        assertNotEquals(
                pilot.get("publication_group").getAsString(),
                sibling.get("publication_group").getAsString());
        assertTrue(pilot.get("relation_count").getAsInt() > 0);
        assertTrue(sibling.get("relation_count").getAsInt() > 0);
        assertNotEquals(
                pilot.get("relation_count").getAsInt(),
                sibling.get("relation_count").getAsInt());

        String source = Files.readString(Path.of(
                "src/test/java/com/masson/cruciblecraft/gametest/LoomGameTests.java"));
        assertTrue(source.contains("loom/pilot/loom"));
        assertTrue(source.contains("PublicationPolicyCounts.relationCount"));
        assertFalse(source.contains("loom/chemical_misc"));
        assertFalse(source.contains("entries().size()"));
    }

    private static JsonObject policy(String file) throws Exception {
        return JsonParser.parseString(Files.readString(Path.of(
                "src/recipe_generated/resources/data/cruciblecraft/recipe/"
                        + "publication_policy/" + file))).getAsJsonObject();
    }
}
