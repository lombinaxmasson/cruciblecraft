package com.masson.cruciblecraft.content.multiblock;

import java.util.List;
import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

/** Immutable geometry snapshot for one resolved builder-wand target. */
public record MultiblockBuildPlan(
        MultiblockBuilderTarget target,
        List<MultiblockBuildCell> cells) {
    public static final int LOCAL_RADIUS = 2;

    public MultiblockBuildPlan {
        Objects.requireNonNull(target, "target");
        cells = List.copyOf(cells);
    }

    public List<MultiblockBuildCell> missingNear(
            Level level,
            BlockPos clicked) {
        return cells.stream()
                .filter(MultiblockBuildCell::placeable)
                .filter(cell -> near(clicked, cell.position()))
                .filter(cell -> level.hasChunkAt(cell.position()))
                .filter(cell -> !cell.matches(
                        level.getBlockState(cell.position())))
                .toList();
    }

    public static boolean near(BlockPos clicked, BlockPos position) {
        return Math.abs(clicked.getX() - position.getX()) < LOCAL_RADIUS
                && Math.abs(clicked.getY() - position.getY()) < LOCAL_RADIUS
                && Math.abs(clicked.getZ() - position.getZ()) < LOCAL_RADIUS;
    }
}
