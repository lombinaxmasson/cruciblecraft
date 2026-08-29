package com.masson.cruciblecraft.network;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.Map;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.masson.cruciblecraft.material.MaterialFingerprint;
import com.masson.cruciblecraft.material.def.MaterialLoader;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;

class MaterialConfigurationHandshakeTest {
    @Test
    void productionScaleStructureRoundTripsBelowCap(@TempDir Path configDirectory) {
        var materials = MaterialLoader.load(configDirectory).values();
        Map<String, String> entries = MaterialFingerprint.structureEntries(
                materials,
                MaterialPrefixCatalog.definitions());
        assertEquals(
                materials.size() + MaterialPrefixCatalog.definitions().size(),
                entries.size());
        assertEquals(1_832, entries.size());
        org.junit.jupiter.api.Assertions.assertTrue(
                entries.size() < MaterialConfigurationHandshake.MAX_STRUCTURE_ENTRIES);
        var payload = new MaterialConfigurationHandshake.StructurePayload(
                MaterialFingerprint.structure(
                        materials,
                        MaterialPrefixCatalog.definitions()),
                entries);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            MaterialConfigurationHandshake.StructurePayload.STREAM_CODEC.encode(
                    buffer, payload);
            var decoded =
                    MaterialConfigurationHandshake.StructurePayload.STREAM_CODEC.decode(buffer);
            assertEquals(payload.fingerprint(), decoded.fingerprint());
            assertEquals(entries, decoded.materials());
        } finally {
            buffer.release();
        }
    }

    @Test
    void encodeRejectsOverCapBeforeWriting() {
        var payload = new MaterialConfigurationHandshake.StructurePayload(
                "too-large",
                entries(MaterialConfigurationHandshake.MAX_STRUCTURE_ENTRIES + 1));
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            var exception = assertThrows(
                    IllegalArgumentException.class,
                    () -> MaterialConfigurationHandshake.StructurePayload.STREAM_CODEC.encode(
                            buffer, payload));
            assertTrue(exception.getMessage().contains("actual=4097"));
            assertTrue(exception.getMessage().contains("cap=4096"));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test
    void validatesEntryCountAtRegistrationBoundary() {
        assertTrue(MaterialConfigurationHandshake.validateEntryCount(4_096));
        assertFalse(MaterialConfigurationHandshake.validateEntryCount(4_097));
    }

    @Test
    void mismatchMessageClassifiesAndSortsEveryDifference() {
        assertEquals(
                "missing locally [alpha]; extra locally [gamma]; "
                        + "different definitions [beta]",
                MaterialConfigurationHandshake.mismatchMessage(
                        Map.of("beta", "server", "alpha", "same"),
                        Map.of("gamma", "same", "beta", "client")));
    }

    @Test
    void decodeRejectsDuplicateMaterialIds() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf("fingerprint");
            buffer.writeVarInt(2);
            buffer.writeUtf("duplicate");
            buffer.writeUtf("first");
            buffer.writeUtf("duplicate");
            buffer.writeUtf("second");

            IllegalArgumentException failure = assertThrows(
                    IllegalArgumentException.class,
                    () -> MaterialConfigurationHandshake.StructurePayload.STREAM_CODEC.decode(
                            buffer));

            assertTrue(failure.getMessage().contains(
                    "Duplicate material id: duplicate"));
        } finally {
            buffer.release();
        }
    }

    private static Map<String, String> entries(int count) {
        LinkedHashMap<String, String> entries = new LinkedHashMap<>();
        for (int index = 0; index < count; index++) {
            entries.put("entry_" + index, "fingerprint_" + index);
        }
        return entries;
    }
}
