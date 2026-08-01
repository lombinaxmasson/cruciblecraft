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
        var firebrickTexture = mcLoc("block/bricks");
        simpleBlockWithItem(
                ModBlocks.FIREBRICK.get(),
                models().cubeAll("firebrick", firebrickTexture));
        simpleBlockWithItem(
                ModBlocks.FIREBOX.get(),
                models().cubeAll("firebox", firebrickTexture));
        simpleBlockWithItem(
                ModBlocks.CRUCIBLE.get(),
                models().getExistingFile(modLoc("block/crucible")));
        var anvil = models().getExistingFile(modLoc("block/anvil"));
        horizontalBlock(ModBlocks.ANVIL.get(), anvil);
        simpleBlockItem(ModBlocks.ANVIL.get(), anvil);
        simpleBlockWithItem(
                ModBlocks.COKE_OVEN.get(),
                models().cubeAll("coke_oven", firebrickTexture));
        configuredMachine("sluice", ModBlocks.SLUICE.get());
        configuredMachine("bath", ModBlocks.BATH.get());
        configuredMachine("centrifuge", ModBlocks.CENTRIFUGE.get());
        configuredMachine("shredder", ModBlocks.SHREDDER.get());
        configuredMachine("sifter", ModBlocks.SIFTER.get());
        configuredMachine("smelter", ModBlocks.SMELTER.get());
        configuredMachine("mortar", ModBlocks.MORTAR.get());
        configuredMachine("extruder", ModBlocks.EXTRUDER.get());
        configuredMachine("cutter", ModBlocks.CUTTER.get());
        configuredMachine("lathe", ModBlocks.LATHE.get());
        configuredMachine("rollingmill", ModBlocks.ROLLINGMILL.get());
        configuredMachine("rollbender", ModBlocks.ROLLBENDER.get());
        configuredMachine("wiremill", ModBlocks.WIREMILL.get());
        configuredMachine("bender", ModBlocks.BENDER.get());
        configuredMachine("assembler", ModBlocks.ASSEMBLER.get());
        configuredMachine("welder", ModBlocks.WELDER.get());
        configuredMachine("press", ModBlocks.PRESS.get());
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
                firebrickTexture,
                firebrickTexture,
                firebrickTexture);
        var filledMold = models().slab(
                "ceramic_mold_filled",
                firebrickTexture,
                mcLoc("block/magma"),
                firebrickTexture);
        getVariantBuilder(ModBlocks.CERAMIC_MOLD.get()).forAllStates(state ->
                ConfiguredModel.builder()
                        .modelFile(state.getValue(CeramicMoldBlock.FILLED) ? filledMold : emptyMold)
                        .build());
    }

    private void configuredMachine(String id, net.minecraft.world.level.block.Block block) {
        var model = models().orientable(
                id,
                mcLoc("block/copper_block"),
                mcLoc("block/furnace_front"),
                mcLoc("block/cut_copper"));
        horizontalBlock(block, model);
        simpleBlockItem(block, model);
    }
}
