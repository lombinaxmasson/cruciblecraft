package com.masson.cruciblecraft.content.fluidbarrel;

import java.util.List;

import net.minecraft.core.Direction;

/**
 * Pure GT6 {@code TileEntityBase08Barrel} decisions.
 *
 * <p>Mode bit 0 is automatic output. Mode bit 1 is sealed. Soft hammer on an
 * empty tank clears automatic output and keeps the sealed bit.
 */
public final class FluidBarrelLogic {
    public enum Reaction {
        NONE,
        MELTDOWN_LAVA,
        MELTDOWN_FIRE,
        MAGIC_DESTROY,
        ACID_DESTROY,
        TRASH,
        FERMENT,
        AUTO_OUTPUT
    }

    public record Mode(boolean autoOutput, boolean sealed, long sealedTime) {
        public Mode {
            if (sealedTime < 0L) {
                sealedTime = 0L;
            }
        }
    }

    private FluidBarrelLogic() {}

    public static Mode softHammer(Mode mode, long amount, boolean canSeal) {
        if (amount <= 0L) {
            return new Mode(false, mode.sealed(), 0L);
        }
        if (!canSeal) {
            return mode;
        }
        return new Mode(mode.autoOutput(), !mode.sealed(), 0L);
    }

    public static Mode wrench(Mode mode) {
        return new Mode(!mode.autoOutput(), mode.sealed(), mode.sealedTime());
    }

    public static Mode plunger(Mode mode) {
        return new Mode(mode.autoOutput(), mode.sealed(), 0L);
    }

    /**
     * Gas goes both ways first. A lighter non-gas goes up. Everything else
     * goes down. Side order matches GT6 {@code {0, 1}} (down, then up).
     */
    public static List<Direction> autoOutputSides(boolean gas, boolean lighter) {
        if (gas) {
            return List.of(Direction.DOWN, Direction.UP);
        }
        if (lighter) {
            return List.of(Direction.UP);
        }
        return List.of(Direction.DOWN);
    }

    public static boolean allow(
            boolean conductsPower,
            long temperature,
            long melting,
            boolean onlySimple,
            boolean simple) {
        return !conductsPower && temperature < melting && (!onlySimple || simple);
    }

    public static Reaction reaction(
            long amount,
            long temperature,
            long melting,
            boolean vanillaLava,
            boolean magic,
            boolean magicProof,
            boolean acid,
            boolean acidProof,
            boolean plasma,
            boolean plasmaProof,
            boolean gas,
            boolean gasProof,
            boolean allow,
            boolean sealed,
            boolean autoOutput) {
        if (amount <= 0L) {
            return Reaction.NONE;
        }
        if (temperature >= melting) {
            return vanillaLava && amount >= 1000L
                    ? Reaction.MELTDOWN_LAVA
                    : Reaction.MELTDOWN_FIRE;
        }
        if (magic && !magicProof) {
            return Reaction.MAGIC_DESTROY;
        }
        if (acid && !acidProof) {
            return Reaction.ACID_DESTROY;
        }
        if (plasma && !plasmaProof) {
            return Reaction.TRASH;
        }
        if (gas && !gasProof) {
            return Reaction.TRASH;
        }
        if (!allow) {
            return Reaction.TRASH;
        }
        if (sealed) {
            return Reaction.FERMENT;
        }
        if (autoOutput) {
            return Reaction.AUTO_OUTPUT;
        }
        return Reaction.NONE;
    }

    /**
     * {@code divup(max(1, abs(eut * duration)) * max(1, tank), max(1, input))}.
     * Products that would overflow saturate at {@link Long#MAX_VALUE}.
     */
    public static long sealedDuration(
            long tankAmount, long eut, int duration, long inputAmount) {
        long power = Math.max(
                1L,
                saturatingMultiply(Math.abs(eut), Math.max(1, duration)));
        return divUp(
                saturatingMultiply(power, Math.max(1L, tankAmount)),
                Math.max(1L, inputAmount));
    }

    /** Floor of {@code output * tank / input}, matching {@code FL.mul(..., false)}. */
    public static long scaledOutput(
            long outputAmount, long tankAmount, long inputAmount) {
        if (outputAmount <= 0L || tankAmount <= 0L || inputAmount <= 0L) {
            return 0L;
        }
        return saturatingMultiply(outputAmount, tankAmount) / inputAmount;
    }

    public static long saturatingMultiply(long left, long right) {
        if (left <= 0L || right <= 0L) {
            return 0L;
        }
        if (left > Long.MAX_VALUE / right) {
            return Long.MAX_VALUE;
        }
        return left * right;
    }

    public static long divUp(long numerator, long denominator) {
        if (denominator <= 0L) {
            throw new IllegalArgumentException("denominator must be positive");
        }
        if (numerator <= 0L) {
            return 0L;
        }
        if (numerator > Long.MAX_VALUE - (denominator - 1L)) {
            return Long.MAX_VALUE;
        }
        return (numerator + denominator - 1L) / denominator;
    }
}
