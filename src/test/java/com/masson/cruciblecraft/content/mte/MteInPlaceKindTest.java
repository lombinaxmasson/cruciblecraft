package com.masson.cruciblecraft.content.mte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MteInPlaceKindTest {
    @Test
    void barrelIsMassStorageNotAChest() {
        assertTrue(MteInPlaceKind.BARREL.massStorage());
        assertTrue(MteInPlaceKind.MASS_STORAGE.massStorage());
        assertEquals(1, MteInPlaceKind.BARREL.slots());
        assertEquals(1, MteInPlaceKind.MASS_STORAGE.slots());
        assertTrue(MteInPlaceKind.BARREL.storageTab());
        assertFalse(MteInPlaceKind.BARREL.playerInventoryGui());
        assertFalse(MteInPlaceKind.CHEST.massStorage());
        assertEquals(54, MteInPlaceKind.CHEST.slots());
    }
}
