package com.masson.cruciblecraft.mixin;

import com.masson.cruciblecraft.CrucibleCraft;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Splits EMI {@code VanillaPlugin.register}. Phase names follow that method:
 * everything before the first {@code safely} call, then each {@code safely}
 * block, then tag recipes plus the per-item anvil pass.
 */
@Mixin(targets = "dev.emi.emi.VanillaPlugin", remap = false)
abstract class EmiVanillaPluginTimingMixin {
    @Unique
    private long cruciblecraft$vanillaPluginMark;

    @Unique
    private int cruciblecraft$safelyIndex;

    @Inject(method = "register", at = @At("HEAD"))
    private void cruciblecraft$vanillaPluginStart(CallbackInfo ci) {
        cruciblecraft$vanillaPluginMark = System.nanoTime();
        cruciblecraft$safelyIndex = 0;
    }

    @Inject(
            method = "register",
            at = @At(
                    value = "INVOKE",
                    target = "Ldev/emi/emi/VanillaPlugin;safely(Ljava/lang/String;Ljava/lang/Runnable;)V"))
    private void cruciblecraft$beforeSafely(CallbackInfo ci) {
        cruciblecraft$vanillaPluginPhase(switch (cruciblecraft$safelyIndex) {
            case 0 -> "recipe types";
            case 1 -> "repair";
            case 2 -> "brewing";
            case 3 -> "world interaction";
            case 4 -> "fuel";
            default -> "safely " + cruciblecraft$safelyIndex;
        });
        cruciblecraft$safelyIndex++;
    }

    @Inject(
            method = "register",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/emi/emi/registry/EmiTags;TAGS:Ljava/util/List;"))
    private void cruciblecraft$beforeTagRecipes(CallbackInfo ci) {
        cruciblecraft$vanillaPluginPhase("composting");
    }

    @Inject(method = "register", at = @At("RETURN"))
    private void cruciblecraft$vanillaPluginEnd(CallbackInfo ci) {
        cruciblecraft$vanillaPluginPhase("tags and per-item anvil");
    }

    @Unique
    private void cruciblecraft$vanillaPluginPhase(String phase) {
        long now = System.nanoTime();
        CrucibleCraft.LOGGER.info(
                "EMI vanilla plugin: {}={}ms",
                phase,
                (now - cruciblecraft$vanillaPluginMark) / 1_000_000L);
        cruciblecraft$vanillaPluginMark = now;
    }
}
