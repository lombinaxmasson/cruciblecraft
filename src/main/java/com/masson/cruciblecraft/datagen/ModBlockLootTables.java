package com.masson.cruciblecraft.datagen;

import java.util.Set;
import java.util.stream.Stream;

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
    public ModBlockLootTables(HolderLookup.Provider lookupProvider) {
        super(Set.of(), FeatureFlags.REGISTRY.allFlags(), lookupProvider);
    }

    @Override
    protected void generate() {
        dropSelf(ModBlocks.FIREBRICK.get());
        dropSelf(ModBlocks.FIREBOX.get());
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
        dropSelf(ModBlocks.BELLOWS.get());
        ModMachineVariants.ALL.forEach(variant ->
                dropSelf(ModBlocks.configuredProcessingBlock(variant)));
        dropSelf(ModBlocks.ELECTRIC_MOTOR.get());
        dropSelf(ModBlocks.ROTATIONAL_AXLE.get());
        dropSelf(ModBlocks.ROTATIONAL_GEARBOX.get());
        dropSelf(ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get());
        dropSelf(ModBlocks.FUEL_ENGINE.get());
        dropSelf(ModBlocks.BURNING_GAS_GENERATOR.get());
        ModBlocks.electricalConductorBlocks().forEach(
                block -> dropSelf(block.get()));
        ModBlocks.pipeBlocks().forEach(
                block -> dropSelf(block.get()));
        ModBlocks.hopperBlocks().forEach(block -> dropSelf(block.get()));
        ModBlocks.variantStorageBlocks().forEach(block -> dropSelf(block.get()));
        dropSelf(ModBlocks.STEEL_DUST_FUNNEL.get());
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
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(Stream.concat(Stream.concat(Stream.of(
                (Block) ModBlocks.FIREBRICK.get(),
                ModBlocks.FIREBOX.get(),
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
                ModBlocks.BELLOWS.get(),
                ModBlocks.ELECTRIC_MOTOR.get(),
                ModBlocks.ROTATIONAL_AXLE.get(),
                ModBlocks.ROTATIONAL_GEARBOX.get(),
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get(),
                ModBlocks.FUEL_ENGINE.get(),
                ModBlocks.BURNING_GAS_GENERATOR.get()),
                ModMachineVariants.ALL.stream()
                        .map(ModBlocks::configuredProcessingBlock)),
                ModBlocks.electricalConductorBlocks().stream()
                        .map(holder -> (Block) holder.get())),
                Stream.concat(
                        ModBlocks.pipeBlocks().stream()
                                .map(holder -> (Block) holder.get()),
                                Stream.concat(
                                ModBlocks.hopperBlocks().stream()
                                        .map(holder -> (Block) holder.get()),
                                Stream.concat(
                                        ModBlocks.variantStorageBlocks().stream()
                                                .map(holder -> (Block) holder.get()),
                                        Stream.concat(
                                        Stream.of(ModBlocks.STEEL_DUST_FUNNEL.get()),
                                        Stream.concat(
                                        ModBlocks.gtStoneBlocks().stream()
                                                .map(holder -> holder.get()),
                                        ModBlocks.gtBlockObjectBlocks().stream()
                                                .map(holder -> holder.get())))))))
                .toList();
    }
}
