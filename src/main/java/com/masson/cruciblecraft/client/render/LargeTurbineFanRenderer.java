package com.masson.cruciblecraft.client.render;

import org.joml.Matrix4f;

import com.masson.cruciblecraft.CrucibleCraft;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * GT6 {@code MultiTileEntityLargeTurbine} render pass 1: a 3x3 facing fan
 * slightly in front of the frontal hull.
 */
public final class LargeTurbineFanRenderer {
    public static final ResourceLocation GAS_IDLE =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "block/machine/gas_turbine/fan");
    public static final ResourceLocation GAS_ACTIVE =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "block/machine/gas_turbine/fan_active");
    public static final ResourceLocation STEAM_IDLE =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "block/machine/large_steam_turbine/fan");
    public static final ResourceLocation STEAM_ACTIVE =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID,
                    "block/machine/large_steam_turbine/fan_active");

    private LargeTurbineFanRenderer() {}

    public static void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            Direction facing,
            ResourceLocation idle,
            ResourceLocation active,
            boolean running) {
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(running ? active : idle);
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.entityCutout(TextureAtlas.LOCATION_BLOCKS));
        fan(vertices, poseStack, packedLight, facing, sprite);
    }

    private static void fan(
            VertexConsumer vertices,
            PoseStack poseStack,
            int packedLight,
            Direction facing,
            TextureAtlasSprite sprite) {
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        switch (facing) {
            case NORTH -> quad(
                    vertices, poseStack, packedLight, facing,
                    1.999f, 1.999f, -0.001f,
                    -0.999f, 1.999f, -0.001f,
                    -0.999f, -0.999f, -0.001f,
                    1.999f, -0.999f, -0.001f,
                    u0, v0, u1, v1);
            case SOUTH -> quad(
                    vertices, poseStack, packedLight, facing,
                    -0.999f, 1.999f, 1.001f,
                    1.999f, 1.999f, 1.001f,
                    1.999f, -0.999f, 1.001f,
                    -0.999f, -0.999f, 1.001f,
                    u0, v0, u1, v1);
            case WEST -> quad(
                    vertices, poseStack, packedLight, facing,
                    -0.001f, 1.999f, -0.999f,
                    -0.001f, 1.999f, 1.999f,
                    -0.001f, -0.999f, 1.999f,
                    -0.001f, -0.999f, -0.999f,
                    u0, v0, u1, v1);
            case EAST -> quad(
                    vertices, poseStack, packedLight, facing,
                    1.001f, 1.999f, 1.999f,
                    1.001f, 1.999f, -0.999f,
                    1.001f, -0.999f, -0.999f,
                    1.001f, -0.999f, 1.999f,
                    u0, v0, u1, v1);
            case DOWN -> quad(
                    vertices, poseStack, packedLight, facing,
                    -0.999f, -0.001f, 1.999f,
                    1.999f, -0.001f, 1.999f,
                    1.999f, -0.001f, -0.999f,
                    -0.999f, -0.001f, -0.999f,
                    u0, v0, u1, v1);
            case UP -> quad(
                    vertices, poseStack, packedLight, facing,
                    -0.999f, 1.001f, -0.999f,
                    1.999f, 1.001f, -0.999f,
                    1.999f, 1.001f, 1.999f,
                    -0.999f, 1.001f, 1.999f,
                    u0, v0, u1, v1);
        }
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack poseStack,
            int packedLight,
            Direction facing,
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
            float v1) {
        Matrix4f pose = poseStack.last().pose();
        vertex(vertices, poseStack, pose, x0, y0, z0, u0, v0, facing, packedLight);
        vertex(vertices, poseStack, pose, x1, y1, z1, u1, v0, facing, packedLight);
        vertex(vertices, poseStack, pose, x2, y2, z2, u1, v1, facing, packedLight);
        vertex(vertices, poseStack, pose, x3, y3, z3, u0, v1, facing, packedLight);
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
            Direction facing,
            int packedLight) {
        vertices.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(packedLight)
                .setNormal(
                        poseStack.last(),
                        facing.getStepX(),
                        facing.getStepY(),
                        facing.getStepZ());
    }
}
