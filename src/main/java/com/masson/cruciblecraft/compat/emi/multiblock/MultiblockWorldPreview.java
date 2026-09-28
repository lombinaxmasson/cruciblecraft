package com.masson.cruciblecraft.compat.emi.multiblock;

/**
 * Client-installed toggle for the in-world structure overlay.
 * The EMI page calls this without loading a client class.
 */
public final class MultiblockWorldPreview {
    @FunctionalInterface
    public interface Toggle {
        void toggle(MultiblockProjectionGrid grid);
    }

    private static Toggle action = grid -> {};

    private MultiblockWorldPreview() {}

    public static void register(Toggle action) {
        MultiblockWorldPreview.action = action == null ? grid -> {} : action;
    }

    public static void toggle(MultiblockProjectionGrid grid) {
        action.toggle(grid);
    }
}
