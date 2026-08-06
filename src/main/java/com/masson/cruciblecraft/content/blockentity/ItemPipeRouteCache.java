package com.masson.cruciblecraft.content.blockentity;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

import com.masson.cruciblecraft.logistics.pipe.item
        .ItemPipeNetworkTraversal.Route;

import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;

/** Bounded access-order cache for item-pipe route discovery. */
final class ItemPipeRouteCache {
    static final int MAX_ENTRIES = 256;

    private final Map<RouteKey, List<Route>> routes =
            new LinkedHashMap<>(MAX_ENTRIES, 0.75F, true) {
                @Override
                protected boolean removeEldestEntry(
                        Map.Entry<RouteKey, List<Route>> eldest) {
                    return size() > MAX_ENTRIES;
                }
            };

    List<Route> getOrDiscover(
            Direction ingress,
            Item item,
            Supplier<List<Route>> discover) {
        Objects.requireNonNull(ingress, "ingress");
        Objects.requireNonNull(item, "item");
        Objects.requireNonNull(discover, "discover");
        return routes.computeIfAbsent(
                new RouteKey(ingress, item),
                ignored -> Objects.requireNonNull(
                        discover.get(), "Discovered routes"));
    }

    void clear() {
        routes.clear();
    }

    int size() {
        return routes.size();
    }

    private record RouteKey(Direction ingress, Item item) {}
}
