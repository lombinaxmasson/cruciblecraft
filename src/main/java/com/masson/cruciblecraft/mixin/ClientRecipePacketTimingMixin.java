package com.masson.cruciblecraft.mixin;

import java.util.Collection;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.recipe.gt.RecipeLoginSync;

import net.minecraft.network.protocol.game.ClientboundUpdateRecipesPacket;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The server builds this packet before encoding it. The client builds it
 * again after the codec has decoded the recipe list. Thread name separates
 * those two moments.
 */
@Mixin(ClientboundUpdateRecipesPacket.class)
abstract class ClientRecipePacketTimingMixin {
    @ModifyVariable(
            method = "<init>(Ljava/util/Collection;)V",
            at = @At("HEAD"),
            argsOnly = true)
    private static Collection<?> cruciblecraft$omitOnDemandFamilies(Collection<?> recipes) {
        return RecipeLoginSync.loginPacketRecipes(recipes);
    }

    @Inject(method = "<init>(Ljava/util/Collection;)V", at = @At("RETURN"))
    private void cruciblecraft$packetCreated(Collection<?> recipes, CallbackInfo ci) {
        CrucibleCraft.LOGGER.info(
                "Recipe sync packet object: count={} thread={}",
                recipes.size(),
                Thread.currentThread().getName());
    }
}
