package com.masson.cruciblecraft.recipe;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.recipe.AnvilStrikeContext.Facing;
import com.masson.cruciblecraft.recipe.AnvilStrikeContext.HitFace;

class AnvilStrikeContextTest {
    @Test
    void topAlwaysRoutesToNormalAnvilMode() {
        for (Facing facing : Facing.values()) {
            assertEquals(
                    AnvilMode.ANVIL,
                    context(facing, HitFace.UP, 0.25, 0.75).mode().orElseThrow());
        }
    }

    @Test
    void portsAllFourGt6HorizontalRoutingTables() {
        Map<Facing, Map<HitFace, AnvilMode>> expectedAtQuarter = Map.of(
                Facing.WEST, Map.of(
                        HitFace.WEST, AnvilMode.BEND_SMALL,
                        HitFace.EAST, AnvilMode.BEND_SMALL,
                        HitFace.NORTH, AnvilMode.BEND_SMALL,
                        HitFace.SOUTH, AnvilMode.BEND_BIG),
                Facing.EAST, Map.of(
                        HitFace.WEST, AnvilMode.BEND_BIG,
                        HitFace.EAST, AnvilMode.BEND_BIG,
                        HitFace.NORTH, AnvilMode.BEND_BIG,
                        HitFace.SOUTH, AnvilMode.BEND_SMALL),
                Facing.NORTH, Map.of(
                        HitFace.WEST, AnvilMode.BEND_SMALL,
                        HitFace.EAST, AnvilMode.BEND_BIG,
                        HitFace.NORTH, AnvilMode.BEND_SMALL,
                        HitFace.SOUTH, AnvilMode.BEND_SMALL),
                Facing.SOUTH, Map.of(
                        HitFace.WEST, AnvilMode.BEND_BIG,
                        HitFace.EAST, AnvilMode.BEND_SMALL,
                        HitFace.NORTH, AnvilMode.BEND_BIG,
                        HitFace.SOUTH, AnvilMode.BEND_BIG));

        expectedAtQuarter.forEach((facing, faces) -> faces.forEach((face, mode) ->
                assertEquals(mode, context(facing, face, 0.25, 0.25).mode().orElseThrow())));

        assertEquals(AnvilMode.BEND_BIG, context(Facing.WEST, HitFace.WEST, 0.75, 0.75).mode().orElseThrow());
        assertEquals(AnvilMode.BEND_SMALL, context(Facing.EAST, HitFace.EAST, 0.75, 0.75).mode().orElseThrow());
        assertEquals(AnvilMode.BEND_BIG, context(Facing.NORTH, HitFace.NORTH, 0.75, 0.75).mode().orElseThrow());
        assertEquals(AnvilMode.BEND_SMALL, context(Facing.SOUTH, HitFace.SOUTH, 0.75, 0.75).mode().orElseThrow());
    }

    @Test
    void selectsTopSlotsAlongTheFacingAxis() {
        assertEquals(0, context(Facing.NORTH, HitFace.UP, 0.25, 0.75).topSlot());
        assertEquals(1, context(Facing.SOUTH, HitFace.UP, 0.75, 0.25).topSlot());
        assertEquals(0, context(Facing.WEST, HitFace.UP, 0.75, 0.25).topSlot());
        assertEquals(1, context(Facing.EAST, HitFace.UP, 0.25, 0.75).topSlot());
    }

    private static AnvilStrikeContext context(
            Facing facing,
            HitFace face,
            double x,
            double z) {
        return new AnvilStrikeContext(facing, face, x, z);
    }
}
