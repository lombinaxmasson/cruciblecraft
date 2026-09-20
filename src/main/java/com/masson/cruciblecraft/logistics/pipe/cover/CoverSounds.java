package com.masson.cruciblecraft.logistics.pipe.cover;

import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;

/**
 * GT6 torch covers play {@code MC_DIG_WOOD} on place and crowbar. Decorative
 * plates play screwdriver-like attach and {@code MC_BREAK} on pry.
 */
public final class CoverSounds {
    private CoverSounds() {}

    public static void placed(Level level, BlockPos pos, PipeCover cover) {
        play(level, pos, cover, true);
    }

    public static void removed(Level level, BlockPos pos, PipeCover cover) {
        play(level, pos, cover, false);
    }

    private static void play(
            Level level, BlockPos pos, PipeCover cover, boolean placed) {
        if (level == null
                || level.isClientSide
                || pos == null
                || cover == null) {
            return;
        }
        if (MachineCoverKinds.isWireOnlyCover(cover.definitionId())
                || DecorativeCovers.isWood(cover)
                || "cover_crafting".equals(cover.definitionId().getPath())) {
            level.playSound(
                    null,
                    pos,
                    placed ? SoundEvents.WOOD_PLACE : SoundEvents.WOOD_BREAK,
                    SoundSource.BLOCKS,
                    1.0F,
                    0.8F);
            return;
        }
        if (DecorativeCovers.isDecorative(cover) || PlateCovers.isPlate(cover)) {
            level.playSound(
                    null,
                    pos,
                    placed ? SoundEvents.STONE_PLACE : SoundEvents.STONE_BREAK,
                    SoundSource.BLOCKS,
                    0.8F,
                    placed ? 1.1F : 0.9F);
            return;
        }
        level.playSound(
                null,
                pos,
                placed
                        ? SoundEvents.IRON_TRAPDOOR_OPEN
                        : SoundEvents.STONE_BREAK,
                SoundSource.BLOCKS,
                0.8F,
                placed ? 1.2F : 0.9F);
    }
}
