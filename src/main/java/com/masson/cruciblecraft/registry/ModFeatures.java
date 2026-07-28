package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.worldgen.LargeVeinFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, CrucibleCraft.MODID);

    public static final DeferredHolder<Feature<?>, LargeVeinFeature> LARGE_VEIN =
            FEATURES.register("large_vein", LargeVeinFeature::new);

    private ModFeatures() {}
}
