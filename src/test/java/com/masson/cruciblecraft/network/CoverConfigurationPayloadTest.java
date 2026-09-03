package com.masson.cruciblecraft.network;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;

import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class CoverConfigurationPayloadTest {
    @Test
    void handlerLeavesOnlyPureBoundsOnTheNetworkThread() throws Exception {
        String source = Files.readString(Path.of(
                "src/main/java/com/masson/cruciblecraft/network/"
                        + "CoverConfigurationPayload.java"));
        int handler = source.indexOf("private static void handle(");
        int enqueue = source.indexOf("context.enqueueWork", handler);
        int bounds = source.indexOf("withinBounds(", handler);
        assertTrue(handler >= 0 && enqueue > handler);
        assertTrue(bounds > handler && bounds < enqueue);
        for (String worldAccess : java.util.List.of(
                "context.player()",
                "player.level()",
                "player.blockPosition()",
                "getBlockEntity(")) {
            assertTrue(
                    source.indexOf(worldAccess, handler) > enqueue,
                    worldAccess + " must run in enqueueWork");
        }
    }

    @Test
    void packetRejectsEveryOrdinalAndValueOverflow() {
        int selector = CoverDefinition.ConfigField.SELECTOR.ordinal();
        assertTrue(CoverConfigurationPayload.withinBounds(
                5, selector, CoverDefinition.MAX_SELECTOR));
        assertFalse(CoverConfigurationPayload.withinBounds(
                6, selector, CoverDefinition.MAX_SELECTOR));
        assertFalse(CoverConfigurationPayload.withinBounds(
                5, selector, CoverDefinition.MAX_SELECTOR + 1));
        assertFalse(CoverConfigurationPayload.withinBounds(
                0,
                CoverDefinition.ConfigField.MATCH_ID.ordinal(),
                0));
        assertFalse(CoverConfigurationPayload.withinBounds(
                0,
                CoverDefinition.ConfigField.values().length,
                0));
        int networkId = CoverDefinition.ConfigField.NETWORK_ID.ordinal();
        assertTrue(CoverConfigurationPayload.withinBounds(
                0, networkId, 0));
        assertTrue(CoverConfigurationPayload.withinBounds(
                0, networkId, CoverDefinition.MAX_NETWORK_ID));
        assertFalse(CoverConfigurationPayload.withinBounds(
                0, networkId, CoverDefinition.MAX_NETWORK_ID + 1));
    }

    @Test
    void maximumConfigurationPayloadHasAThirteenByteHardBound() {
        CoverConfigurationPayload payload = new CoverConfigurationPayload(
                new BlockPos(Integer.MIN_VALUE, -20_000_000, Integer.MAX_VALUE),
                5,
                CoverDefinition.ConfigField.PRESSURE_THRESHOLD.ordinal(),
                CoverDefinition.MAX_PRESSURE_THRESHOLD);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        CoverConfigurationPayload.STREAM_CODEC.encode(buffer, payload);
        assertTrue(buffer.readableBytes()
                <= CoverConfigurationPayload.MAX_ENCODED_BYTES);
        assertTrue(CoverConfigurationPayload.withinBounds(
                payload.side(), payload.field(), payload.value()));
        buffer.release();
    }
}
