package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.worldgen.LargeVeinFeature;
import com.masson.cruciblecraft.worldgen.SubsurfaceFluidDepositFeature;
import com.masson.cruciblecraft.worldgen.SurfaceRockFeature;
import com.masson.cruciblecraft.worldgen.tree.GtTreeFeature;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, CrucibleCraft.MODID);

    public static final DeferredHolder<Feature<?>, LargeVeinFeature> LARGE_VEIN =
            FEATURES.register("large_vein", LargeVeinFeature::new);
    public static final DeferredHolder<
            Feature<?>,
            SubsurfaceFluidDepositFeature> SUBSURFACE_FLUID_DEPOSIT =
                    FEATURES.register(
                            "subsurface_fluid_deposit",
                            SubsurfaceFluidDepositFeature::new);
    public static final DeferredHolder<Feature<?>, SurfaceRockFeature>
            SURFACE_ROCK_SCATTER =
                    FEATURES.register(
                            "surface_rock_scatter",
                            SurfaceRockFeature::new);

    /** GT6 WorldgenTree* via com.masson.cruciblecraft.worldgen.tree.prep GtTreeGrower / GtTreeSpecies / GtTreePlacement. */
    public static final DeferredHolder<Feature<?>, GtTreeFeature> GT_TREE =
            FEATURES.register("gt_tree", GtTreeFeature::new);

    /** GT6 WorldgenGlowtus / WorldgenBushes. */
    public static final DeferredHolder<
            Feature<?>,
            com.masson.cruciblecraft.worldgen.crop.GtCropFeature> GT_CROP =
                    FEATURES.register(
                            "gt_crop",
                            com.masson.cruciblecraft.worldgen.crop.GtCropFeature::new);

    private ModFeatures() {}
}
