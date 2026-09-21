package com.masson.cruciblecraft.content.mte;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

/**
 * Locks GT6 {@code allowCovers} for furniture / drive MTE kinds.
 */
class MteInPlaceKindCoverTest {
    @Test
    void chestsAndLockersAcceptEveryFace() {
        assertTrue(MteInPlaceKind.CHEST.allowCover(Direction.NORTH, Direction.NORTH));
        assertTrue(MteInPlaceKind.LOCKER.allowCover(Direction.SOUTH, Direction.UP));
        assertTrue(MteInPlaceKind.MASS_STORAGE.allowCover(Direction.EAST, Direction.WEST));
    }

    @Test
    void safesAndDrawersSkipTheFront() {
        assertFalse(MteInPlaceKind.SAFE.allowCover(Direction.NORTH, Direction.NORTH));
        assertTrue(MteInPlaceKind.SAFE.allowCover(Direction.NORTH, Direction.SOUTH));
        assertFalse(MteInPlaceKind.DRAWER.allowCover(Direction.WEST, Direction.WEST));
        assertTrue(MteInPlaceKind.DRAWER.allowCover(Direction.WEST, Direction.UP));
    }

    @Test
    void bookshelvesSkipTheFacingAxis() {
        assertFalse(MteInPlaceKind.BOOKSHELF.allowCover(Direction.NORTH, Direction.NORTH));
        assertFalse(MteInPlaceKind.BOOKSHELF.allowCover(Direction.NORTH, Direction.SOUTH));
        assertTrue(MteInPlaceKind.BOOKSHELF.allowCover(Direction.NORTH, Direction.WEST));
        assertTrue(MteInPlaceKind.BOOKSHELF.allowCover(Direction.NORTH, Direction.UP));
    }

    @Test
    void craftingTablesSkipTopAndFacingAxis() {
        assertFalse(MteInPlaceKind.CRAFTING_TABLE.allowCover(
                Direction.NORTH, Direction.UP));
        assertFalse(MteInPlaceKind.CRAFTING_TABLE.allowCover(
                Direction.NORTH, Direction.NORTH));
        assertTrue(MteInPlaceKind.CRAFTING_TABLE.allowCover(
                Direction.NORTH, Direction.EAST));
        assertTrue(MteInPlaceKind.CRAFTING_TABLE.allowCover(
                Direction.NORTH, Direction.DOWN));
    }

    @Test
    void bottleCratesAndAttachmentsRefuseCovers() {
        assertFalse(MteInPlaceKind.BOTTLE_CRATE.allowCover(
                Direction.NORTH, Direction.UP));
        assertFalse(MteInPlaceKind.FAUCET.allowCover(Direction.NORTH, Direction.NORTH));
        assertFalse(MteInPlaceKind.ROPE.allowCover(Direction.UP, Direction.NORTH));
    }

    @Test
    void steamTurbinesKeepDefaultTrue() {
        assertTrue(MteInPlaceKind.STEAM_TURBINE.allowCover(
                Direction.NORTH, Direction.NORTH));
        assertTrue(MteInPlaceKind.AXLE.allowCover(Direction.UP, Direction.DOWN));
    }
}
