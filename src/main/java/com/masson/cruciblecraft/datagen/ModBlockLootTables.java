package com.masson.cruciblecraft.datagen;

import java.util.Set;
import java.util.stream.Stream;

import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModMachineVariants;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.block.Block;

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
        dropSelf(ModBlocks.BELLOWS.get());
        dropSelf(ModBlocks.SLUICE.get());
        dropSelf(ModBlocks.BATH.get());
        dropSelf(ModBlocks.CENTRIFUGE.get());
        dropSelf(ModBlocks.STEEL_CENTRIFUGE.get());
        dropSelf(ModBlocks.TITANIUM_CENTRIFUGE.get());
        dropSelf(ModBlocks.SIFTER.get());
        dropSelf(ModBlocks.STEEL_SIFTER.get());
        dropSelf(ModBlocks.TITANIUM_SIFTER.get());
        ModMachineVariants.T16_SELECTED.forEach(variant ->
                dropSelf(ModBlocks.configuredProcessingBlock(variant)));
        ModMachineVariants.T17_SELECTED.forEach(variant ->
                dropSelf(ModBlocks.configuredProcessingBlock(variant)));
        dropSelf(ModBlocks.MORTAR.get());
        dropSelf(ModBlocks.EXTRUDER.get());
        dropSelf(ModBlocks.CUTTER.get());
        dropSelf(ModBlocks.ROLLBENDER.get());
        dropSelf(ModBlocks.BENDER.get());
        dropSelf(ModBlocks.ASSEMBLER.get());
        dropSelf(ModBlocks.WELDER.get());
        dropSelf(ModBlocks.ELECTROLYZER.get());
        dropSelf(ModBlocks.ALUMINIUM_ELECTROLYZER.get());
        dropSelf(ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get());
        dropSelf(ModBlocks.MIXER.get());
        dropSelf(ModBlocks.AUTOCLAVE.get());
        dropSelf(ModBlocks.COMPRESSOR.get());
        dropSelf(ModBlocks.GENERIFIER.get());
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
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(Stream.concat(Stream.concat(Stream.concat(Stream.of(
                (Block) ModBlocks.FIREBRICK.get(),
                ModBlocks.FIREBOX.get(),
                ModBlocks.CRUCIBLE.get(),
                ModBlocks.ANVIL.get(),
                ModBlocks.COKE_OVEN.get(),
                ModBlocks.MULTIBLOCK_CASING.get(),
                ModBlocks.MULTIBLOCK_ITEM_FLUID_PORT.get(),
                ModBlocks.MULTIBLOCK_ENERGY_INPUT_PORT.get(),
                ModBlocks.LARGE_CENTRIFUGE.get(),
                ModBlocks.BELLOWS.get(),
                ModBlocks.SLUICE.get(),
                ModBlocks.BATH.get(),
                ModBlocks.CENTRIFUGE.get(),
                ModBlocks.STEEL_CENTRIFUGE.get(),
                ModBlocks.TITANIUM_CENTRIFUGE.get(),
                ModBlocks.SIFTER.get(),
                ModBlocks.STEEL_SIFTER.get(),
                ModBlocks.TITANIUM_SIFTER.get(),
                ModBlocks.MORTAR.get(),
                ModBlocks.EXTRUDER.get(),
                ModBlocks.CUTTER.get(),
                ModBlocks.ROLLBENDER.get(),
                ModBlocks.BENDER.get(),
                ModBlocks.ASSEMBLER.get(),
                ModBlocks.WELDER.get(),
                ModBlocks.ELECTROLYZER.get(),
                ModBlocks.ALUMINIUM_ELECTROLYZER.get(),
                ModBlocks.STAINLESS_STEEL_ELECTROLYZER.get(),
                ModBlocks.MIXER.get(),
                ModBlocks.AUTOCLAVE.get(),
                ModBlocks.COMPRESSOR.get(),
                ModBlocks.GENERIFIER.get(),
                ModBlocks.ELECTRIC_MOTOR.get(),
                ModBlocks.ROTATIONAL_AXLE.get(),
                ModBlocks.ROTATIONAL_GEARBOX.get(),
                ModBlocks.FLUID_DEPOSIT_EXTRACTOR.get(),
                ModBlocks.FUEL_ENGINE.get(),
                ModBlocks.BURNING_GAS_GENERATOR.get()),
                ModMachineVariants.T16_SELECTED.stream()
                        .map(ModBlocks::configuredProcessingBlock)),
                ModMachineVariants.T17_SELECTED.stream()
                        .map(ModBlocks::configuredProcessingBlock)),
                ModBlocks.electricalConductorBlocks().stream()
                        .map(holder -> (Block) holder.get())),
                ModBlocks.pipeBlocks().stream()
                        .map(holder -> (Block) holder.get())).toList();
    }
}
