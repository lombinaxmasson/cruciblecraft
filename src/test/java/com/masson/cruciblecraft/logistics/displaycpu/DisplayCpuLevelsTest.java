package com.masson.cruciblecraft.logistics.displaycpu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import org.junit.jupiter.api.Test;

class DisplayCpuLevelsTest {
    @Test
    void redstoneAndVisualFollowGt6Bounds() {
        assertEquals(0, DisplayCpuLevels.redstone(0, 1));
        assertEquals(0, DisplayCpuLevels.visual(0, 1));
        assertEquals(0, DisplayCpuLevels.redstone(1, 0));
        assertEquals(0, DisplayCpuLevels.visual(1, 0));
        assertEquals(15, DisplayCpuLevels.redstone(1, 1));
        assertEquals(10, DisplayCpuLevels.visual(1, 1));
        assertEquals(15, DisplayCpuLevels.redstone(4, 1));
        assertEquals(10, DisplayCpuLevels.visual(4, 1));
        assertEquals(1, DisplayCpuLevels.redstone(1, 14));
        assertEquals(1, DisplayCpuLevels.visual(1, 9));
    }

    @Test
    void displayCoversAreStatusNotTransfer() {
        CoverBehaviorRegistry.validateDefinitions();
        PipeCover logic = PipeCover.of(DisplayCpuKinds.LOGIC);
        assertTrue(DisplayCpuKinds.isDisplay(logic.definitionId()));
        assertFalse(logic.behavior().allowsIncoming(
                logic,
                logic.definition().orElseThrow(),
                new com.masson.cruciblecraft.logistics.pipe.cover
                        .CoverBehavior.Access(
                        CoverDefinitionCatalog.require(DisplayCpuKinds.LOGIC)
                                .medium(),
                        0,
                        64)));
        assertFalse(logic.behavior().allowsOutgoing(
                logic,
                logic.definition().orElseThrow(),
                new com.masson.cruciblecraft.logistics.pipe.cover
                        .CoverBehavior.Access(
                        CoverDefinitionCatalog.require(DisplayCpuKinds.LOGIC)
                                .medium(),
                        0,
                        64)));
        assertEquals(
                DisplayCpuKinds.BEHAVIOR,
                CoverDefinitionCatalog.require(DisplayCpuKinds.CONTROL)
                        .behaviorId());
        assertEquals(0, PipeCover.of(DisplayCpuKinds.STORAGE).config().visual());
        assertEquals(0, PipeCover.of(DisplayCpuKinds.STORAGE).config().redstone());
    }
}
