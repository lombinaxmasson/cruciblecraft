package com.masson.cruciblecraft.content.multiblock;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.masson.cruciblecraft.content.item.tool.ToolClick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Server-authoritative GT6-style local multiblock completion. */
public final class MultiblockBuilderInteraction {
    private MultiblockBuilderInteraction() {}

    /**
     * Resolves and owns a builder-wand click when it targets a known
     * controller or structure part. A resolved target always consumes the
     * click, including when the inventory has no matching part.
     */
    public static InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.PASS;
        }
        Level level = context.getLevel();
        Optional<MultiblockBuilderTarget> resolved =
                MultiblockBuilderRegistry.resolve(
                        level,
                        context.getClickedPos());
        if (resolved.isEmpty()) {
            return InteractionResult.PASS;
        }

        MultiblockBuilderTarget target = resolved.orElseThrow();
        if (!level.isClientSide) {
            MultiblockBuildPlan plan =
                    target.adapter().plan(level, target);
            Map<String, Block> selectedUniformBlocks =
                    new HashMap<>();
            int placed = 0;
            List<MultiblockBuildCell> missing = plan.missingNear(
                    level,
                    target.controller());
            boolean blockedBySolid = missing.stream().anyMatch(cell -> {
                BlockState current = level.getBlockState(cell.position());
                return !cell.matches(current) && !current.canBeReplaced();
            });
            boolean mayPlace = !player.isSpectator() && !blockedBySolid;
            if (mayPlace) {
                for (MultiblockBuildCell cell : missing) {
                    if (place(
                            level,
                            player,
                            context,
                            cell,
                            selectedUniformBlocks)) {
                        placed++;
                    }
                }
            }
            if (placed > 0) {
                target.adapter().recheck(level, target);
            }
            /*
             * GT6's controller returns a non-zero tool-click damage value even
             * when this click finds no inventory part. The wand therefore
             * spends one durability per recognized structure click.
             */
            ToolClick.hurt(context);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }

    private static boolean place(
            Level level,
            Player player,
            UseOnContext original,
            MultiblockBuildCell cell,
            Map<String, Block> selectedUniformBlocks) {
        BlockPos target = cell.position();
        BlockState current = level.getBlockState(target);
        if (cell.matches(current)) {
            return false;
        }
        if (!current.canBeReplaced() || !player.mayInteract(level, target)) {
            return false;
        }

        Inventory inventory = player.getInventory();
        for (int index = inventory.getContainerSize() - 1;
                index >= 0;
                index--) {
            ItemStack source = inventory.getItem(index);
            if (!(source.getItem() instanceof BlockItem blockItem)
                    || !cell.accepts(source, selectedUniformBlocks)) {
                continue;
            }
            ItemStack attempt = source.copyWithCount(1);
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(target).add(
                            0.0D,
                            Direction.UP.getStepY() * 0.5D,
                            0.0D),
                    Direction.UP,
                    target,
                    false);
            BlockPlaceContext placement = new BlockPlaceContext(
                    level,
                    player,
                    original.getHand(),
                    attempt,
                    hit);
            if (!blockItem.place(placement).consumesAction()) {
                continue;
            }
            if (!player.getAbilities().instabuild) {
                source.shrink(1);
            }
            cell.uniformGroup().ifPresent(
                    group -> selectedUniformBlocks.putIfAbsent(
                            group,
                            blockItem.getBlock()));
            return true;
        }
        return false;
    }
}
