package com.masson.cruciblecraft.logistics.machinecover;

import org.jetbrains.annotations.Nullable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.content.item.tool.ToolClick;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

/** Shared block hooks for covers on all processing-machine block shells. */
public final class MachineCoverBlockInteraction {
    private MachineCoverBlockInteraction() {}

    public static boolean rightClick(
            MachineCoverHost machine,
            BlockHitResult hit) {
        return MachineCoverBehaviors.onRightClick(
                machine, hit.getDirection(), hit);
    }

    public static ToolResult useTool(
            MachineCoverHost machine,
            ToolAction action,
            UseOnContext context) {
        Direction side = context.getClickedFace();
        if (action == ToolAction.CROWBAR) {
            if (!machine.removeCover(side, context.getPlayer())) {
                return ToolResult.PASS;
            }
            ToolClick.hurt(context);
            return ToolResult.SUCCESS;
        }
        if (MachineCoverBehaviors.onTool(machine, side, action)) {
            ToolClick.hurt(context);
            return ToolResult.SUCCESS;
        }
        return ToolResult.PASS;
    }

    public static int weakRedstone(
            BlockGetter level,
            BlockPos pos,
            Direction queryDirection) {
        if (!(level.getBlockEntity(pos) instanceof MachineCoverHost machine)) {
            return 0;
        }
        return MachineCoverBehaviors.weakRedstone(
                machine, queryDirection.getOpposite());
    }

    public static int directRedstone(
            BlockGetter level,
            BlockPos pos,
            Direction queryDirection) {
        if (!(level.getBlockEntity(pos) instanceof MachineCoverHost machine)) {
            return 0;
        }
        return MachineCoverBehaviors.directRedstone(
                machine, queryDirection.getOpposite());
    }

    public static boolean canConnectRedstone(
            BlockGetter level,
            BlockPos pos,
            @Nullable Direction direction) {
        if (!(level.getBlockEntity(pos) instanceof MachineCoverHost machine)) {
            return false;
        }
        if (direction == null) {
            return java.util.Arrays.stream(Direction.values())
                    .anyMatch(side -> machine.covers().get(side).isPresent());
        }
        return machine.covers().get(direction.getOpposite()).isPresent();
    }

    public static boolean rightClick(
            MachineCoverHost machine,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hit) {
        return !player.isShiftKeyDown() && rightClick(machine, hit);
    }

    public static void dropCovers(Level level, BlockPos pos) {
        if (level.getBlockEntity(pos) instanceof MachineCoverHost machine) {
            machine.dropCovers();
        }
    }
}
