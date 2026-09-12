package com.masson.cruciblecraft.network;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverDefinition;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Bounded play packet used by the minimal cover configuration surface. */
public record CoverConfigurationPayload(
        BlockPos position,
        int side,
        int field,
        int value) implements CustomPacketPayload {
    private static final String NETWORK_VERSION = "1";
    public static final int MAX_DISTANCE_SQUARED = 64;
    /** BlockPos long + side byte + field byte + bounded three-byte VarInt. */
    public static final int MAX_ENCODED_BYTES = 13;
    public static final Type<CoverConfigurationPayload> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "configure_cover"));
    public static final StreamCodec<FriendlyByteBuf, CoverConfigurationPayload>
            STREAM_CODEC = StreamCodec.of(
                    CoverConfigurationPayload::encode,
                    CoverConfigurationPayload::decode);

    public static void register(RegisterPayloadHandlersEvent event) {
        event.registrar(NETWORK_VERSION).playToServer(
                TYPE, STREAM_CODEC, CoverConfigurationPayload::handle);
    }

    public static boolean withinBounds(
            int side, int field, int value) {
        if (side < 0 || side >= Direction.values().length
                || field < 0
                || field >= CoverDefinition.ConfigField.values().length) {
            return false;
        }
        return switch (CoverDefinition.ConfigField.values()[field]) {
            case MATCH_ID -> false;
            case RATE -> value >= 0
                    && value <= CoverDefinition.MAX_FLUID_RATE;
            case PRESSURE_THRESHOLD -> value >= 0
                    && value <= CoverDefinition.MAX_PRESSURE_THRESHOLD;
            case EXACT_COUNT -> value >= 0
                    && value <= CoverDefinition.MAX_EXACT_COUNT;
            case MODE -> value >= 0
                    && value < CoverDefinition.TransferMode.values().length;
            case SELECTOR -> value >= 0
                    && value <= CoverDefinition.MAX_SELECTOR;
            case NETWORK_ID -> value >= 0
                    && value <= CoverDefinition.MAX_NETWORK_ID;
        };
    }

    private static void encode(
            FriendlyByteBuf buffer, CoverConfigurationPayload payload) {
        buffer.writeBlockPos(payload.position);
        buffer.writeByte(payload.side);
        buffer.writeByte(payload.field);
        buffer.writeVarInt(payload.value);
    }

    private static CoverConfigurationPayload decode(FriendlyByteBuf buffer) {
        return new CoverConfigurationPayload(
                buffer.readBlockPos(),
                buffer.readUnsignedByte(),
                buffer.readUnsignedByte(),
                buffer.readVarInt());
    }

    private static void handle(
            CoverConfigurationPayload payload, IPayloadContext context) {
        if (!withinBounds(payload.side, payload.field, payload.value)) {
            return;
        }
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) {
                return;
            }
            var level = player.level();
            if (player.blockPosition().distSqr(payload.position)
                    > MAX_DISTANCE_SQUARED) {
                return;
            }
            var blockEntity = level.getBlockEntity(payload.position);
            Direction side = Direction.values()[payload.side];
            CoverDefinition.ConfigField field =
                    CoverDefinition.ConfigField.values()[payload.field];
            if (blockEntity instanceof ItemPipeBlockEntity pipe) {
                pipe.configureCover(side, field, payload.value);
            } else if (blockEntity instanceof FluidPipeBlockEntity pipe) {
                pipe.configureCover(side, field, payload.value);
            } else if (blockEntity instanceof MachineCoverHost machine) {
                machine.configureCover(side, field, payload.value);
            }
        }).exceptionally(failure -> {
            CrucibleCraft.LOGGER.warn(
                    "Cover configuration payload failed", failure);
            return null;
        });
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
