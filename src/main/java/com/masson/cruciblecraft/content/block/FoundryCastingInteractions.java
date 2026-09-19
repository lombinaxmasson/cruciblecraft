package com.masson.cruciblecraft.content.block;

import java.util.Optional;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.blockentity.FoundryCastingBlockEntity;
import com.masson.cruciblecraft.content.item.tool.ToolClick;
import com.masson.cruciblecraft.content.mold.CruciblePour;
import com.masson.cruciblecraft.content.mold.MoldRecipes;
import com.masson.cruciblecraft.heat.TemperatureDamage;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** GT6 foundry mold / basin tools. Basins skip chisel and auto-pull. */
public final class FoundryCastingInteractions {
    private FoundryCastingInteractions() {}

    public static ToolResult useTool(ToolAction action, UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity mold)) {
            return ToolResult.PASS;
        }
        if (mold.basin()) {
            return useBasinTool(action, context, mold);
        }
        if (action == ToolAction.CHISEL) {
            if (mold.isFilled()) {
                return ToolResult.PASS;
            }
            Vec3 location = context.getClickLocation();
            Optional<Integer> bit = MoldRecipes.chiselBit(
                    location.x - pos.getX(),
                    location.z - pos.getZ());
            if (bit.isEmpty() || (mold.pattern() & bit.get()) != 0) {
                return ToolResult.PASS;
            }
            if (!level.isClientSide && mold.chiselBit(bit.get())) {
                ToolClick.hurt(context);
                level.playSound(
                        null,
                        pos,
                        SoundEvents.STONE_HIT,
                        SoundSource.BLOCKS,
                        1.0F,
                        0.8F);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.PINCERS) {
            return takeWithPincers(context, mold, false);
        }
        if (action == ToolAction.WRENCH || action == ToolAction.SCREWDRIVER) {
            if (!level.isClientSide) {
                mold.rotatePattern();
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action == ToolAction.SOFT_HAMMER) {
            if (!level.isClientSide) {
                mold.clearAutoInput();
                Player player = context.getPlayer();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    "message.cruciblecraft.mold_auto_input_cleared"),
                            true);
                }
                ToolClick.hurt(context);
            }
            return ToolResult.SUCCESS;
        }
        if (action != ToolAction.MONKEY_WRENCH) {
            return ToolResult.PASS;
        }
        if (context.getClickedFace() != Direction.UP) {
            return ToolResult.PASS;
        }
        if (!level.isClientSide) {
            Direction target = Gt6StyleConnections.sideFromHit(ToolClick.hit(context));
            Player player = context.getPlayer();
            if (target.getAxis().isHorizontal()) {
                boolean enabled = mold.toggleAutoPull(target);
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    enabled
                                            ? "message.cruciblecraft.mold_auto_input_on"
                                            : "message.cruciblecraft.mold_auto_input_off"),
                            true);
                }
            } else {
                boolean redstone = mold.toggleRedstoneMode();
                if (player != null) {
                    player.displayClientMessage(
                            Component.translatable(
                                    redstone
                                            ? "message.cruciblecraft.mold_auto_input_redstone"
                                            : "message.cruciblecraft.mold_auto_input_no_redstone"),
                            true);
                }
            }
            ToolClick.hurt(context);
        }
        return ToolResult.SUCCESS;
    }

    public static InteractionResult useWithoutItem(
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity mold)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide) {
            return InteractionResult.SUCCESS;
        }
        if (mold.basin()) {
            if (hit.getDirection() != Direction.UP) {
                return InteractionResult.PASS;
            }
            ItemStack output = mold.takeOutput(player, true);
            if (!output.isEmpty()) {
                if (!player.addItem(output)) {
                    player.drop(output, false);
                }
                return InteractionResult.CONSUME;
            }
            if (mold.isFilled()) {
                player.displayClientMessage(
                        Component.translatable(
                                "message.cruciblecraft.mold_cooling",
                                Math.round(mold.temperature())),
                        true);
            }
            return InteractionResult.CONSUME;
        }
        ItemStack output = mold.takeOutput(player, true);
        if (!output.isEmpty()) {
            if (!player.addItem(output)) {
                player.drop(output, false);
            }
            return InteractionResult.CONSUME;
        }
        if (mold.isFilled()) {
            player.displayClientMessage(
                    Component.translatable(
                            "message.cruciblecraft.mold_cooling",
                            Math.round(mold.temperature())),
                    true);
            return InteractionResult.CONSUME;
        }
        if (hit.getDirection() != Direction.UP) {
            return InteractionResult.CONSUME;
        }
        Direction target = Gt6StyleConnections.sideFromHit(hit);
        boolean poured = target.getAxis().isVertical()
                ? pourAllHorizontal(level, pos, mold)
                : pourFrom(level, pos, mold, target);
        if (poured) {
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.mold_filled"),
                    true);
        } else {
            player.displayClientMessage(
                    Component.translatable("message.cruciblecraft.mold_no_molten_material"),
                    true);
        }
        return InteractionResult.CONSUME;
    }

    public static void applyContactDamage(Level level, BlockPos pos, Entity entity) {
        if (!level.isClientSide
                && level.getBlockEntity(pos) instanceof FoundryCastingBlockEntity mold) {
            TemperatureDamage.apply(entity, mold.temperature(), 1.0F, 5.0F);
        }
    }

    private static ToolResult useBasinTool(
            ToolAction action,
            UseOnContext context,
            FoundryCastingBlockEntity basin) {
        if (action == ToolAction.PINCERS) {
            if (context.getClickedFace() != Direction.UP) {
                return ToolResult.PASS;
            }
            return takeWithPincers(context, basin, false);
        }
        if (action == ToolAction.CHISEL
                || action == ToolAction.SOFT_HAMMER
                || action == ToolAction.WRENCH
                || action == ToolAction.MONKEY_WRENCH
                || action == ToolAction.SCREWDRIVER) {
            return ToolResult.PASS;
        }
        return ToolResult.PASS;
    }

    private static ToolResult takeWithPincers(
            UseOnContext context,
            FoundryCastingBlockEntity mold,
            boolean causeDamage) {
        Player player = context.getPlayer();
        if (player == null) {
            return ToolResult.PASS;
        }
        Level level = context.getLevel();
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        ItemStack output = mold.takeOutput(player, causeDamage);
        if (output.isEmpty()) {
            return ToolResult.PASS;
        }
        if (!player.addItem(output)) {
            player.drop(output, false);
        }
        ToolClick.hurt(context);
        return ToolResult.SUCCESS;
    }

    private static boolean pourAllHorizontal(
            Level level, BlockPos pos, FoundryCastingBlockEntity mold) {
        for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (pourFrom(level, pos, mold, direction)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pourFrom(
            Level level,
            BlockPos pos,
            FoundryCastingBlockEntity mold,
            Direction side) {
        if (!side.getAxis().isHorizontal()) {
            return false;
        }
        CruciblePour crucible = CruciblePour.at(level, pos.relative(side));
        return crucible != null
                && crucible.fillMoldAtSide(mold, side.getOpposite(), side);
    }
}
