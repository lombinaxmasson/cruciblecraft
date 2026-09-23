package com.masson.cruciblecraft.datagen;

import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.registry.ModMachineVariants;
import com.masson.cruciblecraft.material.MaterialCatalog;

import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.ApplyBonusCount;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.BonusLevelTableCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;

public class ModBlockLootTables extends BlockLootSubProvider {
    private static final Set<String> HANDWRITTEN_CONVERTER_LOOT = Set.of(
            "bronze_boiler",
            "bronze_steam_engine",
            "bronze_dynamo");

    public ModBlockLootTables(HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), lookupProvider);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.FIREBRICK.get());
        dropSelf(ModBlocks.ANVIL.get());
        dropSelf(ModBlocks.COKE_OVEN.get());
        dropSelf(ModBlocks.MULTIBLOCK_CASING.get());
        dropSelf(ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get());
        dropSelf(ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get());
        dropSelf(ModBlocks.MULTIBLOCK_FLUID_OUT_PORT.get());
        dropSelf(ModBlocks.LARGE_CENTRIFUGE.get());
        dropSelf(ModBlocks.LARGE_MIXER.get());
        dropSelf(ModBlocks.LARGE_ELECTROLYZER.get());
        dropSelf(ModBlocks.LARGE_OVEN.get());
        dropSelf(ModBlocks.LARGE_CRUSHER.get());
        dropSelf(ModBlocks.LARGE_SHREDDER.get());
        dropSelf(ModBlocks.LARGE_BATH.get());
        dropSelf(ModBlocks.LARGE_COAGULATOR.get());
        dropSelf(ModBlocks.LARGE_AUTOCLAVE.get());
        dropSelf(ModBlocks.IMPLOSION_COMPRESSOR.get());
        dropSelf(ModBlocks.LARGE_FERMENTER.get());
        dropSelf(ModBlocks.DISTILLATION_TOWER.get());
        dropSelf(ModBlocks.CRYO_DISTILLATION_TOWER.get());
        add(ModBlocks.LARGE_BOILER.get(), noDrop());
        dropSelf(ModBlocks.TANK_3X3X3.get());
        dropSelf(ModBlocks.LARGE_CRUCIBLE.get());
        dropSelf(ModBlocks.LOGISTICS_CORE.get());
        dropSelf(ModBlocks.GALVANIZED_STEEL_WALL.get());
        dropSelf(ModBlocks.VENTILATION_UNIT.get());
        dropSelf(ModBlocks.VERSATILE_PROCESSOR_UNIT.get());
        dropSelf(ModBlocks.LOGIC_PROCESSOR_UNIT.get());
        dropSelf(ModBlocks.CONTROL_PROCESSOR_UNIT.get());
        dropSelf(ModBlocks.STORAGE_PROCESSOR_UNIT.get());
        dropSelf(ModBlocks.CONVERSION_PROCESSOR_UNIT.get());
        ModMachineVariants.ALL.forEach(variant ->
                dropSelf(ModBlocks.configuredProcessingBlock(variant)));
        dropSelf(ModBlocks.ROTATIONAL_AXLE.get());
        dropSelf(ModBlocks.ROTATIONAL_GEARBOX.get());
        dropSelf(ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get());
        ModBlocks.converterBlocksById().forEach((id, block) -> {
            if (!HANDWRITTEN_CONVERTER_LOOT.contains(id.getPath())) {
                dropSelf(block.get());
            }
        });
        ModBlocks.batteryBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.transformerBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.quantumEnergizerBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.longDistanceTransformerBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.longDistanceWireBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.heatExchangerBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.coolerBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.fluxBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.electricalConductorBlocks().forEach(
                block -> dropSelf(block.get()));
        ModBlocks.pipeBlocks().forEach(
                block -> dropSelf(block.get()));
        ModBlocks.hopperBlocks().forEach(block -> dropSelf(block.get()));
        ModBlocks.sensorBlocks().forEach(block -> dropSelf(block.get()));
        ModBlocks.redstoneWireCatalog().forEach(block -> dropSelf(block.get()));
        ModBlocks.variantStorageBlocks().forEach(block -> dropSelf(block.get()));
        dropSelf(ModBlocks.STEEL_DUST_FUNNEL.get());
        dropSelf(ModBlocks.MIXING_BOWL.get());
        dropSelf(ModBlocks.LU_FIBER_CABLE.get());
        dropSelf(ModBlocks.LASER_ENGRAVER.get());
        dropSelf(ModBlocks.AUTOMATIC_HAMMER.get());
        dropSelf(ModBlocks.STEEL_AUTOMATIC_HAMMER.get());
        dropSelf(ModBlocks.TITANIUM_AUTOMATIC_HAMMER.get());
        dropSelf(ModBlocks.TUNGSTENSTEEL_AUTOMATIC_HAMMER.get());
        dropSelf(ModBlocks.BOOMSTICK.get());
        dropSelf(ModBlocks.DYNAMITE.get());
        dropSelf(ModBlocks.STRONG_DYNAMITE.get());
        dropSelf(ModBlocks.FUSION_REACTOR.get());
        dropSelf(ModBlocks.LARGE_HEAT_EXCHANGER.get());
        dropSelf(ModBlocks.BEDROCK_DRILL.get());
        dropSelf(ModBlocks.BEDROCK_DRILL_HEAD.get());
        dropSelf(ModBlocks.REACTOR_CORE_1X1.get());
        dropSelf(ModBlocks.REACTOR_CORE_2X2.get());
        dropSelf(ModBlocks.TUNGSTENSTEEL_WALL.get());
        dropSelf(ModBlocks.STAINLESS_STEEL_WALL.get());
        for (var species : com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies.ALL) {
            dropSelf(ModBlocks.treeSapling(species).get());
            dropSelf(ModBlocks.treeLog(species).get());
            dropSelf(ModBlocks.treeBeam(species).get());
            add(
                    ModBlocks.treeLeaves(species).get(),
                    createSilkTouchOrShearsDispatchTable(
                            ModBlocks.treeLeaves(species).get(),
                            applyExplosionCondition(
                                    ModBlocks.treeLeaves(species).get(),
                                    LootItem.lootTableItem(
                                                    ModBlocks.treeSapling(species).get())
                                            .when(
                                                    BonusLevelTableCondition.bonusLevelFlatChance(
                                                            registries
                                                                    .lookupOrThrow(
                                                                            Registries
                                                                                    .ENCHANTMENT)
                                                                    .getOrThrow(
                                                                            Enchantments
                                                                                    .FORTUNE),
                                                            NORMAL_LEAVES_SAPLING_CHANCES)))));
            if (species.hasHole()) {
                add(
                        ModBlocks.treeHole(species).get(),
                        createSingleItemTable(ModBlocks.treeLog(species).get()));
            }
        }
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            Block block = ModBlocks.gtStoneBlocksById().get(variant.id()).get();
            if (variant.slab()) {
                add(block, createSlabItemTable((SlabBlock) block));
            } else {
                dropSelf(block);
            }
        }
        for (com.masson.cruciblecraft.worldgen.StoneLayerStones.Cube cube :
                com.masson.cruciblecraft.worldgen.StoneLayerStones.cubes()) {
            if (!ModBlocks.hasLayerStone(cube.registryPath())) {
                continue;
            }
            Block block = ModBlocks.layerStone(cube.registryPath()).get();
            if (cube.role()
                    == com.masson.cruciblecraft.worldgen.StoneLayerStones.Role.STONE) {
                add(
                        block,
                        createSingleItemTableWithSilkTouch(
                                block,
                                ModBlocks.layerStone(
                                        cube.material() + "/cobble").get()));
            } else {
                dropSelf(block);
            }
        }
        for (com.masson.cruciblecraft.worldgen.StoneLayerStones.Cube cube :
                com.masson.cruciblecraft.worldgen.StoneLayerStones.villageBricks()) {
            if (!ModBlocks.hasLayerStone(cube.registryPath())) {
                continue;
            }
            dropSelf(ModBlocks.layerStone(cube.registryPath()).get());
        }
        for (com.masson.cruciblecraft.worldgen.StoneLayerStones.Cube cube :
                com.masson.cruciblecraft.worldgen.StoneLayerStones.rockOres()) {
            if (!ModBlocks.hasLayerStone(cube.registryPath())) {
                continue;
            }
            Block block = ModBlocks.layerStone(cube.registryPath()).get();
            add(block, denseRockOreLoot(block, cube.material()));
        }
        for (GtBlockObjectCatalog.Variant variant : GtBlockObjectCatalog.variants()) {
            Block block = ModBlocks.gtBlockObjectBlocksById().get(variant.id()).get();
            if (variant.slab()) {
                add(block, createSlabItemTable((SlabBlock) block));
            } else {
                dropSelf(block);
            }
        }
        for (GtBlockObjectCatalog.Variant variant : com.masson.cruciblecraft.content.item.BathRemainderBlockObjectCatalog.variants()) {
            Block block = ModBlocks.bathRemainderBlockObjectBlocksById().get(variant.id()).get();
            if (variant.slab()) {
                add(block, createSlabItemTable((SlabBlock) block));
            } else {
                dropSelf(block);
            }
        }
        for (GtBlockObjectCatalog.Variant variant :
                com.masson.cruciblecraft.content.item.GtBuildingBlockCatalog.variants()) {
            Block block = ModBlocks.gtBuildingBlockObjectBlocksById().get(variant.id()).get();
            if (variant.slab()) {
                add(block, createSlabItemTable((SlabBlock) block));
            } else {
                dropSelf(block);
            }
        }
        ModBlocks.gtWoodBlocks().forEach(holder -> dropSelf(holder.get()));
        ModBlocks.bathPanelBlocksById().values().forEach(
                holder -> dropSelf(holder.get()));
    }

    private net.minecraft.world.level.storage.loot.LootTable.Builder denseRockOreLoot(
            Block block, String material) {
        var drop = LootItem.lootTableItem(
                        ModItems.materialItem(material, MaterialPrefixes.RAW_ORE).get())
                .apply(SetItemCountFunction.setCount(ConstantValue.exactly(2)))
                .apply(ApplyBonusCount.addUniformBonusCount(
                        this.registries
                                .lookupOrThrow(Registries.ENCHANTMENT)
                                .getOrThrow(Enchantments.FORTUNE),
                        2));
        if (MaterialCatalog.find(material)
                .filter(definition -> !definition.formItems()
                        .containsKey(MaterialPrefixes.RAW_ORE))
                .isPresent()) {
            drop = drop.apply(SetComponentsFunction.setComponent(
                    ModComponents.PREFIX_MATERIAL.get(), material));
        }
        return createSilkTouchDispatchTable(
                block,
                applyExplosionDecay(block, drop));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        java.util.ArrayList<Block> blocks = new java.util.ArrayList<>();
        java.util.Collections.addAll(
                blocks,
                ModBlocks.FIREBRICK.get(),
                ModBlocks.ANVIL.get(),
                ModBlocks.COKE_OVEN.get(),
                ModBlocks.MULTIBLOCK_CASING.get(),
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get(),
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get(),
                ModBlocks.MULTIBLOCK_FLUID_OUT_PORT.get(),
                ModBlocks.LARGE_CENTRIFUGE.get(),
                ModBlocks.LARGE_MIXER.get(),
                ModBlocks.LARGE_ELECTROLYZER.get(),
                ModBlocks.LARGE_OVEN.get(),
                ModBlocks.LARGE_CRUSHER.get(),
                ModBlocks.LARGE_SHREDDER.get(),
                ModBlocks.LARGE_BATH.get(),
                ModBlocks.LARGE_COAGULATOR.get(),
                ModBlocks.LARGE_AUTOCLAVE.get(),
                ModBlocks.IMPLOSION_COMPRESSOR.get(),
                ModBlocks.LARGE_FERMENTER.get(),
                ModBlocks.DISTILLATION_TOWER.get(),
                ModBlocks.CRYO_DISTILLATION_TOWER.get(),
                ModBlocks.LARGE_BOILER.get(),
                ModBlocks.TANK_3X3X3.get(),
                ModBlocks.LARGE_CRUCIBLE.get(),
                ModBlocks.LOGISTICS_CORE.get(),
                ModBlocks.GALVANIZED_STEEL_WALL.get(),
                ModBlocks.VENTILATION_UNIT.get(),
                ModBlocks.VERSATILE_PROCESSOR_UNIT.get(),
                ModBlocks.LOGIC_PROCESSOR_UNIT.get(),
                ModBlocks.CONTROL_PROCESSOR_UNIT.get(),
                ModBlocks.STORAGE_PROCESSOR_UNIT.get(),
                ModBlocks.CONVERSION_PROCESSOR_UNIT.get(),
                ModBlocks.ROTATIONAL_AXLE.get(),
                ModBlocks.ROTATIONAL_GEARBOX.get(),
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get(),
                ModBlocks.STEEL_DUST_FUNNEL.get(),
                ModBlocks.MIXING_BOWL.get(),
                ModBlocks.LU_FIBER_CABLE.get(),
                ModBlocks.LASER_ENGRAVER.get(),
                ModBlocks.AUTOMATIC_HAMMER.get(),
                ModBlocks.STEEL_AUTOMATIC_HAMMER.get(),
                ModBlocks.TITANIUM_AUTOMATIC_HAMMER.get(),
                ModBlocks.TUNGSTENSTEEL_AUTOMATIC_HAMMER.get(),
                ModBlocks.BOOMSTICK.get(),
                ModBlocks.DYNAMITE.get(),
                ModBlocks.STRONG_DYNAMITE.get(),
                ModBlocks.FUSION_REACTOR.get(),
                ModBlocks.LARGE_HEAT_EXCHANGER.get(),
                ModBlocks.BEDROCK_DRILL.get(),
                ModBlocks.BEDROCK_DRILL_HEAD.get(),
                ModBlocks.REACTOR_CORE_1X1.get(),
                ModBlocks.REACTOR_CORE_2X2.get(),
                ModBlocks.TUNGSTENSTEEL_WALL.get(),
                ModBlocks.STAINLESS_STEEL_WALL.get());
        ModBlocks.converterBlocksById().forEach((id, holder) -> {
            if (!HANDWRITTEN_CONVERTER_LOOT.contains(id.getPath())) {
                blocks.add(holder.get());
            }
        });
        ModBlocks.batteryBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.transformerBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.quantumEnergizerBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.longDistanceTransformerBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.longDistanceWireBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.heatExchangerBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.coolerBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.fluxBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModMachineVariants.ALL.forEach(variant ->
                blocks.add(ModBlocks.configuredProcessingBlock(variant)));
        ModBlocks.electricalConductorBlocks().forEach(
                holder -> blocks.add(holder.get()));
        ModBlocks.redstoneWireCatalog().forEach(
                holder -> blocks.add(holder.get()));
        ModBlocks.pipeBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.hopperBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.sensorBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.variantStorageBlocks().forEach(
                holder -> blocks.add(holder.get()));
        ModBlocks.gtStoneBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.layerStoneBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.gtBlockObjectBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.bathRemainderBlockObjectBlocks().forEach(
                holder -> blocks.add(holder.get()));
        ModBlocks.gtBuildingBlockObjectBlocks().forEach(
                holder -> blocks.add(holder.get()));
        ModBlocks.gtWoodBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.bathPanelBlocksById().values().forEach(
                holder -> blocks.add(holder.get()));
        ModBlocks.treeSaplings().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.treeLogs().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.treeBeams().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.treeLeavesBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.treeHoles().forEach(holder -> blocks.add(holder.get()));
        return blocks;
    }
}
