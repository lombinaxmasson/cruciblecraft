package com.masson.cruciblecraft.logistics.fluidnet;

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

class FluidNetworkKindsTest {
    @Test
    void twoKindsThreeDefinitionsAndGenericDumpStaysForbidden() {
        CoverBehaviorRegistry.validateDefinitions();
        assertTrue(CoverDefinitionCatalog.find(FluidNetworkKinds.STORAGE)
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(FluidNetworkKinds.IMPORT)
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(FluidNetworkKinds.EXPORT)
                .isPresent());
        assertTrue(CoverDefinitionCatalog.find(
                ResourceLocation.parse("cruciblecraft:logistics_generic_dump"))
                .isEmpty());
        assertFalse(ItemNetworkKinds.isForbiddenFrozenKind(
                FluidNetworkKinds.STORAGE));
        assertTrue(ItemNetworkKinds.isForbiddenFrozenKind(
                ResourceLocation.parse("cruciblecraft:logistics_generic_dump")));
        assertEquals(
                Optional.of(FluidNetworkKinds.TransferDirection.IMPORT),
                FluidNetworkKinds.direction(FluidNetworkKinds.IMPORT));
        assertEquals(
                Optional.of(FluidNetworkKinds.TransferDirection.EXPORT),
                FluidNetworkKinds.direction(FluidNetworkKinds.EXPORT));
        assertTrue(FluidNetworkKinds.direction(
                FluidNetworkKinds.STORAGE).isEmpty());
        assertFalse(FluidNetworkKinds.isJoined(0));
        assertTrue(FluidNetworkKinds.isJoined(1));
        assertTrue(FluidNetworkKinds.isJoined(CoverDefinition.MAX_NETWORK_ID));
        assertFalse(FluidNetworkKinds.isJoined(
                CoverDefinition.MAX_NETWORK_ID + 1));
        PipeCover unset = PipeCover.of(FluidNetworkKinds.EXPORT);
        assertEquals(0, FluidNetworkKinds.networkId(unset));
        PipeCover joined = unset.configure(
                CoverDefinition.ConfigField.NETWORK_ID, 4);
        assertEquals(4, FluidNetworkKinds.networkId(joined));
        assertEquals(4096, FluidNetworkLimits.MAX_VISITED_PIPES);
        assertEquals(256, FluidNetworkLimits.MAX_ENDPOINTS_PER_COMPONENT);
        assertEquals(1000, FluidNetworkLimits.DEFAULT_RATE);
        PipeCover export = PipeCover.of(FluidNetworkKinds.EXPORT).configure(
                CoverDefinition.ConfigField.NETWORK_ID, 1);
        CoverBehavior.Access access = new CoverBehavior.Access(
                CoverDefinition.Medium.FLUID, 0, 1000);
        assertFalse(export.behavior().allowsIncoming(
                export, export.definition().orElseThrow(), access));
        assertFalse(export.behavior().allowsOutgoing(
                export, export.definition().orElseThrow(), access));
    }
}
