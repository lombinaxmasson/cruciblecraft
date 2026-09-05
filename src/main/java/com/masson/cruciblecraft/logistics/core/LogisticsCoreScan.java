package com.masson.cruciblecraft.logistics.core;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.displaycpu.DisplayCpuKinds;
import com.masson.cruciblecraft.logistics.genericnet.GenericNetworkKinds;
import com.masson.cruciblecraft.logistics.genericnet.GenericNetworkLimits;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;

/**
 * SOURCE_DERIVED flood: hull logistics adjacency into item/fluid pipes
 * within cubic range. Not a world-wide same-id broadcast.
 */
public final class LogisticsCoreScan {
    public record Endpoint(BlockPos pipe, Direction side, int networkId) {}

    public record Result(
            List<Endpoint> dumps,
            List<Endpoint> genericItemStorage,
            List<Endpoint> displays,
            int controlUsed,
            Set<ResourceLocation> filteredFor) {
        public static Result empty() {
            return new Result(List.of(), List.of(), List.of(), 0, Set.of());
        }
    }

    private LogisticsCoreScan() {}

    public static Result scan(
            Level level, BlockPos center, int controlCpus) {
        if (level == null || center == null) {
            return Result.empty();
        }
        int range = LogisticsCoreGeometry.HALF + Math.max(0, controlCpus);
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        for (int i = -LogisticsCoreGeometry.HALF;
                i <= LogisticsCoreGeometry.HALF;
                i++) {
            for (int j = -LogisticsCoreGeometry.HALF;
                    j <= LogisticsCoreGeometry.HALF;
                    j++) {
                for (int k = -LogisticsCoreGeometry.HALF;
                        k <= LogisticsCoreGeometry.HALF;
                        k++) {
                    BlockPos hull = center.offset(i, j, k);
                    if (!level.hasChunkAt(hull)) {
                        continue;
                    }
                    if (LogisticsCorePart.of(level.getBlockState(hull).getBlock())
                            == null) {
                        continue;
                    }
                    for (Direction side : Direction.values()) {
                        BlockPos adjacent = hull.relative(side);
                        if (inRange(center, adjacent, range)) {
                            open.add(adjacent.immutable());
                        }
                    }
                }
            }
        }
        ArrayList<Endpoint> dumps = new ArrayList<>();
        ArrayList<Endpoint> storage = new ArrayList<>();
        ArrayList<Endpoint> displays = new ArrayList<>();
        HashSet<ResourceLocation> filtered = new HashSet<>();
        int controlUsed = 0;
        while (!open.isEmpty()
                && visited.size() < GenericNetworkLimits.MAX_VISITED_PIPES) {
            BlockPos pos = open.remove();
            if (!visited.add(pos) || !level.hasChunkAt(pos)) {
                continue;
            }
            if (!inRange(center, pos, range)) {
                continue;
            }
            BlockEntity be = level.getBlockEntity(pos);
            Map<Direction, PipeCover> covers = coversOf(be);
            if (covers == null) {
                continue;
            }
            controlUsed = Math.max(
                    controlUsed,
                    Math.max(
                            0,
                            LogisticsCoreGeometry.chebyshev(
                                    pos.getX() - center.getX(),
                                    pos.getY() - center.getY(),
                                    pos.getZ() - center.getZ())
                                    - LogisticsCoreGeometry.HALF));
            BlockState state = level.getBlockState(pos);
            for (Map.Entry<Direction, PipeCover> entry : covers.entrySet()) {
                PipeCover cover = entry.getValue();
                cover.matchId().ifPresent(id -> {
                    ResourceLocation parsed = ResourceLocation.tryParse(id);
                    if (parsed != null) {
                        filtered.add(parsed);
                    }
                });
                Direction side = entry.getKey();
                int networkId = LogisticsDumpKinds.networkId(cover);
                if (DisplayCpuKinds.isDisplay(cover.definitionId())) {
                    displays.add(new Endpoint(pos.immutable(), side, networkId));
                } else if (LogisticsDumpKinds.isDump(cover.definitionId())) {
                    dumps.add(new Endpoint(pos.immutable(), side, networkId));
                } else if (GenericNetworkKinds.isStorage(cover.definitionId())
                        && inventoryAt(level, pos, side) != null) {
                    storage.add(new Endpoint(pos.immutable(), side, networkId));
                }
            }
            for (Direction side : Direction.values()) {
                if (!AbstractPipeBlock.isConnected(state, side)) {
                    continue;
                }
                BlockPos next = pos.relative(side);
                if (inRange(center, next, range)) {
                    open.add(next.immutable());
                }
            }
        }
        return new Result(
                List.copyOf(dumps),
                List.copyOf(storage),
                List.copyOf(displays),
                controlUsed,
                Set.copyOf(filtered));
    }

    public static IItemHandler inventoryAt(
            Level level, BlockPos pipePos, Direction side) {
        BlockPos target = pipePos.relative(side);
        if (!level.hasChunkAt(target)) {
            return null;
        }
        try {
            return level.getCapability(
                    Capabilities.ItemHandler.BLOCK,
                    target,
                    side.getOpposite());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static boolean inRange(BlockPos center, BlockPos pos, int range) {
        return LogisticsCoreGeometry.chebyshev(
                pos.getX() - center.getX(),
                pos.getY() - center.getY(),
                pos.getZ() - center.getZ())
                <= range;
    }

    private static Map<Direction, PipeCover> coversOf(BlockEntity be) {
        if (be instanceof ItemPipeBlockEntity item) {
            return item.coverSnapshot();
        }
        if (be instanceof FluidPipeBlockEntity fluid) {
            return fluid.coverSnapshot();
        }
        return null;
    }
}
