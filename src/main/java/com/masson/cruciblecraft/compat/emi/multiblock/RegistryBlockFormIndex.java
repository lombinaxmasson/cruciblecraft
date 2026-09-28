package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** Live block and item registries used when EMI builds a projection. */
public final class RegistryBlockFormIndex implements BlockFormIndex {
    public static final RegistryBlockFormIndex INSTANCE =
            new RegistryBlockFormIndex();

    private RegistryBlockFormIndex() {}

    @Override
    public List<ResourceLocation> blocksInTag(ResourceLocation tag) {
        TagKey<Block> key = TagKey.create(Registries.BLOCK, tag);
        List<ResourceLocation> blocks = new ArrayList<>();
        BuiltInRegistries.BLOCK.forEach(block -> {
            if (block.defaultBlockState().is(key)) {
                blocks.add(BuiltInRegistries.BLOCK.getKey(block));
            }
        });
        blocks.sort(Comparator.comparing(ResourceLocation::toString));
        return List.copyOf(blocks);
    }

    @Override
    public boolean hasItemForm(ResourceLocation block) {
        return BuiltInRegistries.BLOCK.getOptional(block)
                .map(value -> !new ItemStack(value).isEmpty())
                .orElse(false);
    }
}
