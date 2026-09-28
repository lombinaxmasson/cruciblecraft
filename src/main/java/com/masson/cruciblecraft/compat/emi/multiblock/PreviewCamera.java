package com.masson.cruciblecraft.compat.emi.multiblock;

import java.util.Optional;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Perspective orbit camera around a scene's center. The eye sits on +Z of
 * view space, so yaw 180 looks at the controller's north face.
 */
public final class PreviewCamera {
    public static final float FOV_DEGREES = 30f;
    private static final float NEAR = 0.05f;

    private final Matrix4f projection;
    private final Matrix4f view;

    private PreviewCamera(Matrix4f projection, Matrix4f view) {
        this.projection = projection;
        this.view = view;
    }

    public static PreviewCamera orbit(
            PreviewScene scene,
            float yawDegrees,
            float pitchDegrees,
            float zoom,
            int width,
            int height) {
        Vector3f center = scene.center();
        float radius = scene.radius();
        float halfFov = (float) Math.toRadians(FOV_DEGREES / 2f);
        float aspect = Math.max(width, 1) / (float) Math.max(height, 1);
        float fitHalfAngle = aspect < 1f
                ? (float) Math.atan(Math.tan(halfFov) * aspect)
                : halfFov;
        float distance = radius / (float) Math.sin(fitHalfAngle) / zoom;
        distance = Math.max(distance, radius + NEAR * 4f);
        Matrix4f projection = new Matrix4f().setPerspective(
                (float) Math.toRadians(FOV_DEGREES),
                aspect,
                NEAR,
                distance + radius * 4f + 16f);
        Matrix4f view = new Matrix4f()
                .translation(0f, 0f, -distance)
                .rotateX((float) Math.toRadians(pitchDegrees))
                .rotateY((float) Math.toRadians(yawDegrees))
                .translate(-center.x, -center.y, -center.z);
        return new PreviewCamera(projection, view);
    }

    public Matrix4f projection() {
        return new Matrix4f(projection);
    }

    public Matrix4f view() {
        return new Matrix4f(view);
    }

    /** Scene-space segment under a widget-local mouse position. */
    public Vec3[] ray(double localX, double localY, int width, int height) {
        float ndcX = (float) (2.0 * localX / Math.max(width, 1) - 1.0);
        float ndcY = (float) (1.0 - 2.0 * localY / Math.max(height, 1));
        Matrix4f inverse = new Matrix4f(projection).mul(view).invert();
        Vector3f near = inverse.transformProject(new Vector3f(ndcX, ndcY, -1f));
        Vector3f far = inverse.transformProject(new Vector3f(ndcX, ndcY, 1f));
        return new Vec3[] {
                new Vec3(near.x, near.y, near.z),
                new Vec3(far.x, far.y, far.z)
        };
    }

    /** Nearest drawn cell the mouse ray enters. */
    public Optional<BlockPos> pick(
            PreviewScene scene,
            double localX,
            double localY,
            int width,
            int height) {
        Vec3[] segment = ray(localX, localY, width, height);
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;
        for (BlockPos pos : scene.cells().keySet()) {
            Optional<Vec3> hit = new AABB(pos).clip(segment[0], segment[1]);
            if (hit.isEmpty()) {
                continue;
            }
            double distance = hit.get().distanceToSqr(segment[0]);
            if (distance < bestDistance) {
                bestDistance = distance;
                best = pos;
            }
        }
        return Optional.ofNullable(best);
    }
}
