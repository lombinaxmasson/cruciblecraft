package com.masson.cruciblecraft.machine;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

public record MachineDurabilityComponent(long current, long max) {
    public static final Codec<MachineDurabilityComponent> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.LONG.fieldOf("current").forGetter(MachineDurabilityComponent::current),
                    Codec.LONG.fieldOf("max").forGetter(MachineDurabilityComponent::max)
            ).apply(instance, MachineDurabilityComponent::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, MachineDurabilityComponent> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.VAR_LONG,
                    MachineDurabilityComponent::current,
                    ByteBufCodecs.VAR_LONG,
                    MachineDurabilityComponent::max,
                    MachineDurabilityComponent::new);

    public MachineDurabilityComponent {
        if (max <= 0 || current < 0 || current > max) {
            throw new IllegalArgumentException("Invalid machine durability " + current + "/" + max);
        }
    }
}
