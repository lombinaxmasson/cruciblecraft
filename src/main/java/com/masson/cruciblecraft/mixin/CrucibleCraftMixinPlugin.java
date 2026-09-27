package com.masson.cruciblecraft.mixin;

import java.util.List;
import java.util.Set;

import net.neoforged.fml.loading.LoadingModList;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

/**
 * Reliable EMI is an optional dev client mod. Its mixins must not apply when
 * that jar is absent.
 */
public final class CrucibleCraftMixinPlugin implements IMixinConfigPlugin {
    private static final String REMI_LOCK = "RemiStackGroupLockMixin";
    private static final String REMI_FIELDS = "RemiStackGroupFields";

    @Override
    public void onLoad(String mixinPackage) {}

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (mixinClassName.endsWith(REMI_LOCK) || mixinClassName.endsWith(REMI_FIELDS)) {
            LoadingModList mods = LoadingModList.get();
            return mods != null && mods.getModFileById("remi") != null;
        }
        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}

    @Override
    public void postApply(
            String targetClassName,
            ClassNode targetClass,
            String mixinClassName,
            IMixinInfo mixinInfo) {}
}
