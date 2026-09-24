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
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.material.Fluid;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;

/** Adds the configured GT6 fluid texture to the otherwise static spring block. */
public final class FluidSpringRenderer
        implements BlockEntityRenderer<FluidSpringBlockEntity> {
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
        ResourceLocation texture = "natural_gas".equals(fluidId.getPath())
                ? ResourceLocation.fromNamespaceAndPath(
                        "cruciblecraft", "fluid/gt6_import/natural_gas")
                : IClientFluidTypeExtensions.of(fluid).getStillTexture();
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(texture);
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.translucent());
        poseStack.pushPose();
        PoseStack.Pose pose = poseStack.last();
        quad(vertices, pose, sprite, packedLight);
        poseStack.popPose();
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            int light) {
        vertex(vertices, pose, sprite, 0.0F, 1.001F, 0.0F,
                sprite.getU0(), sprite.getV0(), light);
        vertex(vertices, pose, sprite, 0.0F, 1.001F, 1.0F,
                sprite.getU0(), sprite.getV1(), light);
        vertex(vertices, pose, sprite, 1.0F, 1.001F, 1.0F,
                sprite.getU1(), sprite.getV1(), light);
        vertex(vertices, pose, sprite, 1.0F, 1.001F, 0.0F,
                sprite.getU1(), sprite.getV0(), light);
    }

    private static void vertex(
            VertexConsumer vertices,
            PoseStack.Pose pose,
            TextureAtlasSprite sprite,
            float x,
            float y,
            float z,
            float u,
            float v,
            int light) {
        vertices.addVertex(pose, x, y, z)
                .setColor(0xFFFFFFFF)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light == 0 ? LightTexture.FULL_BRIGHT : light)
                .setNormal(pose, Direction.UP.getStepX(),
                        Direction.UP.getStepY(), Direction.UP.getStepZ());
    }
}
