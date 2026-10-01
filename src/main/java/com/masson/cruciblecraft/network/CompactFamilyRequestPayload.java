package com.masson.cruciblecraft.network;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.gt.CompactFamilyOnDemand;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Client asks for compact families that target one recipe map. */
public record CompactFamilyRequestPayload(ResourceLocation targetMap)
        implements CustomPacketPayload {
    private static final String NETWORK_VERSION = "1";
    public static final Type<CompactFamilyRequestPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "compact_family_request"));
    public static final StreamCodec<FriendlyByteBuf, CompactFamilyRequestPayload>
            STREAM_CODEC = StreamCodec.of(
                    CompactFamilyRequestPayload::encode,
                    CompactFamilyRequestPayload::decode);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToServer(
                TYPE,
                STREAM_CODEC,
                CompactFamilyRequestPayload::handle);
    }

    private static void encode(
            FriendlyByteBuf buffer, CompactFamilyRequestPayload payload) {
        buffer.writeResourceLocation(payload.targetMap);
    }

    private static CompactFamilyRequestPayload decode(FriendlyByteBuf buffer) {
        return new CompactFamilyRequestPayload(buffer.readResourceLocation());
    }

    private static void handle(
            CompactFamilyRequestPayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                CompactFamilyOnDemand.send(player, payload.targetMap);
            }
        }).exceptionally(failure -> {
            CrucibleCraft.LOGGER.warn(
                    "Compact family request failed for {}",
                    payload.targetMap,
                    failure);
            return null;
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
