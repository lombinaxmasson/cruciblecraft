package com.masson.cruciblecraft.registry;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.IdentityHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

class ModMenusTest {
    @Test
    void everyConfiguredMachineHasAMenu() {
        for (var spec : ModMenus.menuHostSpecs()) {
            assertNotNull(ModMenus.forMachine(spec), spec.id().toString());
        }
    }

    @Test
    void mappingCountMatchesConfiguredMachines() {
        assertEquals(
                ModMenus.menuHostSpecs().size(),
                ModMenus.processingMenuCount());
    }

    @Test
    void unknownMachineFailsLoudly() {
        IllegalArgumentException failure = assertThrows(
                IllegalArgumentException.class,
                () -> ModMenus.forMachine(ModProcessingMachines.CRUSHER));

        assertTrue(failure.getMessage().contains(
                ModProcessingMachines.CRUSHER.id().toString()));
    }

    @Test
    void bidirectionalValidationReportsMissingAndExtraSpecs() {
        Map<com.masson.cruciblecraft.machine.processing.ProcessingMachineSpec, Object>
                mappings = new IdentityHashMap<>();
        mappings.put(ModProcessingMachines.CRUSHER, new Object());

        IllegalStateException failure = assertThrows(
                IllegalStateException.class,
                () -> ModMenus.validateProcessingMenuMapping(
                        ModProcessingMachines.CONFIGURED_MACHINES,
                        mappings));

        assertTrue(failure.getMessage().contains("missing="));
        assertTrue(failure.getMessage().contains("extra="));
        assertTrue(failure.getMessage().contains(
                ModProcessingMachines.CRUSHER.id().toString()));
    }
}
