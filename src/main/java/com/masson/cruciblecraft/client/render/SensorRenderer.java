package com.masson.cruciblecraft.client.render;

import java.util.function.Function;

import org.joml.Matrix4f;

import com.masson.cruciblecraft.client.render.SensorDisplayLayout.Glyph;
import com.masson.cruciblecraft.content.block.SensorBlock;
import com.masson.cruciblecraft.content.blockentity.SensorBlockEntity;
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
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;

/**
 * GT6 sensor readout: six character icons, one per display window.
 *
 * <p>{@code MultiTileEntitySensor.getRenderPasses2} draws the plate on pass
 * 0 and one character on each of passes 1–6. The plate is the block model.
 * This renderer draws the six character quads in those windows.
 */
public final class SensorRenderer
        implements BlockEntityRenderer<SensorBlockEntity> {
    public SensorRenderer(BlockEntityRendererProvider.Context context) {}

    @Override
    public void render(
            SensorBlockEntity sensor,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay) {
        Direction facing = sensor.getBlockState().getValue(SensorBlock.FACING);
        Glyph[] glyphs = SensorDisplayLayout.glyphs(
                sensor.kind(),
                sensor.mode(),
                sensor.hexadecimal(),
                sensor.displayedNumber());
        Function<ResourceLocation, TextureAtlasSprite> sprites =
                Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS);
        VertexConsumer vertices = buffers.getBuffer(RenderType.cutoutMipped());
        float[] normal = SensorCharacterCells.normal(facing);
        Matrix4f pose = poseStack.last().pose();
        for (int index = 0; index < glyphs.length; index++) {
            Glyph glyph = glyphs[index];
            if (glyph == null) {
                continue;
            }
            TextureAtlasSprite sprite = sprites.apply(
                    SensorDisplayLayout.sprite(glyph.icon()));
            quad(
                    vertices,
                    poseStack,
                    pose,
                    SensorCharacterCells.worldCorners(facing, index),
                    SensorCharacterCells.textureCoordinates(facing, index),
                    sprite,
                    glyph.argb(),
                    normal);
        }
    }

    private static void quad(
            VertexConsumer vertices,
            PoseStack poseStack,
            Matrix4f pose,
            float[][] corners,
            float[][] textureCoordinates,
            TextureAtlasSprite sprite,
            int argb,
            float[] normal) {
        int red = (argb >>> 16) & 0xFF;
        int green = (argb >>> 8) & 0xFF;
        int blue = argb & 0xFF;
        int alpha = (argb >>> 24) & 0xFF;
        for (int corner = 0; corner < corners.length; corner++) {
            float[] point = corners[corner];
            float[] textureCoordinate = textureCoordinates[corner];
            vertices.addVertex(pose, point[0], point[1], point[2])
                    .setColor(red, green, blue, alpha)
                    .setUv(
                            sprite.getU(textureCoordinate[0] / 16.0F),
                            sprite.getV(textureCoordinate[1] / 16.0F))
                    .setOverlay(OverlayTexture.NO_OVERLAY)
                    .setLight(LightTexture.FULL_BRIGHT)
                    .setNormal(poseStack.last(), normal[0], normal[1], normal[2]);
        }
    }
}
