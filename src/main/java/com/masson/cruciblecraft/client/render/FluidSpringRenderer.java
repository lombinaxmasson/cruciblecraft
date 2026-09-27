package com.masson.cruciblecraft.client.render;

import com.masson.cruciblecraft.content.blockentity.FluidSpringBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/**
 * GT6 draws {@code BlockTextureFluid} under {@code FLUID_SPRING} on every face.
 * The block model is that grayscale crust. The still texture sits just inside
 * it, tinted by the fluid, so crust holes show the reference texture. Natural
 * gas uses its own still texture rather than a solid white top.
 */
public final class FluidSpringRenderer
        implements BlockEntityRenderer<FluidSpringBlockEntity> {
    private static final float INSET = 0.002F;

    public FluidSpringRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            FluidSpringBlockEntity spring,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        ResourceLocation fluidId = ResourceLocation.tryParse(spring.fluidId());
        if (fluidId == null) {
            return;
        }
        Fluid fluid = BuiltInRegistries.FLUID.get(fluidId);
        if (fluid == null) {
            return;
        }
        IClientFluidTypeExtensions extensions = IClientFluidTypeExtensions.of(fluid);
        ResourceLocation texture = extensions.getStillTexture();
        if (texture == null) {
            return;
        }
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);
        int tint = extensions.getTintColor();
        if ((tint >>> 24) == 0) {
            tint |= 0xFF000000;
        }
        int light = packedLight == 0 ? LightTexture.FULL_BRIGHT : packedLight;
        VertexConsumer vertices = buffers.getBuffer(RenderType.translucent());
        poseStack.pushPose();
        cube(vertices, poseStack.last(), sprite, tint, light);
        poseStack.popPose();
    }

    private static void cube(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int color,
            int light) {
        float min = INSET;
        float max = 1.0F - INSET;
        float u0 = sprite.getU0();
        float u1 = sprite.getU1();
        float v0 = sprite.getV0();
        float v1 = sprite.getV1();
        quad(vertices, pose, color, light, 0.0F, 1.0F, 0.0F,
                min, max, min, u0, v0,
                min, max, max, u0, v1,
                max, max, max, u1, v1,
                max, max, min, u1, v0);
        quad(vertices, pose, color, light, 0.0F, -1.0F, 0.0F,
                min, min, max, u0, v0,
                min, min, min, u0, v1,
                max, min, min, u1, v1,
                max, min, max, u1, v0);
        quad(vertices, pose, color, light, 0.0F, 0.0F, -1.0F,
                max, max, min, u0, v0,
                max, min, min, u0, v1,
                min, min, min, u1, v1,
                min, max, min, u1, v0);
        quad(vertices, pose, color, light, 0.0F, 0.0F, 1.0F,
                min, max, max, u0, v0,
                min, min, max, u0, v1,
                max, min, max, u1, v1,
                max, max, max, u1, v0);
        quad(vertices, pose, color, light, -1.0F, 0.0F, 0.0F,
                min, max, min, u0, v0,
                min, min, min, u0, v1,
                min, min, max, u1, v1,
                min, max, max, u1, v0);
        quad(vertices, pose, color, light, 1.0F, 0.0F, 0.0F,
                max, max, max, u0, v0,
                max, min, max, u0, v1,
                max, min, min, u1, v1,
                max, max, min, u1, v0);
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            int color,
            int light,
            float nx,
            float ny,
            float nz,
            float x0, float y0, float z0, float u0, float v0,
            float x1, float y1, float z1, float u1, float v1,
            float x2, float y2, float z2, float u2, float v2,
            float x3, float y3, float z3, float u3, float v3) {
        vertex(vertices, pose, color, light, nx, ny, nz, x0, y0, z0, u0, v0);
        vertex(vertices, pose, color, light, nx, ny, nz, x1, y1, z1, u1, v1);
        vertex(vertices, pose, color, light, nx, ny, nz, x2, y2, z2, u2, v2);
        vertex(vertices, pose, color, light, nx, ny, nz, x3, y3, z3, u3, v3);
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            int color,
            int light,
            float nx,
            float ny,
            float nz,
            float x,
            float y,
            float z,
            float u,
            float v) {
        vertices.addVertex(pose, x, y, z)
                .setColor(color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(pose, nx, ny, nz);
    }
}
