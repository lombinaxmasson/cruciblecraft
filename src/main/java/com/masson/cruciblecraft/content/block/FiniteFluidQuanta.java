package com.masson.cruciblecraft.content.block;

/**
 * GT6 {@code BlockFluidFinite} quanta: metadata 0–15 is 1–16 units. {@code
 * UT.Code.bind4} clamps, it does not wrap.
 */
public final class FiniteFluidQuanta {
    public static final int QUANTA_PER_BLOCK = 8;
    public static final int FULL_META = 15;
    public static final int DRIP_META = 7;
    /** GT6 {@code BlockBaseFluid} {@code mAmountPerQuanta}. */
    public static final int MILLIBUCKETS_PER_QUANTUM = 125;
    /** Vanilla bucket is 1000 mB = 8 quanta. */
    public static final int BUCKET_QUANTA = 8;

    private FiniteFluidQuanta() {}

    public static int bind4(int value) {
        return Math.max(0, Math.min(FULL_META, value));
    }

    public static int quanta(int meta) {
        return bind4(meta) + 1;
    }
}
