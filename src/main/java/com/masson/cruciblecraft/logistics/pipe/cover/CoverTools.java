package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

/**
 * Shared screwdriver / chisel actions for pipe, wire, cable, and machine
 * covers. GT6 shutter toggles invert then connect/disconnect.
 */
public final class CoverTools {
    private CoverTools() {}

    public static boolean onTool(
            Level level,
            BlockPos pos,
            Direction side,
            ToolAction action,
            PipeCover cover,
            Replacer replacer) {
        if (level == null
                || pos == null
                || side == null
                || action == null
                || cover == null
                || replacer == null) {
            return false;
        }
        if (action == ToolAction.CHISEL && CoverTextureCycle.cycles(cover)) {
            replacer.replace(CoverTextureCycle.cycle(cover));
            return true;
        }
        if (action == ToolAction.SCREWDRIVER
                && "shutter".equals(cover.definitionId().getPath())) {
            int next = cover.config().invert().orElse(0) == 0 ? 1 : 0;
            replacer.replace(cover.withConfig(cover.config().withInvert(next)));
            boolean open = next == 0;
            Gt6StyleConnections.setConnection(level, pos, side, open);
            return true;
        }
        return CoverFilterLogic.onTool(action, cover, replacer);
    }

    @FunctionalInterface
    public interface Replacer {
        void replace(PipeCover cover);
    }
}
