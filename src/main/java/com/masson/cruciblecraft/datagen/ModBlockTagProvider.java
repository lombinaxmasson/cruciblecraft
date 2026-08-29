package com.masson.cruciblecraft.datagen;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.registry.ModBlockTags;
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
        var stone = tag(BlockTags.NEEDS_STONE_TOOL);
        pickaxe.add(
                ModBlocks.FIREBOX.getKey(),
                ModBlocks.BRONZE_BOILER.getKey(),
                ModBlocks.BRONZE_STEAM_ENGINE.getKey(),
                ModBlocks.BRONZE_DYNAMO.getKey(),
                ModBlocks.ELECTRIC_MOTOR.getKey(),
                ModBlocks.ROTATIONAL_AXLE.getKey(),
                ModBlocks.ROTATIONAL_GEARBOX.getKey(),
                ModBlocks.BRONZE_CRUSHER.getKey(),
                ModBlocks.MULTIBLOCK_CASING.getKey(),
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.getKey(),
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.getKey(),
                ModBlocks.LARGE_CENTRIFUGE.getKey(),
                ModBlocks.DISTILLATION_TOWER.getKey(),
                ModBlocks.LARGE_BOILER.getKey(),
                ModBlocks.TANK_3X3X3.getKey(),
                ModBlocks.LARGE_CRUCIBLE.getKey(),
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.getKey(),
                ModBlocks.FUEL_ENGINE.getKey(),
                ModBlocks.BURNING_GAS_GENERATOR.getKey());
        ModBlocks.configuredProcessingBlockEntries().stream()
                .sorted(Comparator.comparing(
                        block -> block.getId().toString()))
                .forEach(block -> {
                    pickaxe.add(block.getKey());
                    stone.add(block.getKey());
                });
        ModBlocks.electricalConductorBlocks().forEach(
                block -> pickaxe.add(block.getKey()));
        ModBlocks.pipeBlocks().forEach(
                block -> pickaxe.add(block.getKey()));
        ModBlocks.hopperBlocks().forEach(block -> {
            pickaxe.add(block.getKey());
            stone.add(block.getKey());
        });
        var axe = tag(BlockTags.MINEABLE_WITH_AXE);
        ModBlocks.variantStorageBlocks().forEach(block -> {
            var variant = block.get().variant();
            boolean wood = variant.plankIndex() != null
                    || "mass_storage_barrel".equals(variant.family())
                    || "mass_storage_box".equals(variant.family());
            if (wood) {
                axe.add(block.getKey());
            } else {
                pickaxe.add(block.getKey());
                stone.add(block.getKey());
            }
        });
        pickaxe.add(ModBlocks.STEEL_DUST_FUNNEL.getKey());
        stone.add(ModBlocks.STEEL_DUST_FUNNEL.getKey());
        var gtStones = tag(ModBlockTags.GT_STONES);
        ModBlocks.gtStoneBlocks().forEach(block -> {
            pickaxe.add(block.getKey());
            stone.add(block.getKey());
            gtStones.add(block.getKey());
        });
        var gtBlockObjects = tag(ModBlockTags.GT_BLOCK_OBJECTS);
        var rails = tag(BlockTags.RAILS);
        ModBlocks.gtBlockObjectBlocks().forEach(holder -> {
            var variant = com.masson.cruciblecraft.content.item.GtBlockObjectCatalog
                    .require(holder.getId());
            gtBlockObjects.add(holder.getKey());
            if (variant.log() || variant.bale()) {
                axe.add(holder.getKey());
            } else if (!variant.rail()) {
                pickaxe.add(holder.getKey());
                stone.add(holder.getKey());
            }
            if (variant.rail()) {
                rails.add(holder.getKey());
            }
        });
    }
}
