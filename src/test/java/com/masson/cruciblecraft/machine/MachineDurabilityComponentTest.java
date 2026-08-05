package com.masson.cruciblecraft.machine;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;

import org.junit.jupiter.api.Test;

class MachineDurabilityComponentTest {
    @Test
    void acceptsOnlyClosedDurabilityRangeWithPositiveMaximum() {
        assertEquals(
                new MachineDurabilityComponent(0L, 1L),
                new MachineDurabilityComponent(0L, 1L));
        assertEquals(
                new MachineDurabilityComponent(9L, 9L),
                new MachineDurabilityComponent(9L, 9L));

        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineDurabilityComponent(0L, 0L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineDurabilityComponent(-1L, 10L));
        assertThrows(
                IllegalArgumentException.class,
                () -> new MachineDurabilityComponent(11L, 10L));
    }

    @Test
    void persistentCodecRoundTripsAndRejectsInvalidSavedData() {
        MachineDurabilityComponent original =
                new MachineDurabilityComponent(37L, 100L);
        var encoded = MachineDurabilityComponent.CODEC
                .encodeStart(JsonOps.INSTANCE, original)
                .getOrThrow();
        assertEquals(
                original,
                MachineDurabilityComponent.CODEC
                        .parse(JsonOps.INSTANCE, encoded)
                        .getOrThrow());

        JsonObject invalid = new JsonObject();
        invalid.addProperty("current", 0L);
        invalid.addProperty("max", 0L);
        assertThrows(
                IllegalArgumentException.class,
                () -> MachineDurabilityComponent.CODEC
                        .parse(JsonOps.INSTANCE, invalid)
                        .getOrThrow());
    }

    @Test
    void networkCodecRoundTrips() {
        MachineDurabilityComponent original =
                new MachineDurabilityComponent(1234L, 9999L);
        RegistryFriendlyByteBuf buffer =
                new RegistryFriendlyByteBuf(Unpooled.buffer(), RegistryAccess.EMPTY);

        MachineDurabilityComponent.STREAM_CODEC.encode(buffer, original);

        assertEquals(
                original,
                MachineDurabilityComponent.STREAM_CODEC.decode(buffer));
        assertEquals(0, buffer.readableBytes());
    }
}
