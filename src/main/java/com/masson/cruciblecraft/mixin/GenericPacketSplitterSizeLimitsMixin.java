package com.masson.cruciblecraft.mixin;

import net.minecraft.network.CompressionDecoder;
import net.neoforged.neoforge.network.filters.GenericPacketSplitter;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * NeoForge keeps an 8 MiB uncompressed split ceiling when a compressor is
 * present. Vanilla frames still use a 3-byte VarInt (2 MiB). Compact recipe
 * sync sits between those limits, so cap both SizeLimits accessors to the
 * compressed 2 MiB frame used for in-memory connections.
 */
@Mixin(
        targets = "net.neoforged.neoforge.network.filters.GenericPacketSplitter$SizeLimits",
        remap = false)
abstract class GenericPacketSplitterSizeLimitsMixin {
    @Inject(method = "packet", at = @At("RETURN"), cancellable = true)
    private void cruciblecraft$capSplitPacketBytes(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.min(
                cir.getReturnValueI(), CompressionDecoder.MAXIMUM_COMPRESSED_LENGTH));
    }

    @Inject(method = "part", at = @At("RETURN"), cancellable = true)
    private void cruciblecraft$capSplitPartBytes(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.min(
                cir.getReturnValueI(),
                GenericPacketSplitter.determineMaxPayloadSize(
                        CompressionDecoder.MAXIMUM_COMPRESSED_LENGTH)));
    }
}
