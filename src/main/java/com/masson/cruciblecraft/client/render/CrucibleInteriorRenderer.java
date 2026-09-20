package com.masson.cruciblecraft.client.render;

import org.joml.Matrix4f;

import com.masson.cruciblecraft.CrucibleCraft;
import com.masson.cruciblecraft.machine.component.CrucibleInteriorGeometry;
import com.masson.cruciblecraft.machine.component.CrucibleProcessCore;
import com.masson.cruciblecraft.material.MaterialCatalog;
import com.masson.cruciblecraft.material.def.MaterialDefinition;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;

/**
 * GT6 pass-5 interior: lightest-by-density material, molten ROUGH texture or
 * gray {@code blockRaw}, height from {@link CrucibleInteriorGeometry}.
 */
final class CrucibleInteriorRenderer {
    static final ResourceLocation MOLTEN_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "block/gt6_import/materialicons/rough_molten");
    static final ResourceLocation SOLID_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(
                    CrucibleCraft.MODID, "block/gt6_import/materialicons/rough_block_raw");

    private CrucibleInteriorRenderer() {}

    static void renderSmall(
            CrucibleProcessCore process,
            float temperature,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        int height = CrucibleInteriorGeometry.displayedHeight(
                process.totalUnits(), process.maxUnits());
        if (height == 0) {
            return;
        }
        float y = CrucibleInteriorGeometry.smallSurfaceY(height);
        float inset = CrucibleInteriorGeometry.SMALL_WALL;
        renderLayer(
                process,
                temperature,
                poseStack,
                buffers,
                packedLight,
                inset,
                1.0f - inset,
                inset,
                1.0f - inset,
                y);
    }

    static void renderLarge(
            CrucibleProcessCore process,
            float temperature,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight) {
        int height = CrucibleInteriorGeometry.displayedHeight(
                process.totalUnits(), process.maxUnits());
        if (height == 0) {
            return;
        }
        float y = CrucibleInteriorGeometry.largeSurfaceY(height);
        renderLayer(
                process,
                temperature,
                poseStack,
                buffers,
                packedLight,
                -0.999f,
                1.999f,
                -0.999f,
                1.999f,
                y);
    }

    private static void renderLayer(
            CrucibleProcessCore process,
            float temperature,
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            float minX,
            float maxX,
            float minZ,
            float maxZ,
            float y) {
        var lightestId = process.contents().lightestId();
        if (lightestId.isEmpty() || !MaterialCatalog.contains(lightestId.orElseThrow())) {
            return;
        }
        MaterialDefinition lightest = MaterialCatalog.require(lightestId.orElseThrow());
        boolean molten = temperature >= lightest.thermal().meltingPoint();
        TextureAtlasSprite sprite = Minecraft.getInstance()
                .getTextureAtlas(TextureAtlas.LOCATION_BLOCKS)
                .apply(molten ? MOLTEN_TEXTURE : SOLID_TEXTURE);
        VertexConsumer vertices = buffers.getBuffer(
                RenderType.entityTranslucent(TextureAtlas.LOCATION_BLOCKS));
        int color = molten ? lightest.colorRgb() : CrucibleInteriorGeometry.GRAY_64;
        int light = molten ? LightTexture.FULL_BRIGHT : packedLight;
        poseStack.pushPose();
        Matrix4f pose = poseStack.last().pose();
        vertex(vertices, poseStack, pose, minX, y, minZ, sprite.getU0(), sprite.getV0(), color, light);
        vertex(vertices, poseStack, pose, minX, y, maxZ, sprite.getU0(), sprite.getV1(), color, light);
        vertex(vertices, poseStack, pose, maxX, y, maxZ, sprite.getU1(), sprite.getV1(), color, light);
        vertex(vertices, poseStack, pose, maxX, y, minZ, sprite.getU1(), sprite.getV0(), color, light);
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
