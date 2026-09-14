package com.masson.cruciblecraft.energy.longdistance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class LongDistanceTransformerCatalogTest {
    @Test
    void catalogLocksFiveSourceBackedEndpoints() {
        assertEquals(5, LongDistanceTransformerCatalog.endpoints().size());
        assertEquals(5, LongDistanceTransformerCatalog.wires().size());
        LongDistanceTransformerProfile ev =
                LongDistanceTransformerCatalog.requireEndpoint(
                        net.minecraft.resources.ResourceLocation.parse(
                                "cruciblecraft:long_distance_transformer_ev"));
        assertEquals(10064, ev.sourceId());
        assertEquals(2048L, ev.voltage());
        assertEquals("chromium", ev.material());
        assertEquals(
                "cruciblecraft:electric_transformer_ev_iv",
                ev.hostTransformer().toString());
        LongDistanceTransformerProfile uv =
                LongDistanceTransformerCatalog.requireEndpoint(
                        net.minecraft.resources.ResourceLocation.parse(
                                "cruciblecraft:long_distance_transformer_uv"));
        assertEquals(10068, uv.sourceId());
        assertEquals(524288L, uv.voltage());
        assertTrue(LongDistanceTransformerCatalog.endpoints().stream().noneMatch(
                profile -> profile.sourceId() < 10064
                        || profile.sourceId() > 10068));
        LongDistanceWireProfile graphene =
                LongDistanceTransformerCatalog.requireWire(
                        net.minecraft.resources.ResourceLocation.parse(
                                "cruciblecraft:long_distance_wire_uv"));
        assertEquals("graphene", graphene.core());
        assertEquals(524288L, graphene.voltage());
        assertNotNull(LongDistanceTransformerCatalogTest.class.getResource(
                "/data/cruciblecraft/long_distance_transformers.json"));
    }
}
