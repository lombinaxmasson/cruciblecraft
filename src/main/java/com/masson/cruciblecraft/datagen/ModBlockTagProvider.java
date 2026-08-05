package com.masson.cruciblecraft.datagen;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.tags.TagsProvider;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

/** Static block tags for registry-backed material conductors. */
public final class ModBlockTagProvider extends TagsProvider<Block> {
    public ModBlockTagProvider(
            PackOutput output,
            CompletableFuture<HolderLookup.Provider> lookupProvider,
            ExistingFileHelper existingFileHelper) {
        super(
                output,
                net.minecraft.core.registries.Registries.BLOCK,
                lookupProvider,
                CrucibleCraft.MODID,
                existingFileHelper);
    }

    @Override
    protected void addTags(HolderLookup.Provider provider) {
        var pickaxe = tag(BlockTags.MINEABLE_WITH_PICKAXE);
        pickaxe.add(
                ModBlocks.BRONZE_BOILER.getKey(),
                ModBlocks.BRONZE_STEAM_ENGINE.getKey(),
                ModBlocks.BRONZE_DYNAMO.getKey(),
                ModBlocks.BRONZE_CRUSHER.getKey());
        ModBlocks.configuredProcessingBlockEntries().stream()
                .sorted(Comparator.comparing(
                        block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.electricalConductorBlocks().forEach(
                block -> pickaxe.add(block.getKey()));
        ModBlocks.pipeBlocks().forEach(
                block -> pickaxe.add(block.getKey()));
    }
}
