package com.masson.cruciblecraft.content.item.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;

class ProvidedToolActionsTest {
    @Test
    void worldClickKindsMapOntoTypedActions() {
        assertEquals(Set.of(ToolAction.WRENCH), ProvidedToolActions.of(ToolKind.WRENCH));
        assertEquals(
                Set.of(ToolAction.MONKEY_WRENCH),
                ProvidedToolActions.of(ToolKind.MONKEY_WRENCH));
        assertEquals(
                Set.of(ToolAction.WIRE_CUTTER),
                ProvidedToolActions.of(ToolKind.WIRE_CUTTER));
        assertEquals(
                Set.of(ToolAction.SCREWDRIVER),
                ProvidedToolActions.of(ToolKind.SCREWDRIVER));
        assertEquals(Set.of(ToolAction.CROWBAR), ProvidedToolActions.of(ToolKind.CROWBAR));
        assertEquals(Set.of(ToolAction.PLUNGER), ProvidedToolActions.of(ToolKind.PLUNGER));
        assertEquals(
                Set.of(ToolAction.SOFT_HAMMER),
                ProvidedToolActions.of(ToolKind.SOFT_HAMMER));
        assertEquals(Set.of(ToolAction.PINCERS), ProvidedToolActions.of(ToolKind.PINCERS));
        assertEquals(
                Set.of(ToolAction.CROWBAR),
                ProvidedToolActions.of(ToolKind.UNIVERSAL_SPADE));
        assertTrue(ProvidedToolActions.of(ToolKind.PICKAXE).isEmpty());
        assertTrue(ProvidedToolActions.of(ToolKind.FILE).isEmpty());
    }

    @Test
    void plungerAndCrowbarStayAheadOfWrench() {
        assertTrue(ToolAction.PLUNGER.ordinal() < ToolAction.WRENCH.ordinal());
        assertTrue(ToolAction.CROWBAR.ordinal() < ToolAction.WRENCH.ordinal());
    }

    @Test
    void pipeGridExpansionMatchesThePreviousHolders() {
        assertTrue(ToolAction.WRENCH.expandsPipeGrid());
        assertTrue(ToolAction.CROWBAR.expandsPipeGrid());
        assertTrue(ToolAction.PLUNGER.expandsPipeGrid());
        assertFalse(ToolAction.WIRE_CUTTER.expandsPipeGrid());
        assertFalse(ToolAction.SCREWDRIVER.expandsPipeGrid());
    }
}
