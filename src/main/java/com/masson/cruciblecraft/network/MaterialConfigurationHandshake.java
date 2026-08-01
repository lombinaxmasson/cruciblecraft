package com.masson.cruciblecraft.network;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.function.Consumer;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.MaterialFingerprint;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.network.ConfigurationTask;
import net.neoforged.neoforge.network.configuration.ICustomConfigurationTask;
import net.neoforged.neoforge.network.event.RegisterConfigurationTasksEvent;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public final class MaterialConfigurationHandshake {
    private static final String NETWORK_VERSION = "1";
    public static final int MAX_STRUCTURE_ENTRIES = 4_096;

    private MaterialConfigurationHandshake() {}

    public static void registerPayloads(RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(NETWORK_VERSION);
        registrar.configurationToClient(
                StructurePayload.TYPE,
                StructurePayload.STREAM_CODEC,
                MaterialConfigurationHandshake::handleStructure);
        registrar.configurationToServer(
                AcknowledgementPayload.TYPE,
                AcknowledgementPayload.STREAM_CODEC,
                MaterialConfigurationHandshake::handleAcknowledgement);
    }

    public static void registerTask(RegisterConfigurationTasksEvent event) {
        if (event.getListener().getConnection().isMemoryConnection()) {
            return;
        }
        if (!event.getListener().hasChannel(StructurePayload.TYPE)) {
            event.getListener().disconnect(Component.translatable(
                    "disconnect.cruciblecraft.material_handshake_missing"));
            return;
        }
        StructurePayload payload = currentPayload();
        int entryCount = payload.materials().size();
        if (!validateEntryCount(entryCount)) {
            CrucibleCraft.LOGGER.error(
                    "Material configuration handshake has {} entries, exceeding cap {}; "
                            + "disconnecting before payload registration",
                    entryCount,
                    MAX_STRUCTURE_ENTRIES);
            event.getListener().disconnect(Component.translatable(
                    "disconnect.cruciblecraft.material_handshake_too_large",
                    entryCount,
                    MAX_STRUCTURE_ENTRIES));
            return;
        }
        event.register(new MaterialStructureTask(payload));
    }

    private static StructurePayload currentPayload() {
        return new StructurePayload(
                MaterialFingerprint.structure(MaterialCatalog.values()),
                MaterialFingerprint.structureEntries(MaterialCatalog.values()));
    }

    private static void handleStructure(StructurePayload server, IPayloadContext context) {
        StructurePayload client = currentPayload();
        if (server.fingerprint().equals(client.fingerprint())) {
            context.reply(AcknowledgementPayload.INSTANCE);
            return;
        }
        String differences = mismatchMessage(
                server.materials(),
                client.materials());
        CrucibleCraft.LOGGER.warn("Material configuration handshake rejected: {}", differences);
        context.disconnect(Component.translatable(
                "disconnect.cruciblecraft.material_mismatch",
                differences));
    }

    private static void handleAcknowledgement(
            AcknowledgementPayload payload,
            IPayloadContext context) {
        context.finishCurrentTask(MaterialStructureTask.TYPE);
    }

    static String mismatchMessage(
            Map<String, String> server,
            Map<String, String> client) {
        TreeSet<String> missing = new TreeSet<>(server.keySet());
        missing.removeAll(client.keySet());
        TreeSet<String> extra = new TreeSet<>(client.keySet());
        extra.removeAll(server.keySet());
        TreeSet<String> changed = new TreeSet<>(server.keySet());
        changed.retainAll(client.keySet());
        changed.removeIf(id -> server.get(id).equals(client.get(id)));

        List<String> differences = new ArrayList<>();
        appendDifference(differences, "missing locally", missing);
        appendDifference(differences, "extra locally", extra);
        appendDifference(differences, "different definitions", changed);
        return String.join("; ", differences);
    }

    static boolean validateEntryCount(int count) {
        return count >= 0 && count <= MAX_STRUCTURE_ENTRIES;
    }

    private static void appendDifference(
            List<String> output,
            String label,
            TreeSet<String> ids) {
        if (!ids.isEmpty()) {
            output.add(label + " [" + String.join(", ", ids) + "]");
        }
    }

    public record StructurePayload(
            String fingerprint,
            Map<String, String> materials) implements CustomPacketPayload {
        public static final Type<StructurePayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID,
                        "material_structure"));
        public static final StreamCodec<FriendlyByteBuf, StructurePayload> STREAM_CODEC =
                StreamCodec.of(StructurePayload::encode, StructurePayload::decode);

        public StructurePayload {
            fingerprint = fingerprint == null ? "" : fingerprint;
            materials = Map.copyOf(materials);
        }

        private static void encode(FriendlyByteBuf buffer, StructurePayload payload) {
            List<Map.Entry<String, String>> entries = payload.materials.entrySet().stream()
                    .sorted(Map.Entry.comparingByKey())
                    .toList();
            if (!validateEntryCount(entries.size())) {
                throw new IllegalArgumentException(
                        "Invalid material fingerprint count: actual=" + entries.size()
                                + ", cap=" + MAX_STRUCTURE_ENTRIES);
            }
            buffer.writeUtf(payload.fingerprint);
            buffer.writeVarInt(entries.size());
            for (Map.Entry<String, String> entry : entries) {
                buffer.writeUtf(entry.getKey());
                buffer.writeUtf(entry.getValue());
            }
        }

        private static StructurePayload decode(FriendlyByteBuf buffer) {
            String fingerprint = buffer.readUtf();
            int count = buffer.readVarInt();
            if (!validateEntryCount(count)) {
                throw new IllegalArgumentException(
                        "Invalid material fingerprint count: actual=" + count
                                + ", cap=" + MAX_STRUCTURE_ENTRIES);
            }
            LinkedHashMap<String, String> materials = new LinkedHashMap<>();
            for (int index = 0; index < count; index++) {
                String id = buffer.readUtf();
                String previous = materials.put(id, buffer.readUtf());
                if (previous != null) {
                    throw new IllegalArgumentException("Duplicate material id: " + id);
                }
            }
            return new StructurePayload(fingerprint, materials);
        }

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    public record AcknowledgementPayload() implements CustomPacketPayload {
        public static final AcknowledgementPayload INSTANCE = new AcknowledgementPayload();
        public static final Type<AcknowledgementPayload> TYPE = new Type<>(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID,
                        "material_structure_ack"));
        public static final StreamCodec<FriendlyByteBuf, AcknowledgementPayload> STREAM_CODEC =
                StreamCodec.unit(INSTANCE);

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }
    }

    private record MaterialStructureTask(StructurePayload payload)
            implements ICustomConfigurationTask {
        private static final ConfigurationTask.Type TYPE = new ConfigurationTask.Type(
                ResourceLocation.fromNamespaceAndPath(
                        CrucibleCraft.MODID,
                        "check_material_structure"));

        @Override
        public void run(Consumer<CustomPacketPayload> sender) {
            sender.accept(payload);
        }

        @Override
        public ConfigurationTask.Type type() {
            return TYPE;
        }
    }
}
