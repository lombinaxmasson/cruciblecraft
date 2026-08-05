package com.masson.cruciblecraft.logistics.pipe.item;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;

import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.ItemPipeBlock;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.PipeTransferDiagnostics;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

/** Loaded-only, stable weighted item route discovery. */
public final class ItemPipeNetworkTraversal {
    public static final int MAX_VISITED_PIPES = 32_768;
    private static final Direction[] DIRECTIONS = Direction.values();

    private ItemPipeNetworkTraversal() {}

    public static List<Route> discover(
            Level level,
            BlockPos start,
            Direction ingress,
            ItemStack stack) {
        if (level == null
                || level.isClientSide
                || start == null
                || ingress == null
                || stack.isEmpty()
                || !level.hasChunkAt(start)) {
            return List.of();
        }
        BlockPos source = start.relative(ingress);
        Set<BlockPos> visited = new HashSet<>();
        PriorityQueue<Node> open = new PriorityQueue<>(
                Comparator.comparingLong(Node::cost)
                        .thenComparingLong(
                                node -> node.position().asLong()));
        open.add(new Node(
                start.immutable(), ingress, 0L, List.of()));
        ArrayList<Route> routes = new ArrayList<>();

        while (!open.isEmpty()
                && visited.size() < MAX_VISITED_PIPES) {
            Node node = open.remove();
            if (!visited.add(node.position())) {
                continue;
            }
            if (!level.hasChunkAt(node.position())) {
                continue;
            }
            BlockState state = level.getBlockState(node.position());
            if (!(state.getBlock() instanceof ItemPipeBlock block)
                    || !(level.getBlockEntity(node.position())
                            instanceof ItemPipeBlockEntity pipe)) {
                continue;
            }
            if (!pipe.acceptsIncoming(node.ingress(), stack)) {
                continue;
            }
            ArrayList<BlockPos> path = new ArrayList<>(node.path());
            path.add(node.position().immutable());
            long cost = Math.addExact(
                    node.cost(), block.pipe().item().stepSize());

            for (Direction direction : DIRECTIONS) {
                if (direction == node.ingress()
                        || !AbstractPipeBlock.isConnected(state, direction)
                        || !pipe.allowsOutgoing(direction, stack)) {
                    continue;
                }
                BlockPos target = node.position().relative(direction);
                if (!level.hasChunkAt(target)
                        || target.equals(source)
                        || visited.contains(target)) {
                    continue;
                }
                BlockState targetState = level.getBlockState(target);
                if (targetState.getBlock() instanceof ItemPipeBlock) {
                    open.add(new Node(
                            target.immutable(),
                            direction.getOpposite(),
                            cost,
                            List.copyOf(path)));
                    continue;
                }
                try {
                    var endpoint = level.getCapability(
                            Capabilities.ItemHandler.BLOCK,
                            target,
                            direction.getOpposite());
                    if (endpoint != null) {
                        routes.add(new Route(
                                target.immutable(),
                                direction.getOpposite(),
                                cost,
                                List.copyOf(path)));
                    }
                } catch (RuntimeException failure) {
                    PipeTransferDiagnostics.warnOnce(
                            "item route discovery",
                            null,
                            "Item endpoint discovery failed at " + target,
                            failure);
                }
            }
        }
        routes.sort(
                Comparator.comparingLong(Route::cost)
                        .thenComparingLong(
                                route -> route.endpoint().asLong())
                        .thenComparingInt(
                                route -> route.side().ordinal()));
        return List.copyOf(routes);
    }

    public record Route(
            BlockPos endpoint,
            Direction side,
            long cost,
            List<BlockPos> pipePath) {
        public Route {
            endpoint = endpoint.immutable();
            pipePath = pipePath.stream()
                    .map(BlockPos::immutable)
                    .toList();
            if (cost <= 0L || pipePath.isEmpty()) {
                throw new IllegalArgumentException("Invalid item pipe route");
            }
        }
    }

    private record Node(
            BlockPos position,
            Direction ingress,
            long cost,
            List<BlockPos> path) {}
}
