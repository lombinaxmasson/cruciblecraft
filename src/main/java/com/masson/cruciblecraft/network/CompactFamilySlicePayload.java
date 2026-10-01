package com.masson.cruciblecraft.network;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.gt.CompactFamilyOnDemand;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** One slice of compact family holders for a single target map. */
public record CompactFamilySlicePayload(
        ResourceLocation targetMap,
        int index,
        int count,
        byte[] body) implements CustomPacketPayload {
    private static final String NETWORK_VERSION = "1";
    public static final Type<CompactFamilySlicePayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "compact_family_slice"));
    public static final StreamCodec<FriendlyByteBuf, CompactFamilySlicePayload>
            STREAM_CODEC = StreamCodec.of(
                    CompactFamilySlicePayload::encode,
                    CompactFamilySlicePayload::decode);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToClient(
                TYPE,
                STREAM_CODEC,
                CompactFamilySlicePayload::handle);
    }

    private static void encode(FriendlyByteBuf buffer, CompactFamilySlicePayload payload) {
        buffer.writeResourceLocation(payload.targetMap);
        buffer.writeVarInt(payload.index);
        buffer.writeVarInt(payload.count);
        buffer.writeByteArray(payload.body);
    }

    private static CompactFamilySlicePayload decode(FriendlyByteBuf buffer) {
        return new CompactFamilySlicePayload(
                buffer.readResourceLocation(),
                buffer.readVarInt(),
                buffer.readVarInt(),
                buffer.readByteArray(CompactFamilyOnDemand.MAX_SLICE_BODY_BYTES));
    }

    private static void handle(
            CompactFamilySlicePayload payload, IPayloadContext context) {
        context.enqueueWork(() -> {
            var player = context.player();
            if (!player.level().isClientSide()) {
                return;
            }
            CompactFamilyOnDemand.acceptSlice(
                    payload.targetMap,
                    payload.index,
                    payload.count,
                    payload.body,
                    player.level().registryAccess(),
                    player.level().getRecipeManager());
        }).exceptionally(failure -> {
            CrucibleCraft.LOGGER.warn(
                    "Compact family slice failed for {}",
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
