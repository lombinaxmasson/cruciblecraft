package com.masson.cruciblecraft.content.block;

import java.util.Map;

import com.masson.cruciblecraft.api.energy.EnergyType;
import com.masson.cruciblecraft.api.tool.ToolAction;
import com.masson.cruciblecraft.api.tool.ToolActionSource;
import com.masson.cruciblecraft.api.tool.ToolResult;
import com.masson.cruciblecraft.logistics.pipe.PipeTopology;
import com.masson.cruciblecraft.logistics.pipe.cover.PipeCoverIntercept;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * GTM default {@code gt6StylePipesCables} connections.
 *
 * <p>Placing against a compatible neighbor opens that face and mirrors the
 * neighbor. Other faces stay closed until a wrench (pipes) or wire cutter
 * (cables) toggles them, including machine attachments. Neighbor updates
 * never auto-open or auto-close.
 */
public final class Gt6StyleConnections {
    private Gt6StyleConnections() {}

    public static Map<Direction, BooleanProperty> properties(BlockState state) {
        if (state.getBlock() instanceof CableBlock) {
            return CableBlock.PROPERTY_BY_DIRECTION;
        }
        if (state.getBlock() instanceof RedstoneWireBlock) {
            return RedstoneWireBlock.PROPERTY_BY_DIRECTION;
        }
        if (state.getBlock() instanceof AbstractPipeBlock) {
            return AbstractPipeBlock.PROPERTY_BY_DIRECTION;
        }
        return Map.of();
    }

    public static boolean isOpen(BlockState state, Direction side) {
        BooleanProperty property = properties(state).get(side);
        return property != null && state.getValue(property);
    }

    public static BlockState withSide(
            BlockState state, Direction side, boolean open) {
        BooleanProperty property = properties(state).get(side);
        return property == null ? state : state.setValue(property, open);
    }

    public static boolean sameNetwork(BlockState self, BlockState neighbor) {
        if (self.getBlock() instanceof CableBlock selfCable
                && neighbor.getBlock() instanceof CableBlock neighborCable) {
            return (selfCable.supports(EnergyType.ELECTRIC)
                            && neighborCable.supports(EnergyType.ELECTRIC))
                    || (selfCable.supports(EnergyType.LU)
                            && neighborCable.supports(EnergyType.LU));
        }
        if (self.getBlock() instanceof RedstoneWireBlock
                && neighbor.getBlock() instanceof RedstoneWireBlock) {
            return true;
        }
        return self.getBlock() instanceof AbstractPipeBlock selfPipe
                && neighbor.getBlock() instanceof AbstractPipeBlock neighborPipe
                && selfPipe.pipe().kind() == neighborPipe.pipe().kind();
    }

    public static boolean canConnect(
            Level level, BlockPos pos, Direction side) {
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof CableBlock cable) {
            return cable.connectsTo(level, pos, side);
        }
        if (state.getBlock() instanceof RedstoneWireBlock) {
            return true;
        }
        if (state.getBlock() instanceof AbstractPipeBlock pipe) {
            return pipe.connectsTo(level, pos, side);
        }
        return false;
    }

    public static void setConnection(
            Level level,
            BlockPos pos,
            Direction side,
            boolean open) {
        if (level.isClientSide) {
            return;
        }
        if (open
                && (PipeCoverIntercept.blocksConnect(level, pos, side)
                        || PipeCoverIntercept.blocksConnect(
                                level,
                                pos.relative(side),
                                side.getOpposite()))) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (properties(state).isEmpty() || isOpen(state, side) == open) {
            return;
        }
        BlockState updated = withSide(state, side, open);
        level.setBlock(pos, updated, Block.UPDATE_ALL);
        if (updated.getBlock() instanceof AbstractPipeBlock) {
            PipeTopology.invalidate(level, pos);
        }
        BlockPos neighborPos = pos.relative(side);
        BlockState neighbor = level.getBlockState(neighborPos);
        if (sameNetwork(updated, neighbor)
                && isOpen(neighbor, side.getOpposite()) != open) {
            level.setBlock(
                    neighborPos,
                    withSide(neighbor, side.getOpposite(), open),
                    Block.UPDATE_ALL);
            if (neighbor.getBlock() instanceof AbstractPipeBlock) {
                PipeTopology.invalidate(level, neighborPos);
            }
        }
    }

    public static void applyPlacement(
            Level level, BlockPos pos, Direction clickedFace) {
        if (level.isClientSide) {
            return;
        }
        Direction towardsClicked = clickedFace.getOpposite();
        if (canConnect(level, pos, towardsClicked)) {
            setConnection(level, pos, towardsClicked, true);
        }
        BlockState self = level.getBlockState(pos);
        for (Direction facing : Direction.values()) {
            BlockState neighbor = level.getBlockState(pos.relative(facing));
            if (sameNetwork(self, neighbor)
                    && isOpen(neighbor, facing.getOpposite())
                    && !isOpen(self, facing)) {
                setConnection(level, pos, facing, true);
                self = level.getBlockState(pos);
            }
        }
    }

    public static boolean placeBlock(
            BlockPlaceContext context, boolean placed) {
        if (placed && !context.getLevel().isClientSide) {
            applyPlacement(
                    context.getLevel(),
                    context.getClickedPos(),
                    context.getClickedFace());
        }
        return placed;
    }

    public static ToolResult toggleConnection(
            Level level, BlockPos pos, BlockHitResult hit) {
        if (level.isClientSide) {
            return ToolResult.SUCCESS;
        }
        BlockState state = level.getBlockState(pos);
        Direction side = sideFromHit(hit);
        boolean open = !isOpen(state, side);
        if (open
                && (PipeCoverIntercept.blocksConnect(level, pos, side)
                        || PipeCoverIntercept.blocksConnect(
                                level,
                                pos.relative(side),
                                side.getOpposite()))) {
            return ToolResult.REJECT;
        }
        setConnection(level, pos, side, open);
        return ToolResult.SUCCESS;
    }

    /**
     * Ports GT6 {@code UT.Code.getSideWrenching}
     * ({@code gt6_code/gregtech6/src/main/java/gregapi/util/UT.java:1776-1798}):
     * the clicked face plus the fractional in-block hit position selects the
     * target face of the 3x3 wrenching grid — center = clicked face, edges =
     * adjacent faces, corners = opposite face. Bands are thirds (0.25 / 0.75)
     * and the mapping is axis-consistent, not viewer-relative.
     */
    public static Direction nineGridSide(
            Direction face, double fx, double fy, double fz) {
        Direction opposite = face.getOpposite();
        switch (face.getAxis()) {
            case Y:
                // GT6 sides DOWN(0) / UP(1): tangential axes are X and Z.
                if (fx < 0.25) {
                    return fz < 0.25 || fz > 0.75 ? opposite : Direction.WEST;
                }
                if (fx > 0.75) {
                    return fz < 0.25 || fz > 0.75 ? opposite : Direction.EAST;
                }
                if (fz < 0.25) {
                    return Direction.NORTH;
                }
                return fz > 0.75 ? Direction.SOUTH : face;
            case Z:
                // GT6 sides NORTH(2) / SOUTH(3): tangential axes are X and Y.
                if (fx < 0.25) {
                    return fy < 0.25 || fy > 0.75 ? opposite : Direction.WEST;
                }
                if (fx > 0.75) {
                    return fy < 0.25 || fy > 0.75 ? opposite : Direction.EAST;
                }
                if (fy < 0.25) {
                    return Direction.DOWN;
                }
                return fy > 0.75 ? Direction.UP : face;
            case X:
                // GT6 sides WEST(4) / EAST(5): tangential axes are Z and Y.
                if (fz < 0.25) {
                    return fy < 0.25 || fy > 0.75 ? opposite : Direction.NORTH;
                }
                if (fz > 0.75) {
                    return fy < 0.25 || fy > 0.75 ? opposite : Direction.SOUTH;
                }
                if (fy < 0.25) {
                    return Direction.DOWN;
                }
                return fy > 0.75 ? Direction.UP : face;
            default:
                return face;
        }
    }

    public static Direction sideFromHit(BlockHitResult hit) {
        BlockPos pos = hit.getBlockPos();
        Vec3 location = hit.getLocation();
        return nineGridSide(
                hit.getDirection(),
                location.x - pos.getX(),
                location.y - pos.getY(),
                location.z - pos.getZ());
    }

    /**
     * Face-local UV version of {@link #nineGridSide}: {@code u}/{@code v} are
     * 0–1 along the same tangential axes GT6 uses (Y: X/Z, Z: X/Y, X: Z/Y).
     */
    public static Direction nineGridSideOnFace(
            Direction face, double u, double v) {
        double fx = switch (face.getAxis()) {
            case Y, Z -> u;
            case X -> 0.5;
        };
        double fy = switch (face.getAxis()) {
            case Z, X -> v;
            case Y -> 0.5;
        };
        double fz = switch (face.getAxis()) {
            case Y -> v;
            case X -> u;
            case Z -> 0.5;
        };
        return nineGridSide(face, fx, fy, fz);
    }

    /**
     * GTM {@code PipeBlock.getShape}: holding the tune tool expands the
     * outline/raytrace box to a full cube so the 3×3 grid is clickable.
     * Collision stays the thin pipe via {@code getCollisionShape}.
     */
    public static boolean holdingMatchingTool(
            BlockState state, ItemStack held) {
        if (state.getBlock() instanceof CableBlock
                || state.getBlock() instanceof RedstoneWireBlock) {
            return ToolActionSource.heldProvides(held, ToolAction.WIRE_CUTTER);
        }
        return state.getBlock() instanceof AbstractPipeBlock
                && ToolActionSource.providesAny(
                        held, ToolAction::expandsPipeGrid);
    }

    public static boolean holdingMatchingTool(
            BlockState state, CollisionContext context) {
        if (!(context instanceof EntityCollisionContext entityContext)) {
            return false;
        }
        Entity entity = entityContext.getEntity();
        return entity instanceof Player player
                && holdingMatchingTool(state, player.getMainHandItem());
    }

    public static VoxelShape interactionShape(
            BlockState state,
            CollisionContext context,
            VoxelShape connectedShape) {
        return holdingMatchingTool(state, context)
                ? Shapes.block()
                : connectedShape;
    }
}
