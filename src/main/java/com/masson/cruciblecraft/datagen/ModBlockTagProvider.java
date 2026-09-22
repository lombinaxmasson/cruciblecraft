package com.masson.cruciblecraft.datagen;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.LargeCrucibleHosts;
import com.masson.cruciblecraft.content.block.WoodDebark;
import com.masson.cruciblecraft.registry.ModBlockTags;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceKey;
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
                Registries.BLOCK,
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
                ModBlocks.AUTOMATIC_HAMMER.getKey(),
                ModBlocks.STEEL_AUTOMATIC_HAMMER.getKey(),
                ModBlocks.TITANIUM_AUTOMATIC_HAMMER.getKey(),
                ModBlocks.TUNGSTENSTEEL_AUTOMATIC_HAMMER.getKey(),
                ModBlocks.MULTIBLOCK_CASING.getKey(),
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.getKey(),
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.getKey(),
                ModBlocks.MULTIBLOCK_FLUID_OUT_PORT.getKey(),
                ModBlocks.LARGE_CENTRIFUGE.getKey(),
                ModBlocks.LARGE_MIXER.getKey(),
                ModBlocks.LARGE_ELECTROLYZER.getKey(),
                ModBlocks.LARGE_OVEN.getKey(),
                ModBlocks.LARGE_CRUSHER.getKey(),
                ModBlocks.LARGE_SHREDDER.getKey(),
                ModBlocks.LARGE_BATH.getKey(),
                ModBlocks.LARGE_COAGULATOR.getKey(),
                ModBlocks.LARGE_AUTOCLAVE.getKey(),
                ModBlocks.LARGE_FERMENTER.getKey(),
                ModBlocks.DISTILLATION_TOWER.getKey(),
                ModBlocks.CRYO_DISTILLATION_TOWER.getKey(),
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
                ModBlocks.ANVIL.getKey(),
                ModBlocks.COKE_OVEN.getKey(),
                ModBlocks.LU_FIBER_CABLE.getKey(),
                ModBlocks.LASER_ENGRAVER.getKey(),
                ModBlocks.FUSION_REACTOR.getKey(),
                ModBlocks.LARGE_HEAT_EXCHANGER.getKey(),
                ModBlocks.BEDROCK_DRILL.getKey(),
                ModBlocks.BEDROCK_DRILL_HEAD.getKey(),
                ModBlocks.GT_SMALL_ORE.getKey(),
                ModBlocks.GT_HOSTED_ORE.getKey(),
                ModBlocks.GT_BROKEN_ORE.getKey(),
                ModBlocks.REACTOR_CORE_1X1.getKey(),
                ModBlocks.REACTOR_CORE_2X2.getKey(),
                ModBlocks.TUNGSTENSTEEL_WALL.getKey(),
                ModBlocks.STAINLESS_STEEL_WALL.getKey());
        stone.add(ModBlocks.BRONZE_CRUSHER.getKey());
        stone.add(ModBlocks.GT_SMALL_ORE.getKey());
        stone.add(ModBlocks.GT_HOSTED_ORE.getKey());
        stone.add(ModBlocks.GT_BROKEN_ORE.getKey());
        tag(BlockTags.SMALL_FLOWERS).add(ModBlocks.GT_INDICATOR_FLOWER.getKey());
        tag(BlockTags.FLOWERS).add(ModBlocks.GT_INDICATOR_FLOWER.getKey());
        var shovel = tag(BlockTags.MINEABLE_WITH_SHOVEL);
        shovel.add(ModBlocks.GT_INDICATOR_GRASS.getKey());
        tag(BlockTags.DIRT).add(ModBlocks.GT_INDICATOR_GRASS.getKey());
        ModBlocks.converterBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.batteryBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.transformerBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.quantumEnergizerBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.longDistanceTransformerBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.heatExchangerBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.coolerBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.fluxBlocksById().values().stream()
                .sorted(Comparator.comparing(block -> block.getId().toString()))
                .forEach(block -> pickaxe.add(block.getKey()));
        ModBlocks.longDistanceWireBlocksById().values().stream()
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
        ModBlocks.sensorBlocks().forEach(block -> {
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
        pickaxe.add(ModBlocks.MIXING_BOWL.getKey());
        stone.add(ModBlocks.MIXING_BOWL.getKey());
        var gtStones = tag(ModBlockTags.GT_STONES);
        ModBlocks.gtStoneBlocks().forEach(block -> {
            pickaxe.add(block.getKey());
            stone.add(block.getKey());
            gtStones.add(block.getKey());
        });
        var baseStone = tag(BlockTags.BASE_STONE_OVERWORLD);
        var iron = tag(BlockTags.NEEDS_IRON_TOOL);
        var diamond = tag(BlockTags.NEEDS_DIAMOND_TOOL);
        for (var cube : com.masson.cruciblecraft.worldgen.StoneLayerStones.registeredCubes()) {
            if (!ModBlocks.hasLayerStone(cube.registryPath())) {
                continue;
            }
            var holder = ModBlocks.layerStone(cube.registryPath());
            pickaxe.add(holder.getKey());
            if (cube.harvestLevel() >= 3) {
                diamond.add(holder.getKey());
            } else if (cube.harvestLevel() == 2) {
                iron.add(holder.getKey());
            } else if (cube.harvestLevel() == 1) {
                stone.add(holder.getKey());
            }
            if (cube.role()
                    == com.masson.cruciblecraft.worldgen.StoneLayerStones.Role.STONE
                    && !cube.denseOre()) {
                baseStone.add(holder.getKey());
            }
        }
        var gtBlockObjects = tag(ModBlockTags.GT_BLOCK_OBJECTS);
        var rails = tag(BlockTags.RAILS);
        ModBlocks.gtBlockObjectBlocks().forEach(holder -> {
            var variant = com.masson.cruciblecraft.content.item.GtBlockObjectCatalog
                    .require(holder.getId());
            addGtBlockObjectTags(holder, variant, gtBlockObjects, rails, pickaxe, stone, axe, shovel);
        });
        ModBlocks.bathRemainderBlockObjectBlocks().forEach(holder -> {
            var variant = com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog
                    .require(holder.getId());
            addGtBlockObjectTags(holder, variant, gtBlockObjects, rails, pickaxe, stone, axe, shovel);
        });
        ModBlocks.gtBuildingBlockObjectBlocks().forEach(holder -> {
            var variant = com.masson.cruciblecraft.content.item.GtBuildingBlockCatalog
                    .require(holder.getId());
            addGtBlockObjectTags(holder, variant, gtBlockObjects, rails, pickaxe, stone, axe, shovel);
        });
        var logs = tag(BlockTags.LOGS);
        var logsThatBurn = tag(BlockTags.LOGS_THAT_BURN);
        var woodenBeams = tag(ModBlockTags.WOODEN_BEAMS);
        var leaves = tag(BlockTags.LEAVES);
        var saplings = tag(BlockTags.SAPLINGS);
        var hoe = tag(BlockTags.MINEABLE_WITH_HOE);
        ModBlocks.treeLogs().forEach(holder -> {
            axe.add(holder.getKey());
            logs.add(holder.getKey());
            logsThatBurn.add(holder.getKey());
        });
        ModBlocks.treeBeams().forEach(holder -> {
            axe.add(holder.getKey());
            woodenBeams.add(holder.getKey());
        });
        for (WoodDebark.VanillaPair pair : WoodDebark.VANILLA_PAIRS) {
            woodenBeams.add(ResourceKey.create(Registries.BLOCK, pair.beam()));
        }
        ModBlocks.treeHoles().forEach(holder -> axe.add(holder.getKey()));
        ModBlocks.treeLeavesBlocks().forEach(holder -> {
            leaves.add(holder.getKey());
            hoe.add(holder.getKey());
        });
        ModBlocks.treeSaplings().forEach(holder -> saplings.add(holder.getKey()));
        ModBlocks.gtWoodBlocks().forEach(holder -> {
            axe.add(holder.getKey());
            if (!holder.getId().getPath().endsWith("/crate")) {
                tag(BlockTags.PLANKS).add(holder.getKey());
            }
        });
        ModBlocks.bathPanelBlocksById().forEach((id, holder) -> {
            String path = id.getPath();
            if (path.startsWith("panel/cfoam_")) {
                hoe.add(holder.getKey());
            } else {
                pickaxe.add(holder.getKey());
            }
        });
        hoe.add(ModBlocks.GT_BUSH.getKey());
        var towerControllers = tag(ModBlockTags.DISTILLATION_TOWER_CONTROLLERS);
        towerControllers.add(ModBlocks.DISTILLATION_TOWER.getKey());
        towerControllers.add(ModBlocks.CRYO_DISTILLATION_TOWER.getKey());
        var largeControllers = tag(ModBlockTags.LARGE_CRUCIBLE_CONTROLLERS);
        largeControllers.add(ModBlocks.LARGE_CRUCIBLE.getKey());
        var largeWalls = tag(ModBlockTags.LARGE_CRUCIBLE_WALLS);
        ModBlocks.mteInPlaceBlocksById().values().forEach(holder -> {
            var spec = holder.get().spec();
            pickaxe.add(holder.getKey());
            if (LargeCrucibleHosts.isController(spec)) {
                largeControllers.add(holder.getKey());
            }
            if (LargeCrucibleHosts.isWall(spec)) {
                largeWalls.add(holder.getKey());
            }
        });
    }

    private void addGtBlockObjectTags(
            net.neoforged.neoforge.registries.DeferredBlock<net.minecraft.world.level.block.Block> holder,
            com.masson.cruciblecraft.content.item.GtBlockObjectCatalog.Variant variant,
            TagAppender<Block> gtBlockObjects,
            TagAppender<Block> rails,
            TagAppender<Block> pickaxe,
            TagAppender<Block> stone,
            TagAppender<Block> axe,
            TagAppender<Block> shovel) {
        gtBlockObjects.add(holder.getKey());
        if (variant.treePlanks2()) {
            axe.add(holder.getKey());
            if (variant.slab()) {
                tag(BlockTags.WOODEN_SLABS).add(holder.getKey());
            } else {
                tag(BlockTags.PLANKS).add(holder.getKey());
            }
        } else if (variant.log() || variant.bale()) {
            axe.add(holder.getKey());
        } else if (variant.shovelMineable()) {
            shovel.add(holder.getKey());
        } else {
            pickaxe.add(holder.getKey());
            if (!variant.glassLike() && !variant.rail()) {
                stone.add(holder.getKey());
            }
        }
        if (variant.rail()) {
            rails.add(holder.getKey());
        }
    }
}
