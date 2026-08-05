package com.masson.cruciblecraft.datagen;

import java.util.Set;
import java.util.stream.Stream;

import com.masson.cruciblecraft.registry.ModBlocks;

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
        dropSelf(ModBlocks.BELLOWS.get());
        dropSelf(ModBlocks.SLUICE.get());
        dropSelf(ModBlocks.BATH.get());
        dropSelf(ModBlocks.CENTRIFUGE.get());
        dropSelf(ModBlocks.SHREDDER.get());
        dropSelf(ModBlocks.SIFTER.get());
        dropSelf(ModBlocks.SMELTER.get());
        dropSelf(ModBlocks.MORTAR.get());
        dropSelf(ModBlocks.EXTRUDER.get());
        dropSelf(ModBlocks.CUTTER.get());
        dropSelf(ModBlocks.LATHE.get());
        dropSelf(ModBlocks.ROLLINGMILL.get());
        dropSelf(ModBlocks.ROLLBENDER.get());
        dropSelf(ModBlocks.WIREMILL.get());
        dropSelf(ModBlocks.BENDER.get());
        dropSelf(ModBlocks.ASSEMBLER.get());
        dropSelf(ModBlocks.WELDER.get());
        dropSelf(ModBlocks.PRESS.get());
        dropSelf(ModBlocks.ELECTROLYZER.get());
        dropSelf(ModBlocks.MIXER.get());
        dropSelf(ModBlocks.DISTILLERY.get());
        dropSelf(ModBlocks.AUTOCLAVE.get());
        dropSelf(ModBlocks.DRYING.get());
        dropSelf(ModBlocks.COMPRESSOR.get());
        ModBlocks.electricalConductorBlocks().forEach(
                block -> dropSelf(block.get()));
        ModBlocks.pipeBlocks().forEach(
                block -> dropSelf(block.get()));
    }

    @Override
    protected Iterable<Block> getKnownBlocks() {
        return Stream.concat(Stream.concat(Stream.of(
                (Block) ModBlocks.FIREBRICK.get(),
                ModBlocks.FIREBOX.get(),
                ModBlocks.CRUCIBLE.get(),
                ModBlocks.ANVIL.get(),
                ModBlocks.COKE_OVEN.get(),
                ModBlocks.BELLOWS.get(),
                ModBlocks.SLUICE.get(),
                ModBlocks.BATH.get(),
                ModBlocks.CENTRIFUGE.get(),
                ModBlocks.SHREDDER.get(),
                ModBlocks.SIFTER.get(),
                ModBlocks.SMELTER.get(),
                ModBlocks.MORTAR.get(),
                ModBlocks.EXTRUDER.get(),
                ModBlocks.CUTTER.get(),
                ModBlocks.LATHE.get(),
                ModBlocks.ROLLINGMILL.get(),
                ModBlocks.ROLLBENDER.get(),
                ModBlocks.WIREMILL.get(),
                ModBlocks.BENDER.get(),
                ModBlocks.ASSEMBLER.get(),
                ModBlocks.WELDER.get(),
                ModBlocks.PRESS.get(),
                ModBlocks.ELECTROLYZER.get(),
                ModBlocks.MIXER.get(),
                ModBlocks.DISTILLERY.get(),
                ModBlocks.AUTOCLAVE.get(),
                ModBlocks.DRYING.get(),
                ModBlocks.COMPRESSOR.get()),
                ModBlocks.electricalConductorBlocks().stream()
                        .map(holder -> (Block) holder.get())),
                ModBlocks.pipeBlocks().stream()
                        .map(holder -> (Block) holder.get())).toList();
    }
}
