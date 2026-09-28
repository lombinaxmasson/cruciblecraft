package com.masson.cruciblecraft.client.multiblockpreview;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.world.phys.AABB;

/** Line boxes in camera space, for the world overlay's wrong-block markers. */
final class PreviewOutlines {
    private PreviewOutlines() {}

    static void box(
            Matrix4f cameraRotation,
            VertexConsumer buffer,
            AABB box,
            float red,
            float green,
            float blue,
            float alpha) {
        edge(cameraRotation, buffer, box.minX, box.minY, box.minZ, box.maxX, box.minY, box.minZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.maxX, box.minY, box.minZ, box.maxX, box.minY, box.maxZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.maxX, box.minY, box.maxZ, box.minX, box.minY, box.maxZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.minX, box.minY, box.maxZ, box.minX, box.minY, box.minZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.minX, box.maxY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.maxX, box.maxY, box.minZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.maxX, box.maxY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.minX, box.maxY, box.maxZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.minX, box.minY, box.minZ, box.minX, box.maxY, box.minZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.maxX, box.minY, box.minZ, box.maxX, box.maxY, box.minZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.maxX, box.minY, box.maxZ, box.maxX, box.maxY, box.maxZ, red, green, blue, alpha);
        edge(cameraRotation, buffer, box.minX, box.minY, box.maxZ, box.minX, box.maxY, box.maxZ, red, green, blue, alpha);
    }

    private static void edge(
            Matrix4f cameraRotation,
            VertexConsumer buffer,
            double x1, double y1, double z1,
            double x2, double y2, double z2,
            float red, float green, float blue, float alpha) {
        Vector3f from = cameraRotation.transformPosition(new Vector3f((float) x1, (float) y1, (float) z1));
        Vector3f to = cameraRotation.transformPosition(new Vector3f((float) x2, (float) y2, (float) z2));
        Vector3f normal = new Vector3f(to).sub(from).normalize();
        buffer.addVertex(from.x, from.y, from.z)
                .setColor(red, green, blue, alpha)
                .setNormal(normal.x, normal.y, normal.z);
        buffer.addVertex(to.x, to.y, to.z)
                .setColor(red, green, blue, alpha)
                .setNormal(normal.x, normal.y, normal.z);
    }
}
