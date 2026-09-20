package com.masson.cruciblecraft.gametest;

import java.util.Random;
import java.util.Set;

import com.masson.cruciblecraft.content.block.GtTreeHoleBlock;
import com.masson.cruciblecraft.content.block.WoodDebark;
import com.masson.cruciblecraft.content.blockentity.GtTreeHoleBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.tree.GtTreeHoleTracker;
import com.masson.cruciblecraft.worldgen.tree.LevelGtTreeWorld;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeGrower;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreePlacement;
import com.masson.cruciblecraft.worldgen.tree.prep.GtTreeSpecies;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT trees gate. Run with
 * {@code -PgameTestNamespaces=cruciblecraft_wave_worldgen_gt_trees}.
 * {@code -PwaveRecipes} is for compact recipe waves, not this worldgen slug.
 */
@GameTestHolder(GtTreesGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GtTreesGameTests {
    public static final String NAMESPACE = "cruciblecraft_wave_worldgen_gt_trees";
    private static final String TEMPLATE = "empty";
    private static final BlockPos SAPLING = new BlockPos(8, 2, 8);

    private GtTreesGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            helper.assertTrue(
                    ModBlocks.treeSapling(species).get() != null,
                    "Missing sapling " + species.id());
            helper.assertTrue(
                    ModBlocks.treeLog(species).get() != null,
                    "Missing log " + species.id());
            helper.assertTrue(
                    ModBlocks.treeLeaves(species).get() != null,
                    "Missing leaves " + species.id());
            if (species.hasHole()) {
                helper.assertTrue(
                        ModBlocks.treeHole(species).get() != null,
                        "Missing hole " + species.id());
            }
        }
        helper.assertTrue(
                ModItems.RUBBER_RESIN.get() != null, "rubber_resin item missing");
        helper.assertTrue(
                ModFeatures.GT_TREE.get() != null, "gt_tree feature missing");
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            helper.assertTrue(
                    ModBlocks.treeBeam(species).get() != null,
                    "Missing beam " + species.id());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void everySpeciesGrowsLogsAndLeaves(GameTestHelper helper) {
        GtTreeHoleTracker.clear(helper.getLevel());
        for (GtTreeSpecies species : GtTreeSpecies.ALL) {
            clearTreeArea(helper, SAPLING);
            helper.setBlock(SAPLING.below(), Blocks.DIRT);
            helper.setBlock(SAPLING, ModBlocks.treeSapling(species).get());
            boolean grew = GtTreeGrower.grow(
                    species,
                    new LevelGtTreeWorld(helper.getLevel(), species),
                    helper.absolutePos(SAPLING).getX(),
                    helper.absolutePos(SAPLING).getY(),
                    helper.absolutePos(SAPLING).getZ(),
                    new Random(1L));
            helper.assertTrue(grew, "Grow failed: " + species.id());
            helper.assertTrue(
                    hasNearby(helper, SAPLING, 8, ModBlocks.treeLog(species).get())
                            || (species == GtTreeSpecies.RUBBER
                                    && hasNearby(
                                            helper,
                                            SAPLING,
                                            8,
                                            ModBlocks.treeHole(GtTreeSpecies.RUBBER).get())),
                    "No logs after grow: " + species.id());
            helper.assertTrue(
                    hasNearby(helper, SAPLING, 8, ModBlocks.treeLeaves(species).get()),
                    "No leaves after grow: " + species.id());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void rubberGrowPlacesOneHole(GameTestHelper helper) {
        GtTreeHoleTracker.clear(helper.getLevel());
        helper.setBlock(SAPLING.below(), Blocks.DIRT);
        helper.setBlock(SAPLING, ModBlocks.treeSapling(GtTreeSpecies.RUBBER).get());
        helper.assertTrue(
                GtTreeGrower.grow(
                        GtTreeSpecies.RUBBER,
                        new LevelGtTreeWorld(helper.getLevel(), GtTreeSpecies.RUBBER),
                        helper.absolutePos(SAPLING).getX(),
                        helper.absolutePos(SAPLING).getY(),
                        helper.absolutePos(SAPLING).getZ(),
                        new Random(1L)),
                "Rubber grow failed");
        helper.assertTrue(
                hasNearby(helper, SAPLING, 12, ModBlocks.treeHole(GtTreeSpecies.RUBBER).get()),
                "Rubber grow did not place a resin hole");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void mapleAndRainbowoodGrowPlaceNoHoles(GameTestHelper helper) {
        for (GtTreeSpecies species : new GtTreeSpecies[] {
            GtTreeSpecies.MAPLE, GtTreeSpecies.RAINBOWOOD
        }) {
            clearTreeArea(helper, SAPLING);
            helper.setBlock(SAPLING.below(), Blocks.DIRT);
            helper.setBlock(SAPLING, ModBlocks.treeSapling(species).get());
            helper.assertTrue(
                    GtTreeGrower.grow(
                            species,
                            new LevelGtTreeWorld(helper.getLevel(), species),
                            helper.absolutePos(SAPLING).getX(),
                            helper.absolutePos(SAPLING).getY(),
                            helper.absolutePos(SAPLING).getZ(),
                            new Random(1L)),
                    "Grow failed: " + species.id());
            helper.assertFalse(
                    hasNearby(helper, SAPLING, 8, ModBlocks.treeHole(species).get()),
                    species.id() + " grow must not place a hole");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void handDrillTapsMapleAndRainbowood(GameTestHelper helper) {
        for (GtTreeSpecies species : new GtTreeSpecies[] {
            GtTreeSpecies.MAPLE, GtTreeSpecies.RAINBOWOOD
        }) {
            BlockPos pos = species == GtTreeSpecies.MAPLE
                    ? new BlockPos(2, 2, 2)
                    : new BlockPos(4, 2, 2);
            helper.setBlock(pos, ModBlocks.treeLog(species).get());
            Player player = helper.makeMockPlayer(GameType.SURVIVAL);
            ItemStack drill = ModItems.MATERIAL_HAND_DRILL.get().variant("iron");
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(helper.absolutePos(pos)),
                    Direction.NORTH,
                    helper.absolutePos(pos),
                    false);
            helper.getBlockState(pos).useItemOn(
                    drill, helper.getLevel(), player, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(
                    helper.getBlockState(pos).getBlock()
                            == ModBlocks.treeHole(species).get(),
                    "Hand drill did not tap " + species.id());
            player.discard();
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void rubberHoleEmptyHandGivesResin(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(
                pos,
                ModBlocks.treeHole(GtTreeSpecies.RUBBER)
                        .get()
                        .defaultBlockState()
                        .setValue(GtTreeHoleBlock.FACING, Direction.NORTH)
                        .setValue(GtTreeHoleBlock.HAS_PRODUCT, Boolean.TRUE));
        if (helper.getBlockEntity(pos) instanceof GtTreeHoleBlockEntity hole) {
            hole.setHasProduct(true);
        }
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        BlockHitResult hit = new BlockHitResult(
                Vec3.atCenterOf(helper.absolutePos(pos)),
                Direction.NORTH,
                helper.absolutePos(pos),
                false);
        helper.getBlockState(pos).useWithoutItem(helper.getLevel(), player, hit);
        helper.assertTrue(
                player.getInventory().contains(new ItemStack(ModItems.RUBBER_RESIN.get())),
                "Empty-hand rubber hole did not give rubber_resin");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void axeStripsGtLogToBeamAndDropsBark(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.treeLog(GtTreeSpecies.RUBBER).get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = ModItems.MATERIAL_AXE.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        helper.assertTrue(
                axe.getItem().useOn(useOn(helper, player, axe, pos)).consumesAction(),
                "axe did not strip rubber log");
        helper.assertTrue(
                helper.getBlockState(pos).getBlock()
                        == ModBlocks.treeBeam(GtTreeSpecies.RUBBER).get(),
                "rubber log did not become a beam");
        helper.assertTrue(
                player.getInventory().contains(WoodDebark.barkDust(1)),
                "axe strip did not give bark dust");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void vanillaAxeStripsOakLogAndDropsBark(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, Blocks.OAK_LOG);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = new ItemStack(Items.IRON_AXE);
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        helper.assertTrue(
                axe.getItem().useOn(useOn(helper, player, axe, pos)).consumesAction(),
                "vanilla axe did not strip oak log");
        helper.assertTrue(
                helper.getBlockState(pos).is(Blocks.STRIPPED_OAK_LOG),
                "oak log did not become stripped oak");
        helper.assertTrue(
                player.getInventory().contains(WoodDebark.barkDust(1)),
                "vanilla axe strip did not give bark dust");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void cinnamonAxeStripDropsCatalogBark(GameTestHelper helper) {
        BlockPos pos = new BlockPos(2, 2, 2);
        helper.setBlock(pos, ModBlocks.treeLog(GtTreeSpecies.CINNAMON).get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack axe = ModItems.MATERIAL_AXE.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, axe);
        helper.assertTrue(
                axe.getItem().useOn(useOn(helper, player, axe, pos)).consumesAction(),
                "axe did not strip cinnamon log");
        helper.assertTrue(
                helper.getBlockState(pos).getBlock()
                        == ModBlocks.treeBeam(GtTreeSpecies.CINNAMON).get(),
                "cinnamon log did not become a beam");
        helper.assertTrue(
                player.getInventory().contains(WoodDebark.cinnamonBark(2)),
                "cinnamon strip did not give catalog bark");
        helper.assertTrue(
                !player.getInventory().contains(WoodDebark.barkDust(1)),
                "cinnamon strip must not give bark dust");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void biomeFilterMatchesFrozenMap(GameTestHelper helper) {
        helper.assertTrue(
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.RUBBER, Set.of("minecraft:taiga"), new Random(1L))
                        > 0,
                "Rubber must generate in taiga");
        helper.assertTrue(
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.RUBBER, Set.of("minecraft:desert"), new Random(1L))
                        == 0,
                "Rubber must not generate in desert");
        helper.assertTrue(
                GtTreePlacement.canGenerate(
                        GtTreeSpecies.COCONUT, Set.of("minecraft:beach"), new Random(1L))
                        > 0,
                "Coconut must generate on beach");
        helper.succeed();
    }

    private static UseOnContext useOn(
            GameTestHelper helper,
            Player player,
            ItemStack stack,
            BlockPos pos) {
        BlockPos abs = helper.absolutePos(pos);
        return new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                new BlockHitResult(
                        Vec3.atCenterOf(abs),
                        Direction.NORTH,
                        abs,
                        false));
    }

    private static boolean hasNearby(
            GameTestHelper helper, BlockPos origin, int radius, net.minecraft.world.level.block.Block block) {
        BlockPos worldOrigin = helper.absolutePos(origin);
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -1; dy <= 20; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (helper.getLevel()
                                    .getBlockState(worldOrigin.offset(dx, dy, dz))
                                    .getBlock()
                            == block) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static void clearTreeArea(GameTestHelper helper, BlockPos origin) {
        BlockPos worldOrigin = helper.absolutePos(origin);
        for (int dx = -8; dx <= 8; dx++) {
            for (int dy = -1; dy <= 20; dy++) {
                for (int dz = -8; dz <= 8; dz++) {
                    helper.getLevel()
                            .setBlock(
                                    worldOrigin.offset(dx, dy, dz),
                                    Blocks.AIR.defaultBlockState(),
                                    3);
                }
            }
        }
    }
}
