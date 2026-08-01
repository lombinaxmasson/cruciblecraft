package com.masson.cruciblecraft.machine.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.energy.IEnergyHandler;
import com.masson.cruciblecraft.machine.component.CheckpointTracker;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;

class ProcessingRuntimeTest {
    @Test
    void pausesResumesCompletesAndResetsWhenRecipeDisappears() {
        ProcessingRuntime runtime = new ProcessingRuntime();
        assertEquals(
                ProcessingRuntime.Result.OUTPUT_BLOCKED,
                runtime.tick("test:recipe", 2, true, false, true,
                        ProcessingMachineSpec.BufferPolicy.PAUSE));
        assertEquals(0, runtime.processor().progress());
        assertEquals(
                ProcessingRuntime.Result.UNDERPOWERED,
                runtime.tick("test:recipe", 2, true, true, false,
                        ProcessingMachineSpec.BufferPolicy.PAUSE));
        assertEquals(0, runtime.processor().progress());
        assertEquals(
                ProcessingRuntime.Result.ADVANCED,
                runtime.tick("test:recipe", 2, true, true, true,
                        ProcessingMachineSpec.BufferPolicy.PAUSE));
        assertEquals(
                ProcessingRuntime.Result.COMPLETE,
                runtime.tick("test:recipe", 2, true, true, true,
                        ProcessingMachineSpec.BufferPolicy.PAUSE));
        runtime.completed(2);
        assertFalse(runtime.active());
        assertEquals(0, runtime.processor().progress());

        runtime.tick("test:other", 4, true, true, true,
                ProcessingMachineSpec.BufferPolicy.PAUSE);
        assertTrue(runtime.active());
        assertEquals(
                ProcessingRuntime.Result.IDLE,
                runtime.tick("", 0, false, false, false,
                        ProcessingMachineSpec.BufferPolicy.PAUSE));
        assertFalse(runtime.active());
    }

    @Test
    void versionedStateRoundTripsAndClampsUnsafeValues() {
        ProcessingMachineState original = new ProcessingMachineState(
                "test:recipe", 3, 8, "underpowered", 16, 512, 9);
        CompoundTag written = original.write();
        assertEquals(ProcessingMachineState.VERSION,
                written.getInt("processing_version"));
        assertEquals(9, written.getLong("resource_revision"));
        assertEquals(original, ProcessingMachineState.read(written));

        ProcessingMachineState recovered = new ProcessingMachineState(
                null, 20, 4, null, -1, -2, -3);
        assertEquals("", recovered.activeRecipe());
        assertEquals(4, recovered.progress());
        assertEquals("idle", recovered.status());
        assertEquals(0, recovered.energy());
        assertEquals(0, recovered.resourceRevision());

        CompoundTag future = original.write();
        future.putInt("processing_version", ProcessingMachineState.VERSION + 1);
        assertThrows(
                IllegalArgumentException.class,
                () -> ProcessingMachineState.read(future));
    }

    @Test
    void bufferedEnergySimulatesBeforeMutationAndHonorsPacketLimit() {
        MachineEnergyBuffer energy = new MachineEnergyBuffer(1024, 128);
        assertEquals(4, energy.insert(64, 4, true));
        assertEquals(0, energy.stored());
        assertEquals(4, energy.insert(64, 4, false));
        assertEquals(256, energy.stored());
        assertEquals(0, energy.insert(256, 1, false));
        assertTrue(energy.consume(16));
        assertEquals(240, energy.stored());

        energy.restore(-1);
        assertEquals(0, energy.stored());
        energy.restore(2048);
        assertEquals(1024, energy.stored());
    }

    @Test
    void unchangedIdleTicksDoNotScheduleRepeatedSync() {
        ProcessingRuntime runtime = new ProcessingRuntime();
        CheckpointTracker tracker = new CheckpointTracker();
        runtime.tick("test:recipe", 4, true, true, false,
                ProcessingMachineSpec.BufferPolicy.PAUSE);
        if (runtime.reset()) {
            tracker.markSyncPending();
        }
        assertTrue(tracker.shouldSync(false, 7, 7, 20));
        tracker.synced();

        if (runtime.reset()) {
            tracker.markSyncPending();
        }
        assertFalse(tracker.shouldSync(false, 27, 7, 20));
    }

    @Test
    void adjacentHeatIsSimulatedThenConsumedInWholePackets() {
        class HeatSource implements IEnergyHandler {
            long extracted;
            @Override public boolean handles(EnergyType type, Direction side) {
                return type == EnergyType.HEAT && side == Direction.UP;
            }
            @Override public long outputSize(EnergyType type, Direction side) {
                return 4;
            }
            @Override public long extract(
                    EnergyType type,
                    long size,
                    long amount,
                    Direction side,
                    boolean simulate) {
                if (!handles(type, side) || size != 4) {
                    return 0;
                }
                if (!simulate) {
                    extracted += amount;
                }
                return amount;
            }
        }
        HeatSource source = new HeatSource();
        assertTrue(AdjacentEnergyConsumer.consume(
                source, EnergyType.HEAT, Direction.UP, 5, true));
        assertEquals(0, source.extracted);
        assertTrue(AdjacentEnergyConsumer.consume(
                source, EnergyType.HEAT, Direction.UP, 5, false));
        assertEquals(2, source.extracted);
    }
}
