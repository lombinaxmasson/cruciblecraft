package com.masson.cruciblecraft.logistics.pipe.item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.PipeTopology;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferDiagnostics;
import com.masson.cruciblecraft.logistics.pipe.item
        .ItemPipeNetworkTraversal.Route;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/** Simulated cached routes revalidated before actual item delivery. */
public record ItemPipeTransferPlan(
        ItemStack offered,
        int accepted,
        Map<BlockPos, Long> topologyVersions,
        List<Delivery> deliveries) {
    public ItemPipeTransferPlan {
        offered = offered.copy();
        topologyVersions = Map.copyOf(topologyVersions);
        deliveries = List.copyOf(deliveries);
        if (accepted < 0
                || accepted > offered.getCount()) {
            throw new IllegalArgumentException(
                    "Invalid item pipe transfer plan");
        }
    }

    public static ItemPipeTransferPlan plan(
            Level level,
            ItemStack offered,
            List<Route> routes,
            int ingressLimit) {
        if (offered.isEmpty() || ingressLimit <= 0) {
            return new ItemPipeTransferPlan(
                    offered,
                    0,
                    Map.of(),
                    List.of());
        }
        int remaining = Math.min(offered.getCount(), ingressLimit);
        ArrayList<Delivery> deliveries = new ArrayList<>();
        Map<BlockPos, Integer> reservedByPipe = new HashMap<>();
        for (Route route : routes) {
            if (remaining <= 0) {
                break;
            }
            int routeAvailable = route.pipePath().stream()
                    .map(level::getBlockEntity)
                    .filter(ItemPipeBlockEntity.class::isInstance)
                    .map(ItemPipeBlockEntity.class::cast)
                    .mapToInt(pipe -> Math.max(
                            0,
                            pipe.availableItems()
                                    - reservedByPipe.getOrDefault(
                                            pipe.getBlockPos(), 0)))
                    .min()
                    .orElse(0);
            int request = Math.min(remaining, routeAvailable);
            if (request <= 0 || !level.hasChunkAt(route.endpoint())) {
                continue;
            }
            IItemHandler endpoint;
            try {
                endpoint = level.getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        route.endpoint(),
                        route.side());
            } catch (RuntimeException failure) {
                PipeTransferDiagnostics.warnOnce(
                        "item endpoint simulation",
                        null,
                        "Item endpoint lookup failed at "
                                + route.endpoint(),
                        failure);
                continue;
            }
            if (endpoint == null) {
                continue;
            }
            int simulated = simulateInsert(
                    endpoint, offered.copyWithCount(request));
            if (simulated > 0) {
                deliveries.add(new Delivery(route, simulated));
                route.pipePath().forEach(position ->
                        reservedByPipe.merge(
                                position, simulated, Integer::sum));
                remaining -= simulated;
            }
        }
        Map<BlockPos, Long> topologyVersions = new HashMap<>();
        deliveries.forEach(delivery ->
                delivery.route().pipePath().forEach(position ->
                        topologyVersions.put(
                                position,
                                PipeTopology.version(level, position))));
        return new ItemPipeTransferPlan(
                offered,
                Math.min(offered.getCount(), ingressLimit) - remaining,
                topologyVersions,
                deliveries);
    }

    public Execution execute(Level level) {
        if (topologyVersions.entrySet().stream().anyMatch(
                entry -> PipeTopology.version(level, entry.getKey())
                        != entry.getValue())) {
            PipeTransferDiagnostics.warnOnce(
                    "item route execution",
                    null,
                    "Item pipe topology changed after simulation");
            return new Execution(0, 0);
        }
        int consumed = 0;
        int delivered = 0;
        for (Delivery delivery : deliveries) {
            Execution execution = delivery.execute(level, offered);
            consumed += execution.consumed();
            delivered += execution.delivered();
            for (BlockPos position : delivery.route().pipePath()) {
                if (level.getBlockEntity(position)
                        instanceof ItemPipeBlockEntity pipe) {
                    pipe.recordTransferred(
                            execution.consumed(),
                            execution.delivered());
                }
            }
        }
        return new Execution(consumed, delivered);
    }

    static int revalidateInsert(
            IItemHandler endpoint, ItemStack stack, int amount) {
        if (endpoint == null || stack.isEmpty() || amount <= 0) {
            return 0;
        }
        return simulateInsert(
                endpoint,
                stack.copyWithCount(Math.min(stack.getCount(), amount)));
    }

    private static int simulateInsert(
            IItemHandler endpoint, ItemStack stack) {
        ItemStack remaining = stack.copy();
        try {
            for (int slot = 0;
                    slot < endpoint.getSlots() && !remaining.isEmpty();
                    slot++) {
                ItemStack next = endpoint.insertItem(
                        slot, remaining, true);
                if (!validRemainder(remaining, next)) {
                    PipeTransferDiagnostics.warnOnce(
                            "item endpoint simulation",
                            endpoint,
                            "Item endpoint returned an invalid remainder");
                    return 0;
                }
                remaining = next;
            }
        } catch (RuntimeException failure) {
            PipeTransferDiagnostics.warnOnce(
                    "item endpoint simulation",
                    endpoint,
                    "Item endpoint simulation failed",
                    failure);
            return 0;
        }
        return stack.getCount() - remaining.getCount();
    }

    private static boolean validRemainder(
            ItemStack offered, ItemStack remainder) {
        return remainder != null
                && remainder.getCount() >= 0
                && remainder.getCount() <= offered.getCount()
                && (remainder.isEmpty()
                        || ItemStack.isSameItemSameComponents(
                                offered, remainder));
    }

    public record Execution(int consumed, int delivered) {
        public Execution {
            if (consumed < 0 || delivered < 0 || delivered > consumed) {
                throw new IllegalArgumentException(
                        "Invalid item transfer execution");
            }
        }
    }

    public record Delivery(Route route, int amount) {
        public Delivery {
            if (route == null || amount <= 0) {
                throw new IllegalArgumentException(
                        "Invalid item route delivery");
            }
        }

        private Execution execute(Level level, ItemStack template) {
            for (BlockPos position : route.pipePath()) {
                if (!level.hasChunkAt(position)
                        || !(level.getBlockEntity(position)
                                instanceof ItemPipeBlockEntity pipe)
                        || pipe.availableItems() < amount) {
                    PipeTransferDiagnostics.warnOnce(
                            "item route execution",
                            route,
                            "Item pipe route changed before execution");
                    return new Execution(0, 0);
                }
            }
            if (!level.hasChunkAt(route.endpoint())) {
                PipeTransferDiagnostics.warnOnce(
                        "item endpoint execution",
                        route,
                        "Item endpoint chunk unloaded before execution");
                return new Execution(0, 0);
            }
            IItemHandler current;
            try {
                current = level.getCapability(
                        Capabilities.ItemHandler.BLOCK,
                        route.endpoint(),
                        route.side());
            } catch (RuntimeException failure) {
                PipeTransferDiagnostics.warnOnce(
                        "item endpoint execution",
                        route,
                        "Item endpoint lookup failed during execution",
                        failure);
                return new Execution(0, 0);
            }
            if (current == null) {
                PipeTransferDiagnostics.warnOnce(
                        "item endpoint execution",
                        route,
                        "Item endpoint disappeared before execution");
                return new Execution(0, 0);
            }
            int confirmed = revalidateInsert(
                    current, template, amount);
            if (confirmed <= 0) {
                PipeTransferDiagnostics.warnOnce(
                        "item endpoint execution",
                        current,
                        "Item endpoint no longer accepts the simulated item");
                return new Execution(0, 0);
            }
            ItemStack remaining = template.copyWithCount(confirmed);
            int committed = 0;
            try {
                for (int slot = 0;
                        slot < current.getSlots() && !remaining.isEmpty();
                        slot++) {
                    ItemStack next = current.insertItem(
                            slot, remaining, false);
                    if (!validRemainder(remaining, next)) {
                        PipeTransferDiagnostics.warnOnce(
                                "item endpoint execution",
                                current,
                            "Item endpoint returned invalid executed "
                                    + "remainder; retaining unconfirmed source");
                        return new Execution(committed, committed);
                    }
                    committed += remaining.getCount() - next.getCount();
                    remaining = next;
                }
            } catch (RuntimeException failure) {
                PipeTransferDiagnostics.warnOnce(
                        "item endpoint execution",
                        current,
                        "Item endpoint execution failed; retaining "
                                + "unconfirmed source",
                        failure);
                return new Execution(committed, committed);
            }
            int actual = committed;
            if (actual < confirmed) {
                PipeTransferDiagnostics.warnOnce(
                        "item endpoint execution",
                        current,
                        "Item endpoint executed " + actual
                                + " after revalidating " + confirmed);
            }
            return new Execution(actual, actual);
        }
    }
}
