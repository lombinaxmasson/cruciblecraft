package com.masson.cruciblecraft.mixin;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Splits the client apply of {@code ClientboundUpdateRecipesPacket} into the
 * three calls that run before {@code RecipesUpdatedEvent}.
 */
@Mixin(ClientPacketListener.class)
abstract class ClientRecipeSyncTimingMixin {
    @Unique
    private long cruciblecraft$recipeSyncMark;

    @Inject(method = "handleUpdateRecipes", at = @At("HEAD"))
    private void cruciblecraft$syncStart(
            ClientboundUpdateRecipesPacket packet, CallbackInfo ci) {
        cruciblecraft$recipeSyncMark = System.nanoTime();
        CrucibleCraft.LOGGER.info(
                "Client recipe sync: handler start recipes={}",
                packet.getRecipes().size());
    }

    @Inject(
            method = "handleUpdateRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/item/crafting/RecipeManager;replaceRecipes(Ljava/lang/Iterable;)V",
                    shift = At.Shift.AFTER))
    private void cruciblecraft$afterReplace(CallbackInfo ci) {
        cruciblecraft$syncPhase("replaceRecipes");
    }

    @Inject(
            method = "handleUpdateRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/ClientRecipeBook;setupCollections(Ljava/lang/Iterable;Lnet/minecraft/core/RegistryAccess;)V",
                    shift = At.Shift.AFTER))
    private void cruciblecraft$afterRecipeBook(CallbackInfo ci) {
        cruciblecraft$syncPhase("recipe book");
    }

    @Inject(
            method = "handleUpdateRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/multiplayer/SessionSearchTrees;updateRecipes(Lnet/minecraft/client/ClientRecipeBook;Lnet/minecraft/core/RegistryAccess$Frozen;)V",
                    shift = At.Shift.AFTER))
    private void cruciblecraft$afterSearchTrees(CallbackInfo ci) {
        cruciblecraft$syncPhase("search trees");
    }

    @Inject(
            method = "handleUpdateRecipes",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/neoforged/neoforge/client/ClientHooks;onRecipesUpdated(Lnet/minecraft/world/item/crafting/RecipeManager;)V",
                    remap = false))
    private void cruciblecraft$beforeRecipeEvent(CallbackInfo ci) {
        cruciblecraft$syncPhase("before recipe event");
    }

    @Inject(method = "handleUpdateRecipes", at = @At("RETURN"))
    private void cruciblecraft$syncEnd(CallbackInfo ci) {
        cruciblecraft$syncPhase("recipe event");
    }

    @Unique
    private void cruciblecraft$syncPhase(String phase) {
        long now = System.nanoTime();
        CrucibleCraft.LOGGER.info(
                "Client recipe sync: {}={}ms",
                phase,
                (now - cruciblecraft$recipeSyncMark) / 1_000_000L);
        cruciblecraft$recipeSyncMark = now;
    }
}
