package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.MultiblockPortBlock;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PredicateKind;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.PortType;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code MultiTileEntityMultiBlockPart.setDesign}: 18102 design 1 is the
 * far-face center of each tower layer (the backside holes). 18101 stays
 * design 0.
 */
public final class DistillationTowerPartVisuals {
    private DistillationTowerPartVisuals() {}

    public static boolean isBackHolePart(Offset offset) {
        return offset.x() == 0
                && offset.z() == 2
                && offset.y() >= 0
                && offset.y() <= 7;
    }

    public static void apply(
            Level level,
            MultiblockStructureDefinition structure,
            BlockPos controller,
            Direction facing,
            boolean formed) {
        if (level.isClientSide) {
            return;
        }
        for (var element : structure.structure()) {
            var predicate = structure.predicate(element);
            if (predicate.kind() != PredicateKind.PORT) {
                continue;
            }
            BlockPos position = structure.worldPosition(
                    controller, facing, element.offset());
            BlockState state = level.getBlockState(position);
            if (!state.hasProperty(MultiblockPortBlock.TOWER_SKIN)
                    || !state.hasProperty(MultiblockPortBlock.BACK_HOLE)) {
                continue;
            }
            boolean hole = formed
                    && isBackHolePart(element.offset())
                    && predicate.port().orElseThrow() != PortType.ENERGY_INPUT;
            BlockState next = state
                    .setValue(MultiblockPortBlock.TOWER_SKIN, formed)
                    .setValue(MultiblockPortBlock.BACK_HOLE, hole);
            if (next != state) {
                level.setBlock(position, next, Block.UPDATE_CLIENTS);
            }
        }
    }
}
