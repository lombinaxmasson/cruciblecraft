package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;

public final class ModBlockTags {
    public static final TagKey<Block> GT_STONES = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "gt_stones"));
    public static final TagKey<Block> GT_BLOCK_OBJECTS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "gt_block_objects"));
    public static final TagKey<Block> DISTILLATION_TOWER_CONTROLLERS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "distillation_tower_controllers"));
    public static final TagKey<Block> LARGE_CRUCIBLE_CONTROLLERS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "large_crucible_controllers"));
    public static final TagKey<Block> LARGE_CRUCIBLE_WALLS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "large_crucible_walls"));
    /** GT6 {@code OD.beamWood}: vanilla stripped wood plus GT-tree beams. */
    public static final TagKey<Block> WOODEN_BEAMS = TagKey.create(
            Registries.BLOCK,
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "wooden_beams"));

    private ModBlockTags() {}
}
