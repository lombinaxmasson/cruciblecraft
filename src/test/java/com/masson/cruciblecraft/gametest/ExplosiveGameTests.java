package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.content.block.ExplosiveBlock;
import com.masson.cruciblecraft.content.blockentity.ExplosiveBlockEntity;
import com.masson.cruciblecraft.content.item.RemoteActivatorItem;
import com.masson.cruciblecraft.content.item.tool.ElectricToolCharge;
import com.masson.cruciblecraft.machine.ToolMaterialRules.ToolKind;
import com.masson.cruciblecraft.registry.ModBlocks;
import com.masson.cruciblecraft.registry.ModComponents;
import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.GameType;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * Runtime gates for GT6 Boomstick, Dynamite, Strong Dynamite, and the Remote
 * Activator.
 */
@GameTestHolder(Gt6ToolWorldBehaviorGameTests.NAMESPACE)
@PrefixGameTestTemplate(false)
public final class ExplosiveGameTests {
    private static final String TEMPLATE = "empty";
    private static final BlockPos POS = new BlockPos(4, 2, 4);

    private ExplosiveGameTests() {}

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void explosiveRegistrationsAreDistinct(GameTestHelper helper) {
        helper.assertTrue(
                ModBlocks.BOOMSTICK.get() != ModBlocks.DYNAMITE.get()
                        && ModBlocks.DYNAMITE.get() != ModBlocks.STRONG_DYNAMITE.get(),
                "explosive blocks were registered as one block");
        helper.assertTrue(
                ModItems.BOOMSTICK.get().getBlock() == ModBlocks.BOOMSTICK.get()
                        && ModItems.DYNAMITE.get().getBlock() == ModBlocks.DYNAMITE.get()
                        && ModItems.STRONG_DYNAMITE.get().getBlock()
                                == ModBlocks.STRONG_DYNAMITE.get(),
                "explosive block items lost their block identity");
        helper.assertTrue(
                ModItems.REMOTE_ACTIVATOR.get() instanceof RemoteActivatorItem,
                "remote activator item was not registered");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 130)
    public static void igniterFuseRunsForOneHundredTicks(GameTestHelper helper) {
        place(helper, POS, ModBlocks.BOOMSTICK.get());
        ExplosiveBlockEntity explosive = helper.getBlockEntity(POS);
        helper.assertTrue(explosive.ignite(), "igniter did not start the fuse");
        helper.assertTrue(explosive.fuse() == 100, "igniter fuse is not 100 ticks");
        helper.runAfterDelay(105, () -> {
            helper.assertTrue(
                    helper.getBlockState(POS).isAir(),
                    "boomstick did not detonate after its fuse");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void redstoneUsesShortFuse(GameTestHelper helper) {
        place(helper, POS, ModBlocks.DYNAMITE.get());
        helper.setBlock(POS.west(), Blocks.REDSTONE_BLOCK);
        helper.runAfterDelay(1, () -> {
            ExplosiveBlockEntity explosive = helper.getBlockEntity(POS);
            helper.assertTrue(
                    explosive != null
                            && explosive.fuse() > 0
                            && explosive.fuse() <= 20,
                    "redstone did not start the 20 tick fuse");
        });
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(
                    helper.getBlockState(POS).isAir(),
                    "redstone fuse did not detonate");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 130)
    public static void resistanceCappedCubeKeepsBoundaryAndObsidian(
            GameTestHelper helper) {
        place(helper, POS, ModBlocks.DYNAMITE.get());
        helper.setBlock(POS.east(), Blocks.STONE);
        helper.setBlock(POS.above(), Blocks.STONE);
        helper.setBlock(POS.offset(1, 1, 1), Blocks.STONE);
        helper.setBlock(POS.west(), Blocks.OBSIDIAN);
        helper.setBlock(POS.north(2), Blocks.STONE);
        ExplosiveBlockEntity cubeFuse = helper.getBlockEntity(POS);
        cubeFuse.ignite();
        helper.runAfterDelay(105, () -> {
            helper.assertTrue(
                    helper.getBlockState(POS.east()).isAir()
                            && helper.getBlockState(POS.above()).isAir()
                            && helper.getBlockState(POS.offset(1, 1, 1)).isAir(),
                    "the 3x3x3 cube did not remove breakable blocks");
            helper.assertTrue(
                    helper.getBlockState(POS.west()).is(Blocks.OBSIDIAN),
                    "the resistance cap broke obsidian");
            helper.assertTrue(
                    helper.getBlockState(POS.north(2)).is(Blocks.STONE),
                    "the explosion affected outside its 3x3x3 cube");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 130)
    public static void explosionUsesFortuneDrops(GameTestHelper helper) {
        place(helper, POS, ModBlocks.DYNAMITE.get());
        helper.setBlock(POS.east(), Blocks.IRON_ORE);
        ExplosiveBlockEntity explosive = helper.getBlockEntity(POS);
        helper.assertTrue(explosive != null, "dynamite block entity was not created");
        explosive.ignite();
        helper.runAfterDelay(105, () -> {
            AABB search = new AABB(helper.absolutePos(POS.east())).inflate(1.0D);
            helper.assertTrue(
                    helper.getLevel().getEntitiesOfClass(ItemEntity.class, search)
                            .stream()
                            .anyMatch(entity -> entity.getItem().is(Items.RAW_IRON)),
                    "explosion did not produce the ore drop");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 130)
    public static void neighboringExplosivesChain(GameTestHelper helper) {
        place(helper, POS, ModBlocks.DYNAMITE.get());
        place(helper, POS.east(), ModBlocks.STRONG_DYNAMITE.get());
        ExplosiveBlockEntity chainFuse = helper.getBlockEntity(POS);
        chainFuse.ignite();
        helper.runAfterDelay(105, () -> {
            helper.assertTrue(
                    helper.getBlockState(POS).isAir()
                            && helper.getBlockState(POS.east()).isAir(),
                    "the first explosion did not chain to its neighbor");
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 40)
    public static void handDrillEmbedsAndConsumesExplosive(
            GameTestHelper helper) {
        helper.setBlock(POS, Blocks.STONE);
        helper.setBlock(POS.above(), Blocks.AIR);
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack drill = ModItems.electricTool(ToolKind.HAND_DRILL_LV)
                .get()
                .variant("iron");
        ElectricToolCharge.applyEmpty(drill, 10_000L, 32L);
        ElectricToolCharge.setCharge(drill, 10_000L);
        ItemStack remote = new ItemStack(ModItems.REMOTE_ACTIVATOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, drill);
        player.getInventory().setItem(1, remote);
        player.getInventory().setItem(2, new ItemStack(ModItems.DYNAMITE.get()));

        helper.assertTrue(
                drill.getItem().useOn(
                                context(helper, player, drill, POS, Direction.UP))
                        .consumesAction(),
                "LV hand drill did not place dynamite");
        helper.assertTrue(
                helper.getBlockState(POS.above()).getBlock()
                        instanceof ExplosiveBlock,
                "hand drill did not place an explosive block");
        ExplosiveBlockEntity explosive = helper.getBlockEntity(POS.above());
        helper.assertTrue(
                explosive != null && explosive.isSunk(),
                "hand-drill dynamite was not marked sunk");
        helper.assertTrue(
                player.getInventory().countItem(ModItems.DYNAMITE.get()) == 0,
                "hand drill did not consume the explosive");
        helper.assertTrue(
                remote.get(ModComponents.DYNAMITE_REMOTE_TARGETS.get()) != null,
                "hand drill did not link the hotbar remote activator");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 50)
    public static void remoteActivatorBindsAndTriggers(
            GameTestHelper helper) {
        place(helper, POS, ModBlocks.DYNAMITE.get());
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(Vec3.atCenterOf(helper.absolutePos(POS)));
        ItemStack remote = new ItemStack(ModItems.REMOTE_ACTIVATOR.get());
        player.setItemInHand(InteractionHand.MAIN_HAND, remote);
        player.setShiftKeyDown(true);
        helper.assertTrue(
                remote.getItem().useOn(
                                context(helper, player, remote, POS, Direction.UP))
                        .consumesAction(),
                "remote activator did not bind the explosive");
        player.setShiftKeyDown(false);
        remote.getItem().use(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND);
        helper.assertTrue(
                remote.get(ModComponents.DYNAMITE_REMOTE_TARGETS.get()) == null,
                "remote target was not consumed after activation");
        player.discard();
        helper.runAfterDelay(25, () -> {
            helper.assertTrue(
                    helper.getBlockState(POS).isAir(),
                    "remote activator did not trigger the explosive");
            helper.succeed();
        });
    }

    private static void place(
            GameTestHelper helper,
            BlockPos pos,
            net.minecraft.world.level.block.Block block) {
        helper.setBlock(pos, block.defaultBlockState());
    }

    private static UseOnContext context(
            GameTestHelper helper,
            Player player,
            ItemStack stack,
            BlockPos pos,
            Direction face) {
        BlockPos absolute = helper.absolutePos(pos);
        return new UseOnContext(
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                stack,
                new BlockHitResult(
                        Vec3.atCenterOf(absolute),
                        face,
                        absolute,
                        false));
    }
}
