package com.masson.cruciblecraft.recipe.gt;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

class CompactPublicationGroupsTest {
    static {
        MinecraftTestBootstrap.bootstrap();
    }
    @Test
    void mixerOrdinaryUsesGt6PanelEnvelope() {
        assertEquals(
                CompactPublicationGroups.ENVELOPE_GT6_PANEL,
                CompactPublicationGroups.executionEnvelope(
                        CompactPublicationGroups.MIXER_ORDINARY_OPAQUE));
        assertEquals(
                CompactPublicationGroups.ENVELOPE_GT6_PANEL,
                CompactPublicationGroups.executionEnvelope(
                        CompactPublicationGroups.MIXER_ORDINARY_CONSTRUCTION_FOAM));
        assertEquals(64_000, CompactPublicationGroups.GT6_PANEL_TANK_CAPACITY);
        assertEquals(32_000, CompactPublicationGroups.BRONZE_TANK_CAPACITY);
    }

    @Test
    void smelterOrdinaryUsesBronzeEnvelopeUntilPanelIsDeclared() {
        assertEquals(
                CompactPublicationGroups.ENVELOPE_BRONZE,
                CompactPublicationGroups.executionEnvelope(
                        CompactPublicationGroups.SMELTER_ORDINARY_SINGLETON));
    }
}
