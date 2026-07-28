package com.masson.cruciblecraft.registry;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.content.block.AnvilBlock;
import com.masson.cruciblecraft.content.block.BellowsBlock;
import com.masson.cruciblecraft.content.block.BoilerBlock;
import com.masson.cruciblecraft.content.block.CokeOvenBlock;
import com.masson.cruciblecraft.content.block.CeramicMoldBlock;
import com.masson.cruciblecraft.content.block.CrucibleBlock;
import com.masson.cruciblecraft.content.block.CrusherBlock;
import com.masson.cruciblecraft.content.block.FireboxBlock;
import com.masson.cruciblecraft.content.block.SteamEngineBlock;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(CrucibleCraft.MODID);

    /** M0 placeholder block — later reused as firebox cladding. */
    public static final DeferredBlock<Block> FIREBRICK = BLOCKS.registerSimpleBlock(
            "firebrick",
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.0F, 6.0F)
                    .sound(SoundType.STONE));

    public static final DeferredBlock<FireboxBlock> FIREBOX = BLOCKS.register(
            "firebox",
            () -> new FireboxBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 8.0F)
                    .lightLevel(state -> state.getValue(FireboxBlock.LIT) ? 13 : 0)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<CrucibleBlock> CRUCIBLE = BLOCKS.register(
            "crucible",
            () -> new CrucibleBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_BLACK)
                    .strength(3.0F, 8.0F)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<AnvilBlock> ANVIL = BLOCKS.register(
            "anvil",
            () -> new AnvilBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(5.0F, 1_200.0F)
                    .requiresCorrectToolForDrops()
                    .noOcclusion()
                    .sound(SoundType.ANVIL)));

    public static final DeferredBlock<CokeOvenBlock> COKE_OVEN = BLOCKS.register(
            "coke_oven",
            () -> new CokeOvenBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(3.0F, 8.0F)
                    .lightLevel(state -> state.getValue(CokeOvenBlock.LIT) ? 8 : 0)
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<BellowsBlock> BELLOWS = BLOCKS.register(
            "bellows",
            () -> new BellowsBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WOOD)
                    .strength(1.5F)
                    .sound(SoundType.WOOD)));

    public static final DeferredBlock<CeramicMoldBlock> CERAMIC_MOLD = BLOCKS.register(
            "ceramic_mold",
            () -> new CeramicMoldBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_ORANGE)
                    .strength(1.5F, 4.0F)
                    .noOcclusion()
                    .noLootTable()
                    .sound(SoundType.STONE)));

    public static final DeferredBlock<LiquidBlock> CREOSOTE = BLOCKS.register(
            "creosote",
            () -> new LiquidBlock(
                    ModFluids.CREOSOTE_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.COLOR_BROWN)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()));

    public static final DeferredBlock<LiquidBlock> STEAM = BLOCKS.register(
            "steam",
            () -> new LiquidBlock(
                    ModFluids.STEAM_SOURCE.get(),
                    BlockBehaviour.Properties.of()
                            .mapColor(MapColor.NONE)
                            .replaceable()
                            .noCollission()
                            .strength(100.0F)
                            .pushReaction(PushReaction.DESTROY)
                            .noLootTable()
                            .liquid()));

    public static final DeferredBlock<BoilerBlock> BRONZE_BOILER = BLOCKS.register(
            "bronze_boiler",
            () -> new BoilerBlock(machineProperties()));
    public static final DeferredBlock<SteamEngineBlock> BRONZE_STEAM_ENGINE = BLOCKS.register(
            "bronze_steam_engine",
            () -> new SteamEngineBlock(machineProperties()));
    public static final DeferredBlock<CrusherBlock> BRONZE_CRUSHER = BLOCKS.register(
            "bronze_crusher",
            () -> new CrusherBlock(machineProperties()));

    public static final DeferredBlock<DropExperienceBlock> COPPER_ORE = ore("copper_ore", MapColor.STONE);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_COPPER_ORE = ore("deepslate_copper_ore", MapColor.DEEPSLATE);
    public static final DeferredBlock<DropExperienceBlock> TIN_ORE = ore("tin_ore", MapColor.STONE);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_TIN_ORE = ore("deepslate_tin_ore", MapColor.DEEPSLATE);
    public static final DeferredBlock<DropExperienceBlock> IRON_ORE = ore("iron_ore", MapColor.STONE);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_IRON_ORE = ore("deepslate_iron_ore", MapColor.DEEPSLATE);
    public static final DeferredBlock<DropExperienceBlock> GOLD_ORE = ore("gold_ore", MapColor.STONE);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_GOLD_ORE = ore("deepslate_gold_ore", MapColor.DEEPSLATE);
    public static final DeferredBlock<DropExperienceBlock> ZINC_ORE = ore("zinc_ore", MapColor.STONE);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_ZINC_ORE = ore("deepslate_zinc_ore", MapColor.DEEPSLATE);
    public static final DeferredBlock<DropExperienceBlock> LEAD_ORE = ore("lead_ore", MapColor.STONE);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_LEAD_ORE = ore("deepslate_lead_ore", MapColor.DEEPSLATE);
    public static final DeferredBlock<DropExperienceBlock> NICKEL_ORE = ore("nickel_ore", MapColor.STONE);
    public static final DeferredBlock<DropExperienceBlock> DEEPSLATE_NICKEL_ORE = ore("deepslate_nickel_ore", MapColor.DEEPSLATE);

    private static DeferredBlock<DropExperienceBlock> ore(String id, MapColor color) {
        return BLOCKS.register(
                id,
                () -> new DropExperienceBlock(
                        net.minecraft.util.valueproviders.UniformInt.of(0, 2),
                        BlockBehaviour.Properties.of()
                                .mapColor(color)
                                .strength(3.0F, 3.0F)
                                .requiresCorrectToolForDrops()
                                .sound(SoundType.STONE)));
    }

    private static BlockBehaviour.Properties machineProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.COLOR_ORANGE)
                .strength(3.5F, 8.0F)
                .requiresCorrectToolForDrops()
                .sound(SoundType.METAL);
    }

    private ModBlocks() {}
}
