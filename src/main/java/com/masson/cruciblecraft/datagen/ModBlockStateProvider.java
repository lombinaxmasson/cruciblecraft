package com.masson.cruciblecraft.datagen;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.registry.ModBlocks;

import net.minecraft.data.PackOutput;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ConfiguredModel;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
        super(output, CrucibleCraft.MODID, existingFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        simpleBlockWithItem(ModBlocks.FIREBRICK.get(), cubeAll(ModBlocks.FIREBRICK.get()));
        simpleBlockWithItem(
                ModBlocks.FIREBOX.get(),
                models().cubeAll("firebox", modLoc("block/firebrick")));
        simpleBlockWithItem(
                ModBlocks.CRUCIBLE.get(),
                models().getExistingFile(modLoc("block/crucible")));
        var anvil = models().getExistingFile(modLoc("block/anvil"));
        horizontalBlock(ModBlocks.ANVIL.get(), anvil);
        simpleBlockItem(ModBlocks.ANVIL.get(), anvil);
        simpleBlockWithItem(
                ModBlocks.COKE_OVEN.get(),
                models().cubeAll("coke_oven", modLoc("block/firebrick")));
        var bellows = models().orientable(
                "bellows",
                mcLoc("block/oak_planks"),
                mcLoc("block/piston_top"),
                mcLoc("block/oak_planks"));
        var activeBellows = models().orientable(
                "bellows_active",
                mcLoc("block/oak_planks"),
                mcLoc("block/piston_top_sticky"),
                mcLoc("block/oak_planks"));
        horizontalBlock(
                ModBlocks.BELLOWS.get(),
                state -> state.getValue(BellowsBlock.ACTIVE)
                        ? activeBellows
                        : bellows);
        simpleBlockItem(ModBlocks.BELLOWS.get(), bellows);
        var emptyMold = models().slab(
                "ceramic_mold",
                modLoc("block/firebrick"),
                modLoc("block/firebrick"),
                modLoc("block/firebrick"));
        var filledMold = models().slab(
                "ceramic_mold_filled",
                modLoc("block/firebrick"),
                mcLoc("block/magma"),
                modLoc("block/firebrick"));
        getVariantBuilder(ModBlocks.CERAMIC_MOLD.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(CeramicMoldBlock.FILLED) ? filledMold : emptyMold)
                        .build());
    }
}
