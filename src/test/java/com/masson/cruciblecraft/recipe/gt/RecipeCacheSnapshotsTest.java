package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class RecipeCacheSnapshotsTest {
    @Test
    void cacheDecisionDistinguishesInventoryAndMapChanges() {
        var unchanged = RecipeCacheRules.decide(true, true, 4L, 4L, true);
        assertFalse(unchanged.rebuildQuery());
        assertFalse(unchanged.invalidateResult());
        assertFalse(unchanged.search());

        var inventoryChanged = RecipeCacheRules.decide(true, false, 4L, 4L, true);
        assertTrue(inventoryChanged.rebuildQuery());
        assertTrue(inventoryChanged.invalidateResult());
        assertTrue(inventoryChanged.search());

        var mapReloaded = RecipeCacheRules.decide(true, true, 4L, 5L, true);
        assertFalse(mapReloaded.rebuildQuery());
        assertTrue(mapReloaded.invalidateResult());
        assertTrue(mapReloaded.search());
    }

    @Test
    void unchangedOrderedSnapshotsAreReused() {
        List<Snapshot> cached = List.of(
                new Snapshot("copper", 2),
                new Snapshot("mold", 1));
        List<Snapshot> offered = List.of(
                new Snapshot("copper", 2),
                new Snapshot("mold", 1));

        assertTrue(RecipeCacheSnapshots.same(cached, offered, Snapshot::same));
    }

    @Test
    void sizeCountComponentAndSlotChangesInvalidate() {
        List<Snapshot> cached = List.of(
                new Snapshot("copper", 2),
                new Snapshot("mold", 1));

        assertFalse(RecipeCacheSnapshots.same(
                cached,
                List.of(new Snapshot("copper", 2)),
                Snapshot::same));
        assertFalse(RecipeCacheSnapshots.same(
                cached,
                List.of(new Snapshot("copper", 1), new Snapshot("mold", 1)),
                Snapshot::same));
        assertFalse(RecipeCacheSnapshots.same(
                cached,
                List.of(new Snapshot("tin", 2), new Snapshot("mold", 1)),
                Snapshot::same));
        assertFalse(RecipeCacheSnapshots.same(
                cached,
                List.of(new Snapshot("mold", 1), new Snapshot("copper", 2)),
                Snapshot::same));
    }

    private record Snapshot(String identityAndComponents, int amount) {
        private boolean same(Snapshot other) {
            return identityAndComponents.equals(other.identityAndComponents)
                    && amount == other.amount;
        }
    }
}
