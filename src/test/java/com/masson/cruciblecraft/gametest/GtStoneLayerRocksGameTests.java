package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.GtSurfaceRockBlock;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.content.blockentity.GtSurfaceRockBlockEntity;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.NetherQuartzLayerFeature;
import com.masson.cruciblecraft.worldgen.OreHostVariantCatalog.Host;
import com.masson.cruciblecraft.worldgen.StoneLayerCatalog;
import com.masson.cruciblecraft.worldgen.StoneLayerNoise;
import com.masson.cruciblecraft.worldgen.StoneLayerRockFeature;
import com.masson.cruciblecraft.worldgen.StoneLayerStones;
import com.masson.cruciblecraft.worldgen.VillageStoneBricks;
import com.masson.cruciblecraft.worldgen.SurfaceRockContents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 WorldgenStoneLayers cubes, pebbles, and StoneLayerOres. Run with
 * {@code -PgameTestGrid=worldgen}.
 */
@GameTestHolder(GtStoneLayerRocksGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GtStoneLayerRocksGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_worldgen";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(8, 2, 8);

    private GtStoneLayerRocksGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerCatalogMatchesLoaderWorldgen(GameTestHelper helper) {
        helper.assertTrue(
                StoneLayerCatalog.UNIT == 648648000,
                "GT6 CS.U chance unit drifted");
        helper.assertTrue(
                !StoneLayerCatalog.DEEPSLATE_LAYER.ores().isEmpty(),
                "DEEPSLATE must keep no-mod layer ores");
        helper.assertTrue(
                StoneLayerCatalog.layers().stream()
                        .anyMatch(layer -> !layer.ores().isEmpty()),
                "no-mod LAYERS must keep StoneLayerOres");
        helper.assertTrue(
                StoneLayerCatalog.layers().stream()
                        .anyMatch(layer -> "granite_black".equals(layer.material())),
                "granite_black must be a layer surface material");
        helper.assertTrue(
                "coal".equals(StoneLayerCatalog.layers().getFirst().material())
                        && StoneLayerStones.isDenseOre("coal"),
                "BlockRockOres anthracite/coal must prepend LAYERS");
        helper.assertTrue(
                StoneLayerCatalog.layers().stream()
                        .filter(StoneLayerCatalog.Layer::noDeep)
                        .count()
                        == 11,
                "setNoDeep granite + BlockRockOres layers drifted");
        helper.assertTrue(
                StoneLayerStones.cubes().size() == 45,
                "BlocksGT native stone/cobble/mossy cubes drifted");
        helper.assertTrue(
                StoneLayerStones.rockOres().size() == 9,
                "BlockRockOres 8 overworld cubes plus nether quartz drifted");
        helper.assertTrue(
                StoneLayerStones.villageBricks().size() == 17
                        && "granite_black".equals(
                                StoneLayerStones.villageBricks().getFirst().material())
                        && "shale".equals(
                                StoneLayerStones.villageBricks().getLast().material()),
                "GetVillageBlockID BlocksGT.stones palette drifted");
        helper.assertTrue(
                StoneLayerCatalog.layers().stream()
                        .noneMatch(layer ->
                                "nether_quartz".equals(layer.material())),
                "Nether Quartz must stay out of overworld LAYERS");
        helper.assertTrue(
                ModBlocks.hasLayerStone("granite_black/stone")
                        && ModBlocks.hasLayerStone("granite_black/cobble")
                        && ModBlocks.hasLayerStone("andesite/small_bricks")
                        && ModBlocks.hasLayerStone("coal/dense_ore")
                        && ModBlocks.hasLayerStone("nether_quartz/dense_ore")
                        && !ModBlocks.hasLayerStone("marble/stone")
                        && !ModBlocks.hasLayerStone("marble/small_bricks"),
                "granite_black cubes register; village SBRIK registers; marble reuses gt_stone_catalog");
        helper.assertTrue(
                ModFeatures.STONE_LAYER_ROCKS.get() != null
                        && ModBlocks.GT_SURFACE_ROCK.get() != null,
                "stone_layer_rocks registry missing");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.getKey(ModBlocks.GT_SURFACE_ROCK.get())
                        .getPath()
                        .equals("gt_surface_rock"),
                "32757 placer id drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerPlaces32757WithLayerLoot(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.above(), Blocks.AIR);
        BlockPos pebble = helper.absolutePos(POS.above());
        helper.assertTrue(
                StoneLayerRockFeature.tryPlace(
                        helper.getLevel(), pebble, "granite_black"),
                "layer pebble did not place on stone");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getBlock() instanceof GtSurfaceRockBlock,
                "must place gt_surface_rock");
        helper.assertTrue(
                !(helper.getBlockState(POS.above()).getBlock() instanceof RockBlock),
                "must not place RockBlock");
        helper.assertTrue(
                helper.getBlockState(POS.above())
                        .getValue(SurfaceRockContents.PROPERTY)
                        == SurfaceRockContents.EMPTY,
                "layer pebbles use EMPTY contents plus NBT material");
        BlockEntity blockEntity = helper.getLevel().getBlockEntity(pebble);
        helper.assertTrue(
                blockEntity instanceof GtSurfaceRockBlockEntity rock
                        && "granite_black".equals(rock.materialId()),
                "tLastRock must be granite_black");
        ItemStack expected = new ItemStack(ModItems.materialItem(
                "granite_black",
                MaterialPrefixCatalog.require("rock")).get());
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(
                        GtSurfaceRockBlock.loot(
                                helper.getBlockState(POS.above()),
                                blockEntity),
                        expected),
                "layer pebble must drop granite_black/rock");
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                helper.getBlockState(POS.above()),
                helper.getLevel(),
                pebble,
                blockEntity);
        helper.assertTrue(
                drops.size() == 1
                        && ItemStack.isSameItemSameComponents(drops.get(0), expected),
                "block drops must follow tLastRock");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerReplacesVanillaStone(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.above(), Blocks.AIR);
        helper.assertTrue(
                StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "granite_black",
                        StoneLayerStones.Role.STONE),
                "vanilla stone must become granite_black/stone");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.layerStone("granite_black/stone").get()),
                "replaced cube must be granite_black/stone");
        helper.assertTrue(
                !StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "granite_black",
                        StoneLayerStones.Role.STONE),
                "already-correct cube must not be rewritten");
        helper.setBlock(POS, Blocks.COBBLESTONE);
        helper.assertTrue(
                StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "granite_black",
                        StoneLayerStones.Role.COBBLE),
                "cobble must become granite_black/cobble");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.layerStone("granite_black/cobble").get()),
                "cobble replace drifted");
        helper.setBlock(POS, Blocks.STONE);
        helper.assertTrue(
                !StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "stone",
                        StoneLayerStones.Role.STONE),
                "stone layer must leave vanilla stone");
        helper.assertTrue(
                helper.getBlockState(POS).is(Blocks.STONE),
                "null-first-arg StoneLayer stays Blocks.stone");
        helper.setBlock(POS, Blocks.DEEPSLATE);
        helper.assertTrue(
                StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "basalt",
                        StoneLayerStones.Role.STONE),
                "vanilla deepslate must become the layer cube");
        helper.setBlock(POS, Blocks.TUFF);
        helper.assertTrue(
                StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "basalt",
                        StoneLayerStones.Role.STONE),
                "1.21 tuff must become the layer cube");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.layerStone("basalt/stone").get()),
                "tuff replace drifted");
        helper.setBlock(POS, Blocks.IRON_ORE);
        helper.assertTrue(
                StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "granite_black",
                        StoneLayerStones.Role.STONE),
                "vanilla ore cells must be replaceable like GT6 REPLACEABLE_BLOCKS");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.layerStone("granite_black/stone").get()),
                "vanilla iron ore must become the layer cube");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerPlacesLayerOres(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        StoneLayerCatalog.Ore diamond = new StoneLayerCatalog.Ore(
                "diamond",
                StoneLayerCatalog.UNIT,
                0,
                255,
                false,
                null,
                java.util.List.of(),
                false);
        helper.assertTrue(
                StoneLayerRockFeature.tryPlaceOre(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        diamond,
                        StoneLayerCatalog.layers().getFirst()),
                "StoneLayerOres must place a live ore block");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.oreBlock("diamond", Host.STONE).get()),
                "in-layer diamond must be the unique stone-host ore block");
        helper.assertTrue(
                helper.getBlockState(POS).getValue(
                        com.masson.cruciblecraft.content.block.MaterialOreBlock.HOST)
                        == com.masson.cruciblecraft.content.block.OreStoneHost.COAL,
                "coal-layer diamond must use the anthracite cube as the ore host");
        helper.setBlock(POS, Blocks.STONE);
        StoneLayerCatalog.Ore vanillaIron = new StoneLayerCatalog.Ore(
                "iron",
                StoneLayerCatalog.UNIT,
                40,
                80,
                false,
                "minecraft:iron_ore",
                java.util.List.of(),
                false);
        helper.assertTrue(
                StoneLayerRockFeature.tryPlaceOre(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        vanillaIron,
                        StoneLayerCatalog.layers().getFirst()),
                "vanilla-specified StoneLayerOres must place Blocks.iron_ore");
        helper.assertTrue(
                helper.getBlockState(POS).is(Blocks.IRON_ORE),
                "vanilla iron layer ore drifted");
        helper.setBlock(POS, Blocks.STONE);
        helper.assertTrue(
                StoneLayerRockFeature.tryPlaceOre(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        diamond,
                        StoneLayerCatalog.layers().getFirst(),
                        false),
                "small-ore branch must still place a live ore block");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.oreBlock("diamond", Host.STONE).get()),
                "CC small vs normal share the stone-host ore block");
        helper.assertTrue(
                StoneLayerRockFeature.isVanillaOre(
                        Blocks.IRON_ORE.defaultBlockState()),
                "iron ore must stay on the replaceable list");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerOresMatchLayerCubeHost(GameTestHelper helper) {
        StoneLayerCatalog.Layer granite = StoneLayerCatalog.layers().stream()
                .filter(layer -> "granite_black".equals(layer.material()))
                .findFirst()
                .orElseThrow();
        StoneLayerCatalog.Ore diamond = new StoneLayerCatalog.Ore(
                "diamond",
                StoneLayerCatalog.UNIT,
                0,
                255,
                false,
                null,
                java.util.List.of(),
                false);
        helper.setBlock(POS, Blocks.STONE);
        helper.assertTrue(
                StoneLayerRockFeature.tryPlaceOre(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        diamond,
                        granite),
                "black-granite StoneLayerOres must place unique diamond ore");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.oreBlock("diamond", Host.STONE).get()),
                "black-granite diamond stays the unique diamond_ore id");
        helper.assertTrue(
                helper.getBlockState(POS).getValue(
                        com.masson.cruciblecraft.content.block.MaterialOreBlock.HOST)
                        == com.masson.cruciblecraft.content.block.OreStoneHost.GRANITE_BLACK,
                "black-granite diamond must sit on the granite_black cube texture");
        helper.setBlock(
                POS,
                ModBlocks.oreBlock("copper", Host.STONE).get().defaultBlockState());
        helper.assertTrue(
                StoneLayerRockFeature.tryRestyleOre(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        granite),
                "large-vein unique ores must restyle to the layer cube");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.oreBlock("copper", Host.STONE).get())
                        && helper.getBlockState(POS).getValue(
                                com.masson.cruciblecraft.content.block.MaterialOreBlock.HOST)
                                == com.masson.cruciblecraft.content.block.OreStoneHost
                                        .GRANITE_BLACK,
                "restyled copper_ore must keep its id and take granite_black host");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerPlacesDenseRockOres(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.assertTrue(
                StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "coal",
                        StoneLayerStones.Role.STONE),
                "vanilla stone must become coal/dense_ore");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.layerStone("coal/dense_ore").get()),
                "BlockRockOres coal cube drifted");
        helper.setBlock(POS, Blocks.COBBLESTONE);
        helper.assertTrue(
                !StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "coal",
                        StoneLayerStones.Role.COBBLE),
                "dense layers keep vanilla cobble like GT6 mCobble");
        helper.assertTrue(
                helper.getBlockState(POS).is(Blocks.COBBLESTONE),
                "coal-layer cobble must stay Blocks.cobblestone");
        helper.assertTrue(
                StoneLayerCatalog.noDeepCutoff(-64) == -40,
                "setNoDeep cutoff is minBuildHeight+24");
        for (StoneLayerStones.Cube cube : StoneLayerStones.rockOres()) {
            helper.assertTrue(
                    ModItems.hasMaterialItem(
                            cube.material(), MaterialPrefixes.RAW_ORE),
                    cube.material() + " must keep oreRaw for BlockRockOres drops");
        }
        helper.setBlock(POS, ModBlocks.layerStone("coal/dense_ore").get());
        List<ItemStack> denseDrops = net.minecraft.world.level.block.Block.getDrops(
                helper.getBlockState(POS),
                helper.getLevel(),
                helper.absolutePos(POS),
                null,
                null,
                new ItemStack(Items.WOODEN_PICKAXE));
        ItemStack expectedRaw = MaterialLookup.stack(
                "coal", MaterialPrefixes.RAW_ORE, 2);
        helper.assertTrue(
                denseDrops.size() == 1
                        && ItemStack.isSameItemSameComponents(
                                denseDrops.get(0), expectedRaw),
                "BlockRockOres coal must drop 2 coal/raw_ore without fortune");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerPlacesNetherQuartz(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.NETHERRACK);
        helper.assertTrue(
                NetherQuartzLayerFeature.tryPlace(
                        helper.getLevel(),
                        helper.absolutePos(POS)),
                "WorldgenNetherQuartz must replace netherrack");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.layerStone("nether_quartz/dense_ore").get()),
                "BlockRockOres meta 8 must be nether_quartz/dense_ore");
        helper.setBlock(POS, Blocks.STONE);
        helper.assertTrue(
                !NetherQuartzLayerFeature.tryPlace(
                        helper.getLevel(),
                        helper.absolutePos(POS)),
                "Nether Quartz must not replace overworld stone");
        helper.assertTrue(
                helper.getBlockState(POS).is(Blocks.STONE),
                "overworld stone must stay stone when nether quartz samples it");
        helper.setBlock(POS, Blocks.BLACKSTONE);
        helper.assertTrue(
                !NetherQuartzLayerFeature.tryPlace(
                        helper.getLevel(),
                        helper.absolutePos(POS)),
                "GT6 only writes netherrack, not blackstone");
        StoneLayerNoise noise = new StoneLayerNoise(
                1,
                NetherQuartzLayerFeature.NETHER_NOISE_OFFSET);
        int low = NetherQuartzLayerFeature.sampleY(
                noise, 0, NetherQuartzLayerFeature.SAMPLE_Y_LOW, 0);
        int high = NetherQuartzLayerFeature.sampleY(
                noise, 0, NetherQuartzLayerFeature.SAMPLE_Y_HIGH, 0);
        helper.assertTrue(
                low >= NetherQuartzLayerFeature.BASE_Y
                        && low < NetherQuartzLayerFeature.BASE_Y
                                + NetherQuartzLayerFeature.SPAN
                        && high >= NetherQuartzLayerFeature.BASE_Y
                        && high < NetherQuartzLayerFeature.BASE_Y
                                + NetherQuartzLayerFeature.SPAN,
                "nether quartz Y is 40 + noise 0..199");
        helper.assertTrue(
                NetherQuartzLayerFeature.NETHER_NOISE_OFFSET == -512,
                "GT6 nether NoiseGenerator offset is 512 * dimensionId -1");
        helper.assertTrue(
                ModItems.hasMaterialItem(
                        "nether_quartz", MaterialPrefixes.RAW_ORE),
                "nether quartz must keep oreRaw for BlockRockOres drops");
        helper.setBlock(
                POS,
                ModBlocks.layerStone("nether_quartz/dense_ore").get());
        List<ItemStack> quartzDrops =
                net.minecraft.world.level.block.Block.getDrops(
                        helper.getBlockState(POS),
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        null,
                        null,
                        new ItemStack(Items.WOODEN_PICKAXE));
        ItemStack expectedQuartz = MaterialLookup.stack(
                "nether_quartz", MaterialPrefixes.RAW_ORE, 2);
        helper.assertTrue(
                quartzDrops.size() == 1
                        && ItemStack.isSameItemSameComponents(
                                quartzDrops.get(0), expectedQuartz),
                "nether quartz dense cube must drop 2 nether_quartz/raw_ore");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerOneIn128AndNoDeep(GameTestHelper helper) {
        helper.assertTrue(
                StoneLayerCatalog.PROBABILITY == 128,
                "WorldgenStoneLayers pebble chance is 1/128");
        helper.assertTrue(
                StoneLayerCatalog.NO_DEEP_Y == 24,
                "setNoDeep offset is 24 from minBuildHeight");
        StoneLayerCatalog.Layer noDeep = StoneLayerCatalog.layers().stream()
                .filter(layer -> layer.noDeep() && "granite".equals(layer.material()))
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                "granite".equals(noDeep.material()),
                "no-mod setNoDeep granite layers stay BlocksGT.Granite");
        helper.assertTrue(
                StoneLayerCatalog.DEEPSLATE.equals(
                        StoneLayerCatalog.surfaceMaterial(noDeep, 10)),
                "noDeep below y=24 must become deepslate");
        helper.assertTrue(
                "granite".equals(StoneLayerCatalog.surfaceMaterial(noDeep, 40)),
                "noDeep at y>=24 keeps granite");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerDoesNotDumpCatalog(GameTestHelper helper) {
        helper.assertTrue(
                BuiltInRegistries.FEATURE.getKey(
                                ModFeatures.STONE_LAYER_ROCKS.get())
                        .toString()
                        .equals("cruciblecraft:stone_layer_rocks"),
                "feature id drifted");
        helper.assertTrue(
                !BuiltInRegistries.FEATURE.containsKey(
                        net.minecraft.resources.ResourceLocation.parse(
                                "cruciblecraft:gt_item_scatter")),
                "retired catalog scatter must stay unregistered");
        String removeVeins = resource(
                "/data/cruciblecraft/neoforge/biome_modifier/"
                        + "remove_overworld_large_veins.json");
        helper.assertTrue(
                removeVeins.contains("neoforge:remove_features")
                        && removeVeins.contains("cruciblecraft:large_iron_vein")
                        && removeVeins.contains("underground_ores"),
                "GENERATE_STONE must remove overworld large veins");
        helper.assertTrue(
                BuiltInRegistries.FEATURE.getKey(
                                ModFeatures.NETHER_NETHERQUARTZ.get())
                        .toString()
                        .equals("cruciblecraft:nether_netherquartz"),
                "nether.netherquartz feature id drifted");
        String addNether = resource(
                "/data/cruciblecraft/neoforge/biome_modifier/"
                        + "add_nether_netherquartz.json");
        helper.assertTrue(
                addNether.contains("#minecraft:is_nether")
                        && addNether.contains("cruciblecraft:nether_netherquartz")
                        && addNether.contains("underground_decoration"),
                "WorldgenNetherQuartz must hang on nether biomes");
        String removeQuartz = resource(
                "/data/cruciblecraft/neoforge/biome_modifier/"
                        + "remove_vanilla_nether_quartz.json");
        helper.assertTrue(
                removeQuartz.contains("neoforge:remove_features")
                        && removeQuartz.contains("minecraft:ore_quartz_nether")
                        && removeQuartz.contains("minecraft:ore_quartz_deltas")
                        && removeQuartz.contains("#minecraft:is_nether"),
                "GT6 PREVENTED_ORES QUARTZ must remove vanilla nether quartz");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerManifestResolvesLocalGt6(GameTestHelper helper) {
        String manifest = resource(
                "/assets/cruciblecraft/gt6_gt_stone_layer_rocks_art_manifest.json");
        helper.assertTrue(
                manifest.contains("gt.stone.granite.black/stone.png")
                        && manifest.contains("gt.stone.granite.black/cobble.png")
                        && manifest.contains("gt.stone.granite.black/cobble_mossy.png")
                        && manifest.contains("gt.stone.andesite/small_bricks.png")
                        && manifest.contains("iconsets/ore_anthracite.png")
                        && manifest.contains("iconsets/ore_netherquartz.png")
                        && !manifest.contains("multiblock_casing")
                        && !manifest.contains("conveyor_cover"),
                "stone-layer art manifest drifted from local GT6 stones/iconsets");
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/block/gt6/stones/"
                                + "gt.stone.granite.black/stone.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6/stones/"
                                        + "gt.stone.granite.black/cobble.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6/rock_ores/"
                                        + "ore_anthracite.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6/rock_ores/"
                                        + "ore_netherquartz.png"),
                "GT6 stone cubes and BlockRockOres were not imported");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerDoesNotEatVillageCobble(GameTestHelper helper) {
        helper.assertTrue(
                StoneLayerStones.villageBricks().size() == 17,
                "BlocksGT.stones village SBRIK palette drifted");
        helper.assertTrue(
                "andesite".equals(
                        StoneLayerStones.villageBricks()
                                .get(VillageStoneBricks.ANDESITE_INDEX)
                                .material()),
                "null-biome village cobble must stay Andesite SBRIK");
        helper.assertTrue(
                StoneLayerRockFeature.isVillageHouseCobble(
                        Blocks.COBBLESTONE.defaultBlockState())
                        && !StoneLayerRockFeature.isVillageHouseCobble(
                                Blocks.COBBLED_DEEPSLATE.defaultBlockState())
                        && !StoneLayerRockFeature.isVillageHouseCobble(
                                Blocks.MOSSY_COBBLESTONE.defaultBlockState()),
                "GetVillageBlockID only restyles Blocks.cobblestone");
        helper.assertTrue(
                VillageStoneBricks.brickForBiomeId(-1)
                        .is(ModBlocks.layerOrExistingStone("andesite/small_bricks").get()),
                "biome == null uses BlocksGT.Andesite small bricks");
        helper.assertTrue(
                VillageStoneBricks.brickForBiomeId(0)
                        .is(ModBlocks.layerOrExistingStone("diorite/small_bricks").get()),
                "stones[(0+6)%17] is diorite");
        helper.setBlock(POS, Blocks.COBBLESTONE);
        helper.assertTrue(
                StoneLayerRockFeature.tryReplaceVillageBrick(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        helper.getLevel().getBiome(helper.absolutePos(POS))),
                "village cobble must become biome SBRIK, not the layer cube");
        helper.assertTrue(
                !helper.getBlockState(POS).is(Blocks.COBBLESTONE)
                        && helper.getBlockState(POS)
                                .is(VillageStoneBricks.brickForBiome(
                                                helper.getLevel().getBiome(
                                                        helper.absolutePos(POS)),
                                                helper.getLevel().registryAccess())
                                        .getBlock())
                        && !helper.getBlockState(POS)
                                .is(ModBlocks.layerStone("granite_black/cobble").get())
                        && !helper.getBlockState(POS)
                                .is(ModBlocks.layerStone("granite_black/stone").get()),
                "village house cobble must not become the noise layer cube");
        helper.setBlock(POS, Blocks.COBBLESTONE);
        helper.assertTrue(
                StoneLayerRockFeature.tryReplace(
                        helper.getLevel(),
                        helper.absolutePos(POS),
                        "granite_black",
                        StoneLayerStones.Role.COBBLE),
                "non-village cobble still becomes the layer cobble cube");
        helper.assertTrue(
                helper.getBlockState(POS).is(
                        ModBlocks.layerStone("granite_black/cobble").get()),
                "natural cobble restyle drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerCubeItemModelsExist(GameTestHelper helper) {
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/models/item/granite_black/stone.json")
                        && classpathExists(
                                "/assets/cruciblecraft/models/item/granite_black/cobble.json")
                        && classpathExists(
                                "/assets/cruciblecraft/models/item/coal/dense_ore.json")
                        && classpathExists(
                                "/assets/cruciblecraft/models/item/andesite/small_bricks.json"),
                "slash-id layer cubes must keep models/item/<path>.json");
        String stoneItem = resource(
                "/assets/cruciblecraft/models/item/granite_black/stone.json");
        helper.assertTrue(
                stoneItem.contains("cruciblecraft:granite_black/stone")
                        && !stoneItem.contains("cruciblecraft:item/item/"),
                "item model parent must be the block model, not models/item/item");
        helper.succeed();
    }

    private static boolean classpathExists(String path) {
        return GtStoneLayerRocksGameTests.class.getResource(path) != null;
    }

    private static String resource(String path) {
        try (InputStream stream =
                GtStoneLayerRocksGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(path, failure);
        }
    }
}
