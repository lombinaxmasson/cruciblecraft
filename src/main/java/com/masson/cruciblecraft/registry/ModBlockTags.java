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

    private ModBlockTags() {}
}
