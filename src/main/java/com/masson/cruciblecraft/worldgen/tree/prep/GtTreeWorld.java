package com.masson.cruciblecraft.worldgen.tree.prep;

/**
 * Placement surface used by unregistered GT tree grow. Landing blocks wrap a
 * Level; tests use an in-memory grid.
 */
public interface GtTreeWorld {
    int height();

    boolean canPlaceTree(int x, int y, int z);

    void setLog(int x, int y, int z);

    void setLeaves(int x, int y, int z);

    void setRubberResinHole(int x, int y, int z, HorizontalFacing facing);

    boolean hasNearbyRubberResinHole(int x, int z);

    boolean isAir(int x, int y, int z);

    boolean isDirtOrGrass(int x, int y, int z);

    void setPodzol(int x, int y, int z);
}
