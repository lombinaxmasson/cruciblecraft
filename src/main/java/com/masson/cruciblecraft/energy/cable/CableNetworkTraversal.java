package com.masson.cruciblecraft.energy.cable;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.energy.EnergyPackets;
import com.masson.cruciblecraft.energy.EnergyTransferDiagnostics;
import com.masson.cruciblecraft.registry.ModCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Loaded-only, injection-local GT6-style EU/LU cable traversal. */
public final class CableNetworkTraversal {
    private static final Direction[] DIRECTIONS = Direction.values();

    private CableNetworkTraversal() {}

    public static long applySegmentLoss(
            long packetSize, long lossPerMeter) {
        if (packetSize == 0L || lossPerMeter < 0L) {
            throw new IllegalArgumentException(
                    "Packet size must be non-zero and cable loss non-negative");
        }
        if (lossPerMeter == 0L) {
            return packetSize;
        }
        if (EnergyPackets.magnitude(packetSize) <= lossPerMeter) {
            return 0L;
        }
        return packetSize < 0L
                ? Math.addExact(packetSize, lossPerMeter)
                : Math.subtractExact(packetSize, lossPerMeter);
    }

    public static CableTransferPlan plan(
            Level level,
            BlockPos start,
            Direction ingress,
            long packetSize,
            long offeredAmperes) {
        return plan(
                level,
                start,
                ingress,
                EnergyType.ELECTRIC,
                packetSize,
                offeredAmperes);
    }

    public static CableTransferPlan plan(
            Level level,
            BlockPos start,
            Direction ingress,
            EnergyType type,
            long packetSize,
            long offeredAmperes) {
        if (level == null
                || level.isClientSide
                || start == null
                || ingress == null
                || type == null
                || packetSize == 0L
                || offeredAmperes <= 0L
                || !level.hasChunkAt(start)) {
            return new CableTransferPlan(
                    type == null ? EnergyType.ELECTRIC : type,
                    0L,
                    List.of(),
                    List.of());
        }
        Set<BlockPos> visited = new HashSet<>();
        visited.add(start.immutable());
        BlockPos source = start.relative(ingress);
        visited.add(source.immutable());
        ArrayList<CableTransferPlan.TerminalDelivery> terminals =
                new ArrayList<>();
        ArrayList<CableTransferPlan.CableLoad> loads =
                new ArrayList<>();
        ArrayDeque<Frame> stack = new ArrayDeque<>();
        stack.push(new Frame(
                start.immutable(),
                ingress,
                packetSize,
                offeredAmperes,
                0));
        long rootResult = 0L;

        while (!stack.isEmpty()) {
            Frame frame = stack.peek();
            if (!frame.initialize(level)) {
                rootResult = complete(stack, frame, 0L);
                continue;
            }
            if (frame.usedAmperes >= frame.offeredAmperes
                    || frame.nextDirection >= DIRECTIONS.length) {
                boolean overloaded = frame.usedAmperes > 0L
                        && frame.blockEntity.loadWouldOverload(
                                level.getGameTime(),
                                frame.postLossSize,
                                frame.usedAmperes);
                if (frame.usedAmperes > 0L) {
                    loads.add(new CableTransferPlan.CableLoad(
                            frame.position,
                            frame.block,
                            frame.loadSnapshot,
                            frame.postLossSize,
                            frame.usedAmperes,
                            overloaded,
                            frame.terminalStartIndex,
                            terminals.size()));
                }
                long result = overloaded
                        ? frame.offeredAmperes
                        : frame.usedAmperes;
                rootResult = complete(stack, frame, result);
                continue;
            }

            Direction direction = DIRECTIONS[frame.nextDirection++];
            if (direction == frame.ingress
                    || !CableBlock.isConnected(frame.state, direction)) {
                continue;
            }
            BlockPos target = frame.position.relative(direction);
            if (!level.hasChunkAt(target)
                    || visited.contains(target)) {
                continue;
            }
            long remaining = frame.offeredAmperes - frame.usedAmperes;
            BlockState targetState = level.getBlockState(target);
            if (targetState.getBlock() instanceof CableBlock cable
                    && cable.supports(type)) {
                visited.add(target.immutable());
                stack.push(new Frame(
                        target.immutable(),
                        direction.getOpposite(),
                        frame.postLossSize,
                        remaining,
                        terminals.size()));
                continue;
            }

            visited.add(target.immutable());
            Direction terminalSide = direction.getOpposite();
            IEnergyHandler terminal;
            try {
                terminal = level.getCapability(
                        ModCapabilities.ENERGY, target, terminalSide);
                if (terminal == null
                        || !terminal.handles(type, terminalSide)) {
                    continue;
                }
            } catch (RuntimeException failure) {
                EnergyTransferDiagnostics.warnOnce(
                        "terminal discovery",
                        null,
                        "Cable terminal discovery failed at "
                                + target + " side " + terminalSide,
                        failure);
                continue;
            }
            long accepted;
            try {
                accepted = terminal.insert(
                        type,
                        frame.postLossSize,
                        remaining,
                        terminalSide,
                        true);
            } catch (RuntimeException failure) {
                EnergyTransferDiagnostics.warnOnce(
                        "terminal simulation",
                        terminal,
                        "Cable terminal simulation failed at "
                                + target + " side " + terminalSide,
                        failure);
                continue;
            }
            accepted = bounded(
                    target,
                    terminalSide,
                    terminal,
                    remaining,
                    accepted);
            if (accepted > 0L) {
                terminals.add(
                        new CableTransferPlan.TerminalDelivery(
                                target,
                                terminalSide,
                                terminal,
                                frame.postLossSize,
                                accepted));
                frame.usedAmperes += accepted;
            }
        }
        return new CableTransferPlan(type, rootResult, terminals, loads);
    }

    private static long complete(
            ArrayDeque<Frame> stack,
            Frame completed,
            long result) {
        if (result < 0L || result > completed.offeredAmperes) {
            throw new IllegalStateException(
                    "Cable traversal produced invalid amperage");
        }
        if (stack.pop() != completed) {
            throw new IllegalStateException(
                    "Cable traversal stack corruption");
        }
        if (!stack.isEmpty()) {
            Frame parent = stack.peek();
            long remaining =
                    parent.offeredAmperes - parent.usedAmperes;
            if (result > remaining) {
                throw new IllegalStateException(
                        "Cable child exceeded parent amperage");
            }
            parent.usedAmperes += result;
        }
        return stack.isEmpty() ? result : 0L;
    }

    private static long bounded(
            BlockPos position,
            Direction side,
            IEnergyHandler terminal,
            long requested,
            long actual) {
        if (actual < 0L || actual > requested) {
            EnergyTransferDiagnostics.warnOnce(
                    "terminal simulation",
                    terminal,
                    "Cable terminal at " + position + " side " + side
                            + " returned " + actual
                            + " packets for request " + requested);
            return 0L;
        }
        return actual;
    }

    private static final class Frame {
        private final BlockPos position;
        private final Direction ingress;
        private final long packetSize;
        private final long offeredAmperes;
        private final int terminalStartIndex;
        private int nextDirection;
        private long usedAmperes;
        private boolean initialized;
        private BlockState state;
        private CableBlock block;
        private CableBlockEntity blockEntity;
        private CableLoadState.Snapshot loadSnapshot;
        private long postLossSize;

        private Frame(
                BlockPos position,
                Direction ingress,
                long packetSize,
                long offeredAmperes,
                int terminalStartIndex) {
            this.position = position;
            this.ingress = ingress;
            this.packetSize = packetSize;
            this.offeredAmperes = offeredAmperes;
            this.terminalStartIndex = terminalStartIndex;
        }

        private boolean initialize(Level level) {
            if (initialized) {
                return true;
            }
            initialized = true;
            if (!level.hasChunkAt(position)) {
                return false;
            }
            state = level.getBlockState(position);
            if (!(state.getBlock() instanceof CableBlock cable)
                    || !(level.getBlockEntity(position)
                            instanceof CableBlockEntity entity)) {
                return false;
            }
            long loss = cable.transportProperties().lossPerMeter();
            block = cable;
            blockEntity = entity;
            loadSnapshot = entity.loadSnapshot(level.getGameTime());
            postLossSize = applySegmentLoss(packetSize, loss);
            return postLossSize != 0L;
        }
    }
}
