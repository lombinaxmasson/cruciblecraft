package com.masson.cruciblecraft.nuclear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Stack-local GT6 reactor-rod NBT: durability, moderation, moderator hits. */
public record ReactorRodState(
        long durability,
        boolean moderated,
        boolean previouslyModerated,
        int moderationHits,
        int previousModerationHits) {
    public static final Codec<ReactorRodState> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.fieldOf("durability")
                            .forGetter(ReactorRodState::durability),
                    Codec.BOOL.fieldOf("moderated")
                            .forGetter(ReactorRodState::moderated),
                    Codec.BOOL.fieldOf("previously_moderated")
                            .forGetter(ReactorRodState::previouslyModerated),
                    Codec.INT.fieldOf("moderation_hits")
                            .forGetter(ReactorRodState::moderationHits),
                    Codec.INT.optionalFieldOf("previous_moderation_hits", 0)
                            .forGetter(ReactorRodState::previousModerationHits)
            ).apply(instance, ReactorRodState::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReactorRodState>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    ReactorRodState::durability,
                    ByteBufCodecs.BOOL,
                    ReactorRodState::moderated,
                    ByteBufCodecs.BOOL,
                    ReactorRodState::previouslyModerated,
                    ByteBufCodecs.VAR_INT,
                    ReactorRodState::moderationHits,
                    ByteBufCodecs.VAR_INT,
                    ReactorRodState::previousModerationHits,
                    ReactorRodState::new);

    public static ReactorRodState fresh(long durability) {
        return new ReactorRodState(
                Math.max(0L, durability), false, false, 0, 0);
    }

    public ReactorRodState withDurability(long value) {
        return new ReactorRodState(
                value,
                moderated,
                previouslyModerated,
                moderationHits,
                previousModerationHits);
    }

    public ReactorRodState markModerated() {
        return new ReactorRodState(
                durability,
                true,
                previouslyModerated,
                moderationHits,
                previousModerationHits);
    }

    public ReactorRodState incrementModeration() {
        return new ReactorRodState(
                durability,
                moderated,
                previouslyModerated,
                moderationHits + 1,
                previousModerationHits);
    }

    public ReactorRodState cycleModeration() {
        return new ReactorRodState(durability, false, moderated, 0, 0);
    }

    public ReactorRodState snapshotModeratorHits() {
        return new ReactorRodState(
                durability, moderated, previouslyModerated, 0, moderationHits);
    }
}

