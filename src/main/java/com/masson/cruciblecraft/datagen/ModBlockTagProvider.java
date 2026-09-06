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
                ModBlocks.LOGISTICS_CORE.getKey(),
                ModBlocks.GALVANIZED_STEEL_WALL.getKey(),
                ModBlocks.VENTILATION_UNIT.getKey(),
                ModBlocks.VERSATILE_PROCESSOR_UNIT.getKey(),
                ModBlocks.LOGIC_PROCESSOR_UNIT.getKey(),
                ModBlocks.CONTROL_PROCESSOR_UNIT.getKey(),
                ModBlocks.STORAGE_PROCESSOR_UNIT.getKey(),
                ModBlocks.CONVERSION_PROCESSOR_UNIT.getKey(),
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.getKey(),
                ModBlocks.FIREBRICK.getKey(),
                ModBlocks.CRUCIBLE.getKey(),
                ModBlocks.ANVIL.getKey(),
                ModBlocks.COKE_OVEN.getKey(),
                ModBlocks.LU_FIBER_CABLE.getKey(),
                ModBlocks.LASER_ENGRAVER.getKey(),
                ModBlocks.FUSION_REACTOR.getKey(),
                ModBlocks.REACTOR_CORE_1X1.getKey(),
                ModBlocks.REACTOR_CORE_2X2.getKey(),
                ModBlocks.TUNGSTENSTEEL_WALL.getKey(),
                ModBlocks.STAINLESS_STEEL_WALL.getKey(),
                ModBlocks.LARGE_IRIDIUM_COIL.getKey());
        ModBlocks.converterBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.batteryBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.transformerBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
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
            addGtBlockObjectTags(holder, variant, gtBlockObjects, rails, pickaxe, stone, axe);
        });
        ModBlocks.bathRemainderBlockObjectBlocks().forEach(holder -> {
            var variant = com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog
                    .require(holder.getId());
            addGtBlockObjectTags(holder, variant, gtBlockObjects, rails, pickaxe, stone, axe);
        });
    }

    private void addGtBlockObjectTags(
            net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block> holder,
            com.masson.cruciblecraft.content.item.GtBlockObjectCatalog.Variant variant,
            TagAppender<Block> gtBlockObjects,
            TagAppender<Block> rails,
            TagAppender<Block> pickaxe,
            TagAppender<Block> stone,
            TagAppender<Block> axe) {
        gtBlockObjects.add(holder.getKey());
        if (variant.log() || variant.bale()) {
            axe.add(holder.getKey());
        } else {
            pickaxe.add(holder.getKey());
            if (!variant.rail()) {
                stone.add(holder.getKey());
            }
        }
        if (variant.rail()) {
            rails.add(holder.getKey());
        }
    }
}
