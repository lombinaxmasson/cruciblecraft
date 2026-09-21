package com.masson.cruciblecraft.energy.lightningrod;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.minecraft.util.RandomSource;

import org.junit.jupiter.api.Test;

class LightningRodLogicTest {
    @Test
    void tipUsesWorldYNotControllerPlusSize() {
        assertFalse(LightningRodLogic.tipHighEnough(99));
        assertTrue(LightningRodLogic.tipHighEnough(100));
        assertEquals(
                100,
                LightningRodStructure.tip(
                        net.minecraft.core.BlockPos.ZERO.offset(0, 95, 0), 1)
                        .getY());
        assertEquals(
                99,
                LightningRodStructure.tip(
                        net.minecraft.core.BlockPos.ZERO.offset(0, 95, 0), 0)
                        .getY());
    }

    @Test
    void sourceUsesGt6TickPhaseAndHeightGate() {
        assertFalse(LightningRodLogic.tickDue(299L));
        assertTrue(LightningRodLogic.tickDue(300L));
        assertTrue(LightningRodLogic.tickDue(1_500L));
        assertFalse(LightningRodLogic.sourceHeightHighEnough(95, 4));
        assertTrue(LightningRodLogic.sourceHeightHighEnough(95, 5));
        assertEquals(100, LightningRodLogic.MAX_ACTIVE_SIZE);
    }

    @Test
    void weatherNeedsThunderOrRareRain() {
        assertFalse(LightningRodLogic.weatherRoll(
                RandomSource.create(1L), 0, true, true));
        assertFalse(LightningRodLogic.weatherRoll(
                RandomSource.create(1L), 8, false, false));
    }

    @Test
    void competitionIsUniformAmongNearbyRods() {
        assertFalse(LightningRodLogic.winsCompetition(RandomSource.create(1L), 0));
        assertTrue(LightningRodLogic.winsCompetition(RandomSource.create(1L), 1));
        assertEquals(256, LightningRodLogic.COMPETITION_RANGE);
        assertEquals(32_768L, LightningRodLogic.PACKET);
        assertEquals(16, LightningRodLogic.AMPS);
        assertEquals(18_000L * 32_768L, LightningRodLogic.CAPACITY);
    }

    @Test
    void emitDrainsAcceptedPackets() {
        assertEquals(0L, LightningRodLogic.emitDrain(100L, 1L));
        assertEquals(
                LightningRodLogic.CAPACITY - LightningRodLogic.PACKET * 2L,
                LightningRodLogic.emitDrain(LightningRodLogic.CAPACITY, 2L));
        assertEquals(
                LightningRodLogic.CAPACITY - LightningRodLogic.PACKET,
                LightningRodLogic.emitDrain(LightningRodLogic.CAPACITY, 0L));
    }
}
