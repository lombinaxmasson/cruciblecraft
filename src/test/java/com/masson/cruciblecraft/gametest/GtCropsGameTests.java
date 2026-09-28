package com.masson.cruciblecraft.gametest;

import java.util.Random;
import java.util.Set;

import com.masson.cruciblecraft.content.block.GtBushBlock;
import com.masson.cruciblecraft.content.blockentity.GtBushBlockEntity;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModFeatures;
import com.masson.cruciblecraft.registry.ModItems;
import com.masson.cruciblecraft.worldgen.crop.GlowtusColor;
import com.masson.cruciblecraft.worldgen.crop.GtCropFeature;
import com.masson.cruciblecraft.worldgen.crop.GtCropKind;
import com.masson.cruciblecraft.worldgen.crop.GtCropPlacement;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Isolated GT crops gate. Run with
 * {@code -PgameTestGrid=worldgen}.
 */
@GameTestHolder(GtCropsGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class GtCropsGameTests {
    public static final String NAMESPACE =
            "cruciblecraft_worldgen";
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(8, 2, 8);

    private GtCropsGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void playerSurfaceIsRegistered(GameTestHelper helper) {
        for (GlowtusColor color : GlowtusColor.ALL) {
            helper.assertTrue(
                    ModBlocks.glowtus(color).get() != null,
                    "Missing glowtus " + color.id());
            helper.assertTrue(
                    ModItems.glowtusItem(color).get() != null,
                    "Missing glowtus item " + color.id());
        }
        helper.assertTrue(ModBlocks.GT_BUSH.get() != null, "gt_bush block missing");
        helper.assertTrue(ModItems.GT_BUSH.get() != null, "gt_bush item missing");
        helper.assertTrue(ModFeatures.GT_CROP.get() != null, "gt_crop feature missing");
        helper.assertTrue(
                BuiltInRegistries.ITEM.getKey(ModItems.glowtusItem(GlowtusColor.BLACK).get())
                        .getPath()
                        .equals("plant/glowtus_black"),
                "black glowtus must not reuse lilypad_glowtus/white_glowtus");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void biomeFilterMatchesGt6SurfaceSets(GameTestHelper helper) {
        helper.assertTrue(
                GtCropPlacement.canGenerate(
                        GtCropKind.GLOWTUS, Set.of("minecraft:jungle"))
                        == 16,
                "glowtus jungle amount");
        helper.assertTrue(
                GtCropPlacement.canGenerate(
                        GtCropKind.GLOWTUS, Set.of("minecraft:plains"))
                        == 0,
                "glowtus rejects plains");
        helper.assertTrue(
                GtCropPlacement.canGenerate(
                        GtCropKind.BUSH, Set.of("minecraft:forest"))
                        == 1,
                "bush forest amount");
        helper.assertTrue(
                GtCropPlacement.canGenerate(
                        GtCropKind.BUSH, Set.of("minecraft:snowy_taiga"))
                        == 0,
                "bush rejects frozen");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void glowtusPlacesOnWater(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.WATER);
        helper.setBlock(POS.above(), Blocks.AIR);
        boolean placed = GtCropFeature.tryPlaceGlowtus(
                helper.getLevel(),
                helper.absolutePos(POS).getX(),
                helper.absolutePos(POS).getY(),
                helper.absolutePos(POS).getZ(),
                new Random(1L),
                Blocks.WATER.defaultBlockState());
        helper.assertTrue(placed, "glowtus did not place on water");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getBlock()
                        instanceof com.masson.cruciblecraft.content.block.GlowtusBlock,
                "glowtus block missing above water");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bushPlacesSweetBerriesNotString(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.GRASS_BLOCK);
        helper.setBlock(POS.above(), Blocks.AIR);
        boolean placed = GtCropFeature.tryPlaceBush(
                helper.getLevel(),
                helper.absolutePos(POS).getX(),
                helper.absolutePos(POS).getY(),
                helper.absolutePos(POS).getZ(),
                new Random(1L),
                Blocks.GRASS_BLOCK.defaultBlockState());
        helper.assertTrue(placed, "bush did not place");
        helper.assertTrue(
                helper.getBlockState(POS).is(Blocks.DIRT),
                "grass under bush must become dirt");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getBlock() instanceof GtBushBlock,
                "bush core missing");
        GtBushBlockEntity bush = (GtBushBlockEntity) helper.getBlockEntity(POS.above());
        helper.assertTrue(bush != null, "bush entity missing");
        helper.assertTrue(
                bush.berry().is(Items.SWEET_BERRIES),
                "default berry must be sweet berries");
        helper.assertTrue(!bush.berry().is(Items.STRING), "must not drop string");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void bushHarvestAndBerrySet(GameTestHelper helper) {
        helper.setBlock(POS, Blocks.DIRT);
        helper.setBlock(
                POS.above(),
                ModBlocks.GT_BUSH.get()
                        .defaultBlockState()
                        .setValue(GtBushBlock.FACING, Direction.DOWN)
                        .setValue(GtBushBlock.STAGE, 0));
        GtBushBlockEntity bush = (GtBushBlockEntity) helper.getBlockEntity(POS.above());
        helper.assertTrue(bush != null, "bush entity missing");
        bush.setBerry(ItemStack.EMPTY);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLOW_BERRIES));
        helper.getBlockState(POS.above()).useItemOn(
                player.getItemInHand(InteractionHand.MAIN_HAND),
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(helper.absolutePos(POS.above())),
                        Direction.UP,
                        helper.absolutePos(POS.above()),
                        false));
        helper.assertTrue(
                bush.berry().is(Items.GLOW_BERRIES),
                "right-click did not set glow berries");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        bush.setStage(GtBushBlock.MAX_STAGE);
        helper.getBlockState(POS.above()).useWithoutItem(
                helper.getLevel(),
                player,
                new BlockHitResult(
                        Vec3.atCenterOf(helper.absolutePos(POS.above())),
                        Direction.UP,
                        helper.absolutePos(POS.above()),
                        false));
        helper.assertTrue(
                player.getInventory().contains(new ItemStack(Items.GLOW_BERRIES)),
                "ripe bush did not harvest");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getValue(GtBushBlock.STAGE) == 0,
                "stage after harvest");
        helper.succeed();
    }
}
