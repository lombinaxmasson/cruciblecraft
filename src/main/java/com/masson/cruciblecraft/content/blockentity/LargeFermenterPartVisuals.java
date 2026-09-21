package com.masson.cruciblecraft.content.blockentity;

import com.masson.cruciblecraft.content.block.ProcessingMachineBlock;
import com.masson.cruciblecraft.content.block.StainlessSteelMixerWalls;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureCatalog;
import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;
import com.masson.cruciblecraft.registry.ModMultiblockControllers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GT6 {@code checkAndSetTarget} design 7: both wall layers, center of the
 * face opposite the controller.
 */
public final class LargeFermenterPartVisuals {
    public static final Offset LOWER_BACK = new Offset(0, 0, 4);
    public static final Offset UPPER_BACK = new Offset(0, 1, 4);

    private LargeFermenterPartVisuals() {}

    public static void apply(
            Level level,
            BlockPos controller,
            Direction facing,
            boolean formed) {
        if (level.isClientSide) {
            return;
        }
        var structure = MultiblockStructureCatalog.find(
                ModMultiblockControllers.LARGE_FERMENTER.structureId());
        if (structure.isEmpty()) {
            return;
        }
        for (Offset offset : new Offset[] {LOWER_BACK, UPPER_BACK}) {
            BlockPos position = structure.orElseThrow().worldPosition(
                    controller, facing, offset);
            BlockState state = level.getBlockState(position);
            if (!state.hasProperty(StainlessSteelMixerWalls.DESIGN_HOLE)) {
                continue;
            }
            boolean hole = formed;
            if (state.getValue(StainlessSteelMixerWalls.DESIGN_HOLE) == hole) {
                continue;
            }
            level.setBlock(
                    position,
                    state.setValue(StainlessSteelMixerWalls.DESIGN_HOLE, hole),
                    Block.UPDATE_CLIENTS);
        }
    }

    public static Direction facingOf(BlockState controller) {
        return controller.getValue(ProcessingMachineBlock.FACING);
    }
}
