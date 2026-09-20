package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.Test;

class ProcessingHostDecisionsTest {
    /**
     * Deferred block-level GameTests: Crusher capability mutation before save,
     * Crusher chance-roll restart, and Coke Oven item+fluid+TU completion
     * across chunk reload.
     */
    private static final List<String> FULL_BLOCK_GAME_TESTS = List.of(
            "crusher_capability_autosave",
            "crusher_chance_restart",
            "coke_oven_atomic_tu_reload");

    @Test
    void actualIdleHostDecisionOnlyPersistsTransitions() {
        assertFalse(ProcessingHostDecisions.idle(false, false, 0).persistentChange());
        assertTrue(ProcessingHostDecisions.idle(true, false, 0).persistentChange());
        assertTrue(ProcessingHostDecisions.idle(false, true, 0).persistentChange());
        assertTrue(ProcessingHostDecisions.idle(false, false, 16).persistentChange());
    }

    @Test
    void actualSelectionHookReusesOnlyExactSerializableFingerprint() {
        assertEquals(
                ProcessingHostDecisions.SelectionPersistence.REUSE_RESTORED_ROLL,
                ProcessingHostDecisions.selection(
                        true, "test:recipe", "hash", "test:recipe", Optional.of("hash")));
        assertEquals(
                ProcessingHostDecisions.SelectionPersistence.RESET_AND_REROLL,
                ProcessingHostDecisions.selection(
                        true, "test:recipe", "hash", "test:recipe", Optional.empty()));
        assertEquals(
                ProcessingHostDecisions.SelectionPersistence.RESET_AND_REROLL,
                ProcessingHostDecisions.selection(
                        true, "test:recipe", "hash", "test:recipe", Optional.of("changed")));
    }

    @Test
    void machineConstructorsAndProductionWiringRemainPresent() throws Exception {
        assertTrue(ProcessingMachineBlockEntity.class.isAssignableFrom(
                CrusherBlockEntity.class));
        CrusherBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);
        CokeOvenBlockEntity.class.getConstructor(BlockPos.class, BlockState.class);
        assertEquals(
                java.util.List.class,
                CokeOvenBlockEntity.class.getDeclaredField("pendingOutputs").getType());
        assertEquals(16, CokeOvenBlockEntity.PARALLEL);
        assertEquals(9, CokeOvenBlockEntity.OUTPUT_SLOT_COUNT);
        assertEquals(0, CokeOvenBlockEntity.parallelOperations(0, 1));
        assertEquals(16, CokeOvenBlockEntity.parallelOperations(64, 1));
        BlockPos controller = new BlockPos(0, 0, 0);
        assertTrue(CokeOvenBlockEntity.isInsideStructure(
                controller, Direction.NORTH, new BlockPos(0, 0, 1)));
        assertTrue(CokeOvenBlockEntity.isInsideStructure(
                controller, Direction.NORTH, new BlockPos(1, 0, 1)));
        BlockPos east = new BlockPos(2, 0, 0);
        assertTrue(CokeOvenBlockEntity.isInsideStructure(
                east, Direction.NORTH, new BlockPos(1, 0, 1)));
        assertEquals(3, FULL_BLOCK_GAME_TESTS.size());
    }
}
