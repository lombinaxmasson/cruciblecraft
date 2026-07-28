package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.Test;
import com.masson.cruciblecraft.api.material.MaterialForm;

class CrusherProgressTest {
    @Test void pausesWithoutResetWhenPowerOrOutputIsUnavailable() {
        assertEquals(42, CrusherProgress.advance(42, 128, false, true));
        assertEquals(42, CrusherProgress.advance(42, 128, true, false));
        assertEquals(43, CrusherProgress.advance(42, 128, true, true));
    }
    @Test void rejectsInvalidDurationAndCapsCompletion() {
        assertThrows(IllegalArgumentException.class, () -> CrusherProgress.advance(0, 0, true, true));
        assertEquals(128, CrusherProgress.advance(128, 128, true, true));
    }

    @Test void serializerModelRejectsUnsafeRecipeValues() {
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialForm.RAW_ORE, MaterialForm.CRUSHED_ORE, 0, 16, 128));
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialForm.RAW_ORE, MaterialForm.CRUSHED_ORE, 1, 0, 128));
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialForm.RAW_ORE, MaterialForm.CRUSHED_ORE, 1, 16, 0));
        assertThrows(IllegalArgumentException.class, () -> CrusherRecipeRules.validate(
                MaterialForm.RAW_ORE, MaterialForm.RAW_ORE, 1, 16, 128));
    }
}
