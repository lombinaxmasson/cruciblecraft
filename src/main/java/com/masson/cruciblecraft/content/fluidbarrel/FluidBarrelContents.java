package com.masson.cruciblecraft.content.fluidbarrel;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Item snapshot of a barrel. The ferment recipe and its max time are not
 * stored; they are recomputed while the sealed timer is kept.
 */
public record FluidBarrelContents(
        String fluidId,
        long amount,
        boolean autoOutput,
        boolean sealed,
        long sealedTime) {
    public static final FluidBarrelContents EMPTY =
            new FluidBarrelContents("", 0L, false, false, 0L);

    public static final Codec<FluidBarrelContents> CODEC =
            RecordCodecBuilder.create(instance -> instance.group(
                    Codec.STRING.optionalFieldOf("fluid", "")
                            .forGetter(FluidBarrelContents::fluidId),
                    Codec.LONG.optionalFieldOf("amount", 0L)
                            .forGetter(FluidBarrelContents::amount),
                    Codec.BOOL.optionalFieldOf("auto_output", false)
                            .forGetter(FluidBarrelContents::autoOutput),
                    Codec.BOOL.optionalFieldOf("sealed", false)
                            .forGetter(FluidBarrelContents::sealed),
                    Codec.LONG.optionalFieldOf("sealed_time", 0L)
                            .forGetter(FluidBarrelContents::sealedTime)
            ).apply(instance, FluidBarrelContents::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FluidBarrelContents>
            STREAM_CODEC = StreamCodec.composite(
                    ByteBufCodecs.STRING_UTF8,
                    FluidBarrelContents::fluidId,
                    ByteBufCodecs.VAR_LONG,
                    FluidBarrelContents::amount,
                    ByteBufCodecs.BOOL,
                    FluidBarrelContents::autoOutput,
                    ByteBufCodecs.BOOL,
                    FluidBarrelContents::sealed,
                    ByteBufCodecs.VAR_LONG,
                    FluidBarrelContents::sealedTime,
                    FluidBarrelContents::new);

    public FluidBarrelContents {
        fluidId = fluidId == null ? "" : fluidId;
        if (amount < 0L) {
            amount = 0L;
        }
        if (sealedTime < 0L) {
            sealedTime = 0L;
        }
    }

    public boolean isDefault() {
        return fluidId.isEmpty()
                && amount == 0L
                && !autoOutput
                && !sealed
                && sealedTime == 0L;
    }

    public FluidBarrelLogic.Mode mode() {
        return new FluidBarrelLogic.Mode(autoOutput, sealed, sealedTime);
    }

    public FluidBarrelContents withMode(FluidBarrelLogic.Mode mode) {
        return new FluidBarrelContents(
                fluidId,
                amount,
                mode.autoOutput(),
                mode.sealed(),
                mode.sealedTime());
    }

    public FluidBarrelContents withFluid(String fluidId, long amount) {
        return new FluidBarrelContents(
                fluidId,
                amount,
                autoOutput,
                sealed,
                sealedTime);
    }
}
