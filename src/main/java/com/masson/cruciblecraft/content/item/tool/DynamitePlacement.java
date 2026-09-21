package com.masson.cruciblecraft.content.item.tool;

import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.block.ExplosiveBlock;
import com.masson.cruciblecraft.content.block.GtStoneBlock;
import com.masson.cruciblecraft.content.blockentity.ExplosiveBlockEntity;
import com.masson.cruciblecraft.content.item.RemoteActivatorItem;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 LV hand-drill dynamite placement. The drill embeds a stick into the
 * clicked host block and consumes one explosive from the player's inventory.
 */
public final class DynamitePlacement {
    private DynamitePlacement() {}

    public static ToolResult use(UseOnContext context) {
        Level level = context.getLevel();
        Player player = context.getPlayer();
        if (player == null || !player.mayBuild()) {
            return ToolResult.PASS;
        }
        BlockPos hostPos = context.getClickedPos();
        BlockState host = level.getBlockState(hostPos);
        if (!isDrillableHost(host, level, hostPos)) {
            return ToolResult.PASS;
        }

        Direction facing = context.getClickedFace();
        BlockPos targetPos = hostPos.relative(facing);
        if (!level.getBlockState(targetPos).isAir()) {
            return ToolResult.PASS;
        }
        ItemStack explosive = findExplosive(player);
        if (explosive.isEmpty()) {
            return ToolResult.PASS;
        }
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }

        ExplosiveBlock block = (ExplosiveBlock) Block.byItem(explosive.getItem());
        BlockState state = block.defaultBlockState()
                .setValue(ExplosiveBlock.FACING, facing);
        if (!level.setBlock(targetPos, state, Block.UPDATE_ALL)) {
            return ToolResult.PASS;
        }
        if (level.getBlockEntity(targetPos) instanceof ExplosiveBlockEntity explosiveEntity) {
            explosiveEntity.setSunk(true);
        }
        if (!player.getAbilities().instabuild) {
            explosive.shrink(1);
        }
        RemoteActivatorItem.addTargetFromPlacement(player, level, targetPos);
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private static ItemStack findExplosive(Player player) {
        for (int index = player.getInventory().getContainerSize() - 1;
                index >= 0;
                index--) {
            ItemStack stack = player.getInventory().getItem(index);
            if (Block.byItem(stack.getItem()) instanceof ExplosiveBlock) {
                return stack;
            }
        }
        return ItemStack.EMPTY;
    }

    public static boolean isDrillableHost(
            BlockState state,
            Level level,
            BlockPos pos) {
        if (state.isAir()) {
            return false;
        }
        boolean gt6Host = state.is(BlockTags.BASE_STONE_OVERWORLD)
                || state.is(BlockTags.DIRT)
                || state.is(BlockTags.STONE_ORE_REPLACEABLES)
                || state.is(BlockTags.TERRACOTTA)
                || state.getBlock() instanceof GtStoneBlock
                || state.getBlock() instanceof DropExperienceBlock
                || state.getBlock() == Blocks.CLAY
                || state.getBlock() == Blocks.SNOW
                || state.getBlock() == Blocks.SNOW_BLOCK
                || state.getBlock() == Blocks.GRAVEL
                || state.getBlock() == Blocks.SAND
                || state.getBlock() == Blocks.SANDSTONE
                || state.getBlock() == Blocks.COBBLESTONE
                || state.getBlock() == Blocks.MOSSY_COBBLESTONE
                || state.getBlock() == Blocks.NETHERRACK
                || state.getBlock() == Blocks.END_STONE;
        return gt6Host && state.getDestroySpeed(level, pos) >= 0.0F;
    }
}
