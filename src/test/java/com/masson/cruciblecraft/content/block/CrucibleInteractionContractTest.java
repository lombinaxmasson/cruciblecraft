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

    @Test
    void fluidTransferIsServerGuardedAndPrecedesServerMaterialInsertion() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/CruciblePlayerInteraction.java"));
        String method = source.substring(
                source.indexOf("public static ItemInteractionResult useItemOn"),
                source.indexOf("public static InteractionResult useEmpty"));

        int clientGuard = method.indexOf("if (level.isClientSide)");
        int clientReturn = method.indexOf("return predictsItemUse");
        int fluidInteraction = method.indexOf("FluidUtil.interactWithFluidHandler");
        int serverMaterialResolution = method.indexOf(
                "Optional<MaterialUnits.Entry> material", fluidInteraction);

        assertTrue(clientGuard >= 0);
        assertTrue(clientGuard < clientReturn);
        assertTrue(clientReturn < fluidInteraction);
        assertTrue(fluidInteraction < serverMaterialResolution);
        assertEquals(
                fluidInteraction,
                method.lastIndexOf("FluidUtil.interactWithFluidHandler"),
                "the authoritative transfer helper must be called exactly once");
    }

    @Test
    void publicAirInjectionResultsExposeOnlyTerminalOutcomes() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/content/blockentity/CrucibleBlockEntity.java"));
        int enumStart = source.indexOf("public enum AirInjectionResult");
        String enumBody = source.substring(enumStart, source.indexOf('}', enumStart));

        assertTrue(enumBody.contains("STARTED"));
        assertTrue(enumBody.contains("CONTINUED"));
        assertTrue(enumBody.contains("TOO_COLD"));
        assertTrue(enumBody.contains("INVALID_CHARGE"));
        assertFalse(enumBody.contains("ACCEPTABLE"));
    }

    @Test
    void otherFluidBlocksAlsoGuardAuthoritativeInteractionOnServer() throws Exception {
        for (String block : java.util.List.of("BoilerBlock.java", "SteamEngineBlock.java")) {
            String source = Files.readString(Path.of(
                    "src/main/java/com/masson/cruciblecraft/content/block/" + block));
            int clientGuard = source.indexOf("if (level.isClientSide)");
            int interaction = source.indexOf("FluidUtil.interactWithFluidHandler");
            assertTrue(clientGuard >= 0 && clientGuard < interaction, block);
        }
    }
}
