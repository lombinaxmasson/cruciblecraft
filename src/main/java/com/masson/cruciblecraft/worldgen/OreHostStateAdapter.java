package com.masson.cruciblecraft.worldgen;

import java.util.Optional;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;

/** Resolves cataloged ore states against the block being replaced. */
final class OreHostStateAdapter {
    private OreHostStateAdapter() {}

    static Optional<BlockState> adapt(BlockState selected, BlockState replaced) {
        ResourceLocation selectedId = BuiltInRegistries.BLOCK.getKey(selected.getBlock());
        if (!CrucibleCraft.MODID.equals(selectedId.getNamespace())) {
            return Optional.of(selected);
        }

        Host host;
        if (replaced.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
            host = Host.DEEPSLATE;
        } else if (replaced.is(BlockTags.STONE_ORE_REPLACEABLES)) {
            host = Host.STONE;
        } else {
            return Optional.empty();
        }

        Optional<String> targetPath = OreHostVariantCatalog.adaptPath(selectedId.getPath(), host);
        if (targetPath.isEmpty()) {
            // A CrucibleCraft block not present in the explicit ore-pair catalog
            // is left untouched rather than guessed from its registry name.
            return Optional.of(selected);
        }
        ResourceLocation targetId =
                ResourceLocation.fromNamespaceAndPath(CrucibleCraft.MODID, targetPath.orElseThrow());
        if (!BuiltInRegistries.BLOCK.containsKey(targetId)) {
            return Optional.empty();
        }

        BlockState adapted = BuiltInRegistries.BLOCK.get(targetId).defaultBlockState();
        for (Property<?> property : selected.getProperties()) {
            adapted = copyProperty(selected, adapted, property);
        }
        return Optional.of(adapted);
    }

    private static <T extends Comparable<T>> BlockState copyProperty(
            BlockState source, BlockState target, Property<T> property) {
        return target.hasProperty(property)
                ? target.setValue(property, source.getValue(property))
                : target;
    }
}
