package com.masson.cruciblecraft.logistics.pipe.cover;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.content.block.Gt6StyleConnections;
import com.masson.cruciblecraft.test.MinecraftTestBootstrap;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Locks GT6 connector cover targeting:
 * {@code usePipePlacementMode} + {@code UT.Code.getSideWrenching}.
 */
class CoverPlacementTest {
    @BeforeAll
    static void bootstrap() {
        MinecraftTestBootstrap.bootstrap();
    }

    @Test
    void emptyHandsAreNotCoverStacks() {
        assertFalse(CoverPlacement.isCoverStack(null));
        assertFalse(CoverPlacement.isCoverStack(ItemStack.EMPTY));
    }

    @Test
    void machinesKeepTheClickedFace() {
        BlockHitResult hit = hit(Direction.NORTH, 0.1, 0.1, 0.0);
        assertEquals(Direction.NORTH, CoverPlacement.placeSide(null, hit));
        assertEquals(Direction.NORTH, CoverPlacement.interactSide(null, hit));
        assertFalse(CoverPlacement.usesPipePlacement(null));
        assertFalse(CoverPlacement.hasAnyCover(null));
    }

    @Test
    void connectorNineGridMatchesTheWrenchingTable() {
        BlockHitResult corner = hit(Direction.NORTH, 0.1, 0.1, 0.0);
        assertEquals(
                Gt6StyleConnections.sideFromHit(corner),
                CoverPlacement.connectorPlaceSide(corner));
        assertEquals(Direction.SOUTH, CoverPlacement.connectorPlaceSide(corner));

        BlockHitResult center = hit(Direction.NORTH, 0.5, 0.5, 0.0);
        assertEquals(Direction.NORTH, CoverPlacement.connectorPlaceSide(center));

        BlockHitResult westEdge = hit(Direction.NORTH, 0.1, 0.5, 0.0);
        assertEquals(Direction.WEST, CoverPlacement.connectorPlaceSide(westEdge));
    }

    private static BlockHitResult hit(
            Direction face, double x, double y, double z) {
        return new BlockHitResult(
                new Vec3(x, y, z), face, BlockPos.ZERO, true);
    }
}
