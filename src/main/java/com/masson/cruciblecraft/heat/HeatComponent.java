package com.masson.cruciblecraft.heat;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Snapshot used for lazily evaluated item temperature.
 *
 * <p>The component is only rewritten by thermal interactions (or removed once
 * fully cooled), so inventory stacks never generate per-tick component syncs.
 */
public record HeatComponent(float temperature, long lastUpdateTick) {
    public static final Codec<HeatComponent> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.FLOAT.fieldOf("temperature").forGetter(HeatComponent::temperature),
            Codec.LONG.fieldOf("last_update_tick").forGetter(HeatComponent::lastUpdateTick)
    ).apply(instance, HeatComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, HeatComponent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT,
            HeatComponent::temperature,
            ByteBufCodecs.VAR_LONG,
            HeatComponent::lastUpdateTick,
            HeatComponent::new);
}
