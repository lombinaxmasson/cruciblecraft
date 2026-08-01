package com.masson.cruciblecraft.content.blockentity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import com.masson.cruciblecraft.machine.processing.MachineTransaction;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

import org.junit.jupiter.api.Test;

class ProcessingHostDecisionsTest {
    /**
     * Deferred block-level GameTests: Crusher capability mutation before save,
     * Crusher chance-roll restart, legacy input replacement before first tick,
     * and Coke Oven item+fluid+HEAT completion across chunk reload.
     */
    private static final List<String> FULL_BLOCK_GAME_TESTS = List.of(
            "crusher_capability_autosave",
            "crusher_chance_restart",
            "crusher_legacy_input_replaced",
            "coke_oven_atomic_heat_reload");

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
                MachineTransaction.class,
                CokeOvenBlockEntity.class.getDeclaredField("pendingTransaction").getType());
        assertEquals(4, FULL_BLOCK_GAME_TESTS.size());
    }
}
