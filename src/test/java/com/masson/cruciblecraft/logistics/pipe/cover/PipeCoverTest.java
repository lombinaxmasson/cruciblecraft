package com.masson.cruciblecraft.logistics.pipe.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertTrue(original.allowsIncoming(Direction.SOUTH));

        CompoundTag tag = new CompoundTag();
        original.save(tag, null);
        ListTag persisted = tag.getList("covers", 10);
        for (int index = 0; index < persisted.size(); index++) {
            assertTrue(persisted.getCompound(index).contains("definition"));
            assertFalse(persisted.getCompound(index).contains("type"));
        }
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
                PipeCover.filter("minecraft:iron_ingot"),
                restored.get(Direction.NORTH).orElseThrow());
        assertEquals(3, restored.snapshot().size());
        assertFalse(restored.allowsIncoming(Direction.SOUTH));
        assertFalse(restored.allowsIncoming(Direction.EAST));
    }

    @Test
    void unknownDefinitionsAndIdentityConflictsFailClosed() {
        ListTag rows = new ListTag();
        CompoundTag unknown = new CompoundTag();
        unknown.putString("side", "north");
        unknown.putString("definition", "example:missing");
        rows.add(unknown);
        CompoundTag conflict = new CompoundTag();
        conflict.putString("side", "south");
        conflict.putString("definition", "cruciblecraft:filter");
        conflict.putString("type", "filter");
        rows.add(conflict);
        CompoundTag tag = new CompoundTag();
        tag.put("covers", rows);

        PipeCoverSet restored = new PipeCoverSet();
        assertEquals(2, restored.load(tag, null));
        assertFalse(restored.allowsIncoming(Direction.NORTH));
        assertFalse(restored.allowsOutgoing(
                Direction.SOUTH,
                CoverDefinition.Medium.ITEM,
                0,
                0));
    }

    @Test
    void legacyTypeLoadsButNextSaveWritesOnlyDefinitionId() {
        ListTag legacyRows = new ListTag();
        legacyRows.add(coverRow(
                "north", "filter", "minecraft:iron_ingot"));
        CompoundTag legacy = new CompoundTag();
        legacy.put("covers", legacyRows);
        PipeCoverSet restored = new PipeCoverSet();
        assertEquals(0, restored.load(legacy, null));

        CompoundTag migrated = new CompoundTag();
        restored.save(migrated, null);
        CompoundTag row = migrated.getList(
                "covers", 10).getCompound(0);
        assertEquals(
                "cruciblecraft:filter",
                row.getString("definition"));
        assertFalse(row.contains("type"));
    }

    @Test
    void networkIdPersistsAndForbiddenKindsFailClosed() {
        CoverBehaviorRegistry.validateDefinitions();
        PipeCoverSet original = new PipeCoverSet();
        PipeCover storage = PipeCover.of(
                "cruciblecraft:logistics_item_storage")
                .configure(
                        CoverDefinition.ConfigField.NETWORK_ID,
                        3);
        assertTrue(original.set(Direction.WEST, storage));
        CompoundTag tag = new CompoundTag();
        original.save(tag, null);
        assertEquals(
                3,
                tag.getList("covers", 10).getCompound(0).getInt("network_id"));
        PipeCoverSet restored = new PipeCoverSet();
        restored.load(tag, null);
        assertEquals(
                3,
                restored.get(Direction.WEST).orElseThrow()
                        .config().networkId().orElse(0));

        PipeCover retriever = PipeCover.of("cruciblecraft:retriever_item")
                .withConfig(PipeCoverConfig.EMPTY.withInvert(1));
        assertTrue(original.set(Direction.NORTH, retriever));
        CompoundTag invertTag = new CompoundTag();
        original.save(invertTag, null);
        PipeCoverSet invertRestored = new PipeCoverSet();
        invertRestored.load(invertTag, null);
        assertEquals(
                1,
                invertRestored.get(Direction.NORTH).orElseThrow()
                        .config().invert().orElse(0));

        CompoundTag forbidden = new CompoundTag();
        forbidden.putString("side", "east");
        forbidden.putString(
                "definition", "cruciblecraft:logistics_battery");
        ListTag rows = new ListTag();
        rows.add(forbidden);
        CompoundTag bad = new CompoundTag();
        bad.put("covers", rows);
        PipeCoverSet quarantined = new PipeCoverSet();
        assertEquals(1, quarantined.load(bad, null));
        assertFalse(quarantined.allowsIncoming(Direction.EAST));
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
