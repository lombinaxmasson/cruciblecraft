package com.masson.cruciblecraft.localization;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.network.chat.contents.TranslatableContents;

import org.junit.jupiter.api.Test;

/** GT6 {@code LanguageHandler} / {@code OreDictListenerItem_Rocks} rock names. */
class RockFormNamesTest {
    @Test
    void specialsMatchLanguageHandler() {
        assertEquals(
                "item.cruciblecraft.rock.stone",
                key(RockFormNames.specialName("stone")));
        assertEquals(
                "item.cruciblecraft.rock.netherrack",
                key(RockFormNames.specialName("netherrack")));
        assertEquals(
                "item.cruciblecraft.rock.endstone",
                key(RockFormNames.specialName("endstone")));
        assertEquals(
                "item.cruciblecraft.rock.meteorite",
                key(RockFormNames.specialName("meteorite")));
        assertEquals(
                "item.cruciblecraft.rock.meteorite",
                key(RockFormNames.specialName("meteoric_iron")));
        assertNull(RockFormNames.specialName("granite_black"));
        assertNull(RockFormNames.specialName("iron"));
    }

    @Test
    void indicatesOccurrenceMatchesOreDictListener() {
        assertTrue(RockFormNames.indicatesOccurrence("stone"));
        assertTrue(RockFormNames.indicatesOccurrence("iron"));
        assertTrue(RockFormNames.indicatesOccurrence("granite_black"));
        assertFalse(RockFormNames.indicatesOccurrence("meteorite"));
        assertFalse(RockFormNames.indicatesOccurrence("meteoric_iron"));
        assertFalse(RockFormNames.indicatesOccurrence("ancient_debris"));
        assertFalse(RockFormNames.indicatesOccurrence("obsidian"));
        assertFalse(RockFormNames.indicatesOccurrence("ambrosium"));
        assertFalse(RockFormNames.indicatesOccurrence("glowstone"));
    }

    private static String key(net.minecraft.network.chat.Component name) {
        return ((TranslatableContents) name.getContents()).getKey();
    }
}
