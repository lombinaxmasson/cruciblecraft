package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.masson.cruciblecraft.fluid.CrucibleTransferCoordinator.InsertResult;
import com.masson.cruciblecraft.fluid.CrucibleInteractionMessages;

import org.junit.jupiter.api.Test;

class CrucibleInteractionContractTest {
    @Test
    void insertReasonsMapToAccuratePlayerMessages() {
        assertEquals(
                "message.cruciblecraft.crucible_full",
                CrucibleInteractionMessages.key(InsertResult.FULL));
        assertEquals(
                "message.cruciblecraft.crucible_tier_too_low",
                CrucibleInteractionMessages.key(InsertResult.TIER_TOO_LOW));
        assertEquals(
                "message.cruciblecraft.inexact_material_amount",
                CrucibleInteractionMessages.key(InsertResult.INEXACT_DECOMPOSITION));
        assertEquals(
                "message.cruciblecraft.invalid_material",
                CrucibleInteractionMessages.key(InsertResult.INVALID_MATERIAL));
    }

    @Test
    void droppedCrucibleCarriesCasingOnly() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/block/CrucibleBlock.java"));
        String dropMethod = source.substring(
                source.indexOf("protected List<ItemStack> getDrops"),
                source.indexOf("@Nullable", source.indexOf("protected List<ItemStack> getDrops")));

        assertTrue(dropMethod.contains("ModComponents.MACHINE_MATERIAL"));
        assertFalse(dropMethod.contains("BLOCK_ENTITY_DATA"));
        assertFalse(dropMethod.contains("saveWithoutMetadata"));
    }
}
