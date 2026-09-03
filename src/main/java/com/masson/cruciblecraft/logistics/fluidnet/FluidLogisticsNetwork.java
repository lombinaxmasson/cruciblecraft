package com.masson.cruciblecraft.logistics.fluidnet;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.masson.cruciblecraft.content.block.AbstractPipeBlock;
import com.masson.cruciblecraft.content.block.FluidPipeBlock;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * Fluid cover network: identity ∩ loaded fluid-pipe component.
 * DESIGN_POLICY: not a world broadcast and not adjacent pump transfer.
 */
public final class FluidLogisticsNetwork {
    private static final Direction[] DIRECTIONS = Direction.values();

    private FluidLogisticsNetwork() {}

    public static int tickTransfer(
            PipeCover cover,
            CoverDefinition definition,
            Level level,
            BlockPos pipePos,
            Direction side) {
        if (level == null
                || level.isClientSide
                || pipePos == null
                || side == null
                || cover == null
                || definition == null) {
            return 0;
        }
        Optional<FluidNetworkKinds.TransferDirection> direction =
                FluidNetworkKinds.direction(cover.definitionId());
        if (direction.isEmpty()) {
            return 0;
        }
        int networkId = FluidNetworkKinds.networkId(cover);
        if (!FluidNetworkKinds.isJoined(networkId)) {
            return 0;
        }
        IFluidHandler adjacent = tankAt(level, pipePos, side);
        if (adjacent == null) {
            return 0;
        }
        List<StorageEndpoint> storages = discoverStorage(
                level, pipePos, networkId).endpoints();
        if (storages.isEmpty()) {
            return 0;
        }
        CoverDefinition.Values values = definition.resolve(cover.config());
        int amount = values.rate() > 0
                ? values.rate()
                : FluidNetworkLimits.DEFAULT_RATE;
        Predicate<FluidStack> match = stack -> matches(
                cover.config().matchId(), stack);
        return switch (direction.orElseThrow()) {
            case EXPORT -> exportFluids(
                    level, adjacent, storages, amount, match);
            case IMPORT -> importFluids(
                    level, adjacent, storages, amount, match);
        };
    }

    public static Discovery discoverStorage(
            Level level, BlockPos start, int networkId) {
        ArrayList<StorageEndpoint> endpoints = new ArrayList<>();
        if (level == null
                || level.isClientSide
                || start == null
                || !FluidNetworkKinds.isJoined(networkId)
                || !level.hasChunkAt(start)) {
            return Discovery.empty();
        }
        ArrayDeque<BlockPos> open = new ArrayDeque<>();
        Set<BlockPos> visited = new HashSet<>();
        open.add(start.immutable());
        while (!open.isEmpty()
                && visited.size() < FluidNetworkLimits.MAX_VISITED_PIPES
                && endpoints.size()
                        < FluidNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT) {
            BlockPos pos = open.remove();
            if (!visited.add(pos)) {
                continue;
            }
            if (!level.hasChunkAt(pos)) {
                continue;
            }
            BlockState state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof FluidPipeBlock)
                    || !(level.getBlockEntity(pos)
                            instanceof FluidPipeBlockEntity pipe)
                    || !pipe.offersNetworkDiscovery()) {
                continue;
            }
            collectStorage(level, pipe, pos, networkId, endpoints);
            for (Direction direction : DIRECTIONS) {
                if (!AbstractPipeBlock.isConnected(state, direction)) {
                    continue;
                }
                BlockPos next = pos.relative(direction);
                if (visited.contains(next) || !level.hasChunkAt(next)) {
                    continue;
                }
                if (level.getBlockState(next).getBlock()
                        instanceof FluidPipeBlock) {
                    open.add(next.immutable());
                }
            }
        }
        return new Discovery(List.copyOf(endpoints), visited.size());
    }

    public static boolean isVisibleStorage(
            Level level,
            BlockPos start,
            int networkId,
            BlockPos storagePipe,
            Direction storageSide) {
        for (StorageEndpoint endpoint : discoverStorage(
                level, start, networkId).endpoints()) {
            if (endpoint.pipe().equals(storagePipe)
                    && endpoint.side() == storageSide) {
                return true;
            }
        }
        return false;
    }

    private static void collectStorage(
            Level level,
            FluidPipeBlockEntity pipe,
            BlockPos pipePos,
            int networkId,
            List<StorageEndpoint> endpoints) {
        for (var entry : pipe.coverSnapshot().entrySet()) {
            PipeCover cover = entry.getValue();
            if (cover == null
                    || !FluidNetworkKinds.isStorage(cover.definitionId())
                    || FluidNetworkKinds.networkId(cover) != networkId) {
                continue;
            }
            Direction side = entry.getKey();
            IFluidHandler tank = tankAt(level, pipePos, side);
            if (tank == null) {
                continue;
            }
            endpoints.add(new StorageEndpoint(
                    pipePos.immutable(),
                    side,
                    pipePos.relative(side).immutable()));
            if (endpoints.size()
                    >= FluidNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT) {
                return;
            }
        }
    }

    private static IFluidHandler tankAt(
            Level level, BlockPos pipePos, Direction side) {
        BlockPos target = pipePos.relative(side);
        if (!level.hasChunkAt(target)) {
            return null;
        }
        try {
            return level.getCapability(
                    Capabilities.FluidHandler.BLOCK,
                    target,
                    side.getOpposite());
        } catch (RuntimeException ignored) {
            return null;
        }
    }

    private static IFluidHandler tankOf(
            Level level, StorageEndpoint endpoint) {
        return tankAt(level, endpoint.pipe(), endpoint.side());
    }

    private static int exportFluids(
            Level level,
            IFluidHandler source,
            List<StorageEndpoint> storages,
            int amount,
            Predicate<FluidStack> match) {
        FluidStack simulated = source.drain(amount, IFluidHandler.FluidAction.SIMULATE);
        if (simulated.isEmpty() || !match.test(simulated)) {
            return 0;
        }
        int insertable = 0;
        for (StorageEndpoint endpoint : storages) {
            IFluidHandler dest = tankOf(level, endpoint);
            if (dest == null) {
                continue;
            }
            insertable += dest.fill(
                    simulated.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (insertable >= simulated.getAmount()) {
                break;
            }
        }
        if (insertable <= 0) {
            return 0;
        }
        int take = Math.min(simulated.getAmount(), insertable);
        FluidStack drained = source.drain(take, IFluidHandler.FluidAction.EXECUTE);
        if (drained.isEmpty()) {
            return 0;
        }
        FluidStack remaining = drained.copy();
        for (StorageEndpoint endpoint : storages) {
            if (remaining.isEmpty()) {
                break;
            }
            IFluidHandler dest = tankOf(level, endpoint);
            if (dest == null) {
                continue;
            }
            int filled = dest.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
            if (filled > 0) {
                remaining = remaining.copyWithAmount(
                        remaining.getAmount() - filled);
            }
        }
        if (!remaining.isEmpty()) {
            source.fill(remaining, IFluidHandler.FluidAction.EXECUTE);
        }
        return drained.getAmount() - remaining.getAmount();
    }

    private static int importFluids(
            Level level,
            IFluidHandler dest,
            List<StorageEndpoint> storages,
            int amount,
            Predicate<FluidStack> match) {
        int remainingNeed = amount;
        int moved = 0;
        for (StorageEndpoint endpoint : storages) {
            if (remainingNeed <= 0) {
                break;
            }
            IFluidHandler source = tankOf(level, endpoint);
            if (source == null) {
                continue;
            }
            FluidStack simulated = source.drain(
                    remainingNeed, IFluidHandler.FluidAction.SIMULATE);
            if (simulated.isEmpty() || !match.test(simulated)) {
                continue;
            }
            int insertable = dest.fill(
                    simulated.copy(), IFluidHandler.FluidAction.SIMULATE);
            if (insertable <= 0) {
                continue;
            }
            FluidStack taken = source.drain(
                    insertable, IFluidHandler.FluidAction.EXECUTE);
            if (taken.isEmpty()) {
                continue;
            }
            int accepted = dest.fill(taken, IFluidHandler.FluidAction.EXECUTE);
            if (accepted < taken.getAmount()) {
                FluidStack leftover = taken.copyWithAmount(
                        taken.getAmount() - accepted);
                source.fill(leftover, IFluidHandler.FluidAction.EXECUTE);
            }
            moved += accepted;
            remainingNeed -= accepted;
        }
        return moved;
    }

    private static boolean matches(Optional<String> expected, FluidStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (expected.isEmpty()) {
            return true;
        }
        ResourceLocation key = BuiltInRegistries.FLUID.getKey(stack.getFluid());
        return expected.orElseThrow().equals(key.toString());
    }

    public record StorageEndpoint(
            BlockPos pipe, Direction side, BlockPos tank) {}

    public record Discovery(List<StorageEndpoint> endpoints, int visits) {
        public Discovery {
            endpoints = List.copyOf(endpoints);
            if (visits < 0) {
                throw new IllegalArgumentException("visits");
            }
        }

        public static Discovery empty() {
            return new Discovery(List.of(), 0);
        }
    }
}
