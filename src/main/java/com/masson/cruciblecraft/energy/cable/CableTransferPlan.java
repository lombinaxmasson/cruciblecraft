package com.masson.cruciblecraft.energy.cable;

import java.util.List;
import java.util.Objects;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.energy.EnergyTransferDiagnostics;
import com.masson.cruciblecraft.registry.ModCapabilities;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/** Revalidated terminal deliveries and per-segment load mutations. */
public record CableTransferPlan(
        EnergyType energyType,
        long acceptedAmperes,
        List<TerminalDelivery> terminals,
        List<CableLoad> cableLoads) {
    public CableTransferPlan {
        Objects.requireNonNull(energyType, "energyType");
        if (acceptedAmperes < 0L) {
            throw new IllegalArgumentException(
                    "Accepted cable amperage must be non-negative");
        }
        terminals = List.copyOf(terminals);
        cableLoads = List.copyOf(cableLoads);
    }

    public CableTransferPlan(
            long acceptedAmperes,
            List<TerminalDelivery> terminals,
            List<CableLoad> cableLoads) {
        this(EnergyType.ELECTRIC, acceptedAmperes, terminals, cableLoads);
    }

    public long execute(Level level) {
        Objects.requireNonNull(level, "level");
        if (level.isClientSide || acceptedAmperes <= 0L) {
            return 0L;
        }
        long[] actualByTerminal = new long[terminals.size()];
        long delivered = 0L;
        for (int index = 0; index < terminals.size(); index++) {
            long actual = terminals.get(index).execute(level, energyType);
            actualByTerminal[index] = actual;
            delivered = Math.addExact(delivered, actual);
        }
        for (CableLoad load : cableLoads) {
            load.apply(level, actualAmperes(
                    actualByTerminal,
                    load.terminalStartInclusive(),
                    load.terminalEndExclusive()));
        }
        return delivered;
    }

    static long actualAmperes(
            long[] actualByTerminal,
            int startInclusive,
            int endExclusive) {
        Objects.requireNonNull(actualByTerminal, "actualByTerminal");
        if (startInclusive < 0
                || endExclusive < startInclusive
                || endExclusive > actualByTerminal.length) {
            throw new IllegalArgumentException(
                    "Invalid cable terminal range");
        }
        long total = 0L;
        for (int index = startInclusive; index < endExclusive; index++) {
            long actual = actualByTerminal[index];
            if (actual < 0L) {
                throw new IllegalArgumentException(
                        "Actual cable delivery must be non-negative");
            }
            total = Math.addExact(total, actual);
        }
        return total;
    }

    public record TerminalDelivery(
            BlockPos position,
            Direction side,
            IEnergyHandler handler,
            long packetSize,
            long amperes) {
        public TerminalDelivery {
            position = position.immutable();
            Objects.requireNonNull(side, "side");
            Objects.requireNonNull(handler, "handler");
            if (packetSize == 0L || amperes <= 0L) {
                throw new IllegalArgumentException(
                        "Invalid cable terminal delivery");
            }
        }

        private long execute(Level level, EnergyType energyType) {
            if (!level.hasChunkAt(position)) {
                EnergyTransferDiagnostics.warnOnce(
                        "terminal execution",
                        handler,
                        "Cable terminal chunk unloaded before execution: "
                                + position);
                return 0L;
            }
            IEnergyHandler current;
            try {
                current = level.getCapability(
                        ModCapabilities.ENERGY, position, side);
            } catch (RuntimeException failure) {
                EnergyTransferDiagnostics.warnOnce(
                        "terminal execution",
                        handler,
                        "Cable terminal capability lookup failed at "
                                + position + " side " + side,
                        failure);
                return 0L;
            }
            if (current != handler) {
                EnergyTransferDiagnostics.warnOnce(
                        "terminal execution",
                        handler,
                        "Cable terminal changed before execution at "
                                + position + " side " + side);
                return 0L;
            }
            long simulated = insertBounded(
                    "terminal simulation", amperes, true, energyType);
            if (simulated <= 0L) {
                return 0L;
            }
            return insertBounded(
                    "terminal execution", simulated, false, energyType);
        }

        private long insertBounded(
                String operation,
                long requested,
                boolean simulate,
                EnergyType energyType) {
            long actual;
            try {
                actual = handler.insert(
                        energyType,
                        packetSize,
                        requested,
                        side,
                        simulate);
            } catch (RuntimeException failure) {
                EnergyTransferDiagnostics.warnOnce(
                        operation,
                        handler,
                        "Cable " + operation + " failed at "
                                + position + " side " + side,
                        failure);
                return 0L;
            }
            if (actual < 0L || actual > requested) {
                EnergyTransferDiagnostics.warnOnce(
                        operation,
                        handler,
                        "Cable " + operation + " returned " + actual
                                + " packets for request " + requested
                                + " at " + position + " side " + side);
                return 0L;
            }
            return actual;
        }
    }

    public record CableLoad(
            BlockPos position,
            CableBlock expectedBlock,
            CableLoadState.Snapshot expectedLoad,
            long postLossSize,
            long amperes,
            boolean overloaded,
            int terminalStartInclusive,
            int terminalEndExclusive) {
        public CableLoad {
            position = position.immutable();
            Objects.requireNonNull(expectedBlock, "expectedBlock");
            Objects.requireNonNull(expectedLoad, "expectedLoad");
            if (postLossSize == 0L
                    || amperes <= 0L
                    || terminalStartInclusive < 0
                    || terminalEndExclusive <= terminalStartInclusive) {
                throw new IllegalArgumentException("Invalid cable load");
            }
        }

        private void apply(Level level, long actualAmperes) {
            if (actualAmperes <= 0L) {
                return;
            }
            try {
                if (!level.hasChunkAt(position)
                        || level.getBlockState(position).getBlock()
                                != expectedBlock
                        || !(level.getBlockEntity(position)
                                instanceof CableBlockEntity cable)) {
                    EnergyTransferDiagnostics.warnOnce(
                            "cable load application",
                            expectedBlock,
                            "Cable state changed before execution at " + position);
                    return;
                }
                // A cached simulation may legitimately observe another
                // injection before it commits. Keep the snapshot as review
                // evidence, but recompute overload against the live aggregate.
                boolean expectedStillCurrent =
                        cable.loadSnapshot(level.getGameTime())
                                .equals(expectedLoad);
                boolean effectiveOverloaded = expectedStillCurrent
                        && actualAmperes == amperes
                        ? overloaded
                        : cable.loadWouldOverload(
                                level.getGameTime(),
                                postLossSize,
                                actualAmperes);
                cable.applyLoad(
                        level.getGameTime(),
                        postLossSize,
                        actualAmperes,
                        effectiveOverloaded);
            } catch (RuntimeException failure) {
                EnergyTransferDiagnostics.warnOnce(
                        "cable load application",
                        expectedBlock,
                        "Cable load application failed at " + position,
                        failure);
            }
        }
    }

}
