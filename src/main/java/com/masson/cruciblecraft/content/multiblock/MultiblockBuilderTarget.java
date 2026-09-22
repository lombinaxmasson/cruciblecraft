package com.masson.cruciblecraft.content.multiblock;

import java.util.Objects;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/** Resolved controller/part target for one builder-wand click. */
public record MultiblockBuilderTarget(
        MultiblockBuilderAdapter adapter,
        ResourceLocation structureId,
        BlockPos controller,
        Direction facing,
        BlockPos clicked) {
    public MultiblockBuilderTarget {
        Objects.requireNonNull(adapter, "adapter");
        Objects.requireNonNull(structureId, "structureId");
        controller = Objects.requireNonNull(controller, "controller").immutable();
        clicked = Objects.requireNonNull(clicked, "clicked").immutable();
        if (facing == null) {
            throw new IllegalArgumentException(
                    "Builder target facing must not be null");
        }
    }
}
