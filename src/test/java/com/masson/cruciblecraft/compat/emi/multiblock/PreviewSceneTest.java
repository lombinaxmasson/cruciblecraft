package com.masson.cruciblecraft.compat.emi.multiblock;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import com.masson.cruciblecraft.content.multiblock.MultiblockStructureDefinition.Offset;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.phys.Vec3;

class PreviewSceneTest {
    private final MultiblockProjectionGrid coke =
            MultiblockProjectionGridTest.projectCokeOven();

    @Test
    void sceneKeepsLocalPositionsAndDropsAir() {
        PreviewScene scene = PreviewScene.of(coke, null);
        assertEquals(26, scene.cells().size());
        assertTrue(scene.at(BlockPos.ZERO).isPresent());
        assertTrue(scene.at(new BlockPos(0, 0, 1)).isEmpty());
        Vector3f center = scene.center();
        assertEquals(0.5f, center.x, 0.001f);
        assertEquals(0.5f, center.y, 0.001f);
        assertEquals(1.5f, center.z, 0.001f);
    }

    @Test
    void layerKeepsOnlyThatLocalY() {
        PreviewScene scene = PreviewScene.of(coke, 0);
        assertTrue(scene.cells().values().stream()
                .allMatch(cell -> cell.offset().y() == 0));
        assertTrue(scene.cells().size() < coke.cells().size());
    }

    @Test
    void frontRayHitsTheController() {
        PreviewScene scene = PreviewScene.of(coke, null);
        PreviewCamera camera = PreviewCamera.orbit(
                scene, 180f, 0f, 1f, 200, 200);
        Vec3[] ray = camera.ray(100, 100, 200, 200);
        assertEquals(0.5, ray[0].x, 0.01);
        assertEquals(0.5, ray[0].y, 0.01);
        Optional<BlockPos> hit = camera.pick(scene, 100, 100, 200, 200);
        assertEquals(Optional.of(new BlockPos(0, 0, 0)), hit);
    }

    @Test
    void singleLayerRayCannotHitAnotherLayer() {
        PreviewScene scene = PreviewScene.of(coke, 1);
        PreviewCamera camera = PreviewCamera.orbit(
                scene, 180f, 0f, 1f, 200, 200);
        camera.pick(scene, 100, 100, 200, 200)
                .ifPresent(pos -> assertEquals(1, pos.getY()));
    }

    @Test
    void placedCellFollowsTheValidatorFacing() {
        Offset local = new Offset(-1, -1, 0);
        BlockPos controller = new BlockPos(10, 64, 10);
        Offset rotated = local.rotate(Direction.EAST);
        BlockPos placed = controller.offset(rotated.x(), rotated.y(), rotated.z());
        assertEquals(new BlockPos(10, 63, 9), placed);
    }
}
