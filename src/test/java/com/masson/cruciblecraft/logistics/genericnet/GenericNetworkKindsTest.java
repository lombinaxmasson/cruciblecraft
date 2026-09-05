package com.masson.cruciblecraft.logistics.genericnet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import com.masson.cruciblecraft.logistics.itemnet.ItemNetworkKinds;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehavior;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverBehaviorRegistry;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinitionCatalog;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;

import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class GenericNetworkKindsTest {
    @Test
    void twoKindsThreeDefinitionsAndDumpIsRegisteredOnCore() {
        CoverBehaviorRegistry.validateDefinitions();
        assertTrue(CoverDefinitionCatalog.find(GenericNetworkKinds.STORAGE)
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(GenericNetworkKinds.IMPORT)
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(GenericNetworkKinds.EXPORT)
                .isPresent());
        assertEquals(
                CoverDefinition.Medium.BOTH,
                CoverDefinitionCatalog.find(GenericNetworkKinds.STORAGE)
                        .orElseThrow()
                        .medium());
        assertTrue(CoverDefinitionCatalog.find(
                ResourceLocation.parse("cruciblecraft:logistics_generic_dump"))
                .isPresent());
        assertFalse(ItemNetworkKinds.isForbiddenFrozenKind(
                GenericNetworkKinds.STORAGE));
        assertFalse(ItemNetworkKinds.isForbiddenFrozenKind(
                ResourceLocation.parse("cruciblecraft:logistics_generic_dump")));
        assertFalse(ItemNetworkKinds.isForbiddenFrozenKind(
                ResourceLocation.parse(
                        "cruciblecraft:logistics_display_cpu_logic")));
        assertTrue(ItemNetworkKinds.isForbiddenFrozenKind(
                ResourceLocation.parse("cruciblecraft:logistics_battery")));
        assertEquals(
                Optional.of(GenericNetworkKinds.TransferDirection.IMPORT),
                GenericNetworkKinds.direction(GenericNetworkKinds.IMPORT));
        assertEquals(
                Optional.of(GenericNetworkKinds.TransferDirection.EXPORT),
                GenericNetworkKinds.direction(GenericNetworkKinds.EXPORT));
        assertTrue(GenericNetworkKinds.direction(
                GenericNetworkKinds.STORAGE).isEmpty());
        assertFalse(GenericNetworkKinds.isJoined(0));
        assertTrue(GenericNetworkKinds.isJoined(1));
        assertTrue(GenericNetworkKinds.isJoined(CoverDefinition.MAX_NETWORK_ID));
        assertFalse(GenericNetworkKinds.isJoined(
                CoverDefinition.MAX_NETWORK_ID + 1));
        PipeCover unset = PipeCover.of(GenericNetworkKinds.EXPORT);
        assertEquals(0, GenericNetworkKinds.networkId(unset));
        PipeCover joined = unset.configure(
                CoverDefinition.ConfigField.NETWORK_ID, 4);
        assertEquals(4, GenericNetworkKinds.networkId(joined));
        assertEquals(4096, GenericNetworkLimits.MAX_VISITED_PIPES);
        assertEquals(256, GenericNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT);
        assertEquals(8, GenericNetworkLimits.DEFAULT_ITEM_RATE);
        assertEquals(1000, GenericNetworkLimits.DEFAULT_FLUID_RATE);
        PipeCover export = PipeCover.of(GenericNetworkKinds.EXPORT).configure(
                CoverDefinition.ConfigField.NETWORK_ID, 1);
        CoverBehavior.Access access = new CoverBehavior.Access(
                CoverDefinition.Medium.ITEM, 0, 64);
        assertFalse(export.behavior().allowsIncoming(
                export, export.definition().orElseThrow(), access));
        assertFalse(export.behavior().allowsOutgoing(
                export, export.definition().orElseThrow(), access));
    }
}
