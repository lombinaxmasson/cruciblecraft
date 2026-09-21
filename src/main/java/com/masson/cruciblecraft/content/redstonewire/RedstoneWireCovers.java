package com.masson.cruciblecraft.content.redstonewire;

import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.content.block.RedstoneWireBlock;
import com.masson.cruciblecraft.content.blockentity.RedstoneWireBlockEntity;
import com.masson.cruciblecraft.content.item.PipeCoverItem;
import com.masson.cruciblecraft.content.item.ProgrammedCircuitItem;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverBehaviors;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverKinds;
import com.masson.cruciblecraft.logistics.machinecover.MachineCoverVisuals;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCover;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverItems;
import com.masson.cruciblecraft.logistics.pipe.cover.PlateCovers;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverCrafting;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverFilterLogic;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverPlacement;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverSounds;
import com.masson.cruciblecraft.logistics.pipe.cover.DecorativeCovers;
import com.masson.cruciblecraft.logistics.pipe.cover.CoverTools;
import com.masson.cruciblecraft.registry.ModComponents;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;

/**
 * GT6 covers that attach to redstone connectors: torch/repeater (wire-only),
 * blank, selectors ({@code ITileEntitySwitchableMode}), emitter/conductor
 * (no host restriction), and progress scale ({@code ITileEntityProgress}).
 */
public final class RedstoneWireCovers {
    private static final int STRONG = 1;
    private static final int INVERT = 2;
    private static final int BUTTON_RESET_TICKS = 10;

    private RedstoneWireCovers() {}

    public static boolean canPlace(
            RedstoneWireBlockEntity wire, Direction side, PipeCover cover) {
        if (wire == null || side == null || cover == null) {
            return false;
        }
        if (wire.covers().get(side).isPresent()) {
            return false;
        }
        return MachineCoverKinds.attachesToRedstoneWire(cover.definitionId());
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
        if (!(level.getBlockEntity(pos) instanceof RedstoneWireBlockEntity wire)) {
            return false;
        }
        PipeCover cover = fromItem(stack);
        if (cover == null || !canPlace(wire, side, cover)) {
            return false;
        }
        if (level.isClientSide) {
            return true;
        }
        if (!wire.setCover(side, cover)) {
            return false;
        }
        CoverSounds.placed(level, pos, cover);
        if (player == null || !player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        return true;
    }

    public static PipeCover fromItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        if (stack.is(Items.REDSTONE_TORCH)) {
            return PipeCover.of("cruciblecraft:redstone_torch");
        }
        if (stack.is(Items.REPEATER)) {
            return PipeCover.of("cruciblecraft:redstone_repeater");
        }
        if (stack.getItem() instanceof ProgrammedCircuitItem) {
            int config = ProgrammedCircuitItem.normalize(stack.getOrDefault(
                    ModComponents.CIRCUIT_CONFIG.get(),
                    ProgrammedCircuitItem.DEFAULT_CONFIG));
            return PipeCover.of("cruciblecraft:selector_tag")
                    .withDisplay(0, Math.floorMod(config - 1, 16));
        }
        if (stack.getItem() instanceof PipeCoverItem item
                && MachineCoverKinds.attachesToRedstoneWire(
                        item.definitionId())) {
            return PipeCover.of(item.definitionId());
        }
        PipeCover plate = PlateCovers.fromItem(stack);
        if (plate != null) {
            return plate;
        }
        return DecorativeCovers.fromItem(stack);
    }

    public static boolean interceptConnect(PipeCover cover) {
        return cover != null
                && MachineCoverKinds.isWireOnlyCover(cover.definitionId());
    }

    public static boolean blocksConnect(
            Level level, BlockPos pos, Direction side) {
        if (level == null || pos == null || side == null) {
            return false;
        }
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (!(blockEntity instanceof RedstoneWireBlockEntity wire)) {
            return false;
        }
        return interceptConnect(wire.covers().get(side).orElse(null));
    }

    public static void onPlaced(RedstoneWireBlockEntity wire, Direction side) {
        if (wire == null
                || side == null
                || wire.getLevel() == null
                || wire.getLevel().isClientSide) {
            return;
        }
        PipeCover cover = wire.covers().get(side).orElse(null);
        if (interceptConnect(cover)
                && RedstoneWireBlock.isConnected(wire.getBlockState(), side)) {
            Gt6StyleConnections.setConnection(
                    wire.getLevel(), wire.getBlockPos(), side, false);
        }
        applySelectors(wire);
        if (wire.updateRedstone()) {
            RedstoneWireNetwork.propagate(wire);
        }
        tickOutputs(wire);
        notifyNeighbors(wire, side);
    }

    public static void applySelectors(RedstoneWireBlockEntity wire) {
        if (wire == null) {
            return;
        }
        int mode = wire.mode();
        boolean any = false;
        for (Direction side : Direction.values()) {
            PipeCover cover = wire.covers().get(side).orElse(null);
            if (cover == null) {
                continue;
            }
            String path = cover.definitionId().getPath();
            if ("selector_redstone".equals(path)) {
                mode = wire.incomingRedstone(side);
                any = true;
                replace(
                        wire,
                        side,
                        cover,
                        mode,
                        mode);
            } else if ("selector_tag".equals(path)) {
                mode = cover.config().redstone();
                any = true;
            } else if ("selector_button_panel".equals(path)) {
                any = true;
                int remaining = cover.config().redstone();
                int visual = cover.config().visual();
                if (cover.config().invert().orElse(0) != 0 && remaining > 1) {
                    remaining -= 1;
                    if (remaining == 1) {
                        mode = 0;
                    }
                }
                visual = MachineCoverVisuals.withButtonMode(visual, mode);
                replace(wire, side, cover, visual, remaining);
                mode = MachineCoverVisuals.buttonMode(visual);
            }
        }
        if (any) {
            wire.setStateMode(mode);
        }
    }

    public static void tickOutputs(RedstoneWireBlockEntity wire) {
        if (wire == null) {
            return;
        }
        for (Direction side : Direction.values()) {
            tickOutput(wire, side);
        }
    }

    public static void tickAll(RedstoneWireBlockEntity wire) {
        applySelectors(wire);
        tickOutputs(wire);
    }

    private static void tickOutput(
            RedstoneWireBlockEntity wire, Direction side) {
        PipeCover cover = wire.covers().get(side).orElse(null);
        if (cover == null) {
            return;
        }
        String path = cover.definitionId().getPath();
        if (interceptConnect(cover)) {
            int visual = visual(
                    MachineCoverKinds.isTorch(cover.definitionId()),
                    wire.redstoneValue() > 0L);
            replace(
                    wire,
                    side,
                    cover,
                    visual,
                    cover.config().redstone());
            return;
        }
        if ("redstone_conductor_out".equals(path)) {
            replace(
                    wire,
                    side,
                    cover,
                    cover.config().visual(),
                    conductorOut(wire));
            return;
        }
        if ("scale_progress".equals(path)) {
            int output = MachineCoverBehaviors.scaledRedstone(
                    cover.config().visual(),
                    wire.progressValue(),
                    wire.progressMax());
            replace(
                    wire,
                    side,
                    cover,
                    cover.config().visual(),
                    output);
        }
    }

    private static int conductorOut(RedstoneWireBlockEntity wire) {
        int maximum = 0;
        for (Direction candidate : Direction.values()) {
            PipeCover other = wire.covers().get(candidate).orElse(null);
            if (other != null
                    && "redstone_conductor_in".equals(
                            other.definitionId().getPath())) {
                maximum = Math.max(maximum, wire.incomingRedstone(candidate));
            }
        }
        return maximum;
    }

    private static void replace(
            RedstoneWireBlockEntity wire,
            Direction side,
            PipeCover cover,
            int visual,
            int redstone) {
        if (cover.config().visual() == visual
                && cover.config().redstone() == redstone) {
            return;
        }
        wire.replaceCover(side, cover.withDisplay(visual, redstone));
        notifyNeighbors(wire, side);
    }

    /**
     * GT6 torch {@code condition} is {@code mRedstone > 0} (NOT). Repeater is
     * {@code mRedstone <= 0} (buffer). Visual 1 is the off texture.
     */
    public static int visual(boolean torch, boolean powered) {
        boolean off = torch ? powered : !powered;
        return off ? 1 : 0;
    }

    /** GT6 {@code getRedstoneOutWeak/Strong}: visual 0 emits 15. */
    public static int output(int visual) {
        return visual == 0 ? 15 : 0;
    }

    /**
     * Returns {@code -1} when the face should use the connector's own output.
     */
    public static int weakOut(RedstoneWireBlockEntity wire, Direction side) {
        PipeCover cover = cover(wire, side);
        if (cover == null) {
            return -1;
        }
        if (interceptConnect(cover)) {
            return output(cover.config().visual());
        }
        String path = cover.definitionId().getPath();
        if ("redstone_emitter".equals(path)
                || "redstone_conductor_out".equals(path)
                || "scale_progress".equals(path)) {
            return cover.config().redstone();
        }
        return -1;
    }

    /**
     * Returns {@code -1} when the face should use the connector's own strong
     * output. Conductor OUT passes that default through; emitter/scale emit
     * 0 unless cutter-toggled strong.
     */
    public static int strongOut(RedstoneWireBlockEntity wire, Direction side) {
        PipeCover cover = cover(wire, side);
        if (cover == null) {
            return -1;
        }
        if (interceptConnect(cover)) {
            return output(cover.config().visual());
        }
        String path = cover.definitionId().getPath();
        if ("redstone_emitter".equals(path) || "scale_progress".equals(path)) {
            return (cover.config().visual() & STRONG) != 0
                    ? cover.config().redstone()
                    : 0;
        }
        return -1;
    }

    /**
     * Returns {@code -1} when the face should use the connector's own output.
     */
    public static int redstoneOut(RedstoneWireBlockEntity wire, Direction side) {
        return weakOut(wire, side);
    }

    public static boolean connectsRedstone(
            RedstoneWireBlockEntity wire, Direction side) {
        return weakOut(wire, side) >= 0 || strongOut(wire, side) >= 0;
    }

    public static boolean onRightClick(
            RedstoneWireBlockEntity wire, BlockHitResult hit) {
        return onRightClick(wire, hit, null);
    }

    public static boolean onRightClick(
            RedstoneWireBlockEntity wire,
            BlockHitResult hit,
            net.minecraft.world.entity.player.Player player) {
        if (wire == null || hit == null) {
            return false;
        }
        Direction side = CoverPlacement.interactSide(wire, hit);
        PipeCover cover = cover(wire, side);
        if (cover == null) {
            return false;
        }
        if (CoverCrafting.open(
                player, wire.getLevel(), wire.getBlockPos(), cover)) {
            return true;
        }
        if (CoverFilterLogic.onRightClick(
                player, cover, next -> wire.replaceCover(side, next))) {
            return true;
        }
        String path = cover.definitionId().getPath();
        double[] uv = MachineCoverVisuals.faceUv(
                wire.getBlockPos(), hit, side);
        if ("redstone_emitter".equals(path)) {
            int next = MachineCoverVisuals.emitterClick(
                    cover.config().redstone(), uv[0], uv[1]);
            if (next < 0) {
                return false;
            }
            wire.replaceCover(
                    side,
                    cover.withDisplay(cover.config().visual(), next));
            notifyNeighbors(wire, side);
            return true;
        }
        if ("selector_button_panel".equals(path)) {
            int column = Math.min(3, Math.max(0, (int) (uv[0] * 4)));
            int row = Math.min(3, Math.max(0, (int) (uv[1] * 4)));
            int mode = column + row * 4;
            int visual = MachineCoverVisuals.withButtonMode(
                    cover.config().visual(), mode);
            int redstone = cover.config().invert().orElse(0) != 0
                    ? BUTTON_RESET_TICKS
                    : cover.config().redstone();
            wire.replaceCover(side, cover.withDisplay(visual, redstone));
            wire.setStateMode(mode);
            if (wire.updateRedstone()) {
                RedstoneWireNetwork.propagate(wire);
            }
            return true;
        }
        return false;
    }

    public static boolean onTool(
            RedstoneWireBlockEntity wire,
            Direction side,
            ToolAction action) {
        PipeCover cover = cover(wire, side);
        if (cover == null || action == null) {
            return false;
        }
        String path = cover.definitionId().getPath();
        int visual = cover.config().visual();
        if (action == ToolAction.WIRE_CUTTER
                && ("redstone_emitter".equals(path)
                        || "scale_progress".equals(path))) {
            wire.replaceCover(
                    side,
                    cover.withDisplay(
                            visual ^ STRONG, cover.config().redstone()));
            notifyNeighbors(wire, side);
            return true;
        }
        if (action == ToolAction.SCREWDRIVER && "scale_progress".equals(path)) {
            wire.replaceCover(
                    side,
                    cover.withDisplay(
                            visual ^ INVERT, cover.config().redstone()));
            notifyNeighbors(wire, side);
            return true;
        }
        if (action == ToolAction.SCREWDRIVER
                && "selector_button_panel".equals(path)) {
            int next = cover.config().invert().orElse(0) == 0 ? 1 : 0;
            wire.replaceCover(
                    side, cover.withConfig(cover.config().withInvert(next)));
            return true;
        }
        if (CoverTools.onTool(
                wire.getLevel(),
                wire.getBlockPos(),
                side,
                action,
                cover,
                next -> wire.replaceCover(side, next))) {
            return true;
        }
        if (action == ToolAction.CHISEL
                && "selector_button_panel".equals(path)) {
            wire.replaceCover(
                    side,
                    cover.withDisplay(
                            MachineCoverVisuals.cycleButtonStyle(visual),
                            cover.config().redstone()));
            return true;
        }
        return false;
    }

    public static ItemStack drop(PipeCover cover) {
        return PipeCoverItems.stackFor(cover);
    }

    public static void notifyNeighbors(
            RedstoneWireBlockEntity wire, Direction side) {
        Level level = wire == null ? null : wire.getLevel();
        if (level == null || level.isClientSide) {
            return;
        }
        Block block = wire.getBlockState().getBlock();
        level.updateNeighborsAt(wire.getBlockPos(), block);
        if (side != null) {
            level.updateNeighborsAt(wire.getBlockPos().relative(side), block);
        }
    }

    private static PipeCover cover(
            RedstoneWireBlockEntity wire, Direction side) {
        if (wire == null || side == null) {
            return null;
        }
        return wire.covers().get(side).orElse(null);
    }
}
