package com.masson.cruciblecraft.mixin;

import com.masson.cruciblecraft.material.MissingMaterialStackNbtAdapter;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    @ModifyVariable(
            method = "parse(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/Tag;)Ljava/util/Optional;",
            at = @At("HEAD"),
            argsOnly = true)
    private static Tag cruciblecraft$rewriteParseTag(Tag tag) {
        return MissingMaterialStackNbtAdapter.rewrite(tag);
    }

    @ModifyVariable(
            method = "parseOptional(Lnet/minecraft/core/HolderLookup$Provider;Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"),
            argsOnly = true)
    private static CompoundTag cruciblecraft$rewriteOptionalTag(CompoundTag tag) {
        return MissingMaterialStackNbtAdapter.rewrite(tag);
    }
}
