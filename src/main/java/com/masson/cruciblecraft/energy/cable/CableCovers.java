package com.masson.cruciblecraft.energy.cable;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.content.block.CableBlock;
import com.masson.cruciblecraft.content.blockentity.CableBlockEntity;
import com.masson.cruciblecraft.content.item.PipeCoverItem;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverVisuals;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverCrafting;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverFilterLogic;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverPlacement;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverSounds;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverTools;
import com.masson.cruciblecraft.logistics.pipe.cover.DecorativeCovers;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PlateCovers;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/**
 * GT6 electric wires host blank, decorative plates, emitter, conductor, and
 * progress scale. Torch/repeater stay redstone-wire-only; selectors need
 * {@code ITileEntitySwitchableMode}.
 */
public final class CableCovers {
    private static final int STRONG = 1;
    private static final int INVERT = 2;

    private CableCovers() {}

    public static boolean canPlaceId(
            net.minecraft.resources.ResourceLocation definitionId) {
        if (definitionId == null
                || !"cruciblecraft".equals(definitionId.getNamespace())) {
            return false;
        }
        if (PlateCovers.isPlate(definitionId)
                || DecorativeCovers.isDecorative(definitionId)
                || MachineCoverKinds.isBlank(definitionId)) {
            return true;
        }
        if (MachineCoverKinds.isWireOnlyCover(definitionId)) {
            return false;
        }
        String path = definitionId.getPath();
        return "redstone_emitter".equals(path)
                || "redstone_conductor_in".equals(path)
                || "redstone_conductor_out".equals(path)
                || "scale_progress".equals(path)
                || "cover_crafting".equals(path)
                || "cover_warning".equals(path);
    }

    public static boolean canPlace(
            CableBlockEntity cable, Direction side, PipeCover cover) {
        if (cable == null || side == null || cover == null) {
            return false;
        }
        if (cable.covers().get(side).isPresent()) {
            return false;
        }
        return canPlaceId(cover.definitionId());
    }

    public static PipeCover fromItem(ItemStack stack) {
        PipeCover plate = PlateCovers.fromItem(stack);
        if (plate != null) {
            return plate;
        }
        PipeCover decorative = DecorativeCovers.fromItem(stack);
        if (decorative != null) {
            return decorative;
        }
        if (stack != null
                && stack.getItem() instanceof PipeCoverItem item
                && canPlaceId(item.definitionId())) {
            return PipeCover.of(item.definitionId());
        }
        return null;
    }

    public static boolean tryInstall(
            Level level,
            BlockPos pos,
            BlockHitResult hit,
            ItemStack stack,
            Player player) {
        if (level == null || pos == null || hit == null) {
            return false;
        }
        return tryInstall(
                level,
                pos,
                CoverPlacement.placeSide(level.getBlockEntity(pos), hit),
                stack,
                player);
    }

    public static boolean tryInstall(
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
        if (!(level.getBlockEntity(pos) instanceof CableBlockEntity cable)) {
            return false;
        }
        PipeCover cover = fromItem(stack);
        if (cover == null || !canPlace(cable, side, cover)) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        if (!cable.setCover(side, cover)) {
            return false;
        }
        CoverSounds.placed(level, pos, cover);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    public static void tickOutputs(CableBlockEntity cable) {
        if (cable == null) {
            return;
        }
        for (Direction side : Direction.values()) {
            tickOutput(cable, side);
        }
    }

    public static int weakOut(CableBlockEntity cable, Direction side) {
        PipeCover cover = cover(cable, side);
        if (cover == null) {
            return -1;
        }
        String path = cover.definitionId().getPath();
        if ("redstone_emitter".equals(path)
                || "redstone_conductor_out".equals(path)
                || "scale_progress".equals(path)) {
            return cover.config().redstone();
        }
        return -1;
    }

    public static int strongOut(CableBlockEntity cable, Direction side) {
        PipeCover cover = cover(cable, side);
        if (cover == null) {
            return -1;
        }
        String path = cover.definitionId().getPath();
        if ("redstone_emitter".equals(path) || "scale_progress".equals(path)) {
            return (cover.config().visual() & STRONG) != 0
                    ? cover.config().redstone()
                    : 0;
        }
        return -1;
    }

    public static boolean connectsRedstone(
            CableBlockEntity cable, Direction side) {
        return weakOut(cable, side) >= 0 || strongOut(cable, side) >= 0;
    }

    public static boolean onRightClick(
            CableBlockEntity cable, BlockHitResult hit) {
        return onRightClick(cable, hit, null);
    }

    public static boolean onRightClick(
            CableBlockEntity cable,
            BlockHitResult hit,
            net.minecraft.world.entity.player.Player player) {
        if (cable == null || hit == null) {
            return false;
        }
        Direction side = CoverPlacement.interactSide(cable, hit);
        PipeCover cover = cover(cable, side);
        if (cover == null) {
            return false;
        }
        if (CoverCrafting.open(
                player, cable.getLevel(), cable.getBlockPos(), cover)) {
            return true;
        }
        if (CoverFilterLogic.onRightClick(
                player, cover, next -> cable.replaceCover(side, next))) {
            return true;
        }
        if (!"redstone_emitter".equals(cover.definitionId().getPath())) {
            return false;
        }
        double[] uv = MachineCoverVisuals.faceUv(
                cable.getBlockPos(), hit, side);
        int next = MachineCoverVisuals.emitterClick(
                cover.config().redstone(), uv[0], uv[1]);
        if (next < 0) {
            return false;
        }
        cable.replaceCover(
                side, cover.withDisplay(cover.config().visual(), next));
        notifyNeighbors(cable, side);
        return true;
    }

    public static boolean onTool(
            CableBlockEntity cable, Direction side, ToolAction action) {
        PipeCover cover = cover(cable, side);
        if (cover == null || action == null) {
            return false;
        }
        String path = cover.definitionId().getPath();
        int visual = cover.config().visual();
        if (action == ToolAction.WIRE_CUTTER
                && ("redstone_emitter".equals(path)
                        || "scale_progress".equals(path))) {
            cable.replaceCover(
                    side,
                    cover.withDisplay(
                            visual ^ STRONG, cover.config().redstone()));
            notifyNeighbors(cable, side);
            return true;
        }
        if (CoverTools.onTool(
                cable.getLevel(),
                cable.getBlockPos(),
                side,
                action,
                cover,
                next -> cable.replaceCover(side, next))) {
            return true;
        }
        if (action == ToolAction.SCREWDRIVER && "scale_progress".equals(path)) {
            cable.replaceCover(
                    side,
                    cover.withDisplay(
                            visual ^ INVERT, cover.config().redstone()));
            notifyNeighbors(cable, side);
            return true;
        }
        return false;
    }

    public static void notifyNeighbors(
            CableBlockEntity cable, Direction side) {
        Level level = cable == null ? null : cable.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        Block block = cable.getBlockState().getBlock();
        level.updateNeighborsAt(cable.getBlockPos(), block);
        if (side != null) {
            level.updateNeighborsAt(
                    cable.getBlockPos().relative(side), block);
        }
    }

    private static void tickOutput(CableBlockEntity cable, Direction side) {
        PipeCover cover = cover(cable, side);
        if (cover == null) {
            return;
        }
        String path = cover.definitionId().getPath();
        if ("redstone_conductor_out".equals(path)) {
            replace(cable, side, cover, cover.config().visual(), conductorOut(cable));
            return;
        }
        if ("scale_progress".equals(path)) {
            int output = MachineCoverBehaviors.scaledRedstone(
                    cover.config().visual(),
                    cable.progressValue(),
                    cable.progressMax());
            replace(cable, side, cover, cover.config().visual(), output);
        }
    }

    private static int conductorOut(CableBlockEntity cable) {
        int maximum = 0;
        for (Direction candidate : Direction.values()) {
            PipeCover other = cover(cable, candidate);
            if (other != null
                    && "redstone_conductor_in".equals(
                            other.definitionId().getPath())) {
                maximum = Math.max(
                        maximum, cable.incomingRedstone(candidate));
            }
        }
        return maximum;
    }

    private static void replace(
            CableBlockEntity cable,
            Direction side,
            PipeCover cover,
            int visual,
            int redstone) {
        if (cover.config().visual() == visual
                && cover.config().redstone() == redstone) {
            return;
        }
        cable.replaceCover(side, cover.withDisplay(visual, redstone));
        notifyNeighbors(cable, side);
    }

    private static PipeCover cover(
            CableBlockEntity cable, Direction side) {
        if (cable == null || side == null) {
            return null;
        }
        return cable.covers().get(side).orElse(null);
    }
}
