package com.masson.cruciblecraft.material.prefix;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.material.MaterialPrefix;

/** Pins the 4.5 card F rock (GT6 rockGt) prefix definition. */
class RockPrefixDefinitionTest {
    private static final MaterialPrefix ROCK =
            MaterialPrefixCatalog.require("rock");

    @Test
    void definitionMatchesTheGt6RockGtPort() {
        var definition = MaterialPrefixCatalog.definition(ROCK);
        // GT6 OP.rockGt setMaterialStats(9 * U4); U4 = 36 units.
        assertEquals(324, definition.units());
        assertEquals("rocks", definition.tagDirectory());
        assertEquals("c", definition.tagNamespace());
        assertEquals(
                "cruciblecraft:generates_rock",
                definition.generationFlag());
        assertTrue(
                definition.aliases().contains("rockgt"),
                "the GT6 source identity rockGt must stay an alias");
        assertEquals("rock", definition.serializedPath());
    }
}
