package com.masson.cruciblecraft.logistics.itemnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class ItemNetworkKindsTest {
    @Test
    void twoKindsThreeDefinitionsAndFailClosedFrozenIds() {
        CoverBehaviorRegistry.validateDefinitions();
        assertTrue(CoverDefinitionCatalog.find(ItemNetworkKinds.STORAGE)
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(ItemNetworkKinds.IMPORT)
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(ItemNetworkKinds.EXPORT)
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(
                ResourceLocation.parse("cruciblecraft:logistics_fluid_storage"))
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(
                ResourceLocation.parse("cruciblecraft:logistics_generic_dump"))
                .isEmpty());
        assertFalse(ItemNetworkKinds.isForbiddenFrozenKind(
                ResourceLocation.parse("cruciblecraft:logistics_fluid_storage")));
        assertTrue(ItemNetworkKinds.isForbiddenFrozenKind(
                ResourceLocation.parse("cruciblecraft:logistics_generic_dump")));
        assertFalse(ItemNetworkKinds.isForbiddenFrozenKind(
                ItemNetworkKinds.STORAGE));
        assertEquals(
                Optional.of(ItemNetworkKinds.TransferDirection.IMPORT),
                ItemNetworkKinds.direction(ItemNetworkKinds.IMPORT));
        assertEquals(
                Optional.of(ItemNetworkKinds.TransferDirection.EXPORT),
                ItemNetworkKinds.direction(ItemNetworkKinds.EXPORT));
        assertTrue(ItemNetworkKinds.direction(
                ItemNetworkKinds.STORAGE).isEmpty());
        assertTrue(ItemNetworkKinds.direction(
                ResourceLocation.parse("cruciblecraft:unknown_direction"))
                .isEmpty());
        assertFalse(ItemNetworkKinds.isJoined(0));
        assertTrue(ItemNetworkKinds.isJoined(1));
        assertTrue(ItemNetworkKinds.isJoined(CoverDefinition.MAX_NETWORK_ID));
        assertFalse(ItemNetworkKinds.isJoined(
                CoverDefinition.MAX_NETWORK_ID + 1));
        PipeCover unset = PipeCover.of(ItemNetworkKinds.EXPORT);
        assertEquals(0, ItemNetworkKinds.networkId(unset));
        PipeCover joined = unset.configure(
                CoverDefinition.ConfigField.NETWORK_ID, 4);
        assertEquals(4, ItemNetworkKinds.networkId(joined));
        assertEquals(4096, ItemNetworkLimits.MAX_VISITED_PIPES);
        assertEquals(256, ItemNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT);
        assertEquals(8, ItemNetworkLimits.DEFAULT_RATE);
    }
}
