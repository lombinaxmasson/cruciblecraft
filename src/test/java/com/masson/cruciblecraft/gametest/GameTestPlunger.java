package com.masson.cruciblecraft.gametest;

import com.masson.cruciblecraft.registry.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Survival iron-plunger click used by isolated energy-machine GameTests. */
final class GameTestPlunger {
    private GameTestPlunger() {}

    static void click(GameTestHelper helper, BlockPos pos) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        ItemStack plunger = ModItems.MATERIAL_PLUNGER.get().variant("iron");
        player.setItemInHand(InteractionHand.MAIN_HAND, plunger);
        BlockPos absolute = helper.absolutePos(pos);
        helper.getBlockState(pos).useItemOn(
                plunger,
                helper.getLevel(),
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        new Vec3(
                                absolute.getX() + 0.5,
                                absolute.getY() + 1.0,
                                absolute.getZ() + 0.5),
                        Direction.UP,
                        absolute,
                        false));
        player.discard();
    }
}
