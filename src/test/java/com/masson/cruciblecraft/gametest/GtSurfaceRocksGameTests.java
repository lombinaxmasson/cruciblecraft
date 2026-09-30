package com.masson.cruciblecraft.gametest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Random;
import java.util.Set;

import com.masson.cruciblecraft.api.material.MaterialLookup;
import com.masson.cruciblecraft.api.material.MaterialPrefixes;
import com.masson.cruciblecraft.content.block.GtSurfaceRockBlock;
import com.masson.cruciblecraft.content.block.RockBlock;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.machine.ToolMaterialRules;
import com.masson.cruciblecraft.worldgen.PebbleShape;
import com.masson.cruciblecraft.worldgen.SurfaceRockAppearance;
import com.masson.cruciblecraft.worldgen.SurfaceRockContents;
import com.masson.cruciblecraft.worldgen.SurfaceRockFeature;
import com.masson.cruciblecraft.worldgen.SurfaceRockPlacement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT6 overworld surface rocks. Run with
 * {@code -PgameTestGrid=worldgen}.
 */
@GameTestHolder(GtSurfaceRocksGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GtSurfaceRocksGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_worldgen";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(8, 2, 8);

    private GtSurfaceRocksGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void surfaceRockManifestResolvesLocalGt6(GameTestHelper helper) {
        String manifest = resource(
                "/assets/cruciblecraft/gt6_gt_surface_rocks_art_manifest.json");
        helper.assertTrue(
                manifest.contains("materialicons/dull/rockgt.png")
                        && manifest.contains("rockgt_overlay.png")
                        && !manifest.contains("multiblock_casing")
                        && !manifest.contains("conveyor_cover"),
                "surface rock art manifest drifted from local GT6 rockgt");
        helper.assertTrue(
                classpathExists(
                        "/assets/cruciblecraft/textures/item/material/rock.png")
                        && classpathExists(
                                "/assets/cruciblecraft/textures/item/material/"
                                        + "rock_overlay.png"),
                "GT6 rockgt was not imported");
        helper.assertTrue(
                ModBlocks.GT_SURFACE_ROCK.get() != null
                        && ModItems.GT_SURFACE_ROCK.get() != null
                        && ModFeatures.SURFACE_ROCK_SCATTER.get() != null,
                "gt_surface_rock registry missing");
        helper.assertTrue(
                BuiltInRegistries.BLOCK.getKey(ModBlocks.GT_SURFACE_ROCK.get())
                        .getPath()
                        .equals("gt_surface_rock"),
                "placer id drifted");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void surfaceRockPlaces32757NotCatalogTag(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.DIRT);
        helper.setBlock(POS.above(), Blocks.AIR);
        boolean placed = SurfaceRockFeature.tryPlace(
                helper.getLevel(),
                helper.absolutePos(POS).getX(),
                helper.absolutePos(POS).getY(),
                helper.absolutePos(POS).getZ(),
                new Random(1L),
                Blocks.DIRT.defaultBlockState());
        helper.assertTrue(placed, "pebble did not place on dirt");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getBlock() instanceof GtSurfaceRockBlock,
                "must place gt_surface_rock");
        helper.assertTrue(
                !(helper.getBlockState(POS.above()).getBlock() instanceof RockBlock),
                "must not place RockBlock");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getShape(
                                helper.getLevel(),
                                helper.absolutePos(POS.above()))
                        .bounds()
                        .equals(PebbleShape.shape(
                                helper.absolutePos(POS.above())).bounds()),
                "outline must match packed pebble mesh");

        helper.setBlock(POS.offset(2, 0, 0), Blocks.SAND);
        helper.setBlock(POS.offset(2, 1, 0), Blocks.AIR);
        helper.assertTrue(
                SurfaceRockFeature.tryPlace(
                        helper.getLevel(),
                        helper.absolutePos(POS.offset(2, 0, 0)).getX(),
                        helper.absolutePos(POS.offset(2, 0, 0)).getY(),
                        helper.absolutePos(POS.offset(2, 0, 0)).getZ(),
                        new Random(2L),
                        Blocks.SAND.defaultBlockState()),
                "pebble did not place on sand");
        helper.assertTrue(
                helper.getBlockState(POS.offset(2, 1, 0))
                        .getValue(SurfaceRockAppearance.PROPERTY)
                        == SurfaceRockAppearance.SANDSTONE,
                "sand contact must copy sandstone");
        helper.assertTrue(
                helper.getBlockState(POS.offset(2, 1, 0)).getShape(
                                helper.getLevel(),
                                helper.absolutePos(POS.offset(2, 1, 0)))
                        .bounds()
                        .equals(PebbleShape.shape(
                                helper.absolutePos(POS.offset(2, 1, 0))).bounds()),
                "sandstone pebble outline must follow positional AABB");

        helper.setBlock(POS.offset(4, 0, 0), Blocks.DIRT);
        helper.setBlock(POS.offset(4, 1, 0), Blocks.STONE);
        helper.assertTrue(
                !SurfaceRockFeature.tryPlace(
                        helper.getLevel(),
                        helper.absolutePos(POS.offset(4, 0, 0)).getX(),
                        helper.absolutePos(POS.offset(4, 0, 0)).getY(),
                        helper.absolutePos(POS.offset(4, 0, 0)).getZ(),
                        new Random(3L),
                        Blocks.DIRT.defaultBlockState()),
                "occupied cell must stay");
        helper.assertTrue(
                helper.getBlockState(POS.offset(4, 1, 0)).is(Blocks.STONE),
                "must not overwrite occupied surface cell");
        helper.assertTrue(
                !SurfaceRockFeature.tryPlace(
                        helper.getLevel(),
                        helper.absolutePos(POS).getX(),
                        helper.absolutePos(POS).getY(),
                        helper.absolutePos(POS).getZ(),
                        new Random(4L),
                        Blocks.FARMLAND.defaultBlockState()),
                "farmland must be skipped");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void surfaceRockLootMatchesWorldgenRocks(GameTestHelper helper) {
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(
                        GtSurfaceRockBlock.loot(pebble(SurfaceRockContents.EMPTY)),
                        new ItemStack(ModItems.materialItem(
                                "stone",
                                com.masson.cruciblecraft.material.prefix
                                        .MaterialPrefixCatalog.require("rock")).get())),
                "empty pebble must drop stone/rock");
        helper.assertTrue(
                GtSurfaceRockBlock.loot(pebble(SurfaceRockContents.FLINT)).is(Items.FLINT),
                "flint pebble must drop flint");
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(
                        GtSurfaceRockBlock.loot(pebble(SurfaceRockContents.METEORIC_ROCK)),
                        new ItemStack(ModItems.materialItem(
                                "meteoric_iron",
                                com.masson.cruciblecraft.material.prefix
                                        .MaterialPrefixCatalog.require("rock")).get())),
                "meteoric pebble must drop meteoric_iron/rock");
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(
                        GtSurfaceRockBlock.loot(pebble(SurfaceRockContents.METEORIC_RAW)),
                        MaterialLookup.stack(
                                "meteoric_iron", MaterialPrefixes.RAW_ORE)),
                "meteoric raw pebble must drop meteoric_iron/raw_ore");
        List<ItemStack> drops = net.minecraft.world.level.block.Block.getDrops(
                pebble(SurfaceRockContents.FLINT),
                helper.getLevel(),
                helper.absolutePos(POS),
                null);
        helper.assertTrue(
                drops.size() == 1 && drops.get(0).is(Items.FLINT),
                "loot table must match flint contents");
        helper.assertTrue(
                SurfaceRockFeature.rollContents(new Random(0L), 2)
                        != null,
                "WorldgenRocks amount roll is defined");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void biomeFilterMatchesGt6OverworldSets(GameTestHelper helper) {
        helper.assertTrue(
                SurfaceRockPlacement.canGenerate(Set.of("minecraft:plains")) == 2,
                "plains amount");
        helper.assertTrue(
                SurfaceRockPlacement.canGenerate(Set.of("minecraft:desert")) == 2,
                "desert amount");
        helper.assertTrue(
                SurfaceRockPlacement.canGenerate(Set.of("minecraft:taiga")) == 2,
                "taiga amount");
        helper.assertTrue(
                SurfaceRockPlacement.canGenerate(Set.of("minecraft:jungle")) == 0,
                "jungle is out of overworld.rocks vanilla cores");
        helper.assertTrue(
                SurfaceRockPlacement.canGenerate(Set.of("minecraft:ocean")) == 0,
                "ocean is out of overworld.rocks");
        helper.assertTrue(
                SurfaceRockPlacement.canGenerate(Set.of("minecraft:beach")) == 0,
                "beach is out of overworld.rocks");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void surfaceRockOutlineMatchesPositionalAabb(GameTestHelper helper) {
        BlockPos first = helper.absolutePos(POS);
        BlockPos second = helper.absolutePos(POS.offset(3, 1, 2));
        int packedFirst = PebbleShape.pack(first);
        int packedSecond = PebbleShape.pack(second);
        helper.assertTrue(
                packedFirst != packedSecond,
                "GT6 x^y^z pebble RNG must vary by position");
        helper.assertTrue(
                PebbleShape.shape(first).bounds().equals(
                        PebbleShape.pixels(packedFirst).voxel().bounds()),
                "outline AABB must equal packed mesh");
        helper.assertTrue(
                !PebbleShape.shape(first).bounds().equals(
                        PebbleShape.shape(second).bounds()),
                "two positions must not share one cube");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void surfaceRockPlacesOnlyWhenSneaking(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.SAND);
        helper.setBlock(POS.above(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack stack = new ItemStack(ModItems.GT_SURFACE_ROCK.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos sand = helper.absolutePos(POS);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(sand).add(0.0, 0.5, 0.0),
                Direction.UP,
                sand,
                false);
        UseOnContext standing = new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                hit);
        helper.assertTrue(
                stack.getItem().useOn(standing) == InteractionResult.PASS,
                "standing right-click must not place a pebble");
        helper.assertTrue(
                helper.getBlockState(POS.above()).isAir(),
                "sand surface stays empty without sneak");
        player.setShiftKeyDown(true);
        UseOnContext sneaking = new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                hit);
        helper.assertTrue(
                stack.getItem().useOn(sneaking).consumesAction(),
                "shift+right-click must place");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getBlock()
                        instanceof GtSurfaceRockBlock,
                "sneak place puts gt_surface_rock");
        helper.assertTrue(
                helper.getBlockState(POS.above())
                        .getValue(SurfaceRockAppearance.PROPERTY)
                        == SurfaceRockAppearance.SANDSTONE,
                "placing on sand copies sandstone look");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void stoneToolHarvestFollowsGt6Quality(GameTestHelper helper) {
        helper.assertTrue(
                ToolMaterialRules.miningTier("stone") == Tiers.STONE
                        && ToolMaterialRules.miningTier("granite") == Tiers.STONE,
                "stone and granite tools stay quality 1 / stone harvest");
        helper.assertTrue(
                ToolMaterialRules.miningTier("granite_black") == Tiers.DIAMOND
                        && ToolMaterialRules.miningTier("granite_red")
                                == Tiers.DIAMOND,
                "black/red granite tools are quality 3 / diamond harvest");
        helper.assertTrue(
                ToolMaterialRules.durability(
                                ToolMaterialRules.ToolKind.PICKAXE, "stone")
                        == 16
                        && ToolMaterialRules.durability(
                                ToolMaterialRules.ToolKind.PICKAXE,
                                "granite_black")
                                == 64,
                "black granite pick durability is 64 versus stone 16");
        helper.assertTrue(
                ItemStack.isSameItemSameComponents(
                        GtSurfaceRockBlock.loot(pebble(SurfaceRockContents.EMPTY)),
                        new ItemStack(ModItems.materialItem(
                                "stone",
                                com.masson.cruciblecraft.material.prefix
                                        .MaterialPrefixCatalog.require("rock"))
                                .get())),
                "overworld empty 32757 is still stone/rock, not granite");
        helper.assertTrue(
                ModBlocks.hasRockBlock("granite")
                        && ModBlocks.hasRockBlock("granite_black")
                        && ModBlocks.rockBlock("granite").get().materialId()
                                .equals("granite"),
                "granite OP.rockGt identity is the catalog RockBlock");
        helper.succeed();
    }

    private static BlockState pebble(SurfaceRockContents contents) {
        return ModBlocks.GT_SURFACE_ROCK.get().defaultBlockState()
                .setValue(SurfaceRockContents.PROPERTY, contents);
    }

    private static boolean classpathExists(String path) {
        return GtSurfaceRocksGameTests.class.getResource(path) != null;
    }

    private static String resource(String path) {
        try (InputStream stream =
                GtSurfaceRocksGameTests.class.getResourceAsStream(path)) {
            if (stream == null) {
                throw new IllegalStateException("missing " + path);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException failure) {
            throw new IllegalStateException(path, failure);
        }
    }
}
