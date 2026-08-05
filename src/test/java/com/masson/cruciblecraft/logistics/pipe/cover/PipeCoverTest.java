package com.masson.cruciblecraft.logistics.pipe.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Optional;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.Test;

class PipeCoverTest {
    @Test
    void threeCoverKindsHaveStableIds() {
        assertEquals(
                PipeCoverType.FILTER,
                PipeCoverType.decode("filter"));
        assertEquals(
                PipeCoverType.ONE_WAY_VALVE,
                PipeCoverType.decode("one_way_valve"));
        assertEquals(
                PipeCoverType.OUTPUT_PUMP,
                PipeCoverType.decode("output_pump"));
    }

    @Test
    void onlyFilterMayCarryExactRegistryMatch() {
        PipeCover filter = PipeCover.filter("minecraft:iron_ingot");
        assertTrue(filter.matchId().isPresent());
        assertThrows(
                IllegalArgumentException.class,
                () -> new PipeCover(
                        PipeCoverType.OUTPUT_PUMP,
                        Optional.of("minecraft:iron_ingot")));
    }

    @Test
    void sixFaceStatePersistsAndValveDirectionIsStable() {
        PipeCoverSet original = new PipeCoverSet();
        assertTrue(original.set(
                Direction.NORTH,
                PipeCover.filter("minecraft:iron_ingot")));
        assertTrue(original.set(
                Direction.SOUTH, PipeCover.valve()));
        assertTrue(original.set(
                Direction.UP, PipeCover.pump()));
        assertFalse(original.allowsIncoming(Direction.SOUTH));

        CompoundTag tag = new CompoundTag();
        original.save(tag, null);
        PipeCoverSet restored = new PipeCoverSet();
        restored.load(tag, null);

        assertEquals(original.snapshot(), restored.snapshot());
        assertEquals(3, restored.snapshot().size());
    }

    @Test
    void malformedRowsAreSkippedWithoutDiscardingValidCovers() {
        ListTag rows = new ListTag();
        rows.add(coverRow(
                "north", "filter", "minecraft:iron_ingot"));
        rows.add(coverRow(
                "south", "filter", "NOT A VALID ID"));
        rows.add(coverRow(
                "east", "output_pump", "minecraft:iron_ingot"));
        CompoundTag tag = new CompoundTag();
        tag.put("covers", rows);

        PipeCoverSet restored = new PipeCoverSet();
        assertEquals(2, restored.load(tag, null));
        assertEquals(
                Map.of(
                        Direction.NORTH,
                        PipeCover.filter("minecraft:iron_ingot")),
                restored.snapshot());
    }

    private static CompoundTag coverRow(
            String side, String type, String match) {
        CompoundTag row = new CompoundTag();
        row.putString("side", side);
        row.putString("type", type);
        row.putString("match", match);
        return row;
    }
}
