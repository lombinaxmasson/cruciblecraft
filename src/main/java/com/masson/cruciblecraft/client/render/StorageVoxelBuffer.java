package com.masson.cruciblecraft.client.render;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

final class StorageVoxelBuffer {
    private StorageVoxelBuffer() {}

    static ResourceLocation blockTexture(String path) {
        return ResourceLocation.fromNamespaceAndPath(
                "cruciblecraft", "textures/block/" + path + ".png");
    }

    static void cube(
            VertexConsumer vertices,
            PoseStack poseStack,
            float[] box,
            int argb,
            int packedLight) {
        float x0 = box[0];
        float y0 = box[1];
        float z0 = box[2];
        float x1 = box[3];
        float y1 = box[4];
        float z1 = box[5];
        Matrix4f pose = poseStack.last().pose();
        quad(vertices, poseStack, pose, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, argb, packedLight);
        quad(vertices, poseStack, pose, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, argb, packedLight);
        quad(vertices, poseStack, pose, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0, 0, -1, argb, packedLight);
        quad(vertices, poseStack, pose, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0, 0, 1, argb, packedLight);
        quad(vertices, poseStack, pose, x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0, -1, 0, 0, argb, packedLight);
        quad(vertices, poseStack, pose, x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1, 1, 0, 0, argb, packedLight);
    }

    static void cubeNorthSouth(
            VertexConsumer vertices,
            PoseStack poseStack,
            float[] box,
            int argb,
            int packedLight) {
        float x0 = box[0];
        float y0 = box[1];
        float z0 = box[2];
        float x1 = box[3];
        float y1 = box[4];
        float z1 = box[5];
        Matrix4f pose = poseStack.last().pose();
        quad(vertices, poseStack, pose, x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0, 0, 0, -1, argb, packedLight);
        quad(vertices, poseStack, pose, x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1, 0, 0, 1, argb, packedLight);
    }

    static void cube(
            VertexConsumer vertices,
            PoseStack poseStack,
            float[] box,
            net.minecraft.client.renderer.texture.TextureAtlasSprite sprite,
            int argb,
            int packedLight) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        float x0 = box[0];
        float y0 = box[1];
        float z0 = box[2];
        float x1 = box[3];
        float y1 = box[4];
        float z1 = box[5];
        Matrix4f pose = poseStack.last().pose();
        spriteQuad(
                vertices, poseStack, pose,
                x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0,
                u0, v0, u1, v1, 0, -1, 0, argb, packedLight);
        spriteQuad(
                vertices, poseStack, pose,
                x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1,
                u0, v0, u1, v1, 0, 1, 0, argb, packedLight);
        spriteQuad(
                vertices, poseStack, pose,
                x1, y1, z0, x0, y1, z0, x0, y0, z0, x1, y0, z0,
                u0, v0, u1, v1, 0, 0, -1, argb, packedLight);
        spriteQuad(
                vertices, poseStack, pose,
                x0, y1, z1, x1, y1, z1, x1, y0, z1, x0, y0, z1,
                u0, v0, u1, v1, 0, 0, 1, argb, packedLight);
        spriteQuad(
                vertices, poseStack, pose,
                x0, y1, z0, x0, y1, z1, x0, y0, z1, x0, y0, z0,
                u0, v0, u1, v1, -1, 0, 0, argb, packedLight);
        spriteQuad(
                vertices, poseStack, pose,
                x1, y1, z1, x1, y1, z0, x1, y0, z0, x1, y0, z1,
                u0, v0, u1, v1, 1, 0, 0, argb, packedLight);
    }

    private static void spriteQuad(
            VertexConsumer vertices,
            PoseStack poseStack,
            Matrix4f pose,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float u0,
            float v0,
            float u1,
            float v1,
            float nx,
            float ny,
            float nz,
            int argb,
            int packedLight) {
        vertex(vertices, poseStack, pose, x0, y0, z0, u0, v0, nx, ny, nz, argb, packedLight);
        vertex(vertices, poseStack, pose, x1, y1, z1, u1, v0, nx, ny, nz, argb, packedLight);
        vertex(vertices, poseStack, pose, x2, y2, z2, u1, v1, nx, ny, nz, argb, packedLight);
        vertex(vertices, poseStack, pose, x3, y3, z3, u0, v1, nx, ny, nz, argb, packedLight);
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack poseStack,
            Matrix4f pose,
            float x0,
            float y0,
            float z0,
            float x1,
            float y1,
            float z1,
            float x2,
            float y2,
            float z2,
            float x3,
            float y3,
            float z3,
            float nx,
            float ny,
            float nz,
            int argb,
            int packedLight) {
        vertex(vertices, poseStack, pose, x0, y0, z0, 0.0F, 0.0F, nx, ny, nz, argb, packedLight);
        vertex(vertices, poseStack, pose, x1, y1, z1, 1.0F, 0.0F, nx, ny, nz, argb, packedLight);
        vertex(vertices, poseStack, pose, x2, y2, z2, 1.0F, 1.0F, nx, ny, nz, argb, packedLight);
        vertex(vertices, poseStack, pose, x3, y3, z3, 0.0F, 1.0F, nx, ny, nz, argb, packedLight);
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack poseStack,
            Matrix4f pose,
            float x,
            float y,
            float z,
            float u,
            float v,
            float nx,
            float ny,
            float nz,
            int argb,
            int packedLight) {
        vertices.addVertex(pose, x, y, z)
                .setColor(argb)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(poseStack.last(), nx, ny, nz);
    }
}
