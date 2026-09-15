package com.masson.cruciblecraft.machine.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.api.unit.MaterialUnits;
import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.material.MaterialCatalog;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.neoforged.fml.loading.LoadingModList;

class CrucibleProcessCoreTest {
    @BeforeAll
    static void bootstrapCatalog(@TempDir Path configDirectory) {
        LoadingModList.of(List.of(), List.of(), List.of(), List.of(), Map.of());
        SharedConstants.tryDetectVersion();
        Bootstrap.bootStrap();
        if (!MaterialCatalog.isBootstrapped()) {
            MaterialCatalog.bootstrap(configDirectory);
        }
    }

    @Test
    void singleBlockCapacityStaysSixteenIngots() {
        CrucibleProcessCore core = CrucibleProcessCore.singleBlock();
        assertEquals(CrucibleProcessCore.SINGLE_BLOCK_MAX_INGOTS, core.maxIngots());
        assertEquals(
                MaterialPrefixes.INGOT.units() * 16,
                core.maxUnits());
        assertEquals(InsertResult.SUCCESS, fillIngots(core, 16));
        assertEquals(InsertResult.FULL, fillIngots(core, 1));
        assertEquals(core.maxUnits(), core.totalUnits());
    }

    @Test
    void largeCapacityAccepts432AndRejects433() {
        CrucibleProcessCore core = CrucibleProcessCore.large();
        assertEquals(432, core.maxIngots());
        assertEquals(
                MaterialPrefixes.INGOT.units() * 432,
                core.maxUnits());
        assertEquals(InsertResult.SUCCESS, fillIngots(core, 432));
        assertEquals(InsertResult.FULL, fillIngots(core, 1));
        assertEquals(core.maxUnits(), core.totalUnits());
    }

    @Test
    void freezeStopsInsertionWithoutClearingContents() {
        CrucibleProcessCore core = CrucibleProcessCore.large();
        assertEquals(InsertResult.SUCCESS, fillIngots(core, 4));
        int units = core.totalUnits();
        core.setFrozen(true);
        assertEquals(InsertResult.INVALID_MATERIAL, fillIngots(core, 1));
        assertEquals(units, core.totalUnits());
        assertTrue(core.cast(MaterialPrefixes.INGOT).isEmpty());
    }

    @Test
    void singleBlockAndLargeDoNotShareCapacity() {
        assertNotEquals(
                CrucibleProcessCore.singleBlock().maxUnits(),
                CrucibleProcessCore.large().maxUnits());
    }

    private static InsertResult fillIngots(CrucibleProcessCore core, int count) {
        return core.insert(
                new MaterialUnits.Entry(
                        "iron",
                        MaterialPrefixes.INGOT,
                        Math.multiplyExact(MaterialPrefixes.INGOT.units(), count)),
                CrucibleProcessCore.AMBIENT_TEMPERATURE);
    }
}
