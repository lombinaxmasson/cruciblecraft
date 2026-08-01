package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RecipeProcessorTest {
    @Test
    void selectingNewRecipeResetsProgressButSameRecipeDoesNot() {
        RecipeProcessor processor = new RecipeProcessor();
        assertTrue(processor.select("crusher/ore", 3));
        processor.advance();
        assertFalse(processor.select("crusher/ore", 3));
        assertEquals(1, processor.progress());

        assertTrue(processor.select("crusher/dust", 4));
        assertEquals(0, processor.progress());
        assertEquals(4, processor.duration());
    }

    @Test
    void progressClampsAndCompletionCanKeepDisplayDuration() {
        RecipeProcessor processor = new RecipeProcessor();
        processor.select("coke/coal", 2);
        assertTrue(processor.advance());
        assertTrue(processor.advance());
        assertTrue(processor.complete());
        assertFalse(processor.advance());

        assertTrue(processor.clearActive(5));
        assertEquals("", processor.activeId());
        assertEquals(0, processor.progress());
        assertEquals(5, processor.duration());
    }

    @Test
    void restoreSanitizesPersistedBounds() {
        RecipeProcessor processor = new RecipeProcessor();
        processor.restore("test", 99, 20);
        assertEquals(20, processor.progress());
        processor.restore(null, -4, -1);
        assertEquals("", processor.activeId());
        assertEquals(0, processor.progress());
        assertEquals(0, processor.duration());
    }
}
