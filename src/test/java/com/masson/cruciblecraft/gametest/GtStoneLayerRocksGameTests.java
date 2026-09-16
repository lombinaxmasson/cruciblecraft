package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.masson.cruciblecraft.content.block.GtSurfaceRockBlock;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.content.blockentity.GtSurfaceRockBlockEntity;
import com.masson.cruciblecraft.material.prefix.MaterialPrefixCatalog;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.StoneLayerCatalog;
import com.masson.cruciblecraft.worldgen.StoneLayerNoise;
import com.masson.cruciblecraft.worldgen.StoneLayerRockFeature;
import com.masson.cruciblecraft.worldgen.StoneLayerStones;
import com.masson.cruciblecraft.worldgen.SurfaceRockContents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 WorldgenStoneLayers cubes and pebbles. Run with
 * {@code -PwaveRecipes=worldgen/gt-stone-layer-rocks}.
 */
@GameTestHolder(GtStoneLayerRocksGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GtStoneLayerRocksGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_wave_worldgen_gt_stone_layer_rocks";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(8, 2, 8);

    private GtStoneLayerRocksGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerCatalogMatchesLoaderWorldgen(GameTestHelper helper) {
        helper.assertTrue(
                StoneLayerCatalog.layers().size() == 123,
                "no-mod GT6 LAYERS weight drifted");
        helper.assertTrue(
                StoneLayerCatalog.layers().stream()
                        .anyMatch(layer -> "granite_black".equals(layer.material())),
                "granite_black must be a layer surface material");
        helper.assertTrue(
                StoneLayerCatalog.layers().stream()
                        .filter(StoneLayerCatalog.Layer::noDeep)
                        .count()
                        == 3,
                "setNoDeep granite layers drifted");
        helper.assertTrue(
                StoneLayerStones.cubes().size() == 45,
                "BlocksGT native stone/cobble/mossy cubes drifted");
        helper.assertTrue(
                ModBlocks.hasLayerStone("granite_black/stone")
                        && ModBlocks.hasLayerStone("granite_black/cobble")
                        && !ModBlocks.hasLayerStone("marble/stone"),
                "granite_black cubes register; marble/stone reuses gt_stone_catalog");
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
        helper.setBlock(POS.above(), Blocks.AIR);
        BlockPos ore = helper.absolutePos(POS);
        StoneLayerRockFeature.decorateColumn(
                helper.getLevel(),
                ore.getX(),
                ore.getZ(),
                ore.getY(),
                ore.getY() + 2,
                new StoneLayerNoise(1L),
                RandomSource.create(1L),
                1);
        helper.assertTrue(
                helper.getBlockState(POS).is(Blocks.IRON_ORE),
                "vanilla ore cells must stay");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneLayerOneIn128AndNoDeep(GameTestHelper helper) {
        helper.assertTrue(
                StoneLayerCatalog.PROBABILITY == 128,
                "WorldgenStoneLayers pebble chance is 1/128");
        helper.assertTrue(
                StoneLayerCatalog.NO_DEEP_Y == 24,
                "setNoDeep cutoff is y<24");
        StoneLayerCatalog.Layer noDeep = StoneLayerCatalog.layers().stream()
                .filter(StoneLayerCatalog.Layer::noDeep)
                .findFirst()
                .orElseThrow();
        helper.assertTrue(
                "granite".equals(noDeep.material()),
                "no-mod setNoDeep layers are BlocksGT.Granite");
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
                        && !manifest.contains("multiblock_casing")
                        && !manifest.contains("conveyor_cover"),
                "stone-layer art manifest drifted from local GT6 stones");
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/block/gt6/stones/"
                                + "gt.stone.granite.black/stone.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/block/gt6/stones/"
                                        + "gt.stone.granite.black/cobble.png"),
                "GT6 stone cubes were not imported");
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
