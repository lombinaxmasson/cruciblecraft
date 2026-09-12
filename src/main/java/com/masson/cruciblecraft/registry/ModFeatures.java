package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.worldgen.GtBlockObjectScatterFeature;
import com.masson.cruciblecraft.worldgen.GtItemScatterFeature;
import com.masson.cruciblecraft.worldgen.GtStoneScatterFeature;
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
    public static final DeferredHolder<Feature<?>, GtStoneScatterFeature>
            GT_STONE_SCATTER =
                    FEATURES.register(
                            "gt_stone_scatter",
                            GtStoneScatterFeature::new);
    public static final DeferredHolder<Feature<?>, GtBlockObjectScatterFeature>
            GT_BLOCK_OBJECT_SCATTER =
                    FEATURES.register(
                            "gt_block_object_scatter",
                            GtBlockObjectScatterFeature::new);
    public static final DeferredHolder<Feature<?>, GtItemScatterFeature>
            BATH_MTE_SCATTER =
                    FEATURES.register(
                            "bath_mte_scatter",
                            GtItemScatterFeature::new);
    public static final DeferredHolder<Feature<?>, GtItemScatterFeature>
            SMELTER_MTE_SCATTER =
                    FEATURES.register(
                            "smelter_mte_scatter",
                            GtItemScatterFeature::new);
    public static final DeferredHolder<Feature<?>, GtItemScatterFeature>
            BATH_REMAINDER_SCATTER =
                    FEATURES.register(
                            "bath_remainder_scatter",
                            GtItemScatterFeature::new);
    public static final DeferredHolder<Feature<?>, GtItemScatterFeature>
            BATH_IDENTITY_SCATTER =
                    FEATURES.register(
                            "bath_identity_scatter",
                            GtItemScatterFeature::new);
    public static final DeferredHolder<Feature<?>, GtItemScatterFeature>
            BATH_TINY_WASHED_SCATTER =
                    FEATURES.register(
                            "bath_tiny_washed_scatter",
                            GtItemScatterFeature::new);
    public static final DeferredHolder<Feature<?>, GtItemScatterFeature>
            SEMANTIC_OBJECT_SCATTER =
                    FEATURES.register(
                            "semantic_object_scatter",
                            GtItemScatterFeature::new);

    /** GT6 WorldgenTree* via com.masson.cruciblecraft.worldgen.tree.prep GtTreeGrower / GtTreeSpecies / GtTreePlacement. */
    public static final DeferredHolder<Feature<?>, GtTreeFeature> GT_TREE =
            FEATURES.register("gt_tree", GtTreeFeature::new);

    private ModFeatures() {}
}
