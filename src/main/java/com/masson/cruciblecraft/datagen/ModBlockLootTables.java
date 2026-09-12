package com.masson.cruciblecraft.datagen;

import java.util.Set;

import com.masson.cruciblecraft.content.item.GtBlockObjectCatalog;
import com.masson.cruciblecraft.content.item.GtStoneCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SlabBlock;

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
        dropSelf(ModBlocks.CRUCIBLE.get());
        dropSelf(ModBlocks.ANVIL.get());
        dropSelf(ModBlocks.COKE_OVEN.get());
        dropSelf(ModBlocks.MULTIBLOCK_CASING.get());
        dropSelf(ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get());
        dropSelf(ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get());
        dropSelf(ModBlocks.LARGE_CENTRIFUGE.get());
        dropSelf(ModBlocks.DISTILLATION_TOWER.get());
        dropSelf(ModBlocks.LARGE_BOILER.get());
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
        ModBlocks.heatExchangerBlocksById().forEach(
                (id, block) -> dropSelf(block.get()));
        ModBlocks.electricalConductorBlocks().forEach(
                block -> dropSelf(block.get()));
        ModBlocks.pipeBlocks().forEach(
                block -> dropSelf(block.get()));
        ModBlocks.hopperBlocks().forEach(block -> dropSelf(block.get()));
        ModBlocks.sensorBlocks().forEach(block -> dropSelf(block.get()));
        ModBlocks.variantStorageBlocks().forEach(block -> dropSelf(block.get()));
        dropSelf(ModBlocks.STEEL_DUST_FUNNEL.get());
        dropSelf(ModBlocks.LU_FIBER_CABLE.get());
        dropSelf(ModBlocks.LASER_ENGRAVER.get());
        dropSelf(ModBlocks.FUSION_REACTOR.get());
        dropSelf(ModBlocks.REACTOR_CORE_1X1.get());
        dropSelf(ModBlocks.REACTOR_CORE_2X2.get());
        dropSelf(ModBlocks.TUNGSTENSTEEL_WALL.get());
        dropSelf(ModBlocks.STAINLESS_STEEL_WALL.get());
        dropSelf(ModBlocks.LARGE_IRIDIUM_COIL.get());
        for (GtStoneCatalog.Variant variant : GtStoneCatalog.variants()) {
            Block block = ModBlocks.gtStoneBlocksById().get(variant.id()).get();
            if (variant.slab()) {
                add(block, createSlabItemTable((SlabBlock) block));
            } else {
                dropSelf(block);
            }
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
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        java.util.ArrayList<Block> blocks = new java.util.ArrayList<>();
        java.util.Collections.addAll(
                blocks,
                ModBlocks.FIREBRICK.get(),
                ModBlocks.CRUCIBLE.get(),
                ModBlocks.ANVIL.get(),
                ModBlocks.COKE_OVEN.get(),
                ModBlocks.MULTIBLOCK_CASING.get(),
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get(),
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get(),
                ModBlocks.LARGE_CENTRIFUGE.get(),
                ModBlocks.DISTILLATION_TOWER.get(),
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
                ModBlocks.LU_FIBER_CABLE.get(),
                ModBlocks.LASER_ENGRAVER.get(),
                ModBlocks.FUSION_REACTOR.get(),
                ModBlocks.REACTOR_CORE_1X1.get(),
                ModBlocks.REACTOR_CORE_2X2.get(),
                ModBlocks.TUNGSTENSTEEL_WALL.get(),
                ModBlocks.STAINLESS_STEEL_WALL.get(),
                ModBlocks.LARGE_IRIDIUM_COIL.get());
        ModBlocks.converterBlocksById().forEach((id, holder) -> {
            if (!HANDWRITTEN_CONVERTER_LOOT.contains(id.getPath())) {
                blocks.add(holder.get());
            }
        });
        ModBlocks.batteryBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.transformerBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModBlocks.heatExchangerBlocksById().forEach(
                (id, holder) -> blocks.add(holder.get()));
        ModMachineVariants.ALL.forEach(variant ->
                blocks.add(ModBlocks.configuredProcessingBlock(variant)));
        ModBlocks.electricalConductorBlocks().forEach(
                holder -> blocks.add(holder.get()));
        ModBlocks.pipeBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.hopperBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.sensorBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.variantStorageBlocks().forEach(
                holder -> blocks.add(holder.get()));
        ModBlocks.gtStoneBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.gtBlockObjectBlocks().forEach(holder -> blocks.add(holder.get()));
        ModBlocks.bathRemainderBlockObjectBlocks().forEach(
                holder -> blocks.add(holder.get()));
        return blocks;
    }
}
