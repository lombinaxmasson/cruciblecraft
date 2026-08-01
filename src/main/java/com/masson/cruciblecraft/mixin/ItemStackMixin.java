package com.masson.cruciblecraft.mixin;

import java.util.function.Supplier;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.masson.cruciblecraft.material.MissingMaterialStackCodec;
import com.mojang.serialization.Codec;

import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    /*
     * OPTIONAL_STREAM_CODEC carries an already-resolved registry holder, not a
     * legacy resource-location string. Structured decode is the migration
     * boundary; the resulting unknown-material component then syncs normally.
     */
    /*
     * This runs during vanilla class initialization. Keep wrap() and every
     * class it initializes free of registry access and other bootstrap work.
     *
     * ItemStack 1.21.1 has exactly two lazy ItemStack codecs here: CODEC and
     * SINGLE_ITEM_CODEC. Wrapping every match covers both without depending on
     * their field order; the count constraints turn upstream changes into a
     * startup failure instead of silently wrapping the wrong field.
     */
    @WrapOperation(
            method = "<clinit>",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/serialization/Codec;lazyInitialized(Ljava/util/function/Supplier;)Lcom/mojang/serialization/Codec;"),
            require = 2,
            allow = 2)
    private static Codec<ItemStack> cruciblecraft$wrapCodec(
            Supplier<Codec<ItemStack>> supplier,
            Operation<Codec<ItemStack>> original) {
        return MissingMaterialStackCodec.wrap(original.call(supplier));
    }
}
