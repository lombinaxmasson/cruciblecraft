package com.masson.cruciblecraft.content.block;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

import net.minecraft.core.Direction;

/**
 * Locks {@link Gt6StyleConnections#nineGridSide} to a literal per-cell
 * transcription of the pinned GT6 source
 * {@code gt6_code/gregtech6/src/main/java/gregapi/util/UT.java:1776-1798}
 * (UT.Code.getSideWrenching). The mapping is axis-consistent, not
 * viewer-relative: NORTH and SOUTH share the same X/Y table, WEST and EAST
 * share the same Z/Y table, DOWN and UP share the same X/Z table.
 */
class NineGridSideTest {
    private static final String FACE = "FACE";
    private static final String OPPOSITE = "OPPOSITE";

    // Cell rows indexed [v][u]; "FACE"/"OPPOSITE" resolve per direction.
    // Corner rows: both tangential coords in edge bands -> opposite face;
    // middle rows: single edge band -> the neighbor in that direction.
    private static final Object[][] GRID_Y = {
            {OPPOSITE, Direction.NORTH, OPPOSITE},
            {Direction.WEST, FACE, Direction.EAST},
            {OPPOSITE, Direction.SOUTH, OPPOSITE},
    };
    private static final Object[][] GRID_Z = {
            {OPPOSITE, Direction.DOWN, OPPOSITE},
            {Direction.WEST, FACE, Direction.EAST},
            {OPPOSITE, Direction.UP, OPPOSITE},
    };
    private static final Object[][] GRID_X = {
            {OPPOSITE, Direction.DOWN, OPPOSITE},
            {Direction.NORTH, FACE, Direction.SOUTH},
            {OPPOSITE, Direction.UP, OPPOSITE},
    };

    @Test
    void everyCellMatchesTheGt6Transcription() {
        for (Direction face : Direction.values()) {
            Object[][] grid = switch (face.getAxis()) {
                case Y -> GRID_Y;
                case Z -> GRID_Z;
                case X -> GRID_X;
            };
            for (int cv = 0; cv < 3; cv++) {
                for (int cu = 0; cu < 3; cu++) {
                    double u = (cu + 0.5) / 3.0;
                    double v = (cv + 0.5) / 3.0;
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
                    assertEquals(
                            resolve(grid[cv][cu], face),
                            Gt6StyleConnections.nineGridSide(
                                    face, fx, fy, fz),
                            face + " cell [" + cu + "," + cv + "]");
                }
            }
        }
    }

    @Test
    void bandThresholdsAreInclusiveLikeTheGt6Source() {
        // GT6 uses < 0.25 / > 0.75, so 0.25 and 0.75 stay in the middle band.
        assertEquals(
                Direction.DOWN,
                Gt6StyleConnections.nineGridSide(
                        Direction.DOWN, 0.25, 0.0, 0.5));
        assertEquals(
                Direction.DOWN,
                Gt6StyleConnections.nineGridSide(
                        Direction.DOWN, 0.75, 0.0, 0.5));
        assertEquals(
                Direction.WEST,
                Gt6StyleConnections.nineGridSide(
                        Direction.DOWN, 0.249, 0.0, 0.5));
        assertEquals(
                Direction.EAST,
                Gt6StyleConnections.nineGridSide(
                        Direction.DOWN, 0.751, 0.0, 0.5));
        assertEquals(
                Direction.DOWN,
                Gt6StyleConnections.nineGridSide(
                        Direction.NORTH, 0.5, 0.249, 0.0));
        assertEquals(
                Direction.UP,
                Gt6StyleConnections.nineGridSide(
                        Direction.NORTH, 0.5, 0.751, 0.0));
    }

    @Test
    void cornersAlwaysTargetTheOppositeFace() {
        List<Direction> faces = List.of(
                Direction.DOWN,
                Direction.UP,
                Direction.NORTH,
                Direction.SOUTH,
                Direction.WEST,
                Direction.EAST);
        for (Direction face : faces) {
            double fx = switch (face.getAxis()) {
                case Y, Z -> 0.1;
                case X -> 0.5;
            };
            double fy = switch (face.getAxis()) {
                case Z, X -> 0.1;
                case Y -> 0.5;
            };
            double fz = switch (face.getAxis()) {
                case Y, X -> 0.1;
                case Z -> 0.5;
            };
            assertEquals(
                    face.getOpposite(),
                    Gt6StyleConnections.nineGridSide(face, fx, fy, fz),
                    face + " corner");
        }
    }

    @Test
    void faceUvUsesTheSameGt6TangentialAxes() {
        for (Direction face : Direction.values()) {
            Object[][] grid = switch (face.getAxis()) {
                case Y -> GRID_Y;
                case Z -> GRID_Z;
                case X -> GRID_X;
            };
            for (int cv = 0; cv < 3; cv++) {
                for (int cu = 0; cu < 3; cu++) {
                    // 25/50/25 bands used by the GTM overlay, not equal thirds.
                    double u = cu == 0 ? 0.125 : cu == 1 ? 0.5 : 0.875;
                    double v = cv == 0 ? 0.125 : cv == 1 ? 0.5 : 0.875;
                    assertEquals(
                            resolve(grid[cv][cu], face),
                            Gt6StyleConnections.nineGridSideOnFace(face, u, v),
                            face + " overlay cell [" + cu + "," + cv + "]");
                }
            }
        }
    }

    @Test
    void holdingMatchingToolSplitsWrenchAndCutterByBlockKind()
            throws Exception {
        String connections = java.nio.file.Files.readString(
                java.nio.file.Path.of(
                        "src/main/java/com/masson/cruciblecraft/content/block/Gt6StyleConnections.java"));
        assertTrue(
                connections.contains("holdingMatchingTool")
                        && connections.contains("Shapes.block()")
                        && connections.contains("MaterialWireCutterItem")
                        && connections.contains("MaterialWrenchItem"));
        String overlay = java.nio.file.Files.readString(
                java.nio.file.Path.of(
                        "src/main/java/com/masson/cruciblecraft/client/render/ConnectionGridOverlay.java"));
        assertTrue(overlay.contains("0.25F"));
        assertTrue(overlay.contains("0.75F"));
        assertTrue(overlay.contains("-camera.x"));
        assertTrue(overlay.contains("tool_pipe_connect.png"));
        assertTrue(overlay.contains("tool_pipe_block.png"));
        assertTrue(overlay.contains("RenderType.lines()"));
        assertTrue(overlay.contains("lineWidth(3)"));
        String pipes = java.nio.file.Files.readString(
                java.nio.file.Path.of(
                        "src/main/java/com/masson/cruciblecraft/content/block/AbstractPipeBlock.java"));
        String cables = java.nio.file.Files.readString(
                java.nio.file.Path.of(
                        "src/main/java/com/masson/cruciblecraft/content/block/CableBlock.java"));
        assertTrue(pipes.contains("interactionShape"));
        assertTrue(cables.contains("interactionShape"));
        assertTrue(pipes.contains("getCollisionShape"));
        assertTrue(cables.contains("getCollisionShape"));
    }

    private static Direction resolve(Object cell, Direction face) {
        if (cell == FACE) {
            return face;
        }
        if (cell == OPPOSITE) {
            return face.getOpposite();
        }
        return (Direction) cell;
    }
}
