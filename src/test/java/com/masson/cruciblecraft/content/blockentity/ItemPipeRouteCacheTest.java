package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import net.minecraft.SharedConstants;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.neoforged.fml.loading.LoadingModList;

class ItemPipeRouteCacheTest {
    @BeforeAll
    static void bootstrapMinecraft() {
        LoadingModList.of(
                List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
    }

    @Test
    void twoHundredFiftySeventhKeyEvictsEldestButKeepsHotKey() {
        ItemPipeRouteCache cache = new ItemPipeRouteCache();
        AtomicInteger discoveries = new AtomicInteger();
        List<Item> items = new ArrayList<>(
                BuiltInRegistries.ITEM.stream()
                        .limit(ItemPipeRouteCache.MAX_ENTRIES + 1L)
                        .toList());
        assertEquals(
                ItemPipeRouteCache.MAX_ENTRIES + 1,
                items.size());

        for (int index = 0;
                index < ItemPipeRouteCache.MAX_ENTRIES;
                index++) {
            discover(cache, Direction.WEST, items.get(index), discoveries);
        }
        assertEquals(ItemPipeRouteCache.MAX_ENTRIES, cache.size());
        assertEquals(ItemPipeRouteCache.MAX_ENTRIES, discoveries.get());

        discover(cache, Direction.WEST, items.getFirst(), discoveries);
        discover(cache, Direction.WEST, items.getLast(), discoveries);
        assertEquals(ItemPipeRouteCache.MAX_ENTRIES, cache.size());
        assertEquals(ItemPipeRouteCache.MAX_ENTRIES + 1, discoveries.get());

        discover(cache, Direction.WEST, items.get(1), discoveries);
        assertEquals(ItemPipeRouteCache.MAX_ENTRIES + 2, discoveries.get());
        discover(cache, Direction.WEST, items.getFirst(), discoveries);
        assertEquals(ItemPipeRouteCache.MAX_ENTRIES + 2, discoveries.get());
    }

    @Test
    void ingressDirectionParticipatesInIdentityAndClearRediscovers() {
        ItemPipeRouteCache cache = new ItemPipeRouteCache();
        AtomicInteger discoveries = new AtomicInteger();
        Item item = Items.IRON_INGOT;

        discover(cache, Direction.WEST, item, discoveries);
        discover(cache, Direction.EAST, item, discoveries);
        discover(cache, Direction.WEST, item, discoveries);
        assertEquals(2, cache.size());
        assertEquals(2, discoveries.get());

        cache.clear();
        assertEquals(0, cache.size());
        discover(cache, Direction.WEST, item, discoveries);
        assertEquals(1, cache.size());
        assertEquals(3, discoveries.get());
    }

    private static void discover(
            ItemPipeRouteCache cache,
            Direction ingress,
            Item item,
            AtomicInteger discoveries) {
        cache.getOrDiscover(ingress, item, () -> {
            discoveries.incrementAndGet();
            return List.of();
        });
    }
}
