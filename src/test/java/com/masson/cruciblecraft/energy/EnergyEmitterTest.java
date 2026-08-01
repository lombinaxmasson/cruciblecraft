package com.masson.cruciblecraft.energy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;

import org.junit.jupiter.api.Test;

class EnergyEmitterTest {
    private static final EnergyType TYPE = EnergyType.KINETIC;
    private static final long SIZE = 16L;
    private static final BlockPos SOURCE_POSITION = new BlockPos(10, 64, 10);
    private static final BlockPos FIRST_POSITION = new BlockPos(10, 64, 9);
    private static final BlockPos SECOND_POSITION = new BlockPos(9, 64, 10);

    @Test
    void partialConsumerAcceptancePreservesPacketCount() {
        BufferHandler source = BufferHandler.source(10L);
        BufferHandler first = BufferHandler.consumer(3L);
        BufferHandler second = BufferHandler.consumer(2L);

        long delivered = EnergyEmitter.executePlan(
                SOURCE_POSITION,
                source,
                TYPE,
                SIZE,
                Direction.NORTH,
                List.of(first, second),
                List.of(FIRST_POSITION, SECOND_POSITION),
                List.of(Direction.SOUTH, Direction.WEST),
                new long[] {5L, 5L});

        assertEquals(5L, delivered);
        assertEquals(5L, source.packets);
        assertEquals(3L, first.packets);
        assertEquals(2L, second.packets);
        assertEquals(10L, source.packets + first.packets + second.packets);
    }

    @Test
    void consumerReturningMoreThanOfferedFailsBeforeMutation() {
        BufferHandler source = BufferHandler.source(10L);
        IEnergyHandler consumer = new IEnergyHandler() {
            @Override
            public long insert(
                    EnergyType type,
                    long size,
                    long amount,
                    Direction side,
                    boolean simulate) {
                return amount + 1L;
            }
        };

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> EnergyEmitter.executePlan(
                        SOURCE_POSITION,
                        source,
                        TYPE,
                        SIZE,
                        Direction.NORTH,
                        List.of(consumer),
                        List.of(FIRST_POSITION),
                        List.of(Direction.SOUTH),
                        new long[] {4L}));

        assertTrue(failure.getMessage().contains("consumer 0 simulation"));
        assertTrue(failure.getMessage().contains("returned 5 packets for request 4"));
        assertTrue(failure.getMessage().contains(FIRST_POSITION.toString()));
        assertTrue(failure.getMessage().contains(consumer.getClass().getName()));
        assertEquals(10L, source.packets);
    }

    @Test
    void consumerExecutionMustMatchItsIdenticalSimulation() {
        BufferHandler source = BufferHandler.source(10L);
        IEnergyHandler consumer = new IEnergyHandler() {
            @Override
            public long insert(
                    EnergyType type,
                    long size,
                    long amount,
                    Direction side,
                    boolean simulate) {
                return simulate ? amount : amount + 1L;
            }
        };

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> EnergyEmitter.executePlan(
                        SOURCE_POSITION,
                        source,
                        TYPE,
                        SIZE,
                        Direction.NORTH,
                        List.of(consumer),
                        List.of(FIRST_POSITION),
                        List.of(Direction.SOUTH),
                        new long[] {4L}));

        assertTrue(failure.getMessage().contains("consumer 0 execution"));
        assertTrue(failure.getMessage().contains("after simulating 4"));
        assertTrue(failure.getMessage().contains(FIRST_POSITION.toString()));
        assertTrue(failure.getMessage().contains(consumer.getClass().getName()));
        assertEquals(6L, source.packets,
                "source executes first so an endpoint violation cannot duplicate energy");
    }

    @Test
    void laterConsumerViolationFailsTowardEnergyLossNotDuplication() {
        BufferHandler source = BufferHandler.source(10L);
        BufferHandler first = BufferHandler.consumer(10L);
        IEnergyHandler second = new IEnergyHandler() {
            @Override
            public long insert(
                    EnergyType type,
                    long size,
                    long amount,
                    Direction side,
                    boolean simulate) {
                return simulate ? amount : amount + 1L;
            }
        };

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> EnergyEmitter.executePlan(
                        SOURCE_POSITION,
                        source,
                        TYPE,
                        SIZE,
                        Direction.NORTH,
                        List.of(first, second),
                        List.of(FIRST_POSITION, SECOND_POSITION),
                        List.of(Direction.SOUTH, Direction.WEST),
                        new long[] {4L, 4L}));

        assertTrue(failure.getMessage().contains("consumer 1 execution"));
        assertTrue(failure.getMessage().contains(SECOND_POSITION.toString()));
        assertTrue(failure.getMessage().contains(second.getClass().getName()));
        assertEquals(2L, source.packets);
        assertEquals(4L, first.packets);
        assertTrue(source.packets + first.packets <= 10L,
                "an irrecoverable consumer violation may lose energy but must not create it");
    }

    @Test
    void sourceExecutionMustMatchItsIdenticalSimulation() {
        BufferHandler consumer = BufferHandler.consumer(10L);
        IEnergyHandler source = new IEnergyHandler() {
            @Override
            public long extract(
                    EnergyType type,
                    long size,
                    long maxAmount,
                    Direction side,
                    boolean simulate) {
                return simulate ? maxAmount : maxAmount - 1L;
            }
        };

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> EnergyEmitter.executePlan(
                        SOURCE_POSITION,
                        source,
                        TYPE,
                        SIZE,
                        Direction.NORTH,
                        List.of(consumer),
                        List.of(FIRST_POSITION),
                        List.of(Direction.SOUTH),
                        new long[] {4L}));

        assertTrue(failure.getMessage().contains("source execution"));
        assertTrue(failure.getMessage().contains("after simulating 4"));
        assertTrue(failure.getMessage().contains(SOURCE_POSITION.toString()));
        assertTrue(failure.getMessage().contains(source.getClass().getName()));
        assertEquals(0L, consumer.packets,
                "a source execution violation must fail before consumer mutation");
    }

    @Test
    void sourceShortfallIsRejectedBeforeAnyConsumerMutation() {
        BufferHandler consumer = BufferHandler.consumer(10L);
        IEnergyHandler source = new IEnergyHandler() {
            @Override
            public long extract(
                    EnergyType type,
                    long size,
                    long maxAmount,
                    Direction side,
                    boolean simulate) {
                return maxAmount - 1L;
            }
        };

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> EnergyEmitter.executePlan(
                        SOURCE_POSITION,
                        source,
                        TYPE,
                        SIZE,
                        Direction.NORTH,
                        List.of(consumer),
                        List.of(FIRST_POSITION),
                        List.of(Direction.SOUTH),
                        new long[] {4L}));

        assertTrue(failure.getMessage().contains("source simulation"));
        assertTrue(failure.getMessage().contains("returned 3"));
        assertEquals(0L, consumer.packets,
                "source capacity must be confirmed before consumers execute");
    }

    private static final class BufferHandler implements IEnergyHandler {
        private long packets;
        private final long capacity;
        private final boolean source;

        private BufferHandler(long packets, long capacity, boolean source) {
            this.packets = packets;
            this.capacity = capacity;
            this.source = source;
        }

        private static BufferHandler source(long packets) {
            return new BufferHandler(packets, packets, true);
        }

        private static BufferHandler consumer(long capacity) {
            return new BufferHandler(0L, capacity, false);
        }

        @Override
        public long extract(
                EnergyType type,
                long size,
                long maxAmount,
                Direction side,
                boolean simulate) {
            if (!source) {
                return 0L;
            }
            long extracted = Math.min(maxAmount, packets);
            if (!simulate) {
                packets -= extracted;
            }
            return extracted;
        }

        @Override
        public long insert(
                EnergyType type,
                long size,
                long amount,
                Direction side,
                boolean simulate) {
            if (source) {
                return 0L;
            }
            long accepted = Math.min(amount, capacity - packets);
            if (!simulate) {
                packets += accepted;
            }
            return accepted;
        }
    }
}
