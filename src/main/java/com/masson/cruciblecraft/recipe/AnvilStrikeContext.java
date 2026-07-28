package com.masson.cruciblecraft.recipe;

import java.util.Optional;

/**
 * Pure translation of GT6's anvil hit routing. Coordinates are local block
 * coordinates in the [0, 1] range.
 */
public record AnvilStrikeContext(
        Facing facing,
        HitFace hitFace,
        double hitX,
        double hitZ) {

    public Optional<AnvilMode> mode() {
        if (hitFace == HitFace.UP) {
            return Optional.of(AnvilMode.ANVIL);
        }
        if (!hitFace.horizontal()) {
            return Optional.empty();
        }

        boolean alongAxis = facing.axis() == hitFace.axis();
        boolean small;
        if (alongAxis) {
            small = switch (facing) {
                case WEST -> hitZ < 0.5;
                case EAST -> hitZ > 0.5;
                case NORTH -> hitX < 0.5;
                case SOUTH -> hitX > 0.5;
                default -> false;
            };
        } else {
            small = switch (facing) {
                case WEST -> hitFace == HitFace.NORTH;
                case EAST -> hitFace == HitFace.SOUTH;
                case NORTH -> hitFace == HitFace.WEST;
                case SOUTH -> hitFace == HitFace.EAST;
            };
        }
        return Optional.of(small ? AnvilMode.BEND_SMALL : AnvilMode.BEND_BIG);
    }

    public int topSlot() {
        double coordinate = facing.axis() == Axis.Z ? hitX : hitZ;
        return coordinate < 0.5 ? 0 : 1;
    }

    public enum Axis {
        X,
        Z
    }

    public enum Facing {
        NORTH(Axis.Z),
        SOUTH(Axis.Z),
        WEST(Axis.X),
        EAST(Axis.X);

        private final Axis axis;

        Facing(Axis axis) {
            this.axis = axis;
        }

        public Axis axis() {
            return axis;
        }
    }

    public enum HitFace {
        DOWN(null),
        UP(null),
        NORTH(Axis.Z),
        SOUTH(Axis.Z),
        WEST(Axis.X),
        EAST(Axis.X);

        private final Axis axis;

        HitFace(Axis axis) {
            this.axis = axis;
        }

        public boolean horizontal() {
            return axis != null;
        }

        public Axis axis() {
            return axis;
        }
    }
}
