package com.masson.cruciblecraft.client.render;

import org.joml.Matrix4f;

import com.masson.cruciblecraft.content.blockentity.CrucibleBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

public final class CrucibleRenderer implements BlockEntityRenderer<CrucibleBlockEntity> {
    private static final ResourceLocation MOLTEN_TEXTURE =
            ResourceLocation.withDefaultNamespace("block/lava_still");

    public CrucibleRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            CrucibleBlockEntity crucible,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        if (!crucible.isMolten()) {
            return;
        }

        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(MOLTEN_TEXTURE);
        VertexConsumer vertices = buffers.getBuffer(RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        int color = crucible.moltenColor();
        int light = crucible.temperature() >= 700.0f ? LightTexture.FULL_BRIGHT : packedLight;
        float y = 0.25f + crucible.fillFraction() * 0.55f;

        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        vertex(vertices, poseStack, pose, 0.1875f, y, 0.1875f, sprite.getU0(), sprite.getV0(), color, light);
        vertex(vertices, poseStack, pose, 0.1875f, y, 0.8125f, sprite.getU0(), sprite.getV1(), color, light);
        vertex(vertices, poseStack, pose, 0.8125f, y, 0.8125f, sprite.getU1(), sprite.getV1(), color, light);
        vertex(vertices, poseStack, pose, 0.8125f, y, 0.1875f, sprite.getU1(), sprite.getV0(), color, light);
        poseStack.popPose();
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
            int color,
            int light) {
        vertices.addVertex(pose, x, y, z)
                .setColor(0xFF000000 | color)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(poseStack.last(), 0.0f, 1.0f, 0.0f);
    }

}
