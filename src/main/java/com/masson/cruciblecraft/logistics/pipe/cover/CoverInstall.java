package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.blockentity.FluidPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.ItemPipeBlockEntity;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.content.redstonewire.RedstoneWireCovers;
import com.masson.cruciblecraft.energy.cable.CableCovers;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverHost;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Places a plate/foil decorative cover on any GT6 cover host. */
public final class CoverInstall {
    private CoverInstall() {}

    public static boolean tryPlace(
            Level level,
            BlockPos pos,
            Direction side,
            ItemStack stack,
            Player player) {
        if (level == null
                || pos == null
                || side == null
                || stack == null
                || stack.isEmpty()) {
            return false;
        }
        PipeCover cover = PlateCovers.fromItem(stack);
        if (cover == null) {
            cover = DecorativeCovers.fromItem(stack);
        }
        if (cover == null) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!canPlace(blockEntity, side, cover)) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        if (!place(blockEntity, side, cover)) {
            return false;
        }
        CoverSounds.placed(level, pos, cover);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    private static boolean canPlace(
            BlockEntity blockEntity, Direction side, PipeCover cover) {
        if (blockEntity instanceof RedstoneWireBlockEntity wire) {
            return RedstoneWireCovers.canPlace(wire, side, cover);
        }
        if (blockEntity instanceof CableBlockEntity cable) {
            return CableCovers.canPlace(cable, side, cover);
        }
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            return pipe.coverSnapshot().get(side) == null;
        }
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            return pipe.coverSnapshot().get(side) == null;
        }
        if (blockEntity instanceof MachineCoverHost machine) {
            return machine.covers().get(side).isEmpty()
                    && MachineCoverBehaviors.canPlace(machine, side, cover);
        }
        return false;
    }

    private static boolean place(
            BlockEntity blockEntity, Direction side, PipeCover cover) {
        if (blockEntity instanceof RedstoneWireBlockEntity wire) {
            return wire.setCover(side, cover);
        }
        if (blockEntity instanceof CableBlockEntity cable) {
            return cable.setCover(side, cover);
        }
        if (blockEntity instanceof ItemPipeBlockEntity pipe) {
            return pipe.setCover(side, cover);
        }
        if (blockEntity instanceof FluidPipeBlockEntity pipe) {
            return pipe.setCover(side, cover);
        }
        if (blockEntity instanceof MachineCoverHost machine) {
            return machine.setCover(side, cover);
        }
        return false;
    }
}
