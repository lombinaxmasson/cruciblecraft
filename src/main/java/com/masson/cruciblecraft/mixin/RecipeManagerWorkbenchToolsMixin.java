package com.masson.cruciblecraft.mixin;

import java.util.Map;

import com.google.gson.JsonElement;
import com.masson.cruciblecraft.recipe.crafting.WorkbenchToolRuntimeRecipes;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.crafting.RecipeManager;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Supplies the large GT6 workbench-tool recipe family at reload time.
 *
 * <p>Datapack entries are loaded first. The runtime defaults use
 * {@code putIfAbsent}, so a datapack can still replace one of the generated
 * ids exactly as it could before this optimization.
 */
@Mixin(RecipeManager.class)
abstract class RecipeManagerWorkbenchToolsMixin {
    @Inject(
            method = "apply(Ljava/util/Map;Lnet/minecraft/server/packs/resources/ResourceManager;Lnet/minecraft/util/profiling/ProfilerFiller;)V",
            at = @At("HEAD"))
    private void cruciblecraft$addWorkbenchToolRecipes(
            Map<ResourceLocation, JsonElement> recipes,
            ResourceManager resourceManager,
            ProfilerFiller profiler,
            CallbackInfo ci) {
        WorkbenchToolRuntimeRecipes.addDefaults(recipes);
    }
}
